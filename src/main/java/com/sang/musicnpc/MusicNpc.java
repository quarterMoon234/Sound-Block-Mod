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

        // ✅ 레지스트리 등록 (예제 코드 전부 제거)
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModSounds.SOUND_EVENTS.register(modEventBus);

        // ✅ 공통 셋업(네트워크 등록 등)
        modEventBus.addListener(this::commonSetup);

        // ✅ Forge 게임 이벤트 버스 등록 (서버 시작 로그 같은 것)
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(ModNetwork::register);
        LOGGER.info("[musicnpc] commonSetup done");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[musicnpc] server starting");
    }
}
