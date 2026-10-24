package com.sheridan.gcr.modularSys.builder;

/**
 * 定义了验证问题的严重等级。
 */
public enum ErrorLevel {
    /**
     * 错误：严重问题，会阻止提交。
     */
    ERROR("error"),
    /**
     * 警告：提示潜在问题，但允许提交。
     */
    WARNING("warning");

    private final String name;

    ErrorLevel(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
