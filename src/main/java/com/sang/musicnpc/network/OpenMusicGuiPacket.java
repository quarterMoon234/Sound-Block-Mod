package com.sang.musicnpc.network;

import com.sang.musicnpc.client.screen.MusicSelectScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenMusicGuiPacket {

    public final BlockPos pos;
    public final String currentKey;

    public OpenMusicGuiPacket(BlockPos pos, String currentKey) {
        this.pos = pos;
        this.currentKey = currentKey == null ? "" : currentKey;
    }

    public static void encode(OpenMusicGuiPacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.pos);
        buf.writeUtf(msg.currentKey);
    }

    public static OpenMusicGuiPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String key = buf.readUtf();
        return new OpenMusicGuiPacket(pos, key);
    }

    public static void handle(OpenMusicGuiPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc == null) return;
            mc.setScreen(new MusicSelectScreen(msg.pos, msg.currentKey)); // ✅ 생성자 시그니처 일치
        }));
        ctx.get().setPacketHandled(true);
    }
}
