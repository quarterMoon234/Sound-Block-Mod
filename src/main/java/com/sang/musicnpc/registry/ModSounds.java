package com.sang.musicnpc.registry;

import com.sang.musicnpc.MusicNpc;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MusicNpc.MODID);

    // ✅ "등록명"은 반드시 언더스코어 추천 (music_test / music_maple)
    // ✅ ResourceLocation은 sounds.json의 "이벤트 키"와 매칭되도록 해도 되지만,
    //    현재 네 프로젝트는 "soundKey = music_test" 흐름이므로 아래처럼 통일함.
    public static final RegistryObject<SoundEvent> MUSIC_TEST =
            SOUND_EVENTS.register("music_test",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MusicNpc.MODID, "music_test")));

    public static final RegistryObject<SoundEvent> MUSIC_MAPLE =
            SOUND_EVENTS.register("music_maple",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MusicNpc.MODID, "music_maple")));

    // ✅ GUI에 뿌릴 목록 (스크롤 대상)
    // 곡 추가할 때 여기만 늘리면 됨.
    public static final List<String> MUSIC_KEYS = List.of(
            "music_test",
            "music_maple"
    );

    // 선택 키 -> SoundEvent resolve (패킷/클라에서 사용)
    public static SoundEvent resolveByKey(String key) {
        if (key == null) return null;
        return switch (key) {
            case "music_test" -> MUSIC_TEST.get();
            case "music_maple" -> MUSIC_MAPLE.get();
            default -> null;
        };
    }
}