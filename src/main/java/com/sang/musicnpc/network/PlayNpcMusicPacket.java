package com.sang.musicnpc.network;

import com.sang.musicnpc.client.ClientMusicManager;
import com.sang.musicnpc.registry.ModSounds;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayNpcMusicPacket {

    private final String soundKey;   // 예: "music_test", stop이면 ""
    private final boolean keepAlive;

    public PlayNpcMusicPacket(String soundKey) {
        this(soundKey, false);
    }

    public PlayNpcMusicPacket(String soundKey, boolean keepAlive) {
        this.soundKey = soundKey;
        this.keepAlive = keepAlive;
    }

    public static void encode(PlayNpcMusicPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.soundKey == null ? "" : msg.soundKey);
        buf.writeBoolean(msg.keepAlive);
    }

    public static PlayNpcMusicPacket decode(FriendlyByteBuf buf) {
        String key = buf.readUtf();
        boolean keepAlive = buf.readBoolean();
        return new PlayNpcMusicPacket(key, keepAlive);
    }

    public static void handle(PlayNpcMusicPacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context c = ctx.get();
        c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            // ✅ 반경 신호 갱신
            ClientMusicManager.onServerSignal();

            // stop
            if (msg.soundKey == null || msg.soundKey.isBlank()) {
                ClientMusicManager.stopCurrent();
                return;
            }

            SoundEvent sound = resolveSound(msg.soundKey);
            if (sound == null) return;

            // ✅ 원하는 곡 저장
            ClientMusicManager.onServerMusicDesired(sound);

            // ✅ keepAlive든 스위치든 “실제 재생”을 시도
            //    (볼륨 0이면 인스턴스만 정리되고 desired는 유지됨)
            ClientMusicManager.play(sound);

            // ✅ 볼륨이 살아있는데 재생이 죽었으면 즉시 복구
            if (msg.keepAlive) {
                ClientMusicManager.tryRecoverNow();
            }
        }));
        c.setPacketHandled(true);
    }

    private static SoundEvent resolveSound(String key) {
        return switch (key) {
            case "music_test" -> ModSounds.MUSIC_TEST.get();
            case "music_maple" -> ModSounds.MUSIC_MAPLE.get();
            default -> null;
        };
    }
}
