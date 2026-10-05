package com.hypotheek.makingsomebullshit.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record LinksSyncPacket(List<LinkSnapshot> links) {
    public static void encode(LinksSyncPacket packet, FriendlyByteBuf buf) {
        buf.writeCollection(packet.links, (b, link) -> link.write(b));
    }

    public static LinksSyncPacket decode(FriendlyByteBuf buf) {
        return new LinksSyncPacket(buf.readCollection(ArrayList::new, LinkSnapshot::read));
    }

    public static void handle(LinksSyncPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> ClientLinkData.setLinks(packet.links));
        context.get().setPacketHandled(true);
    }
}
