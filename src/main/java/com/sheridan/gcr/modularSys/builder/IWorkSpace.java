package com.sheridan.gcr.modularSys.builder;

import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.slot.ISlot;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;
import java.util.Optional;

/**
 * 工作区操作器接口，提供了修改工作区树结构的所有方法。
 * 它继承了 IReadOnlyTree，意味着工作区本身也是可读的。
 * 它的实现类会将所有操作委托给内部的 TreeManager 来执行。
 */
public interface IWorkSpace extends IReadOnlyTree {

    Unit setRoot(IModular modular);

    /**
     * 在指定父单元的插槽中，添加一个新的子单元。
     * @param parentUnit 父单元。
     * @param childModuleID 要添加的子单元的模块 ID。
     * @param parentSlotName 父单元的插槽ID。
     * */
    Optional<Unit> addChild(Unit parentUnit, String parentSlotName, String childModuleID);

    /**
     * 【便捷方法】在指定父单元的插槽中，通过模块添加一个新的子单元。
     * 方法内部会自动创建 Unit 实例。
     * @param parent 父单元。
     * @param parentSlot 要挂载到的父单元的插槽。
     * @param child 待添加的子单元。
     * @return 被创建并添加的子 Unit 实例。
     */
    Optional<Unit> addChild(Unit parent, ISlot parentSlot, Unit child);

    /**
     * 在指定的父节点插槽实例中添加一个新的子单元。
     * @param slot 目标父节点的插槽实例。
     * @param child            待添加的新单元。
     * @return 如果操作成功，返回 true。
     */
    Optional<Unit> addChild(SlotInstance slot, Unit child);

    Optional<Unit> addChild(SlotInstance slot, IModular modular);

    /**
     * 从工作区中移除一个单元及其所有后代。
     * @param unitToRemove 待移除的单元。
     * @return 如果操作成功，返回 true。
     */
    boolean removeChild(Unit unitToRemove);

    Optional<Pair<Unit, List<Unit>>> replaceChild(Unit oldUnit, Unit newUnit);

    Optional<Pair<Unit, List<Unit>>> replaceChild(Unit oldUnit, IModular modular);

    boolean isEmpty();

    IWorkSpace deepCopy();

    int getMutateId();

    void increaseMutateId();

    void setOffset(Unit unit, float offset);

    String getUnitId(Unit unit);
}

