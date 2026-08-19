package com.sheridan.gcr.client.recoil;

public record VisualRecoilMix(
        float backScale, float backOmegaMin, float backOmegaMax, float backZeta, float backAdsOmegaFactor, float backAdsZetaFactor,
        float rotScale, float rotOmega, float rotZeta, float rotAdsOmegaFactor, float rotAdsZetaFactor,
        float shakeScale, float shakeAdsScaleFactor
) {}
