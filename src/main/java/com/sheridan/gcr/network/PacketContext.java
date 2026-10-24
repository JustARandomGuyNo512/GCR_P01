package com.sheridan.gcr.network;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

/** Keeps the original packet handler bodies working with Forge's context. */
public final class PacketContext {
    private final NetworkEvent.Context context;
    public PacketContext(NetworkEvent.Context context) { this.context = context; }
    /** Only meaningful for packets received on the logical server. */
    public Player player() { return context.getSender(); }
    public void enqueueWork(Runnable work) { context.enqueueWork(work); }
}
