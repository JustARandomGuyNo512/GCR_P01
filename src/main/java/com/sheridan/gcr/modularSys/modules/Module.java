package com.sheridan.gcr.modularSys.modules;


import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonObject;
import com.sheridan.gcr.items.ModuleItem;
import com.sheridan.gcr.modularSys.Direction;
import com.sheridan.gcr.modularSys.IModular;
import com.sheridan.gcr.modularSys.ModuleRegister;
import com.sheridan.gcr.modularSys.builder.IAccessor;
import com.sheridan.gcr.modularSys.builder.IWriteableAccessor;
import com.sheridan.gcr.modularSys.builder.Unit;
import com.sheridan.gcr.modularSys.builder.ValidateResult;
import com.sheridan.gcr.modularSys.modules.gunProperties.PropertiesAccessor;
import com.sheridan.gcr.modularSys.modules.gunProperties.impl.BaseProperties;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class Module implements IModular {
    protected final String id;
    protected final String simpleId;
    protected boolean fixedPosition;
    protected ModuleItem<?> bindItem;
    protected Direction direction;
    protected boolean finalized = false;
    protected Set<String> tags;
    protected ImmutableSet<String> finialTags;
    protected float weight;

    public Module(ResourceLocation id, boolean fixedPosition, float weight, Direction direction) {
        boolean register = ModuleRegister.register(id.toString(), this);
        if (!register) {
            throw new IllegalArgumentException("failed to register module: " + id + "! please check if the id has been taken by other modules");
        }
        this.id = id.toString();
        this.simpleId = id.getPath();
        this.fixedPosition = fixedPosition;
        this.direction = direction;
        this.tags = new HashSet<>();
        this.weight = weight;
    }

    @Override
    public String getID() {
        return this.id;
    }

    @Override
    public String getSimpleID() {
        return this.simpleId;
    }

    @Override
    public boolean hasTag(String tag) {
        return finialTags.contains(tag);
    }

    @Override
    public Set<String> getTags() {
        return finialTags;
    }

    @Override
    public boolean fixedPosition() {
        return fixedPosition;
    }

    @Override
    public ModuleItem<?> getBindItem() {
        return bindItem;
    }

    @Override
    public boolean bindItem(ModuleItem<?> item) {
        if (this.bindItem == null) {
            this.bindItem = item;
            return true;
        }
        return false;
    }

    @Override
    public Direction getDirection() {
        return direction;
    }

    @Override
    public void validate(IAccessor accessor, Unit thisUnit, ValidateResult result) {
    }

    @Override
    public void onMutated(IWriteableAccessor accessor, Unit thisUnit) {

    }

    @Override
    public void writeToJson(JsonObject jsonObject) {

    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {

    }

    @Override
    public void modifyProperties(PropertiesAccessor accessor) {
        accessor.using(BaseProperties.class, prop -> prop.inc(prop.weight, getWeight()));
    }

    public IModular addTags(String... tags) {
        if (finalized) {
            return this;
        }
        this.tags.addAll(List.of(tags));
        return this;
    }

    @Override
    public void finalizeInit() {
        finalized = true;
        finialTags = ImmutableSet.copyOf(tags);
    }

    @Override
    public float getWeight() {
        return weight;
    }
}
