package com.sheridan.gcr.modularSys.builder;


import net.minecraft.nbt.ListTag;

import java.util.List;
import java.util.Optional;

// 这是一个辅助接口，用于提供只读的树视图
public interface IReadOnlyTree {
    Unit getRootUnit();
    Optional<Unit> findUnitById(String id);
    List<Unit> getSequencedUnits();
    ListTag write();
    String getId(Unit unit);
    /**
     * 获取指定 Unit 的父 Unit。
     * @param childUnit 子单元。
     * @return 父单元的 Optional 封装。
     */
    Optional<Unit> getParentOf(Unit childUnit);

    /**
     * 获取指定 SlotInstance 下的所有直接子 Unit。
     * @param slotInstance 插槽实例。
     * @return 所有直接子 Unit 的列表。
     */
    List<Unit> getChildrenOf(SlotInstance slotInstance);

    ShadowNode getShadowTree();
}
