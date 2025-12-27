package com.sang.musicnpc.network;

import com.sang.musicnpc.MusicNpc;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {

    private static final String PROTOCOL = "1";
    private static boolean REGISTERED = false;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MusicNpc.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        if (REGISTERED) return;     // ✅ 중복 등록 방지
        REGISTERED = true;

        int id = 0;

        CHANNEL.messageBuilder(PlayNpcMusicPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(PlayNpcMusicPacket::encode)
                .decoder(PlayNpcMusicPacket::decode)
                .consumerMainThread(PlayNpcMusicPacket::handle)
                .add();

        CHANNEL.messageBuilder(OpenMusicGuiPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(OpenMusicGuiPacket::encode)
                .decoder(OpenMusicGuiPacket::decode)
                .consumerMainThread(OpenMusicGuiPacket::handle)
                .add();

        CHANNEL.messageBuilder(SetMusicKeyPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetMusicKeyPacket::encode)
                .decoder(SetMusicKeyPacket::decode)
                .consumerMainThread(SetMusicKeyPacket::handle)
                .add();

        CHANNEL.messageBuilder(RequestMusicResyncPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestMusicResyncPacket::encode)
                .decoder(RequestMusicResyncPacket::decode)
                .consumerMainThread(RequestMusicResyncPacket::handle)
                .add();
    }

    public static void sendToServer(Object msg) {
        CHANNEL.sendToServer(msg);
    }

    public static void sendToPlayer(ServerPlayer sp, Object msg) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> sp), msg);
    }
}
