package com.hypotheek.makingsomebullshit.client;

import com.hypotheek.makingsomebullshit.MakingSomeBullshit;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-only entry point. The EMC link overview will hang off this: keybinds, screens, overlays.
 * Nothing in the client package may be touched from common code, or dedicated servers will crash.
 */
@Mod.EventBusSubscriber(modid = MakingSomeBullshit.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    public static final KeyMapping OPEN_LINKS = new KeyMapping(
            "key." + MakingSomeBullshit.MOD_ID + ".open_links",
            InputConstants.KEY_K,
            "key.categories." + MakingSomeBullshit.MOD_ID
    );

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_LINKS);
    }
}
