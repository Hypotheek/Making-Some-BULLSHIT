package com.hypotheek.makingsomebullshit.net;

import com.hypotheek.makingsomebullshit.client.ClientHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record OpenLinksScreenPacket() {
    public static void encode(OpenLinksScreenPacket packet, FriendlyByteBuf buf) {
    }

    public static OpenLinksScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenLinksScreenPacket();
    }

    public static void handle(OpenLinksScreenPacket packet, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> ClientHooks::openLinksScreen));
        context.get().setPacketHandled(true);
    }
}
