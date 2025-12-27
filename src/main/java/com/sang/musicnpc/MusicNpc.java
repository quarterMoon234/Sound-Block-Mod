package com.sang.musicnpc;

import com.mojang.logging.LogUtils;
import com.sang.musicnpc.network.ModNetwork;
import com.sang.musicnpc.registry.ModBlockEntities;
import com.sang.musicnpc.registry.ModBlocks;
import com.sang.musicnpc.registry.ModItems;
import com.sang.musicnpc.registry.ModSounds;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(MusicNpc.MODID)
public class MusicNpc {
    public static final String MODID = "musicnpc";
    private static final Logger LOGGER = LogUtils.getLogger();

    public MusicNpc() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // ✅ enqueueWork로 미루지 말고 즉시 등록
        ModNetwork.register();
        LOGGER.info("[musicnpc] network registered");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[musicnpc] server starting");
    }
}
