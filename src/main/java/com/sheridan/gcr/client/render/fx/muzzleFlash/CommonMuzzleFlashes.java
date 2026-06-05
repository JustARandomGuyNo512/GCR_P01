package com.sheridan.gcr.client.render.fx.muzzleFlash;

import com.sheridan.gcr.GCR;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class CommonMuzzleFlashes {
    public static final MuzzleFlash COMMON = new MuzzleFlash("COMMON",
            List.of(new MuzzleFlashTexture(GCR.RL("textures/fx/muzzle_flash/common.png"), 4)), true, 4);

    public static final MuzzleFlash SUPPRESSOR_COMMON = new MuzzleFlash("SUPPRESSOR_COMMON",
            List.of(new MuzzleFlashTexture(GCR.RL("textures/fx/muzzle_flash/suppressor.png"), 3)), true, 4);

    public static final MuzzleFlash AK_COMPENSATOR = new MuzzleFlash("AK_COMPENSATOR",
            List.of(new MuzzleFlashTexture(GCR.RL("textures/fx/muzzle_flash/ak_compensator.png"), 4)), true, 2);

    public static final MuzzleFlash AR_COMPENSATOR = new MuzzleFlash("AR_COMPENSATOR",
            List.of(new MuzzleFlashTexture(GCR.RL("textures/fx/muzzle_flash/ar_compensator.png"), 4)), true, 4);
}
