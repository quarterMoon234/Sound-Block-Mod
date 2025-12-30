package com.sang.musicnpc;

import com.mojang.logging.LogUtils;
import com.sang.musicnpc.network.ModNetwork;
import com.sang.musicnpc.registry.ModBlockEntities;
import com.sang.musicnpc.registry.ModBlocks;
import com.sang.musicnpc.registry.ModItems;
import com.sang.musicnpc.registry.ModSounds;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraft.world.item.CreativeModeTabs;
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

        // ✅ 1.20.1 Forge에서 크리에이티브 탭에 아이템/블록 넣는 이벤트
        modEventBus.addListener(this::buildCreativeTabContents);

        LOGGER.info("[musicnpc] init");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        ModNetwork.register();
        LOGGER.info("[musicnpc] network registered");
    }

    private void buildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        // ✅ 원하는 탭에 넣기: FUNCTIONAL_BLOCKS(기능 블록)
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            event.accept(ModBlocks.MUSIC_PLAYER.get()); // 블록 아이템이 등록돼 있어야 보임
        }
    }
}
