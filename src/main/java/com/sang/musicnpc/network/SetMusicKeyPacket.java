package com.sang.musicnpc.network;

import com.sang.musicnpc.registry.MusicPlayerBlockEntity;
import com.sang.musicnpc.server.ServerMusicScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
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
        return new SetMusicKeyPacket(buf.readBlockPos(), buf.readUtf());
    }

    public static void handle(SetMusicKeyPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> {
            ServerPlayer sp = c.getSender();
            if (sp == null) return;

            BlockEntity be = sp.level().getBlockEntity(msg.pos);
            if (!(be instanceof MusicPlayerBlockEntity musicBe)) return;

            // 1) 선택 저장
            musicBe.setSoundKey(msg.soundKey);
            musicBe.setChanged(); // 저장 표시

            // 2) ✅ 핵심: 현재 반경 안에 있든 말든 "다음 서버틱에 무조건 재전송"
            ServerMusicScanner.markNeedResync(sp);
        });
        c.setPacketHandled(true);
    }
}
