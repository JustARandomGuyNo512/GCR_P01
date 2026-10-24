package com.sheridan.gcr.network;
/** Vanilla 1.20.1 Forge SimpleChannel message handler contract. */
public interface IPacket<T> {
    void onClient(T packet, PacketContext context);
    void onServer(T packet, PacketContext context);
}
