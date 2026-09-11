package com.sheridan.gcr.modularSys.modules.views;

/**
 * M203 挂载式榴弹发射器的视图。
 *
 * <p>相比 GP25，M203 多一个独立于 {@code empty} 的 {@code fired} 状态（打空的弹壳仍留在膛内）。</p>
 */
public interface IM203View extends IGrenadeLauncherView {
    /** 已击发，弹壳未退出。 */
    String CHAMBER_FIRED = "fired";
}
