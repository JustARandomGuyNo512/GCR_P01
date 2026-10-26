package com.sheridan.gcr.items;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.modularSys.modules.Mag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.Collections;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import java.util.List;

/**
 * A physical magazine inventory item. The backing Mag module is retained
 * ONLY as GCR's internal attachment/render and ammo-source adapter.
 * Players load this item with real cartridges rather than configuring it
 * through the attachment UI. Each item owns its persistent round count.
 */
public final class MagazineItem extends ModuleItem<Mag> {
    public static final String ROUNDS_TAG = "aftermatch_mag_ammo";
    // Tracks round item IDs; old weapon-specific keys are normalized to
    // canonical caliber keys so shared magazines use just one ammo type.
    public static final String ROUND_TYPES_TAG = "aftermatch_mag_round_types";
    private final Mag magazine;

    public MagazineItem(Mag mag) {
        super(mag);
        this.magazine = mag;
    }

    public int getRounds(ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        return nbt != null && nbt.contains(ROUNDS_TAG, 3)
                ? Mth.clamp(nbt.getInt(ROUNDS_TAG), 0, magazine.getMaxCapacity()) : 0;
    }

    public int capacity() { return magazine.getMaxCapacity(); }
    public boolean addOne(ItemStack stack) {
        int count = getRounds(stack);
        if (count >= capacity()) return false;
        stack.getOrCreateTag().putInt(ROUNDS_TAG, count + 1);
        return true;
    }

    public void setRounds(ItemStack stack, int rounds) {
        stack.getOrCreateTag().putInt(ROUNDS_TAG, Mth.clamp(rounds, 0, capacity()));
    }


    /**
     * Legacy names survive in saved ItemStacks and magazine NBT.
     * Normalize every old round ID to its physical caliber.
     */
    public static String canonicalAmmoId(String id) {
        return switch (id) {
            case "gcr:m4a1_cartridge", "gcr:ak24_cartridge" -> "gcr:ammo_5_56x45";
            case "gcr:ak74m_cartridge" -> "gcr:ammo_5_45x39";
            case "gcr:mcx_spear_cartridge" -> "gcr:ammo_6_8x51";
            case "gcr:akmn_cartridge" -> "gcr:ammo_7_62x39";
            default -> id;
        };
    }

    public static CompoundTag normalizeRoundTypes(CompoundTag source) {
        CompoundTag normalized = new CompoundTag();
        for (String id : source.getAllKeys()) {
            int count = Math.max(0, source.getInt(id));
            if (count > 0) {
                String key = canonicalAmmoId(id);
                normalized.putInt(key, normalized.getInt(key) + count);
            }
        }
        return normalized;
    }

    /** Magazine contents are persisted under canonical caliber IDs. */
    public CompoundTag getRoundTypes(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag()
                : normalizeRoundTypes(tag.getCompound(ROUND_TYPES_TAG));
    }

    public void setRoundTypes(ItemStack stack, CompoundTag types) {
        stack.getOrCreateTag().put(ROUND_TYPES_TAG, normalizeRoundTypes(types));
    }

    /** Extra ledger entries are discarded after rounds have been fired in a gun. */
    public static CompoundTag trimRoundTypes(CompoundTag source, int remainingRounds) {
        // Merge old M4A1/AK-24 ammo ledgers into the same 5.56 caliber first.
        source = normalizeRoundTypes(source);
        CompoundTag result = new CompoundTag();
        int remaining = Math.max(0, remainingRounds);
        java.util.List<String> keys = new ArrayList<>(source.getAllKeys());
        Collections.sort(keys);
        for (String id : keys) {
            if (remaining <= 0) break;
            int count = Math.min(Math.max(0, source.getInt(id)), remaining);
            if (count > 0) {
                result.putInt(id, count);
                remaining -= count;
            }
        }
        return result;
    }

    /** Loads a real, type-identified cartridge into this physical magazine. */
    public boolean addOne(ItemStack stack, Item cartridge) {
        if (!addOne(stack)) return false;
        net.minecraft.resources.ResourceLocation id = ForgeRegistries.ITEMS.getKey(cartridge);
        if (id != null) {
            CompoundTag types = getRoundTypes(stack);
            String key = canonicalAmmoId(id.toString());
            types.putInt(key, types.getInt(key) + 1);
            setRoundTypes(stack, types);
        }
        return true;
    }

    /** Removes a single physical round, returning it as an inventory item. */
    public ItemStack unloadOne(ItemStack stack) {
        int count = getRounds(stack);
        if (count <= 0) return ItemStack.EMPTY;

        CompoundTag types = trimRoundTypes(getRoundTypes(stack), count);
        Item cartridge = Items.AIR;
        String used = null;
        java.util.List<String> keys = new ArrayList<>(types.getAllKeys());
        Collections.sort(keys);
        for (String id : keys) {
            if (types.getInt(id) <= 0) continue;
            Item candidate = resolveCartridge(id);
            if (candidate != Items.AIR && isCompatible(candidate)) {
                cartridge = candidate;
                used = id;
                break;
            }
        }
        if (cartridge == Items.AIR) cartridge = fallbackCartridge();
        if (cartridge == Items.AIR) return ItemStack.EMPTY;

        setRounds(stack, count - 1);
        if (used != null) {
            int next = types.getInt(used) - 1;
            if (next > 0) types.putInt(used, next);
            else types.remove(used);
        }
        setRoundTypes(stack, trimRoundTypes(types, count - 1));
        return new ItemStack(cartridge);
    }

    private Item resolveCartridge(String id) {
        return switch (canonicalAmmoId(id)) {
            case "gcr:ammo_5_56x45" -> GCR.AMMO_556X45_ITEM.get();
            case "gcr:ammo_5_45x39" -> GCR.AMMO_545X39_ITEM.get();
            case "gcr:ammo_6_8x51" -> GCR.AMMO_68X51_ITEM.get();
            case "gcr:ammo_7_62x39" -> GCR.AMMO_762X39_ITEM.get();
            default -> Items.AIR;
        };
    }

    private boolean isCompatible(Item item) {
        if (magazine.hasTag("5.56x45")) return item == GCR.AMMO_556X45_ITEM.get();
        if (magazine.hasTag("5.45x39")) return item == GCR.AMMO_545X39_ITEM.get();
        if (magazine.hasTag("6.8x51")) return item == GCR.AMMO_68X51_ITEM.get();
        if (magazine.hasTag("7.62x39")) return item == GCR.AMMO_762X39_ITEM.get();
        return false;
    }

    private Item fallbackCartridge() {
        if (magazine.hasTag("5.56x45")) return GCR.AMMO_556X45_ITEM.get();
        if (magazine.hasTag("5.45x39")) return GCR.AMMO_545X39_ITEM.get();
        if (magazine.hasTag("6.8x51")) return GCR.AMMO_68X51_ITEM.get();
        if (magazine.hasTag("7.62x39")) return GCR.AMMO_762X39_ITEM.get();
        return Items.AIR;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack stack) {
        // Keep the original localized magazine name. Only magazines with at
        // least one round gain (+); empty magazines have no status suffix.
        Component name = super.getName(stack);
        return getRounds(stack) > 0
                ? Component.empty().append(name).append(" (+)")
                : name;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Level level, @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        // Always show the actual rounds saved on this inventory stack.
        // Color distinguishes empty, partially filled and fully loaded mags.
        int rounds = getRounds(stack);
        ChatFormatting color = rounds == 0 ? ChatFormatting.RED
                : rounds >= capacity() ? ChatFormatting.GREEN : ChatFormatting.YELLOW;
        tooltip.add(Component.translatable("tooltip.gcr.magazine_rounds", rounds, capacity())
                .withStyle(color));
        // Render each compatible firearm on its OWN line, instead of
        // a comma-separated inline string that clips long translated names.
        List<String> guns = new ArrayList<>();
        if (magazine.hasTag("mag") && magazine.hasTag("ar") && magazine.hasTag("5.56x45")) {
            guns.add("gcr:m4a1");
            guns.add("gcr:ak24");
        } else if (magazine.hasTag("mag") && magazine.hasTag("ak") && magazine.hasTag("5.45x39")) {
            guns.add("gcr:ak74m");
        } else if (magazine.hasTag("mag") && magazine.hasTag("ak") && magazine.hasTag("7.62x39")) {
            guns.add("gcr:akmn");
        } else if (magazine.hasTag("mag") && magazine.hasTag("mcx_spear") && magazine.hasTag("6.8x51")) {
            guns.add("gcr:mcx_spear");
        }
        if (!guns.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.gcr.compatible_weapons")
                    .withStyle(ChatFormatting.GRAY));
            for (String gunId : guns)
                tooltip.add(Component.literal("  • ").withStyle(ChatFormatting.DARK_GRAY)
                        .append(Component.translatable(gunId).withStyle(ChatFormatting.GRAY)));
        }
        tooltip.add(Component.translatable("tooltip.gcr.magazine_hold_load")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.gcr.magazine_hold_unload")
                .withStyle(ChatFormatting.GRAY));
    }
}
