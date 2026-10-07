package com.hypotheek.makingsomebullshit.client;

import com.hypotheek.makingsomebullshit.MakingSomeBullshit;
import com.hypotheek.makingsomebullshit.registry.ModBlockEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MakingSomeBullshit.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value= Dist.CLIENT)
public final class ClientSetup {
    private ClientSetup() {}

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event) {
        event.register(LinkTerminalRenderer.ORB_ON);
        event.register(LinkTerminalRenderer.ORB_OFF);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.LINK_TERMINAL.get(), LinkTerminalRenderer::new);
    }
}
