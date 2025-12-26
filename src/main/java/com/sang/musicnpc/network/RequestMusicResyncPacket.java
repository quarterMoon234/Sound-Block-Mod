package com.sang.musicnpc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestMusicResyncPacket {

    public static void encode(RequestMusicResyncPacket msg, FriendlyByteBuf buf) { }
    public static RequestMusicResyncPacket decode(FriendlyByteBuf buf) { return new RequestMusicResyncPacket(); }

    public static void handle(RequestMusicResyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp == null) return;

            // 서버 스캐너의 상태를 "resync 필요"로 표시
            com.sang.musicnpc.server.ServerMusicScanner.markNeedResync(sp);
        });
        ctx.get().setPacketHandled(true);
    }
}
