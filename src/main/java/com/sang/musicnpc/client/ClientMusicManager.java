package com.sang.musicnpc.client;

import com.sang.musicnpc.MusicNpc;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class ClientMusicManager {

    // ✅ 주크박스/레코드 볼륨 슬라이더를 타게 하려면 RECORDS
    private static final SoundSource CHANNEL = SoundSource.RECORDS;

    // ✅ 거리감 적용 여부
    // NONE  : 거리 무시(항상 같은 볼륨)
    // LINEAR: 거리감 적용(멀어지면 작아짐)
    private static final SoundInstance.Attenuation ATTENUATION_MODE = SoundInstance.Attenuation.NONE;

    private static SoundInstance currentInstance = null;
    private static ResourceLocation currentId = null;

    // ✅ 서버가 “원한다고 알려준” 곡 (볼륨 0이어도 기억)
    private static ResourceLocation desiredSoundId = null;

    // ✅ "서버 신호" TTL (반경 안에 있다는 신호)
    private static long lastServerSignalClientTime = Long.MIN_VALUE;
    private static final long SIGNAL_TTL_TICKS = 40;

    private static float lastVolume = -1f;

    /**
     * 서버 패킷이 왔다는 신호(반경 판단용 TTL)
     */
    public static void onServerSignal() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.level != null) {
            lastServerSignalClientTime = mc.level.getGameTime();
        }
    }

    /**
     * stop(완전 정지)
     */
    public static void stopCurrent() {
        stopCurrentInstanceOnly();
        desiredSoundId = null;
    }

    /**
     * stopCurrent 라는 이름으로 호출하는 코드가 있으면 이것도 제공
     */
    public static void stopCurrentLegacy() {
        stopCurrent();
    }

    /**
     * “인스턴스만” 정지 (desired는 유지)
     */
    private static void stopCurrentInstanceOnly() {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && currentInstance != null) {
            mc.getSoundManager().stop(currentInstance);
        }
        currentInstance = null;
        currentId = null;
    }

    private static boolean isPlaying() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;
        if (currentInstance == null) return false;
        return mc.getSoundManager().isActive(currentInstance);
    }

    private static boolean isCurrentSound(ResourceLocation id) {
        return id != null && id.equals(currentId);
    }

    public static void playAt(BlockPos pos, String key) {
        if (key == null || key.isBlank()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null || mc.player == null) return;

        // ✅ 핵심: 문자열 → SoundEvent 변환
        ResourceLocation id = new ResourceLocation("musicnpc", key);
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.get(id);

        if (sound == null) {
            System.err.println("[musicnpc] SoundEvent not found: " + id);
            return;
        }

        // 서버 신호 TTL 갱신
        onServerSignal();

        // 기존 play 로직 사용
        play(sound);
    }


    /**
     * 서버가 그냥 "이 사운드 틀어라"만 준 경우(위치 상관 없이)
     * 필요하면 사용.
     */
    public static void play(SoundEvent sound) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;
        if (sound == null) return;

        desiredSoundId = sound.getLocation();

        float v = mc.options.getSoundSourceVolume(CHANNEL);
        if (v <= 0.0001f) {
            stopCurrentInstanceOnly();
            return;
        }

        if (isPlaying() && isCurrentSound(desiredSoundId)) return;

        stopCurrentInstanceOnly();

        currentInstance = new PositionedOneShotSound(
                sound, CHANNEL, ATTENUATION_MODE,
                mc.player.getX(), mc.player.getY(), mc.player.getZ()
        );
        currentId = desiredSoundId;

        mc.getSoundManager().play(currentInstance);
    }

    /**
     * keepAlive나 신호 갱신 시 “죽어있으면” 즉시 복구
     */
    public static void tryRecoverNow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;

        float v = mc.options.getSoundSourceVolume(CHANNEL);
        if (v <= 0.0001f) return;
        if (desiredSoundId == null) return;

        if (isPlaying() && isCurrentSound(desiredSoundId)) return;

        // ✅ 복구 시에는 플레이어 위치에서라도 재생
        SoundEvent ev = BuiltInRegistries.SOUND_EVENT.get(desiredSoundId);
        if (ev == null) return;

        stopCurrentInstanceOnly();
        currentInstance = new PositionedOneShotSound(
                ev, CHANNEL, ATTENUATION_MODE,
                mc.player.getX(), mc.player.getY(), mc.player.getZ()
        );
        currentId = desiredSoundId;
        mc.getSoundManager().play(currentInstance);
    }

    /**
     * 매 틱 호출: 볼륨 0->상승 복구 + 반경 안 자동 반복/복구
     */
    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null || mc.level == null) return;

        long now = mc.level.getGameTime();
        boolean inRange = (now - lastServerSignalClientTime) <= SIGNAL_TTL_TICKS;

        float v = mc.options.getSoundSourceVolume(CHANNEL);
        if (lastVolume < 0f) lastVolume = v;

        boolean unmutedNow = (lastVolume <= 0.0001f && v > 0.0001f);
        lastVolume = v;

        if (!inRange) return;

        if (desiredSoundId != null && (unmutedNow || !isPlaying())) {
            if (v > 0.0001f) {
                tryRecoverNow();
            }
        }
    }

    /**
     * key 문자열을 ResourceLocation으로 안정적으로 파싱
     */
    private static ResourceLocation parseKeyToId(String key) {
        // 1) 이미 "namespace:path" 형태면 그대로
        try {
            ResourceLocation id = new ResourceLocation(key);
            return id;
        } catch (Exception ignored) {
        }

        // 2) "cat" 같은 경우 -> musicnpc:cat 로도 한번 시도(원하면 minecraft로 바꿔도 됨)
        try {
            return new ResourceLocation(MusicNpc.MODID, key);
        } catch (Exception ignored) {
        }

        return null;
    }

    /**
     * ✅ 1.20.1 안전: AbstractSoundInstance로 직접 구현
     */
    private static class PositionedOneShotSound extends AbstractSoundInstance {
        protected PositionedOneShotSound(SoundEvent event, SoundSource src, SoundInstance.Attenuation attenuationMode,
                                         double x, double y, double z) {
            super(event, src, RandomSource.create());
            this.looping = false;
            this.delay = 0;
            this.volume = 1.0f;
            this.pitch = 1.0f;
            this.attenuation = attenuationMode;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

}
