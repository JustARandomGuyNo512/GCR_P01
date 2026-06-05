package com.sheridan.gcr.modularSys.modules.gunProperties;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sheridan.gcr.modularSys.modules.states.Num;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

public class IntProp extends Prop {

    public int value;
    private float minRatio;
    private float maxRatio;
    private final Num ratio;
    public IntProp(String name, int value, float minRatio, float maxRatio, float defaultRatio) {
        if (minRatio > maxRatio) {
            throw new IllegalArgumentException("minRatio must be less than maxRatio");
        }
        defaultRatio = Mth.clamp(defaultRatio, minRatio, maxRatio);
        this.value = value;
        this.minRatio = minRatio;
        this.maxRatio = maxRatio;
        ratio = new Num(name, defaultRatio);
    }

    public IntProp(String name, int value, float minRatio, float maxRatio) {
        this(name, value, minRatio, maxRatio, 1.0f);
    }

    @Override
    public void inc(CompoundTag tag, float incRate) {
        float r = ratio.get(tag);
        r = Mth.clamp(r + incRate, minRatio, maxRatio);
        ratio.set(r, tag);
    }


    @Override
    public float get(CompoundTag properties) {
        float r = getRatio(properties);
        return r * value;
    }

    @Override
    public float getRatio(CompoundTag properties) {
        return ratio.get(properties);
    }

    @Override
    public String getKey() {
        return ratio.name;
    }

    @Override
    public void setUpDefault(CompoundTag tag) {
        ratio.init(tag);
    }

    @Override
    public void dec(CompoundTag tag, float decRate) {
        float r = ratio.get(tag);
        r = Mth.clamp(r - decRate, minRatio, maxRatio);
        ratio.set(r, tag);
    }


    @Override
    public void writeToJson(JsonObject jsonObject) {
        JsonObject prop = new JsonObject();
        prop.addProperty("value", value);
        prop.addProperty("minRatio", minRatio);
        prop.addProperty("maxRatio", maxRatio);
        jsonObject.add(ratio.name, prop);
    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {
        JsonElement jsonElement = jsonObject.get(ratio.name);
        if (jsonElement != null && jsonElement.isJsonObject()) {
            JsonObject prop = jsonElement.getAsJsonObject();
            value = prop.getAsJsonPrimitive("value").getAsInt();
            minRatio = prop.getAsJsonPrimitive("minRatio").getAsFloat();
            maxRatio = prop.getAsJsonPrimitive("maxRatio").getAsFloat();
        }
    }

    @Override
    public void writeData(CompoundTag tag) {
        CompoundTag prop = new CompoundTag();
        prop.putFloat("value", value);
        prop.putFloat("minRatio", minRatio);
        prop.putFloat("maxRatio", maxRatio);
        tag.put(ratio.name, prop);
    }

    @Override
    public void loadData(CompoundTag tag) {
        if (tag.contains(ratio.name)) {
            Tag tag1 = tag.get(ratio.name);
            if (tag1 instanceof CompoundTag compoundTag) {
                value = compoundTag.getInt("value");
                minRatio = compoundTag.getFloat("minRatio");
                maxRatio = compoundTag.getFloat("maxRatio");
            }
        }
    }
}