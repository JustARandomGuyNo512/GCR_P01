package com.sheridan.gcr.client.render.fx;

import com.mojang.blaze3d.shaders.ProgramManager;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.sheridan.gcr.Client;
import com.sheridan.gcr.client.render.IrisExtendRT;
import com.sheridan.gcr.client.render.delayed.Stage;
import com.sheridan.gcr.client.render.delayed.Task;
import com.sheridan.gcr.compat.IrisCompat;
import com.sheridan.gcr.items.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.opengl.GL11C.GL_ONE;
import static org.lwjgl.opengl.GL11C.GL_SRC_ALPHA;

@OnlyIn(Dist.CLIENT)
public class IrisGunPostRenderer {
    static {
        Stage.LOWEST.addTask(new Task((p) -> IrisGunPostRenderer.onRender()).forever());
    }

    static boolean shouldRender = false;

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (Client.isIrisShaderInUse && IrisGunPostShader.isOK() && !IrisCompat.isRenderingShadowPass()) {
            InteractionHand hand = event.getHand();
            Item item = event.getItemStack().getItem();
            if (hand == InteractionHand.MAIN_HAND && item instanceof GunItem) {
                shouldRender = true;
            }
        }
    }

    public static void onRender() {
        if (shouldRender && IrisExtendRT.textureId != -1) {
            if (IrisExtendRT.dirty) {
                RenderSystem.enableBlend();
                RenderSystem.blendFunc(GL_SRC_ALPHA, GL_ONE);
                RenderSystem.disableDepthTest();
                RenderSystem.depthMask(false);
                GL20.glUseProgram(IrisGunPostShader.programId);


                RenderSystem.activeTexture(GL13.GL_TEXTURE0);
                RenderSystem.bindTexture(IrisExtendRT.textureId);
                Uniform.uploadInteger(IrisGunPostShader.muzzleLightContributionSamplerLoc, 0);


                GL30.glBindVertexArray(IrisGunPostShader.vaoId);
                GL11.glDrawArrays(GL11.GL_TRIANGLE_STRIP, 0, 4);


                IrisExtendRT.clearMuzzleTexture();
                GL30.glBindVertexArray(0);
                ProgramManager.glUseProgram(0);
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
                RenderSystem.defaultBlendFunc();
                RenderSystem.activeTexture(GL13.GL_TEXTURE0);
                RenderSystem.bindTexture(0);
                Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            }
            shouldRender = false;
        } else {
            IrisExtendRT.clearMuzzleTexture();
        }
    }
}
