package com.sheridan.gcr.modularSys.modules;

import com.sheridan.gcr.modularSys.builder.Unit;

public interface ISight {
    String ON_SIDE_POSITION = "on_side_position";
    /**
     * 禁用
     * */
    int IGNORE = -1;
    /**
     * 最低优先级，用于安装在侧面瞄具基座上的瞄具，或者其它副瞄具
     * */
    int SIDE = 0;
    /**
     * 用于枪械本身的机瞄，比如ar机匣，或者裸ak，手枪等
     * */
    int GUN_BASE = 1;
    /**
     * 经典机瞄
     * */
    int IRON_SIGHT = 2;
    /**
     * 不具备放大功能的光学瞄具，比如红点
     * */
    int RED_DOT = 3;
    /**
     * 经典光学瞄准具
     * */
    int SCOPE = 4;

    default int getSightPriority(Unit unit) {
        int customParam = unit.getCustomParam(ON_SIDE_POSITION);
        if (customParam == -1) {
            return defaultSightPriority(unit);
        } else {
            return SIDE;
        }
    }

    float getAdsSpeedModifier();

    default float getZCompensation() {
        return 1f;
    }

    int defaultSightPriority(Unit unit);
}
