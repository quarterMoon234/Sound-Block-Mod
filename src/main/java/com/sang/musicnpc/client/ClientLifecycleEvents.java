package com.sang.musicnpc.client;

import com.sang.musicnpc.MusicNpc;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MusicNpc.MODID, value = Dist.CLIENT)
public class ClientLifecycleEvents {

    // 월드 나가기 / 서버 연결 종료 시
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            // stopCurrent() 안에서
            // - currentInstance stop
            // - currentSoundId = null
            // 전부 처리됨
            ClientMusicManager.stopCurrent();
        }
    }
}