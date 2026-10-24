package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.GCRModules;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.items.MagazineItem;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.builder.Builder;
import com.sheridan.gcr.modularSys.builder.IReadOnlyTree;
import com.sheridan.gcr.modularSys.builder.IWorkSpace;
import com.sheridan.gcr.modularSys.builder.Node;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.builder.ValidateResult;
import com.sheridan.gcr.modularSys.builder.WorkSpace;
import com.sheridan.gcr.modularSys.modules.Mag;
import com.sheridan.gcr.modularSys.modules.StatesUpdateContext;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.guns.ISlottedGun;
import com.sheridan.gcr.modularSys.slot.OperationType;
import com.sheridan.gcr.network.IPacket;
import com.sheridan.gcr.network.PacketContext;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Physical magazine action. Both inventory changes and NBT module-tree changes
 * happen atomically on the server thread, never according to a client-supplied tree.
 */
public final class PhysicalMagazinePacket implements IPacket<PhysicalMagazinePacket> {
    public static final ResourceLocation ID = GCR.RL("physical_magazine");
    private static final String MAG_AMMO_KEY = MagazineItem.ROUNDS_TAG;
    private static final String ATTACHED_AMMO_TYPES_KEY = "aftermatch_attached_mag_round_types";
    private static final String LAST_ACTION_KEY = "gcr_aftermatch_mag_action_tick";
    public static final String PREFERRED_MAG_KEY = "aftermatch_preferred_mag_type";
    public static final int REMOVE = 0;
    public static final int INSERT = 1;
    public static final int LOAD_ROUND = 2;
    public static final int START_UNJAM = 3;
    public static final int STOP_UNJAM = 4;
    public static final int UNLOAD_ROUND = 5;
    public static final int ROUND_LOAD_TICKS = 24;
    public static final int ROUND_UNLOAD_TICKS = 20;
    /** Manually chosen inventory slot, validated against an exact ItemStack snapshot. */
    public static final int SELECTED_INSERT = 6;
    public static final String JAM_START_KEY = "gcr_aftermatch_jam_start";
    public static final String JAM_GUN_KEY = "gcr_aftermatch_jam_identity";
    private static final String LAST_LOAD_KEY = "gcr_aftermatch_last_load";

    private final int action;
    private final int selectedSlot;
    private final String gunIdentity;
    private final ItemStack selectedSnapshot;

    public PhysicalMagazinePacket(int action) {
        this(action, -1, "", ItemStack.EMPTY);
    }

    public PhysicalMagazinePacket(int action, int selectedSlot, String gunIdentity, ItemStack selectedSnapshot) {
        this.action = action;
        this.selectedSlot = selectedSlot;
        this.gunIdentity = gunIdentity;
        this.selectedSnapshot = selectedSnapshot.copy();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(action);
        buf.writeVarInt(selectedSlot);
        buf.writeUtf(gunIdentity, 256);
        buf.writeItem(selectedSnapshot);
    }

    public static PhysicalMagazinePacket decode(FriendlyByteBuf buf) {
        return new PhysicalMagazinePacket(buf.readVarInt(), buf.readVarInt(), buf.readUtf(256), buf.readItem());
    }
    @Override public void onClient(PhysicalMagazinePacket msg, PacketContext ctx) { }

    @Override
    public void onServer(PhysicalMagazinePacket msg, PacketContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (msg.action == LOAD_ROUND || msg.action == UNLOAD_ROUND) {
                ItemStack heldMagazine = player.getMainHandItem();
                if (!(heldMagazine.getItem() instanceof MagazineItem magazine)) return;
                long time = player.level().getGameTime();
                long last = player.getPersistentData().getLong(LAST_LOAD_KEY);
                int cooldown = msg.action == LOAD_ROUND ? ROUND_LOAD_TICKS : ROUND_UNLOAD_TICKS;
                if (last > 0 && time >= last && time - last < cooldown) return;
                if (msg.action == UNLOAD_ROUND) {
                    ItemStack round = magazine.unloadOne(heldMagazine);
                    if (round.isEmpty()) return;
                    if (!player.getInventory().add(round)) {
                        player.drop(round, false);
                    }
                    player.getPersistentData().putLong(LAST_LOAD_KEY, time);
                    player.getInventory().setChanged();
                    player.displayClientMessage(Component.translatable("tooltip.gcr.magazine_rounds",
                            magazine.getRounds(heldMagazine), magazine.capacity()), true);
                    return;
                }
                int amount = magazine.getRounds(heldMagazine);
                if (amount >= magazine.capacity()) return;
                ItemStack rounds = ItemStack.EMPTY;
                Mag mag = magazine.getModule();
                for (ItemStack item : player.getInventory().items) {
                    if (!item.isEmpty() && isCompatibleCartridge(item, mag)) {
                        rounds = item;
                        break;
                    }
                }
                if (rounds.isEmpty()) return;
                if (magazine.addOne(heldMagazine, rounds.getItem())) {
                    if (!player.isCreative()) rounds.shrink(1);
                    player.getPersistentData().putLong(LAST_LOAD_KEY, time);
                    player.getInventory().setChanged();
                    player.displayClientMessage(Component.translatable("tooltip.gcr.magazine_rounds",
                            magazine.getRounds(heldMagazine), magazine.capacity()), true);
                }
                return;
            }
            if (msg.action == STOP_UNJAM) {
                player.getPersistentData().remove(JAM_START_KEY);
                player.getPersistentData().remove(JAM_GUN_KEY);
                return;
            }
            if (msg.action == START_UNJAM) {
                ItemStack gunStack = player.getMainHandItem();
                if (gunStack.getItem() instanceof GunItem weapon) {
                    IGun active = weapon.getGun();
                    if (active.isStuck(gunStack)) {
                        player.getPersistentData().putLong(JAM_START_KEY, player.level().getGameTime());
                        player.getPersistentData().putString(JAM_GUN_KEY, active.getIdentityID(gunStack));
                    }
                }
                return;
            }
            if (msg.action != REMOVE && msg.action != INSERT && msg.action != SELECTED_INSERT) return;
            ItemStack held = player.getMainHandItem();
            if (!(held.getItem() instanceof GunItem gunItem)) return;
            long now = player.level().getGameTime();
            long previous = player.getPersistentData().getLong(LAST_ACTION_KEY);
            if (previous > 0L && now >= previous && now - previous < 8L) return;

            IGun gun = gunItem.getGun();
            if (msg.action == SELECTED_INSERT && !gun.getIdentityID(held).equals(msg.gunIdentity)) return;
            ListTag modules = gun.getModulesTag(held).copy();
            if (modules.isEmpty()) return;
            Builder builder = new Builder();
            builder.initAndClear(modules);
            IWorkSpace workspace = builder.getWorkspace();
            Unit root = workspace.getRootUnit();
            if (root == null || root.getModule() != gun) return;
            Node rootNode = ((WorkSpace) workspace).getRoot();
            var magSlot = rootNode.getSlotByName("MAG");
            if (magSlot == null) return;

            Unit installedUnit = null;
            Node installedNode = null;
            for (Unit unit : workspace.getSequencedUnits()) {
                Node node = ((WorkSpace) workspace).getNode(unit);
                if (node != null && node.getParent() == rootNode && "MAG".equals(node.getBelongsToSlotName())
                        && unit.getModule() instanceof Mag) {
                    installedUnit = unit;
                    installedNode = node;
                    break;
                }
            }

            ItemStack removedStack = ItemStack.EMPTY;
            ItemStack consumedStack = ItemStack.EMPTY;
            Mag insertedMag = null;
            int insertedAmmo = 0;
            if (msg.action == REMOVE) {
                if (installedUnit == null || installedNode == null) {
                    // The magazine is already gone. A SECOND double-R ejects the
                    // independent chambered round without modifying module slots.
                    // Both the chamber check and cartridge transfer run on the
                    // server. Return the round to inventory, like unloading a
                    // physical magazine; drop only the remaining item if full.
                    if (!(gun instanceof ISlottedGun slotted) || slotted.getGunAmmoLeft(held) <= 0) return;
                    ItemStack ejected = chamberedCartridgeFor(gun);
                    if (ejected.isEmpty()) return;
                    slotted.setGunAmmoLeft(held, 0);
                    gun.notifyDataChanged(held);
                    player.getPersistentData().putLong(LAST_ACTION_KEY, now);
                    if (!player.getInventory().add(ejected) && !ejected.isEmpty()) {
                        player.drop(ejected, false);
                    }
                    player.getInventory().setChanged();
                    return;
                }
                if (!magSlot.allow(OperationType.REMOVE)) return;
                Mag oldMag = (Mag) installedUnit.getModule();
                if (!(oldMag.getBindItem() instanceof MagazineItem oldItem)) return;
                int oldAmmo = oldMag.getAmmoLeft(gun.getNodeStatesTag(held, installedNode.getID()));
                removedStack = new ItemStack(oldMag.getBindItem());
                oldItem.setRounds(removedStack, oldAmmo);
                // Preserve cartridge identity stored when this magazine was inserted.
                // Trim it to the actual number of rounds left after gunfire.
                CompoundTag attachedTypes = held.getOrCreateTag().getCompound(ATTACHED_AMMO_TYPES_KEY);
                oldItem.setRoundTypes(removedStack,
                        MagazineItem.trimRoundTypes(attachedTypes, oldAmmo));
                if (!workspace.removeChild(installedUnit)) return;
            } else {
                if (!magSlot.allow(OperationType.ADD)) return;
                if (msg.action == SELECTED_INSERT) {
                    // The selected inventory stack must still be the *same*
                    // physical magazine that was highlighted in the wheel.
                    if (msg.selectedSlot < 0 || msg.selectedSlot >= player.getInventory().items.size()) return;
                    ItemStack candidate = player.getInventory().items.get(msg.selectedSlot);
                    if (candidate.isEmpty() || msg.selectedSnapshot.isEmpty()
                            || !ItemStack.matches(candidate, msg.selectedSnapshot)
                            || !(candidate.getItem() instanceof MagazineItem item)
                            || !magSlot.accepts(item.getModule())
                            || item.getRounds(candidate) <= 0) return;
                    consumedStack = candidate;
                    insertedMag = item.getModule();
                    insertedAmmo = item.getRounds(candidate);
                } else {
                    // A quick reload favors the last *manually* chosen type.
                    // Fallback must NOT overwrite that preference.
                    String preferred = held.hasTag()
                            ? held.getTag().getString(PREFERRED_MAG_KEY) : "";
                    ItemStack firstLoaded = ItemStack.EMPTY;
                    Mag firstLoadedMag = null;
                    int firstLoadedRounds = 0;
                    for (ItemStack candidate : player.getInventory().items) {
                        if (candidate.isEmpty() || !(candidate.getItem() instanceof MagazineItem item)) continue;
                        Mag mag = item.getModule();
                        if (!magSlot.accepts(mag)) continue;
                        int rounds = item.getRounds(candidate);
                        if (rounds <= 0) continue;
                        if (firstLoaded.isEmpty()) {
                            firstLoaded = candidate;
                            firstLoadedMag = mag;
                            firstLoadedRounds = rounds;
                        }
                        if (mag.getID().equals(preferred)) {
                            consumedStack = candidate;
                            insertedMag = mag;
                            insertedAmmo = rounds;
                            break;
                        }
                    }
                    if (insertedMag == null) {
                        consumedStack = firstLoaded;
                        insertedMag = firstLoadedMag;
                        insertedAmmo = firstLoadedRounds;
                    }
                }
                // Empty magazines are excluded from both selection modes.
                if (insertedMag == null || consumedStack.isEmpty()) return;

                // Replacing an installed magazine is a single atomic server
                // operation; the independent chambered round remains intact.
                if (installedUnit != null) {
                    if (installedNode == null || !magSlot.allow(OperationType.REMOVE)) return;
                    Mag oldMag = (Mag) installedUnit.getModule();
                    if (!(oldMag.getBindItem() instanceof MagazineItem oldItem)) return;
                    int oldAmmo = oldMag.getAmmoLeft(gun.getNodeStatesTag(held, installedNode.getID()));
                    removedStack = new ItemStack(oldMag.getBindItem());
                    oldItem.setRounds(removedStack, oldAmmo);
                    oldItem.setRoundTypes(removedStack, MagazineItem.trimRoundTypes(
                            held.getOrCreateTag().getCompound(ATTACHED_AMMO_TYPES_KEY), oldAmmo));
                    if (!workspace.removeChild(installedUnit)) return;
                }
                if (workspace.addChild(root, "MAG", insertedMag.getID()).isEmpty()) return;
            }

            try {
                for (ValidateResult result : builder.commit()) {
                    if (!result.isCommitAllowed()) return;
                }
                IReadOnlyTree warehouse = builder.getWarehouse();
                ListTag updatedModules = warehouse.write();
                CompoundTag states = gun.getStatesTag(held).copy();
                new StatesUpdateContext(gun, warehouse.getShadowTree(), states).autoExec();

                if (insertedMag != null) {
                    String rootId = gun.rootNodeId(held);
                    for (int i = 0; i < updatedModules.size(); i++) {
                        CompoundTag node = updatedModules.getCompound(i);
                        if ("MAG".equals(node.getString(Unit.SLOT_NAME))
                                && rootId.equals(node.getString(Unit.PARENT_ID))
                                && insertedMag.getID().equals(node.getString(Unit.MODULE_ID))) {
                            insertedMag.setAmmoLeft(insertedAmmo, states.getCompound(node.getString(Unit.IN_TIME_ID)));
                            break;
                        }
                    }
                }
                CompoundTag updatedProps = gun.reCalculateProperties(warehouse);
                gun.setModulesTag(held, updatedModules);
                gun.setStatesTag(held, states);
                gun.setPropertiesTag(held, updatedProps);
                // An inserted loaded magazine also chambers its first round
                // if the firearm was empty, consuming exactly one round from
                // the magazine. An empty magazine must stay empty.
                if (insertedMag != null && insertedAmmo > 0
                        && gun instanceof com.sheridan.gcr.modularSys.modules.guns.ISlottedGun slotted
                        && slotted.getGunAmmoLeft(held) == 0) {
                    slotted.setGunAmmoLeft(held, 1);
                    slotted.setMagAmmoLeft(held, insertedAmmo - 1);
                }
                // The weapon's magazine tree stores the number of rounds but
                // not their item types. Keep the item type ledger on the gun.
                if (msg.action == INSERT || msg.action == SELECTED_INSERT) {
                    MagazineItem selected = (MagazineItem) consumedStack.getItem();
                    int remaining = insertedAmmo;
                    if (gun instanceof com.sheridan.gcr.modularSys.modules.guns.ISlottedGun slotted) {
                        remaining = slotted.getMagAmmoLeft(held);
                    }
                    held.getOrCreateTag().put(ATTACHED_AMMO_TYPES_KEY,
                            MagazineItem.trimRoundTypes(selected.getRoundTypes(consumedStack), remaining));
                } else {
                    held.getOrCreateTag().remove(ATTACHED_AMMO_TYPES_KEY);
                }
                if (msg.action == SELECTED_INSERT) {
                    // Only an explicit wheel choice updates the preferred type.
                    held.getOrCreateTag().putString(PREFERRED_MAG_KEY, insertedMag.getID());
                }
                gun.notifyDataChanged(held);
                player.getPersistentData().putLong(LAST_ACTION_KEY, now);
                if (msg.action == REMOVE) {
                    if (!player.getInventory().add(removedStack)) {
                        player.drop(removedStack, false);
                    }
                } else {
                    if (!player.isCreative()) consumedStack.shrink(1);
                    if (!removedStack.isEmpty() && !player.getInventory().add(removedStack)) {
                        player.drop(removedStack, false);
                    }
                }
                player.getInventory().setChanged();
            } catch (Exception e) {
                GCR.LOGGER.error("Physical magazine action failed", e);
            }
        });
    }

    /**
     * A loose round must have the right caliber, including when no magazine
     * remains to identify it. The five supported firearms have fixed calibers.
     * Do not clear a chamber if its cartridge item cannot be resolved.
     */
    private static ItemStack chamberedCartridgeFor(IGun gun) {
        if (gun == GCRModules.M4A1 || gun == GCRModules.AK24) {
            return new ItemStack(GCR.AMMO_556X45_ITEM.get());
        }
        if (gun == GCRModules.AK74M) {
            return new ItemStack(GCR.AMMO_545X39_ITEM.get());
        }
        if (gun == GCRModules.MCX_SPEAR) {
            return new ItemStack(GCR.AMMO_68X51_ITEM.get());
        }
        if (gun == GCRModules.AKMN) {
            return new ItemStack(GCR.AMMO_762X39_ITEM.get());
        }
        return ItemStack.EMPTY;
    }

    /**
     * Magazines accept ammunition based on caliber, not weapon name.
     * M4A1 and AK-24 share the same 5.56x45mm round and magazines.
     * The legacy items are accepted so existing inventory stacks are not
     * lost; magazine NBT always writes the new caliber-based ID.
     */
    private static boolean isCompatibleCartridge(ItemStack cartridge, Mag mag) {
        if (mag.hasTag("5.56x45")) {
            return cartridge.is(GCR.AMMO_556X45_ITEM.get())
                    || cartridge.is(GCR.M4A1_CARTRIDGE_ITEM.get())
                    || cartridge.is(GCR.AK24_CARTRIDGE_ITEM.get());
        }
        if (mag.hasTag("5.45x39")) {
            return cartridge.is(GCR.AMMO_545X39_ITEM.get())
                    || cartridge.is(GCR.AK74M_CARTRIDGE_ITEM.get());
        }
        if (mag.hasTag("6.8x51")) {
            return cartridge.is(GCR.AMMO_68X51_ITEM.get())
                    || cartridge.is(GCR.MCX_SPEAR_CARTRIDGE_ITEM.get());
        }
        if (mag.hasTag("7.62x39")) {
            return cartridge.is(GCR.AMMO_762X39_ITEM.get())
                    || cartridge.is(GCR.AKMN_CARTRIDGE_ITEM.get());
        }
        return false;
    }

}
