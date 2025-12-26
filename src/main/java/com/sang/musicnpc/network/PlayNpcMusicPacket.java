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

    private final String soundKey;   // ✅ "music_test" 같은 등록명. stop이면 ""
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

            // ✅ 서버에서 신호 받았다는 기록 (inRange 판단)
            ClientMusicManager.onServerSignal();

            // stop
            if (msg.soundKey == null || msg.soundKey.isBlank()) {
                ClientMusicManager.stopCurrent();
                return;
            }

            SoundEvent sound = ModSounds.resolveByKey(msg.soundKey);
            if (sound != null) {
                ClientMusicManager.onServerMusicDesired(sound);

                // ✅ keepAlive든 switch든, 재생이 죽었으면 즉시 복구
                ClientMusicManager.tryRecoverNow();
            }
        }));
        ctx.get().setPacketHandled(true);
    }
}