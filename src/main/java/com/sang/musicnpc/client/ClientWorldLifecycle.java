package com.sang.musicnpc.client;

import com.sang.musicnpc.MusicNpc;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 클라이언트 월드/서버 연결이 내려갈 때(싱글 월드 나가기, 멀티 서버 연결 종료 등)
 * 남아있는 음악 인스턴스를 정리해서 다음 접속에 꼬이지 않도록 함.
 *
 * ✅ 없어도 동작은 하지만, 있으면 안정성이 크게 올라감.
 */
@Mod.EventBusSubscriber(modid = MusicNpc.MODID, value = Dist.CLIENT)
public class ClientWorldLifecycle {

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        // 클라이언트 레벨이 내려갈 때만 처리
        if (event.getLevel().isClientSide()) {
            ClientMusicManager.stopCurrent(); // ✅ stop + 내부 상태 초기화까지 함께 처리됨
        }
    }
}
