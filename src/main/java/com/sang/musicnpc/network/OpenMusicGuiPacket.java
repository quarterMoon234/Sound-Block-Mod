package com.sang.musicnpc.network;


import com.sang.musicnpc.client.screen.MusicSelectScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class OpenMusicGuiPacket {

    private final BlockPos pos;
    private final String currentKey;

    public OpenMusicGuiPacket(BlockPos pos, String currentKey) {
        this.pos = pos;
        this.currentKey = currentKey == null ? "music_test" : currentKey;
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
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;
            mc.setScreen(new MusicSelectScreen(msg.pos, msg.currentKey));
        });
        ctx.get().setPacketHandled(true);
    }
}