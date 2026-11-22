package com.sheridan.gcr.client.recoil.test;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.recoil.IRecoilUpdater;
import com.sheridan.gcr.client.recoil.RecoilHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * 左上角后坐力调试面板：把 {@link IRecoilUpdater#getDebugInfo()} 按 {@code "\n"} 拆行画出来。
 *
 * <p>文本内容由 updater 自己决定，这里只负责排版（半透明黑底 + 白字，逐行绘制）。
 * 走接口而不是直接依赖 {@code DemoRecoilUpdater}，是为了热重载后仍能显示子类加载器里的
 * 新实例；默认实现返回空串时整个面板不显示。只在开发环境生效。</p>
 */
@EventBusSubscriber(modid = GCR.MODID, value = Dist.CLIENT)
public final class RecoilDebugOverlay {
    private static final int MARGIN_X = 4;
    private static final int MARGIN_Y = 4;
    private static final int LINE_GAP = 1;
    private static final int BACKGROUND_COLOR = 0x90000000;
    private static final int TEXT_COLOR = 0xFFE8E8E8;

    private RecoilDebugOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        if (!GCR.IS_DEVELOPMENT) {
            return;
        }
        IRecoilUpdater updater = RecoilHandler.INSTANCE.getRecoilUpdater();
        if (updater == null) {
            return;
        }
        String info = updater.getDebugInfo();
        if (info == null || info.isEmpty()) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        String[] lines = info.split("\n", -1);
        int lineHeight = minecraft.font.lineHeight + LINE_GAP;

        int width = 0;
        for (String line : lines) {
            width = Math.max(width, minecraft.font.width(line));
        }

        int left = MARGIN_X;
        int top = MARGIN_Y;
        int bottom = top + lines.length * lineHeight;
        graphics.fill(left - 2, top - 2, left + width + 2, bottom + 1, BACKGROUND_COLOR);
        for (int i = 0; i < lines.length; i++) {
            graphics.drawString(minecraft.font, lines[i], left, top + i * lineHeight, TEXT_COLOR);
        }
    }
}
