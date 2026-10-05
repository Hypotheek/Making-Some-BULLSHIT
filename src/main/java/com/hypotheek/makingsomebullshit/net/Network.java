package com.hypotheek.makingsomebullshit.net;

import com.hypotheek.makingsomebullshit.MakingSomeBullshit;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class Network {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(MakingSomeBullshit.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private Network() {
    }

    public static void register() {
        CHANNEL.registerMessage(0, LinksSyncPacket.class, LinksSyncPacket::encode, LinksSyncPacket::decode, LinksSyncPacket::handle);
        CHANNEL.registerMessage(1, RequestLinksPacket.class, RequestLinksPacket::encode, RequestLinksPacket::decode, RequestLinksPacket::handle);
    }
}
