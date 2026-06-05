package com.sheridan.gcr.network;

import net.neoforged.neoforge.network.handling.IPayloadContext;

public interface IPacket<T> {
    void onClient(T packet, IPayloadContext context);
    void onServer(T packet, IPayloadContext context);
}
