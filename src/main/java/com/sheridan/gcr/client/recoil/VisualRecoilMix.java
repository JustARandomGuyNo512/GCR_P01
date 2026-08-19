package com.sheridan.gcr.client.recoil;

public record VisualRecoilMix(
        float backScale, float backOmegaMin, float backOmegaMax, float backZeta, float backAdsFactor,
        float shakeRotScale, float shakeRotOmega, float shakeRotZeta, float shakeRotAdsFactor,
        float shakeMovScale
) {}
