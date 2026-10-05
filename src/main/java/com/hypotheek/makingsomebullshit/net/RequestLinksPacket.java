package com.hypotheek.makingsomebullshit.net;

import com.hypotheek.makingsomebullshit.tracker.LinkTracker;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityEMCLink;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

/** Client to server: "send me my links again". Carries no data. */
public record RequestLinksPacket() {
    public static void encode(RequestLinksPacket packet, FriendlyByteBuf buf) {
    }

    public static RequestLinksPacket decode(FriendlyByteBuf buf) {
        return new RequestLinksPacket();
    }

    public static void handle(RequestLinksPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) return;

            List<LinkSnapshot> links = LinkTracker.getLinks(player).stream()
                    .sorted(Comparator.comparing(BlockEntityEMCLink::getMatter).thenComparingLong(link -> link.getBlockPos().asLong()))
                    .map(LinkSnapshot::of)
                    .toList();
            Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new LinksSyncPacket(links));
        });
        context.get().setPacketHandled(true);
    }
}
