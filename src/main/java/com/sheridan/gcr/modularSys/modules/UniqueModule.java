package com.sheridan.gcr.modularSys.modules;

import com.google.gson.JsonObject;
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.builder.IAccessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.builder.ValidateResult;
import com.sheridan.gcr.modularSys.modules.impl.AttachmentModule;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class UniqueModule extends AttachmentModule {
    public UniqueModule(ResourceLocation id, boolean fixedPosition, float weight, Direction direction) {
        super(id, fixedPosition, weight, direction);
        addTags("unique");
    }

    @Override
    public void validate(IAccessor accessor, Unit thisUnit, ValidateResult result) {
        accessor.first(unit -> unit != thisUnit && unit.getModuleId().equals(thisUnit.getModuleId()) && unit.hasTag("unique"))
                .ifPresent(unit -> {
                    String moduleId = unit.getModuleId();
                    String name = Component.translatable(moduleId).getString();
                    String msg = Component.translatable("validate.result.unique").getString().replace("$name", name);
                    result.recordError(unit, msg);
                });
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {

    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {

    }
}
