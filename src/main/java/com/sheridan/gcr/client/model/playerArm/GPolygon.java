package com.sheridan.gcr.client.model.playerArm;

import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@Deprecated
@OnlyIn(Dist.CLIENT)
public class GPolygon {
    public final GVertex[] vertices;
    public final Vector3f normal;

    public GPolygon(GVertex[] vertices, float u1, float v1, float u2, float v2, float textureWidth, float textureHeight, boolean mirror, Direction direction) {
        this.vertices = vertices;
        float f = 0.0F / textureWidth;
        float f1 = 0.0F / textureHeight;
        vertices[0] = vertices[0].remap(u2 / textureWidth - f, v1 / textureHeight + f1);
        vertices[1] = vertices[1].remap(u1 / textureWidth + f, v1 / textureHeight + f1);
        vertices[2] = vertices[2].remap(u1 / textureWidth + f, v2 / textureHeight - f1);
        vertices[3] = vertices[3].remap(u2 / textureWidth - f, v2 / textureHeight - f1);
        if (mirror) {
            int i = vertices.length;

            for(int j = 0; j < i / 2; ++j) {
                GVertex GVertex = vertices[j];
                vertices[j] = vertices[i - 1 - j];
                vertices[i - 1 - j] = GVertex;
            }
        }

        this.normal = direction.step();
        if (mirror) {
            this.normal.mul(-1.0F, 1.0F, 1.0F);
        }

    }
}
