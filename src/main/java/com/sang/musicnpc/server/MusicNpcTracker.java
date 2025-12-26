package com.sang.musicnpc.server;

import com.sang.musicnpc.network.ModNetwork;
import com.sang.musicnpc.network.PlayNpcMusicPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MusicNpcTracker {

    private static final double RADIUS = 64.0;
    private static final double SWITCH_ADVANTAGE = 4.0; // 버퍼: 4블럭 더 가까워야 교체
    private static final int SWITCH_COOLDOWN_TICKS = 20;

    private static class State {
        UUID currentNpcId;
        double currentDistance;
        String currentSoundKey;
        long lastSwitchTick;
    }

    private static final Map<UUID, State> PLAYER_STATE = new HashMap<>();

    /**
     * 매 틱마다 "이 플레이어에게 지금 어떤 NPC 음악이 적절한가"를 판단해 call.
     * npcId/dist/soundKey는 "현재 반경 내 후보들 중 최적 후보"를 넣어주면 됨.
     * 후보가 없으면 npcId=null로 call.
     */
    public static void update(ServerPlayer player, long gameTime, UUID bestNpcId, double bestDist, String bestSoundKey) {
        State st = PLAYER_STATE.computeIfAbsent(player.getUUID(), k -> new State());

        // 후보 없음 -> 아무것도 안 함(요구사항상 나가도 계속 재생이므로 stop 안 함)
        if (bestNpcId == null) return;

        // 처음 재생
        if (st.currentNpcId == null) {
            switchTo(player, st, gameTime, bestNpcId, bestDist, bestSoundKey);
            return;
        }

        // 같은 NPC면 유지 (거리만 갱신)
        if (bestNpcId.equals(st.currentNpcId)) {
            st.currentDistance = bestDist;
            return;
        }

        // 쿨다운(너무 자주 교체 방지)
        if (gameTime - st.lastSwitchTick < SWITCH_COOLDOWN_TICKS) {
            return;
        }

        // 버퍼 조건: 새 후보가 "충분히 더 가까울 때"만 교체
        if (bestDist + SWITCH_ADVANTAGE < st.currentDistance) {
            switchTo(player, st, gameTime, bestNpcId, bestDist, bestSoundKey);
        }
    }

    private static void switchTo(ServerPlayer player, State st, long gameTime, UUID npcId, double dist, String soundKey) {
        // 같은 음악이면 굳이 패킷 안 보내도 됨(클라에서도 중복 방지하지만, 서버도 최적화)
        if (soundKey != null && soundKey.equals(st.currentSoundKey)) {
            st.currentNpcId = npcId;
            st.currentDistance = dist;
            st.lastSwitchTick = gameTime;
            return;
        }

        st.currentNpcId = npcId;
        st.currentDistance = dist;
        st.currentSoundKey = soundKey;
        st.lastSwitchTick = gameTime;

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new PlayNpcMusicPacket(soundKey)
        );
    }
}
