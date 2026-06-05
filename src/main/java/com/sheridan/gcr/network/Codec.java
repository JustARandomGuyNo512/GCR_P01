package com.sheridan.gcr.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class Codec<P> implements StreamCodec<FriendlyByteBuf, P> {
    private final Function<FriendlyByteBuf, P> decoder;
    private final Encoder<P> encoder;

    public Codec(Function<FriendlyByteBuf, P> decoder, Encoder<P> encoder) {
        this.decoder = decoder;
        this.encoder = encoder;
    }

    @Override
    public @NotNull P decode(@NotNull FriendlyByteBuf friendlyByteBuf) {
        return decoder.apply(friendlyByteBuf);
    }

    @Override
    public void encode(@NotNull FriendlyByteBuf o, @NotNull P p) {
        encoder.encode(o, p);
    }


    public interface Encoder<P> {
        void encode(FriendlyByteBuf buf, P p);
    }
}
