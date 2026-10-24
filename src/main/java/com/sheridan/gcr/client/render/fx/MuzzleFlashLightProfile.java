package com.sheridan.gcr.client.render.fx;

import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.impl.Muzzle;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.Locale;

/**
 * Weapon-only muzzle-light policy. No other dynamic lighting, weapon mechanics
 * or gunshot sounds are altered by this classifier.
 */
public enum MuzzleFlashLightProfile {
    OPEN(15, 1.0f),
    FLASH_HIDER(0, 0.0f),
    SUPPRESSOR(2, 0.045f);

    private final int blockLight;
    private final float screenFlashMultiplier;

    MuzzleFlashLightProfile(int blockLight, float screenFlashMultiplier) {
        this.blockLight = blockLight;
        this.screenFlashMultiplier = screenFlashMultiplier;
    }

    public int blockLight() {
        return blockLight;
    }

    public float screenFlashMultiplier() {
        return screenFlashMultiplier;
    }

    /**
     * Read equipped muzzle modules rather than sound type: a flash hider
     * normally has the same unsuppressed firing sound as an open barrel.
     */
    public static MuzzleFlashLightProfile forGun(IGun gun, ItemStack stack) {
        if (gun == null || stack == null || stack.isEmpty()) return OPEN;

        boolean flashHider = false;
        boolean suppressor = false;
        ListTag modules = gun.getModulesTag(stack);
        for (int i = 0; i < modules.size(); i++) {
            CompoundTag tag = modules.getCompound(i);
            Muzzle muzzle = ModuleRegister.get(tag.getString(Unit.MODULE_ID), Muzzle.class);
            if (muzzle == null) continue;

            if (muzzle.isSuppressor()) {
                suppressor = true;
            } else {
                String id = muzzle.getID().toLowerCase(Locale.ROOT);
                // Include the source mod's historical "flash_hinder" spelling.
                if (id.contains("flash_hider") || id.contains("flash_hinder")
                        || id.contains("flash_suppressor")) {
                    flashHider = true;
                }
            }
        }

        // A flash hider always disables muzzle illumination completely.
        if (flashHider) return FLASH_HIDER;
        if (suppressor) return SUPPRESSOR;
        return OPEN;
    }
}
