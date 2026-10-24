package com.sheridan.gcr.client.recoil;

/**
 * 武器后坐力物理参数配置包
 */


public class RecoilData {
    private RecoilImpulse impulse;
    private RecoilController recoilController;
    private CameraController cameraController;
    private VisualRecoilMix visualRecoilMix;

    public RecoilData(RecoilImpulse impulse, RecoilController controller) {
        this.impulse = impulse;
        this.recoilController = controller;
    }

    public RecoilData(RecoilImpulse impulse, RecoilController controller, VisualRecoilMix mix) {
        this.impulse = impulse;
        this.recoilController = controller;
        this.visualRecoilMix = mix;
    }

    public RecoilData setVisualRecoilMix(VisualRecoilMix mix) {
        this.visualRecoilMix = mix;
        return this;
    }

    public RecoilImpulse getImpulse() {
        return impulse;
    }

    public void setImpulse(RecoilImpulse impulse) {
        this.impulse = impulse;
    }

    public RecoilController getRecoilController() {
        return recoilController;
    }

    public void setRecoilController(RecoilController recoilController) {
        this.recoilController = recoilController;
    }

    public VisualRecoilMix getVisualRecoilMix() {
        return visualRecoilMix;
    }
}
