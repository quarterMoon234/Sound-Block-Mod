package com.sang.musicnpc.client;

import com.sang.musicnpc.MusicNpc;
import com.sang.musicnpc.network.ModNetwork;
import com.sang.musicnpc.network.RequestMusicResyncPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MusicNpc.MODID, value = Dist.CLIENT)
public class ClientResyncOnJoin {

    private static boolean sent = false;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            sent = false; // 메인메뉴로 나가면 다시 보내도록 초기화
            return;
        }

        if (!sent) {
            sent = true;
            ModNetwork.CHANNEL.sendToServer(new RequestMusicResyncPacket());
        }
    }
}
