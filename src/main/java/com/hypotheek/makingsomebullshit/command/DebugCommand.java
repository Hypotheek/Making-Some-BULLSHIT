package com.hypotheek.makingsomebullshit.command;

import com.hypotheek.makingsomebullshit.MakingSomeBullshit;
import com.hypotheek.makingsomebullshit.tracker.LinkSampler;
import com.hypotheek.makingsomebullshit.tracker.LinkTracker;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import cool.furry.mc.forge.projectexpansion.block.entity.BlockEntityEMCLink;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import moze_intel.projecte.api.ProjectEAPI;
import moze_intel.projecte.api.proxy.IEMCProxy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.stream.Collectors;

/** Debug commands for checking what the tracker sees: /bullshit links */
@Mod.EventBusSubscriber(modid = MakingSomeBullshit.MOD_ID)
public final class DebugCommand {
    private DebugCommand() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("bullshit")
                        .then(Commands.literal("links").executes(DebugCommand::links))
                        .then(Commands.literal("items").executes(DebugCommand::items))
                        .then(Commands.literal("rates").executes(DebugCommand::rates))
        );

    }

    private static int links(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        List<BlockEntityEMCLink> links = LinkTracker.getLinks(player);

        Map<Matter, Long> perTier = links.stream().collect(
                Collectors.groupingBy(BlockEntityEMCLink::getMatter, () -> new EnumMap<>(Matter.class), Collectors.counting())
        );

        if(perTier.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No links found"), false);
        }

        perTier.forEach((matter, amount) ->
                source.sendSuccess(() -> Objects.requireNonNull(matter.getEMCLink()).getName().append(" - " + amount), false));
        return links.size();
    }

    private static int items(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        IEMCProxy proxy = IEMCProxy.INSTANCE;
        List<BlockEntityEMCLink> linked = LinkTracker.getLinks(player).stream()
                .filter(link -> !link.itemStack.isEmpty())
                .sorted(Comparator.comparing(BlockEntityEMCLink::getMatter))
                .toList();

        if(linked.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No valid items Linked to EMC links"), false);
        }

        for(BlockEntityEMCLink link : linked) {
            source.sendSuccess(() -> Objects.requireNonNull(link.getMatter().getEMCLink()).getName()
                    .append(" - ")
                    .append(link.itemStack.getHoverName())
                    .append(" (" + link.getBlockPos().toShortString() + ")")
                    .append(" Cost: " + proxy.getValue(link.itemStack) + " EMC per item"), false
            );
        }

        return linked.size();
    }

    private static int rates(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        List<BlockEntityEMCLink> links = LinkTracker.getLinks(player).stream()
                .sorted(Comparator.comparing(BlockEntityEMCLink::getMatter))
                .toList();

        if(links.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No links found"), false);
            return 0;
        }

        for(BlockEntityEMCLink link : links) {
            LinkSampler.Rate rate = LinkSampler.getRate(link);
            source.sendSuccess(() -> Objects.requireNonNull(link.getMatter().getEMCLink()).getName()
                    .append(" (" + link.getBlockPos().toShortString() + ")")
                    .append(" - Spent: " + number(rate.emcSpent()) + " EMC/s")
                    .append(", Gained: " + number(rate.emcGained()) + " EMC/s")
                    .append(", Imported: " + number(rate.itemsImported()) + " items/s"), false
            );
        }

        double totalSpent = links.stream().mapToDouble(link -> LinkSampler.getRate(link).emcSpent()).sum();
        double totalGained = links.stream().mapToDouble(link -> LinkSampler.getRate(link).emcGained()).sum();
        source.sendSuccess(() -> Component.literal("Total - Spent: " + number(totalSpent) + " EMC/s, Gained: " + number(totalGained) + " EMC/s"), false);

        return links.size();
    }

    public static String number(double value) {
        return String.format(Locale.ROOT, "%,.1f", value);
    }
}
