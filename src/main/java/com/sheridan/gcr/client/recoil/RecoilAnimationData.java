package com.sheridan.gcr.client.recoil;

import com.google.gson.JsonObject;
import com.sheridan.gcr.IJsonSync;
import net.minecraft.util.Mth;

import java.util.concurrent.atomic.AtomicInteger;

public class RecoilAnimationData implements IJsonSync {
    private static final AtomicInteger TEMP_ID = new AtomicInteger(0);
    public int id;
    public float back;
    public float backK;
    public float backDamp0;
    public float backDamp1;
    public float rotate;
    public float rotateK;
    public float rotateDamp;
    public float aimingBackScale = 0.6f;
    public float aimingRotateScale = 0.25f;
    public float randomX = 0.05f;
    public float randomY = 0.72f;
    public float rotSplitX;
    public float rotSplitY;
    public float randomXChangeRate = 0.5f;
    public float randomYChangeRate = 0.5f;
    public float vibrateZ;
    public float vibrateNormal;
    public float vibrateSpeed0;
    public float vibrateSpeed1;
    public float vibrateSpeed2;
    public float vibrateDamp0;
    public float vibrateDamp1;
    public float aimingVibrateScale;
    public float defaultStable;
    public boolean mixable;

    public RecoilAnimationData() {
        this.id = TEMP_ID.getAndIncrement();
        this.mixable = false;
    }

    public RecoilAnimationData(boolean mixable) {
        this.id = TEMP_ID.getAndIncrement();
        this.mixable = mixable;
    }

    public RecoilAnimationData backward(float back, float backK, float backDamp0, float backDamp1) {
        this.back = back;
        this.backK = backK;
        this.backDamp0 = backDamp0;
        this.backDamp1 = backDamp1;
        return this;
    }

    public RecoilAnimationData rotate(float rotate, float rotateK, float rotateDamp) {
        this.rotate = rotate;
        this.rotateK = rotateK;
        this.rotateDamp = rotateDamp;
        return this;
    }

    public RecoilAnimationData splitRotate(float rotSplitX, float rotSplitY) {
        this.rotSplitX = rotSplitX;
        this.rotSplitY = rotSplitY;
        return this;
    }

    public RecoilAnimationData random(float randomX, float randomY) {
        return random(randomX, randomY, 0.5f, 0.5f);
    }

    public RecoilAnimationData aimingScaler(float aimingBackScale, float aimingRotateScale, float aimingVibrateScale) {
        this.aimingBackScale = aimingBackScale;
        this.aimingRotateScale = aimingRotateScale;
        this.aimingVibrateScale = aimingVibrateScale;
        return this;
    }

    public RecoilAnimationData random(float randomX, float randomY, float xChangeRate, float yChangeRate) {
        this.randomX = randomX;
        this.randomY = randomY;
        this.randomXChangeRate = xChangeRate;
        this.randomYChangeRate = yChangeRate;
        return this;
    }


    public RecoilAnimationData vibration(float vibrate, float vibrateNormal, float vibrateSpeed, float vibrateDamp0) {
        this.vibrateZ = vibrate;
        this.vibrateNormal = vibrateNormal;
        this.vibrateSpeed0 = vibrateSpeed;
        this.vibrateSpeed1 = vibrateSpeed * 1.25f;
        this.vibrateDamp1 = vibrateDamp0 * 2f;
        this.vibrateSpeed2 = vibrateSpeed * 0.5f;
        this.vibrateDamp0 = vibrateDamp0;
        return this;
    }

    public RecoilAnimationData defaultStable(float defaultStable) {
        this.defaultStable = Mth.clamp(defaultStable, 0, 1);
        return this;
    }

    public RecoilAnimationData vibration(float vibrateNormal, float vibrateSpeed, float vibrateDamp0) {
        return vibration(vibrateNormal, vibrateNormal * 0.25f, vibrateSpeed, vibrateDamp0);
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {
        jsonObject.addProperty("back", back);
        jsonObject.addProperty("backK", backK);
        jsonObject.addProperty("backDamp0", backDamp0);
        jsonObject.addProperty("backDamp1", backDamp1);
        jsonObject.addProperty("rotate", rotate);
        jsonObject.addProperty("rotateK", rotateK);
        jsonObject.addProperty("rotateDamp", rotateDamp);
        jsonObject.addProperty("aimingBackScale", aimingBackScale);
        jsonObject.addProperty("aimingRotateScale", aimingRotateScale);
        jsonObject.addProperty("randomX", randomX);
        jsonObject.addProperty("randomY", randomY);
        jsonObject.addProperty("rotSplitX", rotSplitX);
        jsonObject.addProperty("rotSplitY", rotSplitY);
        jsonObject.addProperty("randomXChangeRate", randomXChangeRate);
        jsonObject.addProperty("randomYChangeRate", randomYChangeRate);
        jsonObject.addProperty("vibrateZ", vibrateZ);
        jsonObject.addProperty("vibrateNormal", vibrateNormal);
        jsonObject.addProperty("vibrateSpeed0", vibrateSpeed0);
        jsonObject.addProperty("vibrateSpeed1", vibrateSpeed1);
        jsonObject.addProperty("vibrateSpeed2", vibrateSpeed2);
        jsonObject.addProperty("vibrateDamp0", vibrateDamp0);
        jsonObject.addProperty("vibrateDamp1", vibrateDamp1);
        jsonObject.addProperty("aimingVibrateScale", aimingVibrateScale);
        jsonObject.addProperty("mixable", mixable);
        jsonObject.addProperty("defaultStable", defaultStable);
    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {
        back = jsonObject.get("back").getAsFloat();
        backK = jsonObject.get("backK").getAsFloat();
        backDamp0 = jsonObject.get("backDamp0").getAsFloat();
        backDamp1 = jsonObject.get("backDamp1").getAsFloat();
        rotate = jsonObject.get("rotate").getAsFloat();
        rotateK = jsonObject.get("rotateK").getAsFloat();
        rotateDamp = jsonObject.get("rotateDamp").getAsFloat();
        aimingBackScale = jsonObject.get("aimingBackScale").getAsFloat();
        aimingRotateScale = jsonObject.get("aimingRotateScale").getAsFloat();
        randomX = jsonObject.get("randomX").getAsFloat();
        randomY = jsonObject.get("randomY").getAsFloat();
        rotSplitX = jsonObject.get("rotSplitX").getAsFloat();
        rotSplitY = jsonObject.get("rotSplitY").getAsFloat();
        randomXChangeRate = jsonObject.get("randomXChangeRate").getAsFloat();
        randomYChangeRate = jsonObject.get("randomYChangeRate").getAsFloat();
        vibrateZ = jsonObject.get("vibrateZ").getAsFloat();
        vibrateNormal = jsonObject.get("vibrateNormal").getAsFloat();
        vibrateSpeed0 = jsonObject.get("vibrateSpeed0").getAsFloat();
        vibrateSpeed1 = jsonObject.get("vibrateSpeed1").getAsFloat();
        vibrateSpeed2 = jsonObject.get("vibrateSpeed2").getAsFloat();
        vibrateDamp0 = jsonObject.get("vibrateDamp0").getAsFloat();
        vibrateDamp1 = jsonObject.get("vibrateDamp1").getAsFloat();
        aimingVibrateScale = jsonObject.get("aimingVibrateScale").getAsFloat();
        mixable = jsonObject.get("mixable").getAsBoolean();
        defaultStable = jsonObject.get("defaultStable").getAsFloat();
    }
}
