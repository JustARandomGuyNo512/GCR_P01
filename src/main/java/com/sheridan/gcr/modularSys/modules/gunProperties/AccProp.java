package com.sheridan.gcr.modularSys.modules.gunProperties;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sheridan.gcr.modularSys.modules.states.Num;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

public class AccProp extends Prop {
    private Num accumulative;
    private float min;
    private float max;

    public AccProp(String name, float defaultNum, float min, float max) {
        accumulative = new Num(name, defaultNum);
        this.min = min;
        this.max = max;
    }

    public AccProp(String name, float defaultNum) {
        this(name, defaultNum, 0, 1e8f);
    }

    public float getDefault() {
        return accumulative.getDefaultValue();
    }

    @Override
    public void inc(CompoundTag tag, float incCount) {
        float count = accumulative.get(tag);
        count += incCount;
        count = Mth.clamp(count, min, max);
        accumulative.set(count, tag);
    }

    @Override
    public void dec(CompoundTag tag, float decCount) {
        float count = accumulative.get(tag);
        count -= decCount;
        count = Mth.clamp(count, min, max);
        accumulative.set(count, tag);
    }

    @Override
    public float get(CompoundTag properties) {
        return accumulative.get(properties);
    }

    @Override
    public float getRatio(CompoundTag properties) {
        return get(properties) / accumulative.getDefaultValue();
    }

    @Override
    public String getKey() {
        return accumulative.name;
    }

    @Override
    public void setUpDefault(CompoundTag tag) {
        accumulative.init(tag);
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {
        JsonObject prop = new JsonObject();
        prop.addProperty("value", accumulative.getDefaultValue());
        prop.addProperty("min", min);
        prop.addProperty("max", max);
        jsonObject.add(accumulative.name, prop);
    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {
        JsonElement jsonElement = jsonObject.get(accumulative.name);
        if (jsonElement != null && jsonElement.isJsonObject()) {
            JsonObject prop = jsonElement.getAsJsonObject();
            float value = prop.getAsJsonPrimitive("value").getAsFloat();
            accumulative = new Num(accumulative.name, value);
            min = prop.getAsJsonPrimitive("min").getAsFloat();
            max = prop.getAsJsonPrimitive("max").getAsFloat();
        }
    }

    @Override
    public void writeData(CompoundTag tag) {
        CompoundTag prop = new CompoundTag();
        prop.putFloat("value", accumulative.getDefaultValue());
        prop.putFloat("min", min);
        prop.putFloat("max", max);
        tag.put(accumulative.name, prop);
    }

    @Override
    public void loadData(CompoundTag tag) {
        if (tag.contains(accumulative.name)) {
            Tag tag1 = tag.get(accumulative.name);
            if (tag1 instanceof CompoundTag compoundTag) {
                float value = compoundTag.getInt("value");
                accumulative = new Num(accumulative.name, value);
                min = compoundTag.getFloat("min");
                max = compoundTag.getFloat("max");
            }
        }
    }
}
