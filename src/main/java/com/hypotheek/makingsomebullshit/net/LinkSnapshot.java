package com.hypotheek.makingsomebullshit.net;

import com.hypotheek.makingsomebullshit.tracker.LinkSampler;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

public record LinkSnapshot(String tier, BlockPos pos, ItemStack itemStack, double emcSpent, double emcGain, double itemsImported) {
    public static LinkSnapshot of(BlockEntityEMCLink link) {
        LinkSampler.Rate rate = LinkSampler.getRate(link);
        return new LinkSnapshot(link.getMatter().name, link.getBlockPos(), link.itemStack.copy(), rate.emcSpent(), rate.emcGained(), rate.itemsImported());
    }

    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(tier); buf.writeBlockPos(pos); buf.writeItem(itemStack); buf.writeDouble(emcSpent); buf.writeDouble(emcGain); buf.writeDouble(itemsImported);
    }

    public static LinkSnapshot read(FriendlyByteBuf buf) {
        return new LinkSnapshot(buf.readUtf(), buf.readBlockPos(), buf.readItem(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
