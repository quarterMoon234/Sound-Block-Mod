package com.sang.musicnpc.server;

import com.sang.musicnpc.MusicNpc;
import com.sang.musicnpc.network.ModNetwork;
import com.sang.musicnpc.registry.ModBlocks;
import com.sang.musicnpc.registry.MusicPlayerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MusicNpc.MODID)
public class ServerMusicScanner {

    private static final int RADIUS = 64;
    private static final int RADIUS_SQ = RADIUS * RADIUS;

    // 겹침 안정 장치(버퍼 + 쿨다운)
    private static final double SWITCH_ADVANTAGE = 4.0;
    private static final int SWITCH_COOLDOWN_TICKS = 20;

    // ✅ keep-alive: 반경 안에 계속 있으면 2초마다 같은 음악을 "유지 신호"로 다시 보냄
    private static final int KEEPALIVE_TICKS = 40;

    private static class State {
        BlockPos currentSourcePos;
        String currentSoundKey;
        int currentDistSq;
        long lastSwitchGameTime;
        boolean needResync;

        long lastKeepAliveGameTime; // ✅ 추가
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    // 싱글플레이 월드 나가기 -> 플레이어 상태만 초기화 (인덱스 clearAll 절대 금지)
    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!event.getLevel().isClientSide() && event.getLevel() instanceof ServerLevel) {
            STATES.clear();
        }
    }

    @SubscribeEvent
    public static void onJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            State st = STATES.computeIfAbsent(sp.getUUID(), k -> new State());
            st.needResync = true;
        }
    }

    @SubscribeEvent
    public static void onLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            STATES.remove(sp.getUUID());
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        long gameTime = event.getServer().overworld().getGameTime();

        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            ServerLevel level = player.serverLevel();                // ✅ 차원별
            MusicBlockIndex index = MusicBlockIndex.get(level);      // ✅ 차원별 SavedData 인덱스

            State st = STATES.computeIfAbsent(player.getUUID(), k -> new State());

            BestCandidate best = findBestCandidate(level, index, player.blockPosition());

            if (best.found) {
                maybeSwitch(player, st, gameTime, best);
            } else {
                // 후보가 없으면 음악 유지(Stop 안 보냄)
                // 단, 현재 소스가 파괴되었으면 stop
                if (st.currentSourcePos != null && !isMusicBlockAlive(level, st.currentSourcePos)) {
                    sendStop(player);
                    clearState(st, gameTime);
                }
            }
        }
    }

    private static void clearState(State st, long gameTime) {
        st.currentSourcePos = null;
        st.currentSoundKey = null;
        st.currentDistSq = 0;
        st.lastSwitchGameTime = gameTime;
        st.needResync = false;
        st.lastKeepAliveGameTime = gameTime;
    }

    private static void maybeSwitch(ServerPlayer player, State st, long gameTime, BestCandidate best) {

        // ✅ 같은 소스면 "keep-alive" 보내서
        // 음악이 끝났을 때도(반경 안이면) 다시 재생되게 한다
        if (st.currentSourcePos != null && st.currentSourcePos.equals(best.pos)) {
            st.currentDistSq = best.distSq;

            // 접속 직후 강제 1회
            if (st.needResync) {
                forceResync(player, st, gameTime, best);
                return;
            }

            // ✅ keep-alive(1초마다)
            if (gameTime - st.lastKeepAliveGameTime >= KEEPALIVE_TICKS) {
                st.lastKeepAliveGameTime = gameTime;
                ModNetwork.CHANNEL.send(
                        PacketDistributor.PLAYER.with(() -> player),
                        new com.sang.musicnpc.network.PlayNpcMusicPacket(best.soundKey, true) // ✅ keepAlive=true
                );
            }
            return;
        }

        // 쿨다운(블록 전환 안정)
        if (gameTime - st.lastSwitchGameTime < SWITCH_COOLDOWN_TICKS) return;

        // 첫 선택
        if (st.currentSourcePos == null) {
            switchTo(player, st, gameTime, best);
            return;
        }

        // 겹침 안정(버퍼): 새 후보가 충분히 더 가까울 때만 교체
        int advantageSq = (int) (SWITCH_ADVANTAGE * SWITCH_ADVANTAGE);
        if (best.distSq + advantageSq < st.currentDistSq) {
            switchTo(player, st, gameTime, best);
        }
    }

    private static void forceResync(ServerPlayer player, State st, long gameTime, BestCandidate best) {
        if (!isMusicBlockAlive(player.serverLevel(), best.pos)) return;

        st.currentSourcePos = best.pos;
        st.currentSoundKey = best.soundKey;
        st.currentDistSq = best.distSq;
        st.lastSwitchGameTime = gameTime;
        st.needResync = false;
        st.lastKeepAliveGameTime = gameTime;

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new com.sang.musicnpc.network.PlayNpcMusicPacket(best.soundKey, false)
        );
    }

    private static void switchTo(ServerPlayer player, State st, long gameTime, BestCandidate best) {
        if (!isMusicBlockAlive(player.serverLevel(), best.pos)) return;

        // 같은 음악이면 패킷 생략(최적화)
        // (다만 keep-alive가 있으므로, 곡이 끝나면 다시 재생 가능)
        if (best.soundKey != null && best.soundKey.equals(st.currentSoundKey)) {
            st.currentSourcePos = best.pos;
            st.currentDistSq = best.distSq;
            st.lastSwitchGameTime = gameTime;
            st.needResync = false;
            st.lastKeepAliveGameTime = gameTime;
            return;
        }

        st.currentSourcePos = best.pos;
        st.currentSoundKey = best.soundKey;
        st.currentDistSq = best.distSq;
        st.lastSwitchGameTime = gameTime;
        st.needResync = false;
        st.lastKeepAliveGameTime = gameTime;

        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new com.sang.musicnpc.network.PlayNpcMusicPacket(best.soundKey, false)
        );
    }

    private static void sendStop(ServerPlayer player) {
        ModNetwork.CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new com.sang.musicnpc.network.PlayNpcMusicPacket("", false)
        );
    }

    private static boolean isMusicBlockAlive(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.MUSIC_PLAYER.get());
    }

    // ---------------- 후보 탐색(최적화 핵심) ----------------
    private static class BestCandidate {
        boolean found;
        BlockPos pos;
        String soundKey;
        int distSq;
    }

    private static BestCandidate findBestCandidate(ServerLevel level, MusicBlockIndex index, BlockPos playerPos) {
        BestCandidate best = new BestCandidate();
        best.found = false;
        best.distSq = Integer.MAX_VALUE;

        for (long packed : index.iterateNearby(playerPos, RADIUS)) {
            BlockPos p = BlockPos.of(packed);

            // 3D 거리 반경 체크
            int dx = p.getX() - playerPos.getX();
            int dy = p.getY() - playerPos.getY();
            int dz = p.getZ() - playerPos.getZ();
            int dsq = dx * dx + dy * dy + dz * dz;
            if (dsq > RADIUS_SQ) continue;

            // stale 좌표면 제거(운영 안정)
            if (!level.getBlockState(p).is(ModBlocks.MUSIC_PLAYER.get())) {
                index.remove(level, p);
                continue;
            }

            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof MusicPlayerBlockEntity musicBe)) continue;

            if (dsq < best.distSq) {
                best.found = true;
                best.pos = p.immutable();
                best.soundKey = musicBe.getSoundKey();
                best.distSq = dsq;
            }
        }

        return best;
    }

    public static void markNeedResync(ServerPlayer sp) {
        State st = STATES.computeIfAbsent(sp.getUUID(), k -> new State());
        st.needResync = true;
    }
}
