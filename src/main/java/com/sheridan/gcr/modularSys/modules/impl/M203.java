package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.modules.*;
import com.sheridan.gcr.modularSys.modules.views.IM203View;
import com.sheridan.gcr.sound.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * M203 枪管下挂榴弹发射器。
 *
 * <p>行为全部继承自 {@link GrenadeLauncher}：击发后弹壳留在膛内，弹膛进入 {@code fired} 状态，
 * 需要再次装填才回到 {@code loaded}。</p>
 */
public class M203 extends GrenadeLauncher implements IM203View {

    public M203(ResourceLocation id, float weight, IVoxelHandler voxelHandler, AdditionalPropModifier modifier,
                float reloadLengthInSeconds, float reloadSendPacketDelayInSeconds,
                float impulseZ, float impulsePitch, float impulseYaw, float impulseRoll,
                float spread, float velocity, float explodeRadius) {
        super(id, weight, voxelHandler, modifier,
                reloadLengthInSeconds, reloadSendPacketDelayInSeconds,
                impulseZ, impulsePitch, impulseYaw, impulseRoll,
                spread, velocity, explodeRadius);
    }

    @Override
    protected SoundEvent getFireSound() {
        return ModSounds.M203_FIRE.get();
    }

    @Override
    protected int getProjectileModelType() {
        return 0;
    }

    @Override
    protected String getChamberStatusAfterFire() {
        return CHAMBER_FIRED;
    }
}
