package com.hypotheek.makingsomebullshit.client;

import net.minecraft.client.Minecraft;

public final class ClientHooks {
    private ClientHooks() {}

    public static void openLinksScreen() {
        Minecraft.getInstance().setScreen(new EmcLinksScreen(new PlayerLinkSource()));
    }
}
