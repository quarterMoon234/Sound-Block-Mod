package com.sang.musicnpc.registry;

import com.sang.musicnpc.MusicNpc;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;


public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MusicNpc.MODID);

    public static final RegistryObject<SoundEvent> MUSIC_TEST =
            SOUND_EVENTS.register("music_test",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MusicNpc.MODID, "music_test")));

    public static final RegistryObject<SoundEvent> MUSIC_MAPLE =
            SOUND_EVENTS.register("music_maple",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MusicNpc.MODID, "music_maple")));
}
