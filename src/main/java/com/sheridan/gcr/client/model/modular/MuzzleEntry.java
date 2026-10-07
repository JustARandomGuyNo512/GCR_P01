package com.sheridan.gcr.client.model.modular;

import com.sheridan.gcr.client.render.fx.muzzleFlash.MuzzleFlash;
import com.sheridan.gcr.client.render.fx.muzzleSmoke.fast.FastMuzzleSmoke;
import com.sheridan.gcr.client.render.fx.muzzleSmoke.slow.SlowSmoke;
import com.sheridan.gcr.client.render.fx.muzzleSmoke.slow.SlowSmokeEntry;
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
    /** 慢速飘散烟雾的贴图集合（有哪些贴图可用 + 基础大小 / 基础透明度），null 表示这条枪口不产生 slow smoke */
    @Nullable
    private SlowSmoke slowSmoke;
    /**
     * 这一条 MuzzleEntry 自己的 slow smoke 参数（fadeSeconds / localScale / expand / flyDir / flyDirRand），
     * 每把枪在这里单独配置，null 表示用 {@link SlowSmokeEntry} 的默认参数
     */
    @Nullable
    private SlowSmokeEntry slowSmokeEntry;
    public boolean enabled;
    public float flashLightIntensity;

    public MuzzleEntry(String name, String boneName, @Nullable String bindSlotName,
                       float scale, MuzzleFlash muzzleFlash,
                       float smokeScale, FastMuzzleSmoke muzzleSmoke,
                       float flashLightIntensity) {
        this(name, boneName, bindSlotName, scale, muzzleFlash, smokeScale, muzzleSmoke, flashLightIntensity, null, null);
    }

    public MuzzleEntry(String name, String boneName, @Nullable String bindSlotName,
                       float scale, MuzzleFlash muzzleFlash,
                       float smokeScale, FastMuzzleSmoke muzzleSmoke,
                       float flashLightIntensity,
                       @Nullable SlowSmoke slowSmoke, @Nullable SlowSmokeEntry slowSmokeEntry) {
        this.name = name;
        this.boneName = boneName;
        this.bindSlotName = bindSlotName;
        this.scale = scale;
        this.muzzleFlash = muzzleFlash;
        this.enabled = true;
        this.muzzleSmoke = muzzleSmoke;
        this.smokeScale = smokeScale;
        this.flashLightIntensity = flashLightIntensity;
        this.slowSmoke = slowSmoke;
        this.slowSmokeEntry = slowSmokeEntry;
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

    @Nullable
    public SlowSmoke getSlowSmoke() {
        return slowSmoke;
    }

    /**
     * 这条 MuzzleEntry 自己的 slow smoke 参数，可以为 null（表示用 {@link SlowSmokeEntry} 的默认参数）
     */
    @Nullable
    public SlowSmokeEntry getSlowSmokeEntry() {
        return slowSmokeEntry;
    }

    public boolean hasSlowSmoke() {
        return slowSmoke != null;
    }

    /** 链式设置 slow smoke 的贴图集合，传 null 表示关闭 */
    public MuzzleEntry withSlowSmoke(@Nullable SlowSmoke slowSmoke) {
        this.slowSmoke = slowSmoke;
        return this;
    }

    /**
     * 链式设置这条枪口自己的 slow smoke 参数，例如
     * {@code new SlowSmokeEntry().fade(2.5f).scale(1.4f).controller(new CommonSlowSmokeController())}
     */
    public MuzzleEntry withSlowSmokeEntry(@Nullable SlowSmokeEntry slowSmokeEntry) {
        this.slowSmokeEntry = slowSmokeEntry;
        return this;
    }

    /**
     * 链式一次性设置 slow smoke 的贴图集合与这条枪口自己的参数
     */
    public MuzzleEntry withSlowSmoke(@Nullable SlowSmoke slowSmoke, @Nullable SlowSmokeEntry slowSmokeEntry) {
        this.slowSmoke = slowSmoke;
        this.slowSmokeEntry = slowSmokeEntry;
        return this;
    }
}
