package com.sang.musicnpc.network;

import com.sang.musicnpc.registry.MusicPlayerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SetMusicKeyPacket {

    private final BlockPos pos;
    private final String soundKey;

    public SetMusicKeyPacket(BlockPos pos, String soundKey) {
        this.pos = pos;
        this.soundKey = soundKey;
    }

    public static void encode(SetMusicKeyPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.soundKey);
    }

    public static SetMusicKeyPacket decode(FriendlyByteBuf buf) {
        return new SetMusicKeyPacket(
                buf.readBlockPos(),
                buf.readUtf()
        );
    }

    public static void handle(SetMusicKeyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp == null) return;

            if (sp.level().getBlockEntity(msg.pos) instanceof MusicPlayerBlockEntity be) {
                be.setSoundKey(msg.soundKey);
                be.setChanged();

                // ✅ 중요: 저장했으면 "지금 바로 재생"을 클라로 보내기
                ModNetwork.sendToPlayer(sp, new PlayNpcMusicPacket(msg.pos, msg.soundKey, false));
            }
        });
        c.setPacketHandled(true);
    }
}
