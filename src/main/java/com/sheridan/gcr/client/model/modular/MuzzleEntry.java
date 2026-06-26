package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.client.render.fx.muzzleFlash.MuzzleFlash;
import com.sheridan.gcr.client.render.fx.muzzleSmoke.fast.FastMuzzleSmoke;
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
    private final FastMuzzleSmoke muzzleSmoke;
    private final float smokeScale;
    public boolean enabled;

    public MuzzleEntry(String name, String boneName, @Nullable String bindSlotName, float scale, MuzzleFlash muzzleFlash, float smokeScale, FastMuzzleSmoke muzzleSmoke) {
        this.name = name;
        this.boneName = boneName;
        this.bindSlotName = bindSlotName;
        this.scale = scale;
        this.muzzleFlash = muzzleFlash;
        this.enabled = true;
        this.muzzleSmoke = muzzleSmoke;
        this.smokeScale = smokeScale;
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

    public FastMuzzleSmoke getMuzzleSmoke() {
        return muzzleSmoke;
    }

    public float getSmokeScale() {
        return smokeScale;
    }
}

