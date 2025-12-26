package com.sang.musicnpc.network;

import com.sang.musicnpc.MusicNpc;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MusicNpc.MODID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals
    );

    public static void register() {
        int id = 0;

        CHANNEL.messageBuilder(PlayNpcMusicPacket.class, id++)
                .encoder(PlayNpcMusicPacket::encode)
                .decoder(PlayNpcMusicPacket::decode)
                .consumerMainThread(PlayNpcMusicPacket::handle)
                .add();

        CHANNEL.messageBuilder(RequestMusicResyncPacket.class, id++)
                .encoder(RequestMusicResyncPacket::encode)
                .decoder(RequestMusicResyncPacket::decode)
                .consumerMainThread(RequestMusicResyncPacket::handle)
                .add();
    }
}
