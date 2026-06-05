package com.sheridan.gcr.client.render.fx.bulletShell;

import com.google.gson.JsonObject;
import com.sheridan.gcr.IJsonSync;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BulletShellDisplay implements IJsonSync {
    public String bindBoneName;
    public ResourceLocation modelID;
    public float front;
    public float up;
    public float upRandomDeg;
    public float frontRandomDeg;
    public float velocity;//弹壳行进速度，默认向x轴正方向
    public float velocityRandom;//随机比例
    public float rotatePitchRandomOffset;
    public float rotateYawRandomOffset;
    public float rotateYawSpeed;
    public float rotateYawSpeedRandom;
    public float rotatePitchSpeed;
    public float rotatePitchSpeedRandom;
    public int lifeTime;//弹壳生命时长， 毫秒
    public float drop;//下落

    public BulletShellDisplay(String bindBoneName, ResourceLocation modelID,
                              float front, float up,
                              float upRandomDeg, float frontRandomDeg,
                              float velocity, float velocityRandom,
                              float rotatePitchRandomOffset, float rotateYawRandomOffset,
                              float rotateYawSpeed, float rotateYawSpeedRandom,
                              float rotatePitchSpeed, float rotatePitchSpeedRandom,
                              int lifeTime, float drop) {
        this.bindBoneName = bindBoneName;
        this.modelID = modelID;
        this.front = front;
        this.up = up;
        this.upRandomDeg = upRandomDeg;
        this.frontRandomDeg = frontRandomDeg;
        this.velocity = velocity;
        this.velocityRandom = velocityRandom;
        this.rotatePitchRandomOffset = (float) Math.toRadians(rotatePitchRandomOffset);
        this.rotateYawRandomOffset = (float) Math.toRadians(rotateYawRandomOffset);
        this.rotateYawSpeed = (float) Math.toRadians(rotateYawSpeed);
        this.rotateYawSpeedRandom = rotateYawSpeedRandom;
        this.rotatePitchSpeed = (float) Math.toRadians(rotatePitchSpeed);
        this.rotatePitchSpeedRandom = rotatePitchSpeedRandom;
        this.lifeTime = lifeTime;
        this.drop = drop;
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {
        jsonObject.addProperty("bindBoneName", bindBoneName);
        jsonObject.addProperty("modelID", modelID.toString());
        jsonObject.addProperty("front", front);
        jsonObject.addProperty("up", up);
        jsonObject.addProperty("upRandomDeg", upRandomDeg);
        jsonObject.addProperty("frontRandomDeg", frontRandomDeg);
        jsonObject.addProperty("velocity", velocity);
        jsonObject.addProperty("velocityRandom", velocityRandom);
        jsonObject.addProperty("rotatePitchRandomOffset", rotatePitchRandomOffset);
        jsonObject.addProperty("rotateYawRandomOffset", rotateYawRandomOffset);
        jsonObject.addProperty("rotateYawSpeed", rotateYawSpeed);
        jsonObject.addProperty("rotateYawSpeedRandom", rotateYawSpeedRandom);
        jsonObject.addProperty("rotatePitchSpeed", rotatePitchSpeed);
        jsonObject.addProperty("rotatePitchSpeedRandom", rotatePitchSpeedRandom);
        jsonObject.addProperty("lifeTime", lifeTime);
        jsonObject.addProperty("drop", drop);
    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {
        bindBoneName = jsonObject.get("bindBoneName").getAsString();
        modelID = ResourceLocation.tryParse(jsonObject.get("modelID").getAsString());
        front = jsonObject.get("front").getAsFloat();
        up = jsonObject.get("up").getAsFloat();
        upRandomDeg = jsonObject.get("upRandomDeg").getAsFloat();
        frontRandomDeg = jsonObject.get("frontRandomDeg").getAsFloat();
        velocity = jsonObject.get("velocity").getAsFloat();
        velocityRandom = jsonObject.get("velocityRandom").getAsFloat();
        rotatePitchRandomOffset = jsonObject.get("rotatePitchRandomOffset").getAsFloat();
        rotateYawRandomOffset = jsonObject.get("rotateYawRandomOffset").getAsFloat();
        rotateYawSpeed = jsonObject.get("rotateYawSpeed").getAsFloat();
        rotateYawSpeedRandom = jsonObject.get("rotateYawSpeedRandom").getAsFloat();
        rotatePitchSpeed = jsonObject.get("rotatePitchSpeed").getAsFloat();
        rotatePitchSpeedRandom = jsonObject.get("rotatePitchSpeedRandom").getAsFloat();
        lifeTime = jsonObject.get("lifeTime").getAsInt();
        drop = jsonObject.get("drop").getAsFloat();
    }
}
