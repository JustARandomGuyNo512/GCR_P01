package com.sheridan.gcr.modularSys.modules.impl;

import com.sheridan.gcr.modularSys.modules.*;
import com.sheridan.gcr.modularSys.modules.views.IGP25View;
import com.sheridan.gcr.sound.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

/**
 * GP25 枪管下挂榴弹发射器。
 *
 * <p>行为全部继承自 {@link GrenadeLauncher}：没有独立的击发后状态，击发即回到 {@code empty}。</p>
 */
public class GP25 extends GrenadeLauncher implements IGP25View {

    public GP25(ResourceLocation id, float weight, IVoxelHandler voxelHandler, AdditionalPropModifier modifier,
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
        return ModSounds.GP25_FIRE.get();
    }

    @Override
    protected int getProjectileModelType() {
        return 1;
    }

    @Override
    protected String getChamberStatusAfterFire() {
        return CHAMBER_EMPTY;
    }
}
