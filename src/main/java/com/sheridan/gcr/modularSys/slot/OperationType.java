package com.sheridan.gcr.modularSys.slot;

/**
 * 定义了 SlotInstance 支持的操作类型。
 * 这是由 Module 的 Slot 设计者决定的硬性条件。
 */
public enum OperationType {
    ADD,
    REMOVE,
    REPLACE
}