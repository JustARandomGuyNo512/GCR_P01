package com.sheridan.gcr.client.render;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class RenderConstants {
    public static final int ANIMATED_LEFT_ARM_MODEL = 0;
    public static final int ANIMATED_RIGHT_ARM_MODEL = 1;

    public static final int LEFT_ARM_LERP_CONTROL = 2;
    public static final int RIGHT_ARM_LERP_CONTROL = 3;

    public static final int SIGHT_POSE_STORAGE_KEY = 4;

    public static final int COVER_STATE_KEY = 5;
    public static final int COVER_STATE_LOCAL_KEY = 6;

    public static final int STATE_COVER_ANIMATIONS = 7;
}
