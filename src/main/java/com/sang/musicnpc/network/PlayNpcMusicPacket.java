package com.sang.musicnpc.network;

import com.sang.musicnpc.client.ClientPacketHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayNpcMusicPacket {

    public final BlockPos pos;
    public final String soundKey;
    public final boolean keepAlive;

    public PlayNpcMusicPacket(BlockPos pos, String soundKey, boolean keepAlive) {
        this.pos = pos;
        this.soundKey = soundKey;
        this.keepAlive = keepAlive;
    }

    // 편의 생성자들 유지
    public PlayNpcMusicPacket(String soundKey) {
        this(BlockPos.ZERO, soundKey, false);
    }

    public PlayNpcMusicPacket(String soundKey, boolean keepAlive) {
        this(BlockPos.ZERO, soundKey, keepAlive);
    }

    public static void encode(PlayNpcMusicPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.soundKey == null ? "" : msg.soundKey);
        buf.writeBoolean(msg.keepAlive);
    }

    public static PlayNpcMusicPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String key = buf.readUtf(32767);
        boolean keepAlive = buf.readBoolean();
        if (key.isEmpty()) key = null;
        return new PlayNpcMusicPacket(pos, key, keepAlive);
    }

    public static void handle(PlayNpcMusicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {

            // 1) 반경 TTL 갱신 (keepAlive든 뭐든 패킷 왔으면 갱신)
            ClientPacketHandlers.onServerSignal();

            // 2) keepAlive=true + soundKey=null => “신호만” 갱신하고 끝
            if (msg.soundKey == null) return;

            // 3) 실제 재생
            ClientPacketHandlers.playNpcMusic(msg.pos, msg.soundKey);
        }));
        c.setPacketHandled(true);
    }
}
