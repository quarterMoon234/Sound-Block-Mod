package com.sang.musicnpc.client;

import com.sang.musicnpc.MusicNpc;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 클라이언트 전용 Tick 처리
 * - 사운드 옵션 변경 감지
 * - 음악 0 → 다시 키웠을 때 강제 재생 복구
 */
@Mod.EventBusSubscriber(
        modid = MusicNpc.MODID,
        value = Dist.CLIENT
)
public class ClientTickHandler {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // ✅ 여기서만 1줄 호출
        ClientMusicManager.clientTick();
    }
}
