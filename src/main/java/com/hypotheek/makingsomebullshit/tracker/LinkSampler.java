package com.hypotheek.makingsomebullshit.tracker;

import com.hypotheek.makingsomebullshit.MakingSomeBullshit;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Measures how much each tracked EMC link is moving. A link stores no usage statistics, so this watches its
 * per-second limit counters every tick and works out how fast they are being used up.
 */
@Mod.EventBusSubscriber(modid = MakingSomeBullshit.MOD_ID)
public final class LinkSampler {
    private static final Map<BlockEntityEMCLink, Stats> STATS = new ConcurrentHashMap<>();

    private LinkSampler() {
    }

    /**
     * What a link has been doing, per second, averaged over the last few seconds.
     * emcImported is what the imported items were worth. The link doesn't record that itself, so it is
     * reported by EmcLinkItemHandlerMixin. emcReceived is EMC pushed into the link by something like a relay;
     * power flowers never go through a link.
     * Fluid cost is the exported volume at the per-mB price. The link rounds every drain up to a whole EMC,
     * and the counters don't show how many drains made up that volume, so real spending can be slightly
     * higher, most noticeably for fluids that are cheap per mB and drained in small amounts.
     */
    public record Rate(double itemsExported, double fluidMbExported, double itemsImported, double emcSpent, double emcReceived, double emcImported) {
        static final Rate NONE = new Rate(0, 0, 0, 0, 0, 0);

        /** All the EMC that came in through the link. */
        public double emcGained() {
            return emcReceived + emcImported;
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Collection<BlockEntityEMCLink> links = LinkTracker.allLinks();
        STATS.keySet().retainAll(links);
        for (BlockEntityEMCLink link : links) {
            STATS.computeIfAbsent(link, key -> new Stats()).sample(link);
        }
    }

    @SubscribeEvent
    public static void onServerShutdown(ServerStoppedEvent event) {
        STATS.clear();
    }

    public static Rate getRate(BlockEntityEMCLink link) {
        Stats stats = STATS.get(link);
        return stats == null ? Rate.NONE : stats.rate(link);
    }

    /** Called by EmcLinkItemHandlerMixin each time a link credits its owner for imported items. */
    public static void recordImportedEmc(BlockEntityEMCLink link, BigInteger emc) {
        STATS.computeIfAbsent(link, key -> new Stats()).pendingImportedEmc.addAndGet(clamp(emc));
    }

    private static long clamp(BigInteger value) {
        return value.bitLength() < 64 ? value.longValue() : Long.MAX_VALUE;
    }

    /** Mirrors the private FluidHandler.getFluidCostPer in BlockEntityEMCLink: EMC per mB of the linked bucket's fluid. */
    private static double fluidCostPerMb(BlockEntityEMCLink link) {
        if (!(link.itemStack.getItem() instanceof BucketItem)) return 0;

        long fullCost = IEMCProxy.INSTANCE.getValue(link.itemStack);
        long bucketCost = IEMCProxy.INSTANCE.getValue(Items.BUCKET);
        if (fullCost == 0 && bucketCost == 0) return 0;

        return (fullCost - ((bucketCost * link.getMatter().getFluidEfficiencyPercentage()) / 100F)) / 1000D;
    }

    private static final class Stats {
        private final Meter itemsExported = new Meter();
        private final Meter fluidExported = new Meter();
        private final Meter itemsImported = new Meter();
        private final Meter emcReceived = new Meter();
        private final Meter emcImported = new Meter();
        private final AtomicLong pendingImportedEmc = new AtomicLong();

        void sample(BlockEntityEMCLink link) {
            Matter matter = link.getMatter();
            int itemLimit = matter.getEMCLinkItemLimit();

            itemsExported.sample(link.remainingExport, itemLimit);
            itemsImported.sample(link.remainingImport, itemLimit);
            fluidExported.sample(link.remainingFluid, matter.getEMCLinkFluidLimit());
            emcReceived.sample(clamp(link.remainingEMC), clamp(matter.getEMCLinkEMCLimit()));
            emcImported.add(pendingImportedEmc.getAndSet(0));
        }

        Rate rate(BlockEntityEMCLink link) {
            double items = itemsExported.perSecond();
            double fluidMb = fluidExported.perSecond();
            double itemValue = link.itemStack.isEmpty() ? 0 : IEMCProxy.INSTANCE.getValue(link.itemStack);
            double emcSpent = items * itemValue + fluidMb * fluidCostPerMb(link);

            return new Rate(items, fluidMb, itemsImported.perSecond(), emcSpent, emcReceived.perSecond(), emcImported.perSecond());
        }
    }
}
