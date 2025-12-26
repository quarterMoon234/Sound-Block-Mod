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
    private final String soundKey;   // 예: "music.test", stop이면 ""
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
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            // ✅ 서버 신호 갱신(반경 안 판단)
            ClientMusicManager.onServerSignal();

            if (msg.soundKey == null || msg.soundKey.isBlank()) {
                ClientMusicManager.stopCurrent();
                return;
            }

            SoundEvent sound = resolveSound(msg.soundKey);
            if (sound != null) {
                // ✅ 여기서 play만 하지 말고, "keepAlive면 복구 시도"까지 하게 한다
                ClientMusicManager.onServerMusicDesired(sound);

                // keepAlive 패킷이든 스위치 패킷이든, 볼륨이 살아있는데 재생이 죽었으면 즉시 복구
                ClientMusicManager.tryRecoverNow();
            }
        }));
        ctx.get().setPacketHandled(true);
    }

    private static SoundEvent resolveSound(String key) {
        if ("music.test".equals(key)) return ModSounds.MUSIC_TEST.get();
        return null;
    }
}
