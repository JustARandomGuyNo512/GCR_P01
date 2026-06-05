package com.sheridan.gcr.modularSys.builder;


import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ISlotProviderModular;
import net.minecraft.nbt.ListTag;

import java.util.List;

/**
 * 采用“仓库/工作区”模式的树构建器顶层接口。
 * 这是与系统外部交互的主要入口。
 * - 仓库区 (Warehouse): 代表了上次成功提交的、绝对合法的稳定状态。
 * - 工作区 (Workspace): 是一个用于实时编辑的“沙盒”，其内容可能随时处于不合法的中间状态。
 */
public interface IBuilder {

    <T extends ISlotProviderModular & IModular> IBuilder root(T modular);
    /**
     * 获取用于修改“工作区”树结构的操作器。
     * 所有的增、删、改、移动操作都应通过此接口进行。
     * @return 工作区操作器实例。
     */
    IWorkSpace getWorkspace();

    /**
     * 获取“仓库区”的只读视图。
     * 这代表了当前系统中稳定、合法、已提交的树结构。
     * @return 仓库区树的只读视图。
     */
    IReadOnlyTree getWarehouse();

    /**
     * 对当前“工作区”的完整状态执行一次合法性检查。
     * 这是一个纯粹的只读操作，不会对任何状态产生影响，主要用于预操作检查和实时反馈。
     * @return 包含了所有发现的错误（ERROR）和警告（WARNING）的验证结果。
     */
    ValidateResult checkWorkspace(boolean checkCollision);

    /**
     * 尝试提交“工作区”的当前更改。
     * 内部会首先调用 checkWorkspace() 进行最终验证。
     * 只有在验证结果中不包含任何 ERROR 级别的 问题时，提交才会成功。
     * 成功后，仓库区的状态将被更新为当前工作区的状态。
     */
    List<ValidateResult> commit();

    /**
     * 撤销“工作区”中所有未提交的更改。
     * 此操作会用“仓库区”的稳定状态覆盖“工作区”，使其恢复到上次成功提交时的样子。
     */
    void revert();

    ICollisionHandler getCollisionHandler();

    void init(ListTag data);

    IAccessor getAccessor();

    IWriteableAccessor getWriteableAccessor();

    /*
    * 工作区是否与仓库存在差异
    * */
    boolean diff();
}