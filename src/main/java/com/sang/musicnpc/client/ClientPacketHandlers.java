package com.sang.musicnpc.client;

import com.sang.musicnpc.client.screen.MusicSelectScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public final class ClientPacketHandlers {

    private ClientPacketHandlers() {
    }

    /**
     * 서버 → 클라 : GUI 열기
     */
    public static void openMusicGui(BlockPos pos, String currentKey) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        mc.setScreen(new MusicSelectScreen(pos, currentKey));
    }

    /**
     * 서버 → 클라 : 음악 재생
     */
    public static void playNpcMusic(BlockPos pos, String key) {
        ClientMusicManager.onServerSignal(); // TTL 갱신
        ClientMusicManager.playAt(pos, key);
    }


    /**
     * 서버 → 클라 : 음악 정지
     */
    public static void stopNpcMusic() {
        ClientMusicManager.stopCurrent();
    }

    public static void onServerSignal() {
        // 서버 패킷이 왔다 = 반경 안에 있다 TTL 갱신
        ClientMusicManager.onServerSignal();
    }

}
