package com.sheridan.gcr.client.render.fx.particles;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;
import com.sheridan.gcr.GCR;
import com.sheridan.gcr.client.render.fx.particles.ember.EmberOption;
import com.sheridan.gcr.client.render.fx.particles.explosion.FlashOption;
import com.sheridan.gcr.client.render.fx.particles.explosion.FragmentOption;
import com.sheridan.gcr.client.render.fx.particles.explosion.SparkOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticles {
    private ModParticles() { }
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, GCR.MODID);

    public static final RegistryObject<SimpleParticleType> WATER_PUFF =
            PARTICLE_TYPES.register("water_puff", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> WATER_DROP =
            PARTICLE_TYPES.register("water_drop", () -> new SimpleParticleType(false));
    public static final RegistryObject<SimpleParticleType> WATER_BUBBLE =
            PARTICLE_TYPES.register("water_bubble", () -> new SimpleParticleType(false));

    public static final RegistryObject<ParticleType<FlashOption>> FLASH = PARTICLE_TYPES.register("flash", () ->
        new ParticleType<FlashOption>(true, new ParticleOptions.Deserializer<FlashOption>() {
            public FlashOption fromCommand(ParticleType<FlashOption> type, StringReader reader) throws CommandSyntaxException {
                reader.expect(' '); return new FlashOption(reader.readFloat());
            }
            public FlashOption fromNetwork(ParticleType<FlashOption> type, FriendlyByteBuf buf) {
                return new FlashOption(buf.readFloat());
            }
        }) { public Codec<FlashOption> codec() { return FlashOption.CODEC; } });

    public static final RegistryObject<ParticleType<FragmentOption>> FRAGMENT = PARTICLE_TYPES.register("fragment", () ->
        new ParticleType<FragmentOption>(false, new ParticleOptions.Deserializer<FragmentOption>() {
            public FragmentOption fromCommand(ParticleType<FragmentOption> type, StringReader reader) throws CommandSyntaxException {
                reader.expect(' '); float radius = reader.readFloat();
                reader.expect(' '); int count = reader.readInt();
                reader.expect(' '); float speed = reader.readFloat();
                reader.expect(' '); int color = reader.readInt();
                return new FragmentOption(radius, count, speed, color);
            }
            public FragmentOption fromNetwork(ParticleType<FragmentOption> type, FriendlyByteBuf buf) {
                return new FragmentOption(buf.readFloat(), buf.readInt(), buf.readFloat(), buf.readInt());
            }
        }) { public Codec<FragmentOption> codec() { return FragmentOption.CODEC; } });

    public static final RegistryObject<ParticleType<SparkOption>> SPARK = PARTICLE_TYPES.register("spark", () ->
        new ParticleType<SparkOption>(false, new ParticleOptions.Deserializer<SparkOption>() {
            public SparkOption fromCommand(ParticleType<SparkOption> type, StringReader reader) throws CommandSyntaxException {
                reader.expect(' '); float radius = reader.readFloat();
                reader.expect(' '); int count = reader.readInt();
                reader.expect(' '); float speed = reader.readFloat();
                reader.expect(' '); int startColor = reader.readInt();
                reader.expect(' '); int fadeColor = reader.readInt();
                return new SparkOption(radius, count, speed, startColor, fadeColor);
            }
            public SparkOption fromNetwork(ParticleType<SparkOption> type, FriendlyByteBuf buf) {
                return new SparkOption(buf.readFloat(), buf.readInt(), buf.readFloat(), buf.readInt(), buf.readInt());
            }
        }) { public Codec<SparkOption> codec() { return SparkOption.CODEC; } });

    public static final RegistryObject<ParticleType<EmberOption>> HEAT_SMOKE = PARTICLE_TYPES.register("heat_smoke", () ->
        new ParticleType<EmberOption>(false, new ParticleOptions.Deserializer<EmberOption>() {
            public EmberOption fromCommand(ParticleType<EmberOption> type, StringReader reader) throws CommandSyntaxException {
                reader.expect(' '); int gridSize = reader.readInt();
                reader.expect(' '); String mode = reader.readUnquotedString();
                EmberOption.EasingType easing = EmberOption.EasingType.LINEAR;
                for (EmberOption.EasingType candidate : EmberOption.EasingType.values())
                    if (candidate.getSerializedName().equals(mode)) easing = candidate;
                reader.expect(' '); int lifetime = reader.readInt();
                reader.expect(' '); float scale = reader.readFloat();
                reader.expect(' '); boolean sort = reader.readBoolean();
                return new EmberOption(type, gridSize, easing, lifetime, scale, sort);
            }
            public EmberOption fromNetwork(ParticleType<EmberOption> type, FriendlyByteBuf buf) {
                return EmberOption.fromNetwork(type, buf);
            }
        }) { public Codec<EmberOption> codec() { return EmberOption.createCodec(this); } });
}
