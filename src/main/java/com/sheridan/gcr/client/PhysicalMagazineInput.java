package com.sheridan.gcr.client;

import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.events.ControllerEvents;
import com.sheridan.gcr.client.screen.MagazineWheelScreen;
import com.sheridan.gcr.client.model.modular.animation.eventSys.EventType;
import com.sheridan.gcr.client.stuck.ClientGunStuckCache;
import com.sheridan.gcr.items.GunItem;
import com.sheridan.gcr.items.MagazineItem;
import com.sheridan.gcr.modularSys.builder.Builder;
import com.sheridan.gcr.modularSys.builder.Node;
import com.sheridan.gcr.modularSys.builder.WorkSpace;
import com.sheridan.gcr.modularSys.modules.Mag;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.guns.ISlottedGun;
import com.sheridan.gcr.modularSys.task.GunTaskHandler;
import com.sheridan.gcr.modularSys.task.IGunTask;
import com.sheridan.gcr.network.GCRNetwork;
import com.sheridan.gcr.network.c2s.PhysicalMagazinePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * R on guns: double-tap removes magazine, then double-tap again (with no
 * magazine) ejects the chambered round. A short hold + release quick-reloads;
 * a longer hold opens the radial selector, whose selection commits on R release.
 * A jammed gun takes precedence: a single R press starts full jam clearing.
 * The action continues after R is released.
 * R on a loose magazine: hold to load one cartridge every 24 ticks (1.2 seconds).
 * RMB on a loose magazine: hold to unload one cartridge every 20 ticks (1 second).
 */
public final class PhysicalMagazineInput {
    private static final int DOUBLE_TAP = 8;
    private static final int HOLD = 10;
    private static final int RADIAL_HOLD = 24;
    private static final int INSERT_ANIMATION_LEAD = 12;

    private static boolean wasDown;
    private static int heldTicks;
    private static int unloadHeldTicks;
    private static int tapWindow;
    private static boolean secondTap;
    private static boolean consumedHold;
    private static boolean clearingJam;
    private static int pendingAction = -1;
    private static int pendingDelay;
    private static String pendingGunId = "";
    private static String trackedGunId = "";
    private static int pendingSlot = -1;
    private static ItemStack pendingSnapshot = ItemStack.EMPTY;
    private static boolean suppressUntilRelease;

    private PhysicalMagazineInput() {}

    private static void stopJam() {
        if (!clearingJam) return;
        clearingJam = false;
        GCRNetwork.sendToServer(new PhysicalMagazinePacket(PhysicalMagazinePacket.STOP_UNJAM));
        if (GunTaskHandler.INSTANCE.hasTask()) GunTaskHandler.INSTANCE.cancelTask();
    }

    private static void reset() {
        stopJam();
        wasDown = false;
        heldTicks = 0;
        unloadHeldTicks = 0;
        tapWindow = 0;
        secondTap = false;
        consumedHold = false;
        pendingAction = -1;
        pendingGunId = "";
        pendingSlot = -1;
        pendingSnapshot = ItemStack.EMPTY;
        suppressUntilRelease = false;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof MagazineWheelScreen wheel) {
            // Screen owns the mouse and the R-release event while it is open.
            if (!mc.isWindowActive() || mc.player == null) wheel.onClose();
            return;
        }
        if (mc.player == null || mc.screen != null || !mc.isWindowActive()) {
            reset();
            return;
        }
        ItemStack stack = mc.player.getMainHandItem();
        boolean down = KeyBinds.RELOAD.isDown();
        if (stack.getItem() instanceof MagazineItem magazine) {
            stopJam();
            pendingAction = -1;
            tapWindow = 0;
            trackedGunId = "";
            // The vanilla use key is remappable; it is RMB by default.
            boolean unloadHeld = mc.options.keyUse.isDown();
            heldTicks = down ? heldTicks + 1 : 0;
            unloadHeldTicks = unloadHeld ? unloadHeldTicks + 1 : 0;
            // Never load and unload in the same tick when both buttons are held.
            if (unloadHeld && unloadHeldTicks >= PhysicalMagazinePacket.ROUND_UNLOAD_TICKS
                    && unloadHeldTicks % PhysicalMagazinePacket.ROUND_UNLOAD_TICKS == 0
                    && magazine.getRounds(stack) > 0) {
                GCRNetwork.sendToServer(new PhysicalMagazinePacket(PhysicalMagazinePacket.UNLOAD_ROUND));
            } else if (!unloadHeld && down && heldTicks >= PhysicalMagazinePacket.ROUND_LOAD_TICKS
                    && heldTicks % PhysicalMagazinePacket.ROUND_LOAD_TICKS == 0
                    && magazine.getRounds(stack) < magazine.capacity()) {
                GCRNetwork.sendToServer(new PhysicalMagazinePacket(PhysicalMagazinePacket.LOAD_ROUND));
            }
            wasDown = down;
            return;
        }
        unloadHeldTicks = 0;
        if (!(stack.getItem() instanceof GunItem weapon)) {
            reset();
            trackedGunId = "";
            return;
        }
        IGun gun = weapon.getGun();
        String identity = gun.getIdentityID(stack);
        if (!Objects.equals(identity, trackedGunId)) {
            reset();
            trackedGunId = identity;
        }
        if (pendingAction != -1) {
            if (!Objects.equals(identity, pendingGunId)) {
                pendingAction = -1;
            } else if (--pendingDelay <= 0) {
                if (pendingAction == PhysicalMagazinePacket.SELECTED_INSERT) {
                    GCRNetwork.sendToServer(new PhysicalMagazinePacket(
                            pendingAction, pendingSlot, pendingGunId, pendingSnapshot));
                } else {
                    GCRNetwork.sendToServer(new PhysicalMagazinePacket(pendingAction));
                }
                pendingAction = -1;
                pendingSlot = -1;
                pendingSnapshot = ItemStack.EMPTY;
            }
        }

        // GUI key-release can arrive before the KeyMapping updates: never treat
        // the residual held key as a new tap or a second reload.
        if (suppressUntilRelease) {
            if (!down) {
                suppressUntilRelease = false;
                wasDown = false;
            }
            return;
        }
        // The task owns the animation independently of the key-up event.
        if (clearingJam && !GunTaskHandler.INSTANCE.hasTask()) {
            clearingJam = false;
        }
        if (tapWindow > 0) tapWindow--;
        if (down && !wasDown) {
            secondTap = tapWindow > 0;
            heldTicks = 0;
            consumedHold = false;
            if (ClientGunStuckCache.get().isStuck(stack, gun)) {
                // Jammed R takes precedence over all reload gestures, immediately.
                consumedHold = true;
                secondTap = false;
                tapWindow = 0;
                if (!clearingJam && !GunTaskHandler.INSTANCE.hasTask()) {
                    ClientGunStuckCache.get().projectToNbt(stack, gun);
                    IGunTask<?> task = gun.getTask(stack, IGunTask.TaskType.REMOVE_STUCK, Map.of());
                    if (task != null) {
                        clearingJam = true;
                        GCRNetwork.sendToServer(new PhysicalMagazinePacket(PhysicalMagazinePacket.START_UNJAM));
                        GunTaskHandler.INSTANCE.setTask(task);
                    }
                }
            }
        }
        if (down) {
            heldTicks++;
            if (!clearingJam && !consumedHold && heldTicks >= RADIAL_HOLD && pendingAction == -1) {
                // No reload is executed at the hold threshold. Releasing R over
                // a wheel entry will commit one specifically chosen magazine.
                List<MagazineChoice> choices = availableMagazines(gun, stack);
                consumedHold = true;
                secondTap = false;
                tapWindow = 0;
                if (!choices.isEmpty()) {
                    String preferred = stack.hasTag()
                            ? stack.getTag().getString(PhysicalMagazinePacket.PREFERRED_MAG_KEY) : "";
                    Client.RIGHT_BUTTON_PRESSED.set(false);
                    Client.LEFT_BUTTON_PRESSED.set(false);
                    mc.setScreen(new MagazineWheelScreen(choices, identity, preferred));
                    wasDown = true;
                    return;
                }
            }
        }

        if (!down && wasDown) {
            // A normal key release must never cancel the unjam animation.
            if (!clearingJam && !consumedHold) {
                if (secondTap && heldTicks < HOLD) {
                    tapWindow = 0;
                    beginMagazine(gun, stack, PhysicalMagazinePacket.REMOVE);
                } else if (heldTicks >= HOLD) {
                    tapWindow = 0;
                    beginMagazine(gun, stack, PhysicalMagazinePacket.INSERT);
                } else {
                    tapWindow = DOUBLE_TAP;
                }
            }
            secondTap = false;
        }
        wasDown = down;
    }

    private static void beginMagazine(IGun gun, ItemStack stack, int action) {
        beginMagazine(gun, stack, action, null);
    }

    private static void beginMagazine(IGun gun, ItemStack stack, int action, MagazineChoice selected) {
        if (Client.WEAPON_STATUS == null || !Client.WEAPON_STATUS.isHoldingGun()) return;
        // Do not queue another command while an earlier reload animation is still
        // waiting to apply its server-authoritative magazine/chamber change.
        if (pendingAction != -1) return;
        String identity = gun.getIdentityID(stack);
        if (identity == null || IGun.NONE.equals(identity)) return;

        if (action == PhysicalMagazinePacket.INSERT && availableMagazines(gun, stack).isEmpty()) return;
        if (action == PhysicalMagazinePacket.SELECTED_INSERT && selected == null) return;

        // REMOVE means "unload one stage": magazine first, then the chamber on
        // the next double-R. Server repeats the check to prevent duplication.
        boolean ejectChamber = false;
        if (action == PhysicalMagazinePacket.REMOVE && gun instanceof ISlottedGun slotted) {
            ejectChamber = slotted.getMagAttachment(stack) == null;
            if (ejectChamber && slotted.getGunAmmoLeft(stack) <= 0) return;
        }
        // A dedicated eject_round clip does not exist yet. All five guns
        // already register check_chamber, which is a safe temporary bolt-action
        // animation; no missing-animation key is ever dispatched.
        boolean replacing = gun instanceof ISlottedGun slotted && slotted.getMagAttachment(stack) != null;
        String animation = action == PhysicalMagazinePacket.REMOVE
                ? (ejectChamber ? "check_chamber" : "mag_reload")
                : (replacing ? "mag_reload" : "mag_reload_empty");
        Client.getGunRenderer().dispatchAnimationEvent(
                EventType.RELOAD, Map.of("animation_name", animation));
        pendingGunId = identity;
        pendingAction = action;
        pendingDelay = INSERT_ANIMATION_LEAD;
        pendingSlot = selected == null ? -1 : selected.slot();
        pendingSnapshot = selected == null ? ItemStack.EMPTY : selected.snapshot().copy();
    }

    /** A wheel entry refers to one exact vanilla inventory slot and NBT snapshot. */
    public record MagazineChoice(int slot, ItemStack snapshot, String magazineType, int rounds, int capacity) {}

    public static List<MagazineChoice> availableMagazines(IGun gun, ItemStack gunStack) {
        ArrayList<MagazineChoice> result = new ArrayList<>();
        if (!(gun instanceof ISlottedGun)) return result;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return result;
        try {
            Builder builder = new Builder();
            builder.initAndClear(gun.getModulesTag(gunStack).copy());
            var root = ((WorkSpace) builder.getWorkspace()).getRoot();
            if (root == null) return result;
            var magSlot = root.getSlotByName("MAG");
            if (magSlot == null) return result;
            List<ItemStack> inventory = mc.player.getInventory().items;
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack candidate = inventory.get(slot);
                if (!(candidate.getItem() instanceof MagazineItem magazine)) continue;
                Mag mag = magazine.getModule();
                if (!magSlot.accepts(mag)) continue;
                int rounds = magazine.getRounds(candidate);
                if (rounds <= 0) continue;
                result.add(new MagazineChoice(slot, candidate.copy(), mag.getID(), rounds, magazine.capacity()));
            }
        } catch (Exception ignored) {
            // On partial equipment synchronization, do not make an unsafe guess.
        }
        return result;
    }

    /** Called only by the radial GUI on R-release (or cancellation). */
    public static void onWheelClosed(String gunIdentity, MagazineChoice choice) {
        // Vanilla may not propagate key-up events to KeyMapping while a Screen
        // captures the keyboard. Explicitly release R to avoid a stuck hold.
        net.minecraft.client.KeyMapping.set(KeyBinds.RELOAD.getKey(), false);
        tapWindow = 0;
        secondTap = false;
        consumedHold = true;
        heldTicks = 0;
        wasDown = false;
        suppressUntilRelease = true;
        if (choice == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !(mc.player.getMainHandItem().getItem() instanceof GunItem weapon)) return;
        ItemStack gunStack = mc.player.getMainHandItem();
        IGun gun = weapon.getGun();
        if (!Objects.equals(gun.getIdentityID(gunStack), gunIdentity)) return;
        beginMagazine(gun, gunStack, PhysicalMagazinePacket.SELECTED_INSERT, choice);
    }
}
