package com.sheridan.gcr.modularSys.builder;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

import java.util.Objects;

public final class ValidationError {
    private final Unit source; // 报告问题的 Unit
    private final Unit target;
    private final String message;
    private final ErrorLevel level;

    public ValidationError(Unit source, Unit target, String message, ErrorLevel level) {
        this.source = source;
        this.target = target;
        this.message = message;
        this.level = level;
    }

    @Override
    public String toString() {
        return String.format("[%s] from %s: %s to: %s", level, source, message, target);
    }

    public Unit target() {
        return target;
    }

    public Unit source() {
        return source;
    }

    public Component getComponentMsg() {
        String msg = Component.translatable("validate.result.common").getString();
        msg = msg.replace("$level", level.toString()).replace("$msg", message);
        return Component.literal(msg).withStyle(
                Style.EMPTY.withColor(level == ErrorLevel.ERROR ? 0xFF0000 : 0xFFFF00)
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ValidationError that = (ValidationError) o;
        return Objects.equals(source, that.source) &&
                Objects.equals(message, that.message) &&
                level == that.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, message, level);
    }

    public ErrorLevel getLevel() {
        return level;
    }
}
