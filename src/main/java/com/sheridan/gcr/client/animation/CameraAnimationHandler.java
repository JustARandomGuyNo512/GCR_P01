package com.sheridan.gcr.client.animation;

import com.sheridan.gcr.client.model.Bone;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.ViewportEvent;

@OnlyIn(Dist.CLIENT)
public class CameraAnimationHandler {
    public static final CameraAnimationHandler INSTANCE = new CameraAnimationHandler();
    public float cameraYaw;
    public float cameraPitch;
    public float cameraRoll;


    public void set(Bone cameraPosePart) {
        cameraYaw = cameraPosePart.yRot;
        cameraPitch = cameraPosePart.xRot;
        cameraRoll = cameraPosePart.zRot;
    }

    public void apply(ViewportEvent.ComputeCameraAngles event) {
        if (cameraYaw != 0 || cameraPitch != 0 || cameraRoll != 0) {
            event.setYaw((float) Math.toDegrees(cameraYaw) + event.getYaw());
            event.setPitch((float) Math.toDegrees(cameraPitch) + event.getPitch());
            event.setRoll((float) Math.toDegrees(cameraRoll) + event.getRoll());
        }
    }

    public void clear() {
        cameraYaw = 0;
        cameraPitch = 0;
        cameraRoll = 0;
    }

}
