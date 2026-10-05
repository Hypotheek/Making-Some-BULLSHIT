package com.hypotheek.makingsomebullshit.tracker;

import com.hypotheek.makingsomebullshit.MakingSomeBullshit;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityEMCLink;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps an index of the EMC links in loaded chunks, since Project Expansion has no registry of them.
 * These events fire on the client as well, so only index server-side levels.
 */
@Mod.EventBusSubscriber(modid = MakingSomeBullshit.MOD_ID)
public final class LinkTracker {
    private static final Set<BlockEntityEMCLink> LINKS = ConcurrentHashMap.newKeySet();

    private LinkTracker() {
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getChunk() instanceof LevelChunk chunk) || event.getLevel().isClientSide()) {
            return;
        }

        for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
            if (blockEntity instanceof BlockEntityEMCLink link) LINKS.add(link);
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        ChunkPos chunkPos = event.getChunk().getPos();
        LINKS.removeIf(link -> link.getLevel() == event.getLevel() && new ChunkPos(link.getBlockPos()).equals(chunkPos));
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide()) return;

        if (event.getLevel().getBlockEntity(event.getPos()) instanceof BlockEntityEMCLink link) {
            LINKS.add(link);
        }

    }

    @SubscribeEvent
    public static void onServerShutdown(ServerStoppedEvent event) {
        LINKS.clear();
    }

    static Collection<BlockEntityEMCLink> allLinks() {
        LINKS.removeIf(BlockEntity::isRemoved);

        return LINKS;
    }

    public static List<BlockEntityEMCLink> getLinks(ServerPlayer player) {
        LINKS.removeIf(BlockEntity::isRemoved);

        return LINKS.stream()
                .filter(link -> link.owner.equals(player.getUUID()))
                .toList();
    }
}
