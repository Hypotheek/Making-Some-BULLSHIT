package com.hypotheek.makingsomebullshit.client;

import com.hypotheek.makingsomebullshit.MakingSomeBullshit;
import com.hypotheek.makingsomebullshit.net.ClientLinkData;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/** Client-side game events. Kept apart from ClientSetup because that one is on the mod bus and these are on the Forge bus. */
@Mod.EventBusSubscriber(modid = MakingSomeBullshit.MOD_ID, value = Dist.CLIENT)
public final class ClientEvents {
    private ClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft minecraft = Minecraft.getInstance();
        while (ClientSetup.OPEN_LINKS.consumeClick()) {
            if (minecraft.player != null) {
                minecraft.setScreen(new EmcLinksScreen());
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientLinkData.setLinks(List.of());
    }
}
