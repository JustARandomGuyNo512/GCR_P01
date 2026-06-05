package com.sheridan.gcr.client.model.playerArm;


import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sheridan.gcr.Client;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Set;

@Deprecated
@OnlyIn(Dist.CLIENT)
public class GCube {
    private final GPolygon[] GPolygons;
    public final float minX;
    public final float minY;
    public final float minZ;
    public final float maxX;
    public final float maxY;
    public final float maxZ;

    public GCube(int texCoordU, int texCoordV, float originX, float originY, float originZ, float dimensionX, float dimensionY, float dimensionZ, float gtowX, float growY, float growZ, boolean mirror, float texScaleU, float texScaleV, Set<Direction> visibleFaces) {
        this.minX = originX;
        this.minY = originY;
        this.minZ = originZ;
        this.maxX = originX + dimensionX;
        this.maxY = originY + dimensionY;
        this.maxZ = originZ + dimensionZ;
        this.GPolygons = new GPolygon[visibleFaces.size()];
        float f = originX + dimensionX;
        float f1 = originY + dimensionY;
        float f2 = originZ + dimensionZ;
        originX -= gtowX;
        originY -= growY;
        originZ -= growZ;
        f += gtowX;
        f1 += growY;
        f2 += growZ;
        if (mirror) {
            float f3 = f;
            f = originX;
            originX = f3;
        }

        GVertex gVertex7 = new GVertex(originX, originY, originZ, 0.0F, 0.0F);
        GVertex gVertex = new GVertex(f, originY, originZ, 0.0F, 8.0F);
        GVertex gVertex1 = new GVertex(f, f1, originZ, 8.0F, 8.0F);
        GVertex gVertex2 = new GVertex(originX, f1, originZ, 8.0F, 0.0F);
        GVertex gVertex3 = new GVertex(originX, originY, f2, 0.0F, 0.0F);
        GVertex gVertex4 = new GVertex(f, originY, f2, 0.0F, 8.0F);
        GVertex gVertex5 = new GVertex(f, f1, f2, 8.0F, 8.0F);
        GVertex gVertex6 = new GVertex(originX, f1, f2, 8.0F, 0.0F);
        float f4 = (float)texCoordU;
        float f5 = (float)texCoordU + dimensionZ;
        float f6 = (float)texCoordU + dimensionZ + dimensionX;
        float f7 = (float)texCoordU + dimensionZ + dimensionX + dimensionX;
        float f8 = (float)texCoordU + dimensionZ + dimensionX + dimensionZ;
        float f9 = (float)texCoordU + dimensionZ + dimensionX + dimensionZ + dimensionX;
        float f10 = (float)texCoordV;
        float f11 = (float)texCoordV + dimensionZ;
        float f12 = (float)texCoordV + dimensionZ + dimensionY;
        int i = 0;
        if (visibleFaces.contains(Direction.DOWN)) {
            this.GPolygons[i++] = new GPolygon(new GVertex[]{gVertex4, gVertex3, gVertex7, gVertex}, f5, f10, f6, f11, texScaleU, texScaleV, mirror, Direction.DOWN);
        }

        if (visibleFaces.contains(Direction.UP)) {
            this.GPolygons[i++] = new GPolygon(new GVertex[]{gVertex1, gVertex2, gVertex6, gVertex5}, f6, f11, f7, f10, texScaleU, texScaleV, mirror, Direction.UP);
        }

        if (visibleFaces.contains(Direction.WEST)) {
            this.GPolygons[i++] = new GPolygon(new GVertex[]{gVertex7, gVertex3, gVertex6, gVertex2}, f4, f11, f5, f12, texScaleU, texScaleV, mirror, Direction.WEST);
        }

        if (visibleFaces.contains(Direction.NORTH)) {
            this.GPolygons[i++] = new GPolygon(new GVertex[]{gVertex, gVertex7, gVertex2, gVertex1}, f5, f11, f6, f12, texScaleU, texScaleV, mirror, Direction.NORTH);
        }

        if (visibleFaces.contains(Direction.EAST)) {
            this.GPolygons[i++] = new GPolygon(new GVertex[]{gVertex4, gVertex, gVertex1, gVertex5}, f6, f11, f8, f12, texScaleU, texScaleV, mirror, Direction.EAST);
        }

        if (visibleFaces.contains(Direction.SOUTH)) {
            this.GPolygons[i] = new GPolygon(new GVertex[]{gVertex3, gVertex4, gVertex5, gVertex6}, f8, f11, f9, f12, texScaleU, texScaleV, mirror, Direction.SOUTH);
        }

    }

    private final Vector3f tempNormal = new Vector3f();
    private final Vector3f tempVertexPos = new Vector3f();
    private final Vector3f tempLightVector = new Vector3f();
    private final Vector3f muzzleFlashPos = new Vector3f();
    public void compile(PoseStack.Pose pose, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        Matrix4f matrix4f = pose.pose();
        float progress = Math.max(0, (0.05f - Client.distFromLastShoot()) * 25f);
        float muzzleFlashRadius = Client.WEAPON_STATUS.getMuzzleFlashRadius() * 16;
        float muzzleFlashIntensity = Client.WEAPON_STATUS.getMuzzleFlashIntensity() * progress;
        muzzleFlashPos.set(Client.WEAPON_STATUS.getMuzzleFlashPos());
        muzzleFlashPos.y = - muzzleFlashPos.y * 2f;
        int existingSkyLight = LightTexture.sky(packedLight);
        int existingBlockLight = LightTexture.block(packedLight);
        for (GPolygon gPolygon : this.GPolygons) {

            Vector3f transformedNormal = pose.transformNormal(gPolygon.normal, this.tempNormal);
            float f = transformedNormal.x();
            float f1 = transformedNormal.y();
            float f2 = transformedNormal.z();

            for (GVertex gVertex : gPolygon.vertices) {
                float f3 = gVertex.pos.x() * 0.0625F;
                float f4 = gVertex.pos.y() * 0.0625F;
                float f5 = gVertex.pos.z() * 0.0625F;

                Vector3f transformedPos = matrix4f.transformPosition(f3, f4, f5, this.tempVertexPos);
                muzzleFlashPos.sub(transformedPos, this.tempLightVector);
                Vector3f lightVector = this.tempLightVector;
                float distance = lightVector.length();
                float attenuation = 1.0f - Mth.clamp(distance / muzzleFlashRadius, 0.0f, 1.0f);
                attenuation = Math.max(0.1f, attenuation * muzzleFlashIntensity);
                lightVector.normalize();
                float diffuse = Math.max(0.1f, transformedNormal.dot(lightVector));
                float muzzleBrightness = Mth.clamp(diffuse * attenuation, 0.0f, 1.0f);

                int muzzleLight = (int) (muzzleBrightness * 16.0f);
                int newPackedLight = LightTexture.pack(
                        Math.min(existingBlockLight + muzzleLight, 15),
                        Math.min(existingSkyLight + muzzleLight, 15)
                );

                buffer.addVertex(
                        transformedPos.x,
                        transformedPos.y,
                        transformedPos.z,
                        color,
                        gVertex.u,
                        gVertex.v,
                        packedOverlay,
                        newPackedLight,
                        f, f1, f2
                );
            }
        }
    }
}