package com.sang.musicnpc.client;

import com.sang.musicnpc.MusicNpc;
import com.sang.musicnpc.network.ModNetwork;
import com.sang.musicnpc.network.RequestMusicResyncPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        value = Dist.CLIENT,
        modid = MusicNpc.MODID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public class ClientResyncOnJoin {

    private static boolean sent = false;

    /**
     * ✅ 서버 접속 직후 (네트워크 준비 완료)
     * 여기서 보내야 Invalid message 절대 안 터짐
     */
    @SubscribeEvent
    public static void onClientLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (sent) return;
        sent = true;

        ModNetwork.sendToServer(new RequestMusicResyncPacket());
    }

    /**
     * ✅ 서버 나갈 때 초기화
     */
    @SubscribeEvent
    public static void onClientLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        sent = false;
    }
}

