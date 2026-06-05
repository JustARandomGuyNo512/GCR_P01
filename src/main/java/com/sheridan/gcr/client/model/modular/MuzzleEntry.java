package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.client.render.fx.muzzleFlash.MuzzleFlash;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MuzzleEntry {
    private final String name;
    private final String boneName;
    private final String bindSlotName;
    private final float scale;
    private final MuzzleFlash muzzleFlash;
    public boolean enabled;

    public MuzzleEntry(String name, String boneName, @Nullable String bindSlotName, float scale, MuzzleFlash muzzleFlash) {
        this.name = name;
        this.boneName = boneName;
        this.bindSlotName = bindSlotName;
        this.scale = scale;
        this.muzzleFlash = muzzleFlash;
        this.enabled = true;
    }

    public String getName() {
        return name;
    }

    public String getBoneName() {
        return boneName;
    }

    @Nullable
    public String getBindSlotName() {
        return bindSlotName;
    }

    public float getScale() {
        return scale;
    }

    public MuzzleFlash getMuzzleFlash() {
        return muzzleFlash;
    }
}

