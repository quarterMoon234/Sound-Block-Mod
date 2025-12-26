package com.sang.musicnpc.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

public class ClientMusicManager {

    private static final SoundSource CHANNEL = SoundSource.RECORDS;

    private static SoundInstance currentInstance = null;

    // ✅ 서버가 마지막으로 "원한다고 알려준" 곡
    private static ResourceLocation desiredSoundId = null;

    private static long lastServerSignalClientTime = Long.MIN_VALUE;
    private static final long SIGNAL_TTL_TICKS = 40;

    private static float lastVolume = -1f;

    public static void onServerSignal() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) lastServerSignalClientTime = mc.level.getGameTime();
    }

    /** ✅ 서버가 "이 곡을 틀어라"라고 말했을 때: 원하는 곡만 저장 (볼륨 0이어도 저장) */
    public static void onServerMusicDesired(SoundEvent sound) {
        if (sound == null) return;
        desiredSoundId = sound.getLocation();
    }

    /** (호환) */
    public static void playMusic(SoundEvent sound) {
        play(sound);
    }

    /** 기존 play: 즉시 재생 시도(볼륨 0이면 인스턴스만 정리하고 desired는 유지) */
    public static void play(SoundEvent sound) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        onServerMusicDesired(sound);

        float v = mc.options.getSoundSourceVolume(CHANNEL);
        if (v <= 0.0001f) {
            stopCurrentInstanceOnly();
            return;
        }

        if (desiredSoundId != null && isPlaying() && isCurrentSound(desiredSoundId)) return;

        forceStart(desiredSoundId);
    }

    /** ✅ 핵심: keepAlive를 받았는데 “재생이 죽어있으면” 즉시 복구 */
    public static void tryRecoverNow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        float v = mc.options.getSoundSourceVolume(CHANNEL);
        if (v <= 0.0001f) return;           // 아직 mute면 복구 시도 안 함
        if (desiredSoundId == null) return; // 원하는 곡이 없으면 복구 불가

        // 이미 재생중이면 OK
        if (isPlaying() && isCurrentSound(desiredSoundId)) return;

        // ✅ 재생이 죽었거나 다른 상태면 강제 시작
        forceStart(desiredSoundId);
    }

    /** 매 틱 호출: 볼륨 0->상승 복구 + 반경 안 자동 반복 */
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

        // 반경 안인데, 원하는 곡이 있고, 재생이 죽었으면 복구
        if (desiredSoundId != null && (!isPlaying() || unmutedNow)) {
            if (v > 0.0001f) {
                forceStart(desiredSoundId);
            }
        }
    }

    public static void stopCurrent() {
        stopCurrentInstanceOnly();
        desiredSoundId = null;
    }

    private static void stopCurrentInstanceOnly() {
        Minecraft mc = Minecraft.getInstance();
        if (currentInstance != null && mc != null) {
            mc.getSoundManager().stop(currentInstance);
        }
        currentInstance = null;
    }

    private static boolean isPlaying() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return false;
        if (currentInstance == null) return false;
        return mc.getSoundManager().isActive(currentInstance);
    }

    private static boolean isCurrentSound(ResourceLocation id) {
        if (id == null) return false;
        if (!(currentInstance instanceof AbstractSoundInstance asi)) return false;
        return id.equals(asi.getLocation());
    }

    private static void forceStart(ResourceLocation id) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (id == null) return;

        SoundEvent ev = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(id);
        if (ev == null) {
            stopCurrentInstanceOnly();
            return;
        }

        stopCurrentInstanceOnly();
        currentInstance = new OneShotNonAttenuatedSound(ev, CHANNEL);
        mc.getSoundManager().play(currentInstance);
    }

    private static class OneShotNonAttenuatedSound extends AbstractSoundInstance {
        protected OneShotNonAttenuatedSound(SoundEvent event, SoundSource src) {
            super(event, src, RandomSource.create());
            this.looping = false;
            this.delay = 0;
            this.volume = 1.0f;
            this.pitch = 1.0f;
            this.attenuation = SoundInstance.Attenuation.NONE;

            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.player != null) {
                this.x = mc.player.getX();
                this.y = mc.player.getY();
                this.z = mc.player.getZ();
            }
        }
    }
}
