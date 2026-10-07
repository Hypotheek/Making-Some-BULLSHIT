package com.hypotheek.makingsomebullshit;

import com.hypotheek.makingsomebullshit.net.Network;
import com.hypotheek.makingsomebullshit.registry.ModBlockEntities;
import com.hypotheek.makingsomebullshit.registry.ModBlocks;
import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// Must match the modId in META-INF/mods.toml (set via mod_id in gradle.properties).
@Mod(MakingSomeBullshit.MOD_ID)
public class MakingSomeBullshit {
    public static final String MOD_ID = "making_some_bullshit";
    public static final Logger LOGGER = LogUtils.getLogger();

    // get() is deprecated in Forge 47.4+ in favour of constructor injection, which older 47.x builds lack.
    @SuppressWarnings("removal")
    public MakingSomeBullshit() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);

        ModBlocks.BLOCKS.register(modEventBus);
        ModBlocks.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);

        // Game events (as opposed to mod lifecycle events) go on the Forge bus.
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Making Some BULLSHIT loaded");
        Network.register();
    }
}
