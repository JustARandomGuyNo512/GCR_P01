package com.sheridan.gcr.client.model.playerArm.builder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Deprecated
@OnlyIn(Dist.CLIENT)
public class GCubeListBuilder {
    private static final Set<Direction> ALL_VISIBLE = EnumSet.allOf(Direction.class);
    private final List<GCubeDefinition> cubes = Lists.newArrayList();
    private int xTexOffs;
    private int yTexOffs;
    private boolean mirror;

    public GCubeListBuilder texOffs(int xTexOffs, int yTexOffs) {
        this.xTexOffs = xTexOffs;
        this.yTexOffs = yTexOffs;
        return this;
    }

    public GCubeListBuilder mirror() {
        return this.mirror(true);
    }

    public GCubeListBuilder mirror(boolean mirror) {
        this.mirror = mirror;
        return this;
    }

    public GCubeListBuilder addBox(String comment, float originX, float originY, float originZ, int dimensionX, int dimensionY, int dimensionZ, GCubeDeformation cubeDeformation, int xTexOffs, int yTexOffs) {
        this.texOffs(xTexOffs, yTexOffs);
        this.cubes.add(new GCubeDefinition(comment, (float)this.xTexOffs, (float)this.yTexOffs, originX, originY, originZ, (float)dimensionX, (float)dimensionY, (float)dimensionZ, cubeDeformation, this.mirror, 1.0F, 1.0F, ALL_VISIBLE));
        return this;
    }

    public GCubeListBuilder addBox(String comment, float originX, float originY, float originZ, int dimensionX, int dimensionY, int dimensionZ, int xTexOffs, int yTexOffs) {
        this.texOffs(xTexOffs, yTexOffs);
        this.cubes.add(new GCubeDefinition(comment, (float)this.xTexOffs, (float)this.yTexOffs, originX, originY, originZ, (float)dimensionX, (float)dimensionY, (float)dimensionZ, GCubeDeformation.NONE, this.mirror, 1.0F, 1.0F, ALL_VISIBLE));
        return this;
    }

    public GCubeListBuilder addBox(float originX, float originY, float originZ, float dimensionX, float dimensionY, float dimensionZ) {
        this.cubes.add(new GCubeDefinition((String)null, (float)this.xTexOffs, (float)this.yTexOffs, originX, originY, originZ, dimensionX, dimensionY, dimensionZ, GCubeDeformation.NONE, this.mirror, 1.0F, 1.0F, ALL_VISIBLE));
        return this;
    }

    public GCubeListBuilder addBox(float originX, float originY, float originZ, float dimensionX, float dimensionY, float dimensionZ, Set<Direction> visibleFaces) {
        this.cubes.add(new GCubeDefinition((String)null, (float)this.xTexOffs, (float)this.yTexOffs, originX, originY, originZ, dimensionX, dimensionY, dimensionZ, GCubeDeformation.NONE, this.mirror, 1.0F, 1.0F, visibleFaces));
        return this;
    }

    public GCubeListBuilder addBox(String comment, float originX, float originY, float originZ, float dimensionX, float dimensionY, float dimensionZ) {
        this.cubes.add(new GCubeDefinition(comment, (float)this.xTexOffs, (float)this.yTexOffs, originX, originY, originZ, dimensionX, dimensionY, dimensionZ, GCubeDeformation.NONE, this.mirror, 1.0F, 1.0F, ALL_VISIBLE));
        return this;
    }

    public GCubeListBuilder addBox(String comment, float originX, float originY, float originZ, float dimensionX, float dimensionY, float dimensionZ, GCubeDeformation cubeDeformation) {
        this.cubes.add(new GCubeDefinition(comment, (float)this.xTexOffs, (float)this.yTexOffs, originX, originY, originZ, dimensionX, dimensionY, dimensionZ, cubeDeformation, this.mirror, 1.0F, 1.0F, ALL_VISIBLE));
        return this;
    }

    public GCubeListBuilder addBox(float originX, float originY, float originZ, float dimensionX, float dimensionY, float dimensionZ, boolean mirror) {
        this.cubes.add(new GCubeDefinition((String)null, (float)this.xTexOffs, (float)this.yTexOffs, originX, originY, originZ, dimensionX, dimensionY, dimensionZ, GCubeDeformation.NONE, mirror, 1.0F, 1.0F, ALL_VISIBLE));
        return this;
    }

    public GCubeListBuilder addBox(float originX, float originY, float originZ, float dimensionX, float dimensionY, float dimensionZ, GCubeDeformation cubeDeformation, float texScaleU, float texScaleV) {
        this.cubes.add(new GCubeDefinition((String)null, (float)this.xTexOffs, (float)this.yTexOffs, originX, originY, originZ, dimensionX, dimensionY, dimensionZ, cubeDeformation, this.mirror, texScaleU, texScaleV, ALL_VISIBLE));
        return this;
    }

    public GCubeListBuilder addBox(float originX, float originY, float originZ, float dimensionX, float dimensionY, float dimensionZ, GCubeDeformation cubeDeformation) {
        this.cubes.add(new GCubeDefinition((String)null, (float)this.xTexOffs, (float)this.yTexOffs, originX, originY, originZ, dimensionX, dimensionY, dimensionZ, cubeDeformation, this.mirror, 1.0F, 1.0F, ALL_VISIBLE));
        return this;
    }

    public List<GCubeDefinition> getCubes() {
        return ImmutableList.copyOf(this.cubes);
    }

    public static GCubeListBuilder create() {
        return new GCubeListBuilder();
    }
}
