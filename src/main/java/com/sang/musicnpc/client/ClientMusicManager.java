package com.sang.musicnpc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class ClientMusicManager {

    // ✅ 주크박스/레코드 볼륨 슬라이더를 타게 하려면 RECORDS
    private static final SoundSource CHANNEL = SoundSource.RECORDS;

    // ✅ 여기만 바꾸면 됨
    private static final SoundInstance.Attenuation ATTENUATION_MODE = SoundInstance.Attenuation.NONE;
    // NONE  : 거리 무시(항상 같은 볼륨)
    // LINEAR: 거리감 적용(멀어지면 작아짐)

    private static SoundInstance currentInstance = null;
    private static ResourceLocation currentId = null;

    // ✅ 서버가 “원한다고 알려준” 곡 (볼륨 0이어도 기억)
    private static ResourceLocation desiredSoundId = null;

    private static long lastServerSignalClientTime = Long.MIN_VALUE;
    private static final long SIGNAL_TTL_TICKS = 40;

    private static float lastVolume = -1f;

    /** 서버 패킷이 왔다는 신호(반경 판단용 TTL) */
    public static void onServerSignal() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) lastServerSignalClientTime = mc.level.getGameTime();
    }

    /** 서버가 “이 곡을 틀어라”라고 말했을 때: 원하는 곡만 저장 */
    public static void onServerMusicDesired(SoundEvent sound) {
        if (sound == null) return;
        desiredSoundId = sound.getLocation();
    }

    /** stop(완전 정지) */
    public static void stopCurrent() {
        stopCurrentInstanceOnly();
        desiredSoundId = null;
    }

    /** “인스턴스만” 정지 (desired는 유지) */
    private static void stopCurrentInstanceOnly() {
        Minecraft mc = Minecraft.getInstance();
        if (currentInstance != null && mc != null) {
            mc.getSoundManager().stop(currentInstance);
        }
        currentInstance = null;
        currentId = null;
    }

    private static boolean isPlaying() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return false;
        if (currentInstance == null) return false;
        return mc.getSoundManager().isActive(currentInstance);
    }

    private static boolean isCurrentSound(ResourceLocation id) {
        return id != null && id.equals(currentId);
    }

    /** 서버가 곡을 보냈을 때 즉시 재생 시도 */
    public static void play(SoundEvent sound) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        onServerMusicDesired(sound);

        float v = mc.options.getSoundSourceVolume(CHANNEL);
        if (v <= 0.0001f) {
            // 볼륨 0이면 “원하는 곡”은 저장하고, 인스턴스만 정리
            stopCurrentInstanceOnly();
            return;
        }

        // 이미 같은 곡이 재생중이면 패스
        if (desiredSoundId != null && isPlaying() && isCurrentSound(desiredSoundId)) return;

        // ✅ 여기서 재생
        forceStart(desiredSoundId);
    }

    /** keepAlive나 신호 갱신 시 “죽어있으면” 즉시 복구 */
    public static void tryRecoverNow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        float v = mc.options.getSoundSourceVolume(CHANNEL);
        if (v <= 0.0001f) return;
        if (desiredSoundId == null) return;

        if (isPlaying() && isCurrentSound(desiredSoundId)) return;

        forceStart(desiredSoundId);
    }

    /** 매 틱 호출: 볼륨 0->상승 복구 + 반경 안 자동 반복/복구 */
    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        long now = mc.level.getGameTime();
        boolean inRange = (now - lastServerSignalClientTime) <= SIGNAL_TTL_TICKS;

        float v = mc.options.getSoundSourceVolume(CHANNEL);
        if (lastVolume < 0f) lastVolume = v;

        boolean unmutedNow = (lastVolume <= 0.0001f && v > 0.0001f);
        lastVolume = v;

        if (!inRange) return;

        if (desiredSoundId != null && (unmutedNow || !isPlaying())) {
            if (v > 0.0001f) forceStart(desiredSoundId);
        }
    }

    /** ✅ 핵심: SimpleSoundInstance.forJukebox 없이 직접 사운드 인스턴스 생성 */
    private static void forceStart(ResourceLocation id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (id == null) return;

        SoundEvent ev = BuiltInRegistries.SOUND_EVENT.get(id);
        if (ev == null) {
            stopCurrentInstanceOnly();
            return;
        }

        stopCurrentInstanceOnly();

        // ✅ “레코드 채널 + 감쇠 방식”을 직접 설정한 인스턴스
        currentInstance = new OneShotSound(ev, CHANNEL, ATTENUATION_MODE);
        currentId = id;

        mc.getSoundManager().play(currentInstance);
    }

    /** ✅ 1.20.1 안전: AbstractSoundInstance로 직접 구현 */
    private static class OneShotSound extends AbstractSoundInstance {
        protected OneShotSound(SoundEvent event, SoundSource src, SoundInstance.Attenuation attenuationMode) {
            super(event, src, RandomSource.create());
            this.looping = false;
            this.delay = 0;
            this.volume = 1.0f;
            this.pitch = 1.0f;
            this.attenuation = attenuationMode;

            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                // 플레이어 위치 기준(원하면 블록 위치로 바꿀 수도 있음)
                this.x = mc.player.getX();
                this.y = mc.player.getY();
                this.z = mc.player.getZ();
            }
        }
    }
}
