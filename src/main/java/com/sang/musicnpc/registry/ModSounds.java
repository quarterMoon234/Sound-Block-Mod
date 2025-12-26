package com.sang.musicnpc.registry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import static com.sang.musicnpc.MusicNpc.MODID;


public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, MODID);

    public static final RegistryObject<SoundEvent> MUSIC_TEST =
            SOUND_EVENTS.register("music.test",
                    () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(MODID, "music.test")));
}
