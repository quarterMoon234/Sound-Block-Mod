package com.sang.musicnpc.registry;

import com.sang.musicnpc.MusicNpc;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MusicNpc.MODID);

    public static final RegistryObject<Item> MUSIC_PLAYER_ITEM =
            ITEMS.register("music_player",
                    () -> new BlockItem(ModBlocks.MUSIC_PLAYER.get(), new Item.Properties()));
}