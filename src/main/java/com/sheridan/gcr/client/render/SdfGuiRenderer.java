package com.sheridan.gcr.client.render;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SdfGuiRenderer {
    private static final RenderType GUI_OVERLAY = RenderType.guiOverlay();

    public static void drawCircle(GuiGraphics graphics, float cx, float cy, float radius, int fillColor, int borderColor, float borderWidth) {
        int rCeil = (int) Math.ceil(radius + borderWidth + 0.5f);
        int yMin = (int) Math.floor(cy - rCeil);
        int yMax = (int) Math.ceil(cy + rCeil);

        boolean hasBorder = borderWidth > 0.001f && (borderColor >>> 24) > 0;
        float rInner = Math.max(0.0f, radius - borderWidth);

        for (int py = yMin; py <= yMax; py++) {
            float dy = (py + 0.5f) - cy;
            float dySq = dy * dy;
            float rOuterSq = radius * radius;
            if (dySq > rOuterSq) continue;

            float dxOuter = (float) Math.sqrt(Math.max(0.0f, rOuterSq - dySq));
            int xLeft = Math.round(cx - dxOuter);
            int xRight = Math.round(cx + dxOuter);
            if (xRight <= xLeft) continue;

            if (hasBorder && rInner > 0.001f && dySq < rInner * rInner) {
                float dxInner = (float) Math.sqrt(Math.max(0.0f, rInner * rInner - dySq));
                int xInnerLeft = Math.round(cx - dxInner);
                int xInnerRight = Math.round(cx + dxInner);

                if (xInnerLeft > xLeft) {
                    graphics.fill(GUI_OVERLAY, xLeft, py, xInnerLeft, py + 1, borderColor);
                }
                if (xInnerRight > xInnerLeft && (fillColor >>> 24) > 0) {
                    graphics.fill(GUI_OVERLAY, xInnerLeft, py, xInnerRight, py + 1, fillColor);
                }
                if (xRight > xInnerRight) {
                    graphics.fill(GUI_OVERLAY, xInnerRight, py, xRight, py + 1, borderColor);
                }
            } else {
                int col = hasBorder ? borderColor : fillColor;
                if ((col >>> 24) > 0) {
                    graphics.fill(GUI_OVERLAY, xLeft, py, xRight, py + 1, col);
                }
            }
        }
    }

    public static void drawRing(GuiGraphics graphics, float cx, float cy, float radius, float thickness, int color) {
        if ((color >>> 24) == 0 || thickness <= 0.001f) return;
        float rOuter = radius + thickness * 0.5f;
        float rInner = Math.max(0.0f, radius - thickness * 0.5f);
        int rCeil = (int) Math.ceil(rOuter + 0.5f);
        int yMin = (int) Math.floor(cy - rCeil);
        int yMax = (int) Math.ceil(cy + rCeil);

        float rOuterSq = rOuter * rOuter;
        float rInnerSq = rInner * rInner;

        for (int py = yMin; py <= yMax; py++) {
            float dy = (py + 0.5f) - cy;
            float dySq = dy * dy;
            if (dySq >= rOuterSq) continue;

            float dxOuter = (float) Math.sqrt(rOuterSq - dySq);
            int xLeft = Math.round(cx - dxOuter);
            int xRight = Math.round(cx + dxOuter);

            if (dySq < rInnerSq) {
                float dxInner = (float) Math.sqrt(rInnerSq - dySq);
                int xInnerLeft = Math.round(cx - dxInner);
                int xInnerRight = Math.round(cx + dxInner);

                if (xInnerLeft > xLeft) {
                    graphics.fill(GUI_OVERLAY, xLeft, py, xInnerLeft, py + 1, color);
                }
                if (xRight > xInnerRight) {
                    graphics.fill(GUI_OVERLAY, xInnerRight, py, xRight, py + 1, color);
                }
            } else {
                if (xRight > xLeft) {
                    graphics.fill(GUI_OVERLAY, xLeft, py, xRight, py + 1, color);
                }
            }
        }
    }

    public static void drawRoundedRect(GuiGraphics graphics, float x, float y, float w, float h, float radius, int fillColor, int borderColor, float borderWidth) {
        float r = Math.min(radius, Math.min(w * 0.5f, h * 0.5f));
        int ix = Math.round(x);
        int iy = Math.round(y);
        int iw = Math.round(w);
        int ih = Math.round(h);
        int ir = Math.round(r);

        boolean hasBorder = borderWidth > 0.001f && (borderColor >>> 24) > 0;
        int bw = Math.max(1, Math.round(borderWidth));

        if ((fillColor >>> 24) > 0) {
            graphics.fill(GUI_OVERLAY, ix, iy + ir, ix + iw, iy + ih - ir, fillColor);
            graphics.fill(GUI_OVERLAY, ix + ir, iy + (hasBorder ? bw : 0), ix + iw - ir, iy + ir, fillColor);
            graphics.fill(GUI_OVERLAY, ix + ir, iy + ih - ir, ix + iw - ir, iy + ih - (hasBorder ? bw : 0), fillColor);
        }

        if (hasBorder) {
            graphics.fill(GUI_OVERLAY, ix, iy + ir, ix + bw, iy + ih - ir, borderColor);
            graphics.fill(GUI_OVERLAY, ix + iw - bw, iy + ir, ix + iw, iy + ih - ir, borderColor);
            graphics.fill(GUI_OVERLAY, ix + ir, iy, ix + iw - ir, iy + bw, borderColor);
            graphics.fill(GUI_OVERLAY, ix + ir, iy + ih - bw, ix + iw - ir, iy + ih, borderColor);
        }

        drawCorner(graphics, ix + ir, iy + ir, ir, bw, fillColor, borderColor, hasBorder, -1, -1);
        drawCorner(graphics, ix + iw - ir, iy + ir, ir, bw, fillColor, borderColor, hasBorder, 1, -1);
        drawCorner(graphics, ix + ir, iy + ih - ir, ir, bw, fillColor, borderColor, hasBorder, -1, 1);
        drawCorner(graphics, ix + iw - ir, iy + ih - ir, ir, bw, fillColor, borderColor, hasBorder, 1, 1);
    }

    private static void drawCorner(GuiGraphics graphics, int ccx, int ccy, int r, int bw, int fillColor, int borderColor, boolean hasBorder, int signX, int signY) {
        float rInner = Math.max(0.0f, r - bw);
        float rSq = r * r;
        float rInnerSq = rInner * rInner;

        for (int dy = 0; dy < r; dy++) {
            float dySq = (dy + 0.5f) * (dy + 0.5f);
            if (dySq > rSq) continue;
            float dxOuter = (float) Math.sqrt(Math.max(0.0f, rSq - dySq));
            int py = (signY < 0) ? (ccy - 1 - dy) : (ccy + dy);

            int xStart = (signX < 0) ? Math.round(ccx - dxOuter) : ccx;
            int xEnd = (signX < 0) ? ccx : Math.round(ccx + dxOuter);

            if (hasBorder && dySq < rInnerSq) {
                float dxInner = (float) Math.sqrt(Math.max(0.0f, rInnerSq - dySq));
                if (signX < 0) {
                    int xInner = Math.round(ccx - dxInner);
                    if (xInner > xStart) {
                        graphics.fill(GUI_OVERLAY, xStart, py, xInner, py + 1, borderColor);
                    }
                    if ((fillColor >>> 24) > 0 && ccx > xInner) {
                        graphics.fill(GUI_OVERLAY, xInner, py, ccx, py + 1, fillColor);
                    }
                } else {
                    int xInner = Math.round(ccx + dxInner);
                    if ((fillColor >>> 24) > 0 && xInner > ccx) {
                        graphics.fill(GUI_OVERLAY, ccx, py, xInner, py + 1, fillColor);
                    }
                    if (xEnd > xInner) {
                        graphics.fill(GUI_OVERLAY, xInner, py, xEnd, py + 1, borderColor);
                    }
                }
            } else {
                int col = hasBorder ? borderColor : fillColor;
                if ((col >>> 24) > 0 && xEnd > xStart) {
                    graphics.fill(GUI_OVERLAY, xStart, py, xEnd, py + 1, col);
                }
            }
        }
    }

    public static void drawArc(GuiGraphics graphics, float cx, float cy, float radius, float thickness, float startRad, float endRad, int color) {
        if ((color >>> 24) == 0 || thickness <= 0.001f) return;
        float totalAngle = endRad - startRad;
        if (totalAngle <= 0.001f) return;

        float stepAngle = (float) (1.8 / radius);
        int steps = Math.max(16, (int) Math.ceil(totalAngle / stepAngle));
        float dotRadius = thickness * 0.5f;

        for (int i = 0; i <= steps; i++) {
            float a = startRad + (totalAngle * i / (float) steps);
            float px = cx + (float) (radius * Math.cos(a));
            float py = cy + (float) (radius * Math.sin(a));
            drawCircle(graphics, px, py, dotRadius, color, 0, 0f);
        }
    }
}
