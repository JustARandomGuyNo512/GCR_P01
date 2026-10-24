package com.sheridan.gcr.network.c2s;

import com.sheridan.gcr.GCR;
import com.sheridan.gcr.network.IPacket;
import com.sheridan.gcr.sound.ModSounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import com.sheridan.gcr.network.PacketContext;

public class PlaySoundPacket implements IPacket<PlaySoundPacket> {
    public static final ResourceLocation ID = GCR.RL("play_sound");

    public String soundName;
    public float vol;
    public float pit;
    public double x, y, z;

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(soundName);
        buf.writeFloat(vol);
        buf.writeFloat(pit);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
    }

    public static PlaySoundPacket decode(FriendlyByteBuf buf) {
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
    public void onClient(PlaySoundPacket packet, PacketContext context) {

    }

    @Override
    public void onServer(PlaySoundPacket packet, PacketContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ResourceLocation name = ResourceLocation.tryParse(packet.soundName);
            ModSounds.sound(packet.vol, packet.pit, packet.x, packet.y, packet.z, player, name);
        });
    }}
