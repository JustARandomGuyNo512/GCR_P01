package com.sheridan.gcr.modularSys.modules.gunProperties;

import com.google.gson.JsonObject;
import com.sheridan.gcr.IJsonSync;
import com.sheridan.gcr.INBTSync;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public class Properties implements IJsonSync, INBTSync, IProperties {
    private final String id;
    private final String nameSpace;
    private final String path;
    private CompoundTag bindProp;
    private final Map<String, IProp> properties;

    public Properties(ResourceLocation id) {
        PropertiesDummies.registerDummy(this);
        this.properties = new HashMap<>();
        this.id = id.toString();
        nameSpace = id.getNamespace();
        path = id.getPath();
    }

    @Override
    public void bindProp(CompoundTag bindProp) {
        this.bindProp = bindProp;
    }

    @Override
    public void unbindProp() {
        this.bindProp = null;
    }

    @Override
    public boolean hasProp(String name) {
        return properties.containsKey(name);
    }

    protected CompoundTag getBindProp() {
        return bindProp;
    }

    protected <T extends IProp> T defProp(T prop) {
        properties.put(prop.getKey(), prop);
        String key = String.format("%s:prop.%s.%s", nameSpace, path, prop.getKey());
        prop.setFullName(key);
        return prop;
    }

    @Override
    public CompoundTag genInitialTag() {
        CompoundTag tag = new CompoundTag();
        for (IProp prop : properties.values()) {
            prop.setUpDefault(tag);
        }
        return tag;
    }

    public void inc(IProp prop, float inc) {
        CompoundTag bindProp = getBindProp();
        if (bindProp != null) {
            prop.inc(bindProp, inc);
        }
    }

    public void dec(IProp prop, float dec) {
        CompoundTag bindProp = getBindProp();
        if (bindProp != null) {
            prop.dec(bindProp, dec);
        }
    }


    @Override
    @Nullable
    public <T extends IProp> T getProp(String name, Class<T> clazz) {
        IProp prop = getProp(name);
        if (clazz.isInstance(prop)) {
            return clazz.cast(prop);
        }
        return null;
    }

    @Override
    @Nullable
    public IProp getProp(String name) {
        return properties.get(name);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void writeData(CompoundTag tag) {

    }

    @Override
    public void loadData(CompoundTag tag) {

    }

    @Override
    public void writeToJson(JsonObject jsonObject) {

    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}