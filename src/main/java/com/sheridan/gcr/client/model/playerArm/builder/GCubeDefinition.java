package com.sheridan.gcr.client.model.playerArm.builder;

import com.sheridan.gcr.client.model.playerArm.GCube;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Set;

@Deprecated
@OnlyIn(Dist.CLIENT)
public final class GCubeDefinition {
    @Nullable
    private final String comment;
    private final Vector3f origin;
    private final Vector3f dimensions;
    private final GCubeDeformation grow;
    private final boolean mirror;
    private final UVPair texCoord;
    private final UVPair texScale;
    private final Set<Direction> visibleFaces;

    protected GCubeDefinition(@Nullable String comment, float texCoordU, float texCoordV, float originX, float originY, float originZ, float dimensionX, float dimensionY, float dimensionZ, GCubeDeformation grow, boolean mirror, float texScaleU, float texScaleV, Set<Direction> visibleFaces) {
        this.comment = comment;
        this.texCoord = new UVPair(texCoordU, texCoordV);
        this.origin = new Vector3f(originX, originY, originZ);
        this.dimensions = new Vector3f(dimensionX, dimensionY, dimensionZ);
        this.grow = grow;
        this.mirror = mirror;
        this.texScale = new UVPair(texScaleU, texScaleV);
        this.visibleFaces = visibleFaces;
    }

    public GCube bake(int texWidth, int texHeight) {
        return new GCube((int)this.texCoord.u(), (int)this.texCoord.v(), this.origin.x(), this.origin.y(), this.origin.z(), this.dimensions.x(), this.dimensions.y(), this.dimensions.z(), this.grow.growX, this.grow.growY, this.grow.growZ, this.mirror, (float)texWidth * this.texScale.u(), (float)texHeight * this.texScale.v(), this.visibleFaces);
    }
}