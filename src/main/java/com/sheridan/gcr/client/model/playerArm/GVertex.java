package com.sheridan.gcr.client.model.playerArm;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

@Deprecated
@OnlyIn(Dist.CLIENT)
public class GVertex {
    public final Vector3f pos;
    public final float u;
    public final float v;

    public GVertex(float x, float y, float z, float u, float v) {
        this(new Vector3f(x, y, z), u, v);
    }

    public GVertex remap(float u, float v) {
        return new GVertex(this.pos, u, v);
    }

    public GVertex(Vector3f pos, float u, float v) {
        this.pos = pos;
        this.u = u;
        this.v = v;
    }
}
