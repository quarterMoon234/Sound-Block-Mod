package com.sang.musicnpc.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RequestMusicResyncPacket {

    public RequestMusicResyncPacket() {}

    public static void encode(RequestMusicResyncPacket msg, FriendlyByteBuf buf) {
        // 보낼 데이터 없음
    }

    public static RequestMusicResyncPacket decode(FriendlyByteBuf buf) {
        return new RequestMusicResyncPacket();
    }

    public static void handle(RequestMusicResyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp == null) return;

            // ✅ 서버 쪽 스캐너에게 "이 플레이어는 1회 강제 재전송" 표시
            com.sang.musicnpc.server.ServerMusicScanner.markNeedResync(sp);
        });
        c.setPacketHandled(true);
    }
}

