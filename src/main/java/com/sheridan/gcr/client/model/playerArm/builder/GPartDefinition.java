package com.sheridan.gcr.client.model.playerArm.builder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.sheridan.gcr.client.model.playerArm.GCube;
import com.sheridan.gcr.client.model.playerArm.GModelPart;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.minecraft.client.model.geom.PartPose;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Deprecated
@OnlyIn(Dist.CLIENT)
public class GPartDefinition {
    private final List<GCubeDefinition> cubes;
    private final PartPose partPose;
    private final Map<String, GPartDefinition> children = Maps.newHashMap();

    GPartDefinition(List<GCubeDefinition> cubes, PartPose partPose) {
        this.cubes = cubes;
        this.partPose = partPose;
    }

    public GPartDefinition addOrReplaceChild(String name, GCubeListBuilder cubes, PartPose partPose) {
        GPartDefinition partDefinition = new GPartDefinition(cubes.getCubes(), partPose);
        GPartDefinition partDefinition1 = this.children.put(name, partDefinition);
        if (partDefinition1 != null) {
            partDefinition.children.putAll(partDefinition1.children);
        }
        return partDefinition;
    }

    public GModelPart bake(int texWidth, int texHeight) {
        Object2ObjectArrayMap<String, GModelPart> object2objectarraymap =this.children.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, (p_171593_) -> ((GPartDefinition)p_171593_.getValue()).bake(texWidth, texHeight), (p_171595_, p_171596_) -> p_171595_, Object2ObjectArrayMap::new));
        List<GCube> list = this.cubes.stream().map((p_171589_) -> p_171589_.bake(texWidth, texHeight)).collect(ImmutableList.toImmutableList());
        GModelPart modelPart = new GModelPart(list, object2objectarraymap, this.partPose);
        modelPart.loadPose(this.partPose);
        return modelPart;
    }

    public GPartDefinition getChild(String name) {
        return (GPartDefinition)this.children.get(name);
    }
}