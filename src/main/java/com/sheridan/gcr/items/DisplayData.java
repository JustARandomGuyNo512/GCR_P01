package com.sheridan.gcr.items;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.sheridan.gcr.IJsonSync;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

public class DisplayData implements IJsonSync {
    public static final float FIRST_PERSON_SCALE = 0.5f;
    public static final int FIRST_PERSON = 0;
    public static final int THIRD_PERSON = 1;
    public static final int GROUND = 2;
    public static final int FRAME = 3;
    public static final int GUN_MODIFY_SCREEN = 4;
    public static final int SPRINTING = 5;

    private final float[][] displayData = new float[][] {
            {0,0,0,0,0,0,FIRST_PERSON_SCALE,FIRST_PERSON_SCALE,FIRST_PERSON_SCALE},// First Person
            {0,0,0,0,0,0,1,1,1},// Third Person
            {0,0,0,0,0,0,1,1,1},// Ground
            {0,0,0,0,0,0,1,1,1},// Frame
            {0,0,0,0,0,0,1,1,1},// Attachment Screen
            {0,0,0,0,0,0,1,1,1}// Sprinting
    };

    private final float[] aimingTranslation = new float[] {
            0,0,0,0,0,0
    };

    public DisplayData() {}

    public String getJavaInitCode() {
        return String.format("""
                new DisplayData()
                    .setTranslation(DisplayData.FIRST_PERSON, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    .setTranslation(DisplayData.THIRD_PERSON, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    .setTranslation(DisplayData.GROUND, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    .setTranslation(DisplayData.FRAME, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    .setTranslation(DisplayData.GUN_MODIFY_SCREEN, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    .setTranslation(DisplayData.SPRINTING, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                    .setAimingTranslation(%s, %s, %s, %s, %s, %s);
                """,
                fmt16(displayData[0][0]), fmt16(displayData[0][1]), fmt16(displayData[0][2]), fmtDeg(displayData[0][3]), fmtDeg(displayData[0][4]), fmtDeg(displayData[0][5]), fmt(displayData[0][6]), fmt(displayData[0][7]), fmt(displayData[0][8]),
                fmt16(displayData[1][0]), fmt16(displayData[1][1]), fmt16(displayData[1][2]), fmtDeg(displayData[1][3]), fmtDeg(displayData[1][4]), fmtDeg(displayData[1][5]), fmt(displayData[1][6]), fmt(displayData[1][7]), fmt(displayData[1][8]),
                fmt16(displayData[2][0]), fmt16(displayData[2][1]), fmt16(displayData[2][2]), fmtDeg(displayData[2][3]), fmtDeg(displayData[2][4]), fmtDeg(displayData[2][5]), fmt(displayData[2][6]), fmt(displayData[2][7]), fmt(displayData[2][8]),
                fmt16(displayData[3][0]), fmt16(displayData[3][1]), fmt16(displayData[3][2]), fmtDeg(displayData[3][3]), fmtDeg(displayData[3][4]), fmtDeg(displayData[3][5]), fmt(displayData[3][6]), fmt(displayData[3][7]), fmt(displayData[3][8]),
                fmt16(displayData[4][0]), fmt16(displayData[4][1]), fmt16(displayData[4][2]), fmtDeg(displayData[4][3]), fmtDeg(displayData[4][4]), fmtDeg(displayData[4][5]), fmt(displayData[4][6]), fmt(displayData[4][7]), fmt(displayData[4][8]),
                fmt16(displayData[5][0]), fmt16(displayData[5][1]), fmt16(displayData[5][2]), fmtDeg(displayData[5][3]), fmtDeg(displayData[5][4]), fmtDeg(displayData[5][5]), fmt(displayData[5][6]), fmt(displayData[5][7]), fmt(displayData[5][8]),
                fmt16(aimingTranslation[0]), fmt16(aimingTranslation[1]), fmt16(aimingTranslation[2]), fmtDeg(aimingTranslation[3]), fmtDeg(aimingTranslation[4]), fmtDeg(aimingTranslation[5]));
    }

    private String fmt16(float v) {
        return fmt(v * 16f);
    }

    private String fmtDeg(float v) {
        return fmt((float) Math.toDegrees(v));
    }

    private String fmt(float v) {
        if (Math.abs(v) < 1e-5) return "0";
        if (Math.abs(v - Math.round(v)) < 1e-5) return Integer.toString(Math.round(v));
        return String.format("%.6f", v).replaceAll("0+$", "").replaceAll("\\.$", "") + "f";
    }

    @OnlyIn(Dist.CLIENT)
    public void applyTranslation(PoseStack poseStack, ItemDisplayContext context, float partialTicks) {
        applyTranslation(poseStack, getIndexFor(context), partialTicks);
    }

    @OnlyIn(Dist.CLIENT)
    public void applyTranslation(PoseStack poseStack, int index, float partialTicks) {
        if (index == FIRST_PERSON) {
            applyFirstPersonTranslation(poseStack);
        } else {
            translateAndRotate(displayData[index], poseStack);
        }
    }

    public void applyGunModifyScreenTranslation(PoseStack poseStack,  float x, float y, float rx, float ry, float scale) {
        float[] trans = displayData[GUN_MODIFY_SCREEN];
        poseStack.translate(trans[0] + x, trans[1] + y, trans[2]);
        poseStack.mulPose(new Quaternionf().rotateXYZ(trans[3] + rx, trans[4] + ry, trans[5]));
        poseStack.scale(trans[6] * scale, trans[7] * scale, trans[8] * scale);
    }

    @OnlyIn(Dist.CLIENT)
    public void applyFirstPersonTranslation(PoseStack poseStack) {
        translateAndRotate(displayData[FIRST_PERSON], poseStack);
    }

    public float[] getFirstPersonTranslate() {
        return displayData[FIRST_PERSON];
    }

    @OnlyIn(Dist.CLIENT)
    public int getIndexFor(ItemDisplayContext context) {
        switch (context) {
            case FIRST_PERSON_RIGHT_HAND -> {
                return FIRST_PERSON;
            }
            case THIRD_PERSON_RIGHT_HAND -> {
                return THIRD_PERSON;
            }
            case GROUND -> {
                return GROUND;
            }
            case FIXED -> {
                return FRAME;
            }
            default -> {return GUN_MODIFY_SCREEN;}
        }
    }

    public float[] getSprintingTranslate() {
        return displayData[SPRINTING];
    }


    @OnlyIn(Dist.CLIENT)
    private void translateAndRotate(float[] trans, PoseStack poseStack) {
        if (trans[0] != 0 || trans[1] != 0 || trans[2] != 0) {
            poseStack.translate(trans[0], trans[1], trans[2]);
        }
        if (trans[3] != 0 || trans[4] != 0 || trans[5] != 0) {
            poseStack.mulPose(new Quaternionf().rotateXYZ(trans[3], trans[4], trans[5]));
        }
        if (trans[6] != 1 || trans[7] != 1 || trans[8] != 1) {
            poseStack.scale(trans[6], trans[7], trans[8]);
        }
    }

    public DisplayData setTranslation(int index, float x, float y, float z, float rotX, float rotY, float rotZ, float scale) {
        return setTranslation(index, x, y, z, rotX, rotY, rotZ, scale, scale, scale);
    }

    public DisplayData setTranslation(int index, float x, float y, float z, float rotX, float rotY, float rotZ, float scaleX, float scaleY, float scaleZ) {
        if (index >= 0 && index < displayData.length) {
            displayData[index][0] = x / 16f;
            displayData[index][1] = y / 16f;
            displayData[index][2] = z / 16f;
            displayData[index][3] = (float) Math.toRadians(rotX);
            displayData[index][4] = (float) Math.toRadians(rotY);
            displayData[index][5] = (float) Math.toRadians(rotZ);
            displayData[index][6] = scaleX;
            displayData[index][7] = scaleY;
            displayData[index][8] = scaleZ;
        }
        return this;
    }

    public DisplayData setAimingTranslation(float x, float y, float z, float rotX, float rotY, float rotZ) {
        aimingTranslation[0] = x / 16f;
        aimingTranslation[1] = y / 16f;
        aimingTranslation[2] = z / 16f;
        aimingTranslation[3] = (float) Math.toRadians(rotX);
        aimingTranslation[4] = (float) Math.toRadians(rotY);
        aimingTranslation[5] = (float) Math.toRadians(rotZ);
        return this;
    }

    public void set(int i, int j, float value) {
        if (i >= 0 && i < displayData.length && j >= 0 && j < displayData[i].length) {
            displayData[i][j] = value;
        }
    }

    public float get(int i, int j) {
        if (i >= 0 && i < displayData.length && j >= 0 && j < displayData[i].length) {
            return displayData[i][j];
        }
        return Float.NaN;
    }

    public void inc(int i, int j, float value) {
        if (i >= 0 && i < displayData.length && j >= 0 && j < displayData[i].length) {
            displayData[i][j] += value;
        }
    }

    public void dec(int i, int j, float value) {
        if (i >= 0 && i < displayData.length && j >= 0 && j < displayData[i].length) {
            displayData[i][j] -= value;
        }
    }

    private String getKey(int index) {
        return switch (index) {
            case 0 -> "first_person";
            case 1 -> "third_person";
            case 2 -> "ground";
            case 3 -> "frame";
            case 4 -> "attachment_screen";
            case 5 -> "sprinting";
            default -> throw new IllegalArgumentException("Invalid index: " + index);
        };
    }

    @Override
    public void writeToJson(JsonObject jsonObject) {
        for (int i = 0; i < displayData.length; i++) {
            JsonArray array = new JsonArray();
            for (float f : displayData[i]) {
                array.add(f);
            }
            jsonObject.add(getKey(i), array);
        }
        JsonArray array = new JsonArray();
        for (float f : aimingTranslation) {
            array.add(f);
        }
        jsonObject.add("aiming", array);
    }

    @Override
    public void loadFromJson(JsonObject jsonObject) {
        for (int i = 0; i < displayData.length; i++){
            JsonArray array = jsonObject.getAsJsonArray(getKey(i));
            for (int j = 0; j < array.size(); j++) {
                displayData[i][j] = array.get(j).getAsFloat();
            }
        }
        JsonArray array = jsonObject.getAsJsonArray("aiming");
        for (int i = 0; i < array.size(); i++) {
            aimingTranslation[i] = array.get(i).getAsFloat();
        }
    }

    public void copyFrom(DisplayData displayData) {
        for (int i = 0; i < this.displayData.length; i++) {
            System.arraycopy(displayData.displayData[i], 0, this.displayData[i], 0, this.displayData[i].length);
        }
    }

    public DisplayData copy() {
        DisplayData displayData = new DisplayData();
        displayData.copyFrom(this);
        return displayData;
    }
}
