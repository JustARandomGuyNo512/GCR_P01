package com.sheridan.gcr.mixin;

import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(VertexBuffer.class)
@OnlyIn(Dist.CLIENT)
public interface VertexBufferAccessor {

    @Accessor("mode")
    VertexFormat.Mode getMode();

    @Invoker("getIndexType")
    VertexFormat.IndexType invokeGetIndexType();
}
