package com.sheridan.gcr.modularSys.modules.views;

import com.sheridan.gcr.modularSys.modules.states.Str;
import net.minecraft.nbt.CompoundTag;

/**
 * 榴弹发射器（挂载式副武器）的视图契约。
 *
 * <p>M203 与 GP25 的弹膛状态键与语义完全一致，放在这里避免两个接口各写一份。
 * 具体型号的接口只需声明自己特有的状态（例如 M203 的 {@code fired}），
 * 以及各自动画控制器/状态视图所需的额外查询。</p>
 */
public interface IGrenadeLauncherView extends IStateView {
    /** 弹膛为空。 */
    String CHAMBER_EMPTY = "empty";
    /** 弹膛已装填。 */
    String CHAMBER_LOADED = "loaded";

    /** 弹膛状态，存于节点 states 中。 */
    Str CHAMBER_STATUS = new Str("chamber_status", CHAMBER_EMPTY);

    String getChamberStatus(CompoundTag states);
}
