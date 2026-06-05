package com.sheridan.gcr.client.render.fx.muzzleSmoke.fast;

import com.sheridan.gcr.GCR;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector2f;

@OnlyIn(Dist.CLIENT)
public class CommonMuzzleSmokeEffects {
    public static final FastMuzzleSmoke COMMON = new FastMuzzleSmoke(150, 3.5f, 3f, new Vector2f(0.9f, 0.6f),
            GCR.RL("textures/fx/muzzle_smoke/common_0.png"), 4).randomRotate();
}
