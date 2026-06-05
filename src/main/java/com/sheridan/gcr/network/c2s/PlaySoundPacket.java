package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.network.Codec;
import com.sheridan.gcr.network.IPacket;
import com.sheridan.gcr.sound.ModSounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class PlaySoundPacket  implements CustomPacketPayload, IPacket<PlaySoundPacket> {
    public static final ResourceLocation ID = GCR.RL("play_sound");
    public static final Type<PlaySoundPacket> TYPE = new Type<>(ID);
    public static final Codec<PlaySoundPacket> STREAM_CODEC = new Codec<> (
            PlaySoundPacket::decode,
            (buf, p) -> p.encode(buf));

    public String soundName;
    public float vol;
    public float pit;
    public double x, y, z;

    private void encode(FriendlyByteBuf buf) {
        buf.writeUtf(soundName);
        buf.writeFloat(vol);
        buf.writeFloat(pit);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    private static PlaySoundPacket decode(FriendlyByteBuf buf) {
        return new PlaySoundPacket(
                buf.readUtf(),
                buf.readFloat(),
                buf.readFloat(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble());
    }

    public PlaySoundPacket(String soundName, float vol, float pit, double x, double y, double z) {
        this.soundName = soundName;
        this.vol = vol;
        this.pit = pit;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void onClient(PlaySoundPacket packet, IPayloadContext context) {

    }

    @Override
    public void onServer(PlaySoundPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ResourceLocation name = ResourceLocation.tryParse(packet.soundName);
            ModSounds.sound(packet.vol, packet.pit, packet.x, packet.y, packet.z, player, name);
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
