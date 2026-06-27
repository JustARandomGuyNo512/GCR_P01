package com.sheridan.gcr.client.render.fx.muzzleFlash;


import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@OnlyIn(Dist.CLIENT)
public class MuzzleFlash {
    static final Map<String, MuzzleFlash> REGISTRY = new HashMap<>();
    private static final Random RANDOM = new Random();
    private final List<MuzzleFlashTexture> textures;
    private boolean randomRotate;
    private float rotation;
    private int rotateSeed;
    public String name;
    public int length;

    public MuzzleFlash(String name, List<MuzzleFlashTexture> textures, boolean randomRotate, int rotateSeed) {
        this (name, textures, randomRotate, rotateSeed, 30);
    }

    public MuzzleFlash(String name, List<MuzzleFlashTexture> textures, boolean randomRotate, int rotateSeed, int length) {
        this(textures);
        if (rotateSeed > 0) {
            this.randomRotate = randomRotate;
            this.rotateSeed = rotateSeed;
            this.rotation = (float) Math.toRadians(360f / rotateSeed);
        }
        this.name = name;
        this.length = length;
        REGISTRY.put(name, this);
    }

    public static MuzzleFlash get(String name) {
        return REGISTRY.get(name);
    }

    /**
     * create a muzzle flash without random rotation
     * */
    public MuzzleFlash(List<MuzzleFlashTexture> textures) {
        this.textures = textures;
        this.randomRotate = false;
        this.rotateSeed = 0;
        this.rotation = 0;
    }

    public void render(PoseStack.Pose pose, MultiBufferSource bufferSource, float scale, long startTime, boolean isFirstPerson, int light) {
        if (!textures.isEmpty()) {
            long timeDis = System.currentTimeMillis() - startTime;
            boolean muzzleFlashNotEnded = timeDis <= length;
            if (muzzleFlashNotEnded) {
                PoseStack.Pose renderPose = pose.copy();
                renderPose.pose().scale(scale, scale, scale);
                int texNum = textures.size();
                int texIndex = texNum > 1 ? RANDOM.nextInt(texNum) : 0;
                MuzzleFlashTexture muzzleFlashTexture = textures.get(texIndex);
                if (randomRotate) {
                    int seed = Math.max(0, RANDOM.nextInt(6)) % rotateSeed;
                    if (seed != 0) {
                        renderPose.pose().rotate(new Quaternionf().rotateXYZ(0,0,seed * rotation));
                    }
                }
                int index = RANDOM.nextInt(muzzleFlashTexture.getCount());
                muzzleFlashTexture.render(index, renderPose, bufferSource, isFirstPerson);
            }
        }
    }
}
