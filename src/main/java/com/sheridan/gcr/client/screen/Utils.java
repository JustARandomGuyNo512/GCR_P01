package com.sheridan.gcr.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.GCR;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.FastColor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public class Utils {

    /**
     * 绘制一条带有粗细的线段。
     *
     * @param guiGraphics 渲染类型，例如 RenderType.gui()
     * @param x1         起点X坐标
     * @param y1         起点Y坐标
     * @param x2         终点X坐标
     * @param y2         终点Y坐标
     * @param size       线段的粗细 (宽度)
     * @param color      线段的颜色 (整数格式, 例如 0xFFRRGGBB)
     */
    public static void drawLine(GuiGraphics guiGraphics, float x1, float y1, float x2, float y2, float size, int color) {
        // 获取当前的变换矩阵
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        // 获取用于绘制的顶点消费者 (VertexConsumer)
        VertexConsumer vertexConsumer = guiGraphics.bufferSource().getBuffer(RenderType.GUI);

        // 计算线段的向量表示
        float deltaX = x2 - x1;
        float deltaY = y2 - y1;

        // 计算线段的长度
        float length = (float) Math.sqrt(deltaX * deltaX + deltaY * deltaY);

        // 如果长度为0，则无需绘制
        if (length == 0) {
            return;
        }

        // 计算线段方向的单位法线向量 (perpendicular vector)
        // 这个向量将用于确定矩形的宽度方向
        float pX = -deltaY / length; // 法线向量的x分量
        float pY = deltaX / length;  // 法线向量的y分量

        // 计算矩形的四个顶点
        // 我们将线段视为一个中心线，然后向两侧扩展 size / 2 的距离来形成矩形
        float halfSize = size / 2.0f;

        // 顶点1: 起点向法线方向扩展
        float v1x = x1 + pX * halfSize;
        float v1y = y1 + pY * halfSize;

        // 顶点2: 终点向法线方向扩展
        float v2x = x2 + pX * halfSize;
        float v2y = y2 + pY * halfSize;

        // 顶点3: 终点向法线反方向扩展
        float v3x = x2 - pX * halfSize;
        float v3y = y2 - pY * halfSize;

        // 顶点4: 起点向法线反方向扩展
        float v4x = x1 - pX * halfSize;
        float v4y = y1 - pY * halfSize;

        // 使用顶点消费者添加这四个顶点来定义一个四边形 (矩形)
        // 注意：为了构成一个完整的四边形，顶点的添加顺序是重要的。
        // 这里我们按照 v1 -> v2 -> v3 -> v4 的顺序来绘制。
        vertexConsumer.addVertex(matrix4f, v1x, v1y, 0).setColor(color);
        vertexConsumer.addVertex(matrix4f, v2x, v2y, 0).setColor(color);
        vertexConsumer.addVertex(matrix4f, v3x, v3y, 0).setColor(color);
        vertexConsumer.addVertex(matrix4f, v4x, v4y, 0).setColor(color);

        guiGraphics.flush();
    }

    /**
     * 绘制一个实心圆。
     *
     * @param guiGraphics 渲染类型
     * @param centerX    圆心的X坐标
     * @param centerY    圆心的Y坐标
     * @param radius     圆的半径

     * @param color      颜色
     * @param segments   分段数，用于近似圆形。数值越高，圆形越平滑，但性能开销越大。推荐 30-60。
     */
    public static void drawSolidCircle(GuiGraphics guiGraphics, float centerX, float centerY, float radius, int color, int segments) {
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        VertexConsumer vertexConsumer = guiGraphics.bufferSource().getBuffer(RenderType.gui());

        // 圆被分解为一系列从圆心到边缘的三角形
        for (int i = 0; i < segments; i++) {
            double angle1 = 2.0 * Math.PI * i / segments;
            double angle2 = 2.0 * Math.PI * (i + 1) / segments;

            float x1 = centerX + radius * (float) Math.cos(angle1);
            float y1 = centerY + radius * (float) Math.sin(angle1);
            float x2 = centerX + radius * (float) Math.cos(angle2);
            float y2 = centerY + radius * (float) Math.sin(angle2);

            // 添加构成扇形小三角形的三个顶点
            vertexConsumer.addVertex(matrix4f, centerX, centerY, 0).setColor(color);
            vertexConsumer.addVertex(matrix4f, x1, y1, 0).setColor(color);
            vertexConsumer.addVertex(matrix4f, x2, y2, 0).setColor(color);
        }

        guiGraphics.flush();
    }

    /**
     * 绘制一个"V"形箭头。
     *
     * @param graphics      渲染类型
     * @param tipX            箭头尖端的X坐标
     * @param tipY            箭头尖端的Y坐标
     * @param length          箭头每个臂的长度
     * @param thickness       箭头臂的粗细
     * @param directionAngle  箭头朝向的角度 (度)。0度朝右, 90度朝下, 180度朝左, 270度朝上。
     * @param armAngle        箭头两臂之间的夹角 (度)。例如 45 度。
     * @param color           颜色
     */
    public static void drawVArrow(GuiGraphics graphics, float tipX, float tipY, float length, float thickness, float directionAngle, float armAngle, int color) {
        // 将角度转换为弧度
        float directionRad = (float) Math.toRadians(directionAngle);
        float armAngleRad = (float) Math.toRadians(armAngle / 2.0f);

        // 计算两个臂的末端点坐标
        // 臂1
        float angle1 = directionRad - armAngleRad;
        float startX1 = tipX - length * (float) Math.cos(angle1);
        float startY1 = tipY - length * (float) Math.sin(angle1);

        // 臂2
        float angle2 = directionRad + armAngleRad;
        float startX2 = tipX - length * (float) Math.cos(angle2);
        float startY2 = tipY - length * (float) Math.sin(angle2);

        // 分别绘制构成箭头的两条线段
        drawLine(graphics, startX1, startY1, tipX, tipY, thickness, color);
        drawLine(graphics, startX2, startY2, tipX, tipY, thickness, color);
    }


    public static void drawDevHelperLayers(
            GuiGraphics guiGraphics, Font font, int mouseX, int mouseY, int width, int height, int leftPos, int topPos) {
        RenderSystem.disableDepthTest();
        if (GCR.IS_DEVELOPMENT) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0f, 0f, 1000f);
            guiGraphics.drawString(font, mouseX + " " + mouseY, mouseX + font.width("."), mouseY - font.lineHeight, 0xffffff);
            guiGraphics.hLine(0, width, mouseY, FastColor.ABGR32.color(255,0,255,0));
            guiGraphics.vLine(mouseX, 0, height, FastColor.ABGR32.color(255,0,0,255));

            guiGraphics.hLine(leftPos, leftPos + width, topPos, FastColor.ABGR32.color(255,0,255,255));
            guiGraphics.hLine(leftPos, leftPos + width, topPos + height, FastColor.ABGR32.color(255,0,255,255));
            guiGraphics.vLine(leftPos, topPos, topPos + height, FastColor.ABGR32.color(255,0,255,255));
            guiGraphics.vLine(leftPos + width, topPos, topPos + height, FastColor.ABGR32.color(255,0,255,255));

            guiGraphics.pose().popPose();
        }
        RenderSystem.enableDepthTest();
    }


}
