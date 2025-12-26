package com.sang.musicnpc.registry;

import com.sang.musicnpc.MusicNpc;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MusicNpc.MODID);

    public static final RegistryObject<BlockEntityType<MusicPlayerBlockEntity>> MUSIC_PLAYER_BE =
            BLOCK_ENTITIES.register("music_player",
                    () -> BlockEntityType.Builder
                            .of(MusicPlayerBlockEntity::new, ModBlocks.MUSIC_PLAYER.get())
                            .build(null));
}