package com.sheridan.gcr.modularSys.modules.guns;

import net.minecraft.world.item.ItemStack;

public interface BoltHoldOpenCapable {
    String BOLT_LOCKED_KEY = "gcr_bolt_locked";
    /** 当前是否锁机（开膛锁定） */
    boolean isBoltLocked(ItemStack stack);

    /** 设置锁机状态 */
    void setBoltLocked(ItemStack stack, boolean locked);
}
