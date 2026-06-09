package com.sheridan.gcr.client.animation.command;

import com.sheridan.gcr.client.animation.AnimationDef;
import com.sheridan.gcr.client.animation.IAnimated;
import com.sheridan.gcr.client.animation.IAnimationSequence;
import com.sheridan.gcr.client.render.ModuleRenderContext;
import com.sheridan.gcr.modularSys.modules.IAmmoSource;
import com.sheridan.gcr.modularSys.modules.guns.IGun;
import com.sheridan.gcr.modularSys.modules.guns.SlottedGunMainPart;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ShowMagAmmoLeftCommand extends Command{
    private float coolDown;
    private long lastShow;
    private float startProgress;

    public ShowMagAmmoLeftCommand(String command, float timeStamp) {
        super(command, timeStamp);
        if (args.size() != 1) {
            throw new IllegalArgumentException("Invalid command args: " + command);
        }
        this.coolDown = Float.parseFloat(args.getFirst());
    }

    @Override
    public void bindDef(AnimationDef def) {
        super.bindDef(def);
        startProgress = timeStamp / def.lengthInSeconds();
        coolDown = Math.max(coolDown, def.lengthInSeconds() - timeStamp);
    }


    @Override
    public void onFrame(IAnimated animated, IAnimationSequence sequence, ModuleRenderContext context) {
        if (sequence.getCurrentAnimatingProgress() >= startProgress) {
            float timeDist = (System.currentTimeMillis() - lastShow) * 0.001f;
            if (timeDist > coolDown) {
                IGun gun = context.gun;
                if (gun instanceof SlottedGunMainPart mainPart) {
                    IAmmoSource magAttachment = mainPart.getMagAttachment(context.itemStack);
                    if (magAttachment != null) {
                        int magAmmoLeft = mainPart.getMagAmmoLeft(context.itemStack);
                        String msgKey;
                        if (magAmmoLeft == 0) {
                            msgKey = "gcr.ani.mag_ammo_left_empty";
                        } else {
                            int maxCapacity = magAttachment.getMaxCapacity();
                            float ratio = magAmmoLeft / (float) maxCapacity;
                            if (ratio < 0.125f) {
                                msgKey = "gcr.ani.mag_ammo_left_few";
                            } else if (ratio < 0.375f) {
                                msgKey = "gcr.ani.mag_ammo_left_quarter";
                            } else if (ratio < 0.625f) {
                                msgKey = "gcr.ani.mag_ammo_left_half";
                            } else if (ratio < 0.875f) {
                                msgKey = "gcr.ani.mag_ammo_left_three_quarters";
                            } else {
                                msgKey = "gcr.ani.mag_ammo_left_almost_full";
                            }
                        }
                        Minecraft.getInstance().gui.setOverlayMessage(Component.translatable(msgKey), false);
                    }
                }
                lastShow = System.currentTimeMillis();
            }
        }
    }
}
