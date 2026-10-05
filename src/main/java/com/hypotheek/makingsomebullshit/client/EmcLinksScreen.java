package com.hypotheek.makingsomebullshit.client;

import com.hypotheek.makingsomebullshit.net.ClientLinkData;
import com.hypotheek.makingsomebullshit.net.LinkSnapshot;
import com.hypotheek.makingsomebullshit.net.Network;
import com.hypotheek.makingsomebullshit.net.RequestLinksPacket;
import com.mojang.blaze3d.platform.NativeImage;
import cool.furry.mc.forge.projectexpansion.block.BlockEMCLink;
import cool.furry.mc.forge.projectexpansion.util.EMCFormat;
import cool.furry.mc.forge.projectexpansion.util.Matter;
import moze_intel.projecte.api.capabilities.IKnowledgeProvider;
import moze_intel.projecte.api.capabilities.PECapabilities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import static com.hypotheek.makingsomebullshit.command.DebugCommand.number;

public class EmcLinksScreen extends Screen {
    private static final int REFRESH_TICKS = 20;
    private static final int HISTORY_SIZE = 60;

    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_MARGIN = 10;
    private static final int PAD = 10;
    private static final int TITLE_HEIGHT = 18;
    private static final int ROW_HEIGHT = 20;
    private static final int INNER_WIDTH = PANEL_WIDTH - 2 * PAD;

    private static final int STRIPE_WIDTH = 3;
    private static final int NAME_X = 8;
    private static final int ITEM_X = 150;
    private static final int SPENT_X = 184;
    private static final int GAINED_X = 272;
    private static final int COLUMN_WIDTH = 76;

    private static final int PANEL_COLOR = 0xE6101018;
    private static final int BORDER_COLOR = 0xFF4B4B7A;
    private static final int TITLE_FROM = 0xFF4A2F9E;
    private static final int TITLE_TO = 0xFF1A1A3C;
    private static final int TRACK_COLOR = 0x40FFFFFF;
    private static final int HOVER_COLOR = 0x30FFFFFF;
    private static final int SPENT_COLOR = 0xFFFF5555;
    private static final int GAINED_COLOR = 0xFF55FF55;
    private static final int DIM = 0xFFAAAAAA;

    private enum Sort { TIER, ITEM, SPENT, GAINED }

    private final Deque<Double> netHistory = new ArrayDeque<>();
    private final Map<String, Integer> tierColors = new HashMap<>();
    private int ticksUntilRefresh;
    private double showNet;

    private Sort sort = Sort.TIER;
    private boolean descending;
    private int scroll;

    private List<LinkSnapshot> sortedCache = List.of();
    private List<LinkSnapshot> sortedSource;
    private Sort sortedBy;
    private boolean sortedDescending;

    private int headerY;
    private int innerLeft;
    private int rowsTop;
    private int visibleRows;
    private List<Component> graphTooltip;

    public EmcLinksScreen() {
        super(Component.literal("EMC Links"));
    }

    @Override
    protected void init() {
        requestLinks();
    }

    private void requestLinks() {
        Network.CHANNEL.sendToServer(new RequestLinksPacket());
        ticksUntilRefresh = REFRESH_TICKS;
    }

    @Override
    public void tick() {
        List<LinkSnapshot> links = ClientLinkData.getLinks();
        double net = totalGained(links) - totalSpent(links);
        showNet += (net - showNet) * 0.3;

        if (--ticksUntilRefresh <= 0) {
            requestLinks();
            netHistory.addLast(net);
            while (netHistory.size() > HISTORY_SIZE) netHistory.removeFirst();
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        List<LinkSnapshot> links = sorted();
        int panelHeight = height - 2 * PANEL_MARGIN;
        int left = (width - PANEL_WIDTH) / 2;
        int top = PANEL_MARGIN;
        int bottom = top + panelHeight;
        innerLeft = left + PAD;

        graphics.fill(left, top, left + PANEL_WIDTH, bottom, PANEL_COLOR);
        graphics.fillGradient(left + 1, top + 1, left + PANEL_WIDTH - 1, top + TITLE_HEIGHT, TITLE_FROM, TITLE_TO);
        graphics.renderOutline(left, top, PANEL_WIDTH, panelHeight, BORDER_COLOR);
        graphics.drawCenteredString(font, title.copy().withStyle(style -> style.withBold(true)), left + PANEL_WIDTH / 2, top + 4, 0xFFFFFFFF);

        double spent = totalSpent(links);
        double gained = totalGained(links);
        double net = gained - spent;
        BigInteger balance = balance();
        int summaryY = top + TITLE_HEIGHT + 8;

        int netColor = net >= 0 ? GAINED_COLOR : SPENT_COLOR;
        graphics.drawString(font, font.plainSubstrByWidth("Balance: " + emc(new BigDecimal(balance)), 140), innerLeft, summaryY, 0xFFFFFFFF);
        graphics.drawString(font, "Spent: " + compact(spent) + "/s", innerLeft + 150, summaryY, SPENT_COLOR);
        graphics.drawString(font, "Gained: " + compact(gained) + "/s", innerLeft + 272, summaryY, GAINED_COLOR);

        graphics.drawString(font, "Runway: " + runway(net, balance), innerLeft, summaryY + 12, DIM);
        graphics.drawString(font, "Net: " + (net >= 0 ? "+" : "-") + compact(Math.abs(net)) + " EMC/s", innerLeft + 150, summaryY + 12, netColor);

        int graphY = summaryY + 24;
        int graphHeight = 28;
        drawGraph(graphics, innerLeft, graphY, INNER_WIDTH, graphHeight, mouseX, mouseY);

        headerY = graphY + graphHeight + 4;
        drawHeader(graphics, "Link", innerLeft + NAME_X, headerY, Sort.TIER, mouseX, mouseY, NAME_X - 4, ITEM_X - NAME_X);
        graphics.drawString(font, "Item", innerLeft + ITEM_X, headerY, DIM);
        drawHeader(graphics, "Spent / s", innerLeft + SPENT_X, headerY, Sort.SPENT, mouseX, mouseY, SPENT_X, COLUMN_WIDTH);
        drawHeader(graphics, "Gained / s", innerLeft + GAINED_X, headerY, Sort.GAINED, mouseX, mouseY, GAINED_X, COLUMN_WIDTH);
        graphics.fill(innerLeft, headerY + 11, innerLeft + INNER_WIDTH, headerY + 12, TRACK_COLOR);

        rowsTop = headerY + 12;
        int rowsBottom = bottom - 34;
        visibleRows = Math.max(1, (rowsBottom - rowsTop) / ROW_HEIGHT);
        scroll = Mth.clamp(scroll, 0, Math.max(0, links.size() - visibleRows));

        double maxSpent = links.stream().mapToDouble(LinkSnapshot::emcSpent).max().orElse(1);
        double maxGained = links.stream().mapToDouble(LinkSnapshot::emcGain).max().orElse(1);
        LinkSnapshot hovered = null;

        if (links.isEmpty()) {
            graphics.drawCenteredString(font, "No links found >:(", width / 2, rowsTop + 20, DIM);
        }

        graphics.enableScissor(innerLeft, rowsTop, innerLeft + INNER_WIDTH, rowsBottom);
        for(int i = scroll; i < Math.min(links.size(), scroll + visibleRows); i++) {
            LinkSnapshot link = links.get(i);
            int rowY = rowsTop + (i - scroll) * ROW_HEIGHT;
            boolean hover = mouseY >= rowY && mouseY < rowY + ROW_HEIGHT;

            boolean rowHovered = mouseX >= left && mouseX < left + PANEL_WIDTH && mouseY >= rowY && mouseY < rowY + ROW_HEIGHT && mouseY < rowsBottom;
            if (rowHovered) {
                graphics.fill(left, rowY, left + PANEL_WIDTH, rowY + ROW_HEIGHT, HOVER_COLOR);
                hovered = link;
            }

            graphics.fill(innerLeft, rowY, innerLeft + STRIPE_WIDTH, rowY + ROW_HEIGHT, tierColor(link.tier()));
            graphics.drawString(font, font.plainSubstrByWidth(tierName(link).getString(), ITEM_X - NAME_X - 4), innerLeft + NAME_X, rowY + 4, 0xFFFFFF);
            graphics.renderItem(link.itemStack(), innerLeft + ITEM_X, rowY + 2);
            drawFlow(graphics, link.emcSpent(), maxSpent, innerLeft + SPENT_X, rowY, SPENT_COLOR);
            drawFlow(graphics, link.emcGain(), maxGained, innerLeft + GAINED_X, rowY, GAINED_COLOR);
        }
        graphics.disableScissor();

        if (links.size() > visibleRows) {
            int trackHeight = rowsBottom - rowsTop;
            int thumbHeight = Math.max(10, trackHeight * visibleRows / links.size());
            int thumbY = rowsTop + (trackHeight - thumbHeight) * scroll / Math.max(1, links.size() - visibleRows);
            graphics.fill(left + PANEL_WIDTH - 5, rowsTop, left + PANEL_WIDTH - 3, rowsBottom, TRACK_COLOR);
            graphics.fill(left + PANEL_WIDTH - 5, thumbY, left + PANEL_WIDTH - 3, thumbY + thumbHeight, 0xFFB0B0FF);
        }

        int totalsY = bottom - 28;
        graphics.fill(innerLeft, totalsY - 4, innerLeft + INNER_WIDTH, totalsY - 3, TRACK_COLOR);
        graphics.drawString(font, "Total  (" + links.size() + " links)", innerLeft + NAME_X, totalsY, 0xFFFFFF);
        graphics.drawString(font, compact(spent), innerLeft + SPENT_X + COLUMN_WIDTH - font.width(compact(spent)), totalsY, SPENT_COLOR);
        graphics.drawString(font, compact(gained), innerLeft + GAINED_X + COLUMN_WIDTH - font.width(compact(gained)), totalsY, GAINED_COLOR);
        graphics.drawCenteredString(font, "Click a header to sort  -  Scroll  -  Shift: exact numbers", width / 2, bottom - 14, 0xFF707080);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (graphTooltip != null) graphics.renderComponentTooltip(font, graphTooltip, mouseX, mouseY);

        if (hovered != null) {
            List<Component> lines = new ArrayList<>();
            lines.add(tierName(hovered).copy().withStyle(ChatFormatting.BOLD));
            lines.add(Component.literal("Exports: ").withStyle(ChatFormatting.GRAY).append(hovered.itemStack().isEmpty() ? Component.literal("nothing set") : hovered.itemStack().getHoverName()));
            lines.add(Component.literal("Position: " + hovered.pos().toShortString()).withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("Spent: " + exact(hovered.emcSpent()) + " EMC/s").withStyle(ChatFormatting.RED));
            lines.add(Component.literal("Gained: " + exact(hovered.emcGain()) + " EMC/s").withStyle(ChatFormatting.GREEN));
            lines.add(Component.literal("Imported: " + exact(hovered.itemsImported()) + " items/s").withStyle(ChatFormatting.GRAY));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }


    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseY >= headerY && mouseY < headerY + 11) {
            Sort clicked = null;
            if (inColumn(mouseX, NAME_X - 4, ITEM_X - NAME_X)) clicked = Sort.TIER;
            else if (inColumn(mouseX, SPENT_X, COLUMN_WIDTH)) clicked = Sort.SPENT;
            else if (inColumn(mouseX, GAINED_X, COLUMN_WIDTH)) clicked = Sort.GAINED;

            if (clicked != null) {
                if (sort == clicked) {
                    descending = !descending;
                } else {
                    sort = clicked;
                    descending = clicked != Sort.TIER;
                }
                scroll = 0;
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0f));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        scroll = Math.max(0, scroll - (int) Math.signum(delta));
        return true;
    }

    private boolean inColumn(double mouseX, int x, int columnWidth) {
        return mouseX >= innerLeft + x && mouseX < innerLeft + x + columnWidth;
    }

    private void drawHeader(GuiGraphics graphics, String label, int x, int y, Sort column, int mouseX, int mouseY, int columnX, int columnWidth) {
        boolean hover = mouseY >= y && mouseY < y + 11 && inColumn(mouseX, columnX, columnWidth);
        String arrow = sort == column ? (descending ? " v" : " ^") : "";
        graphics.drawString(font, label + arrow, x, y, sort == column ? 0xFFFFFF : hover ? 0xFFDDDDFF : DIM);
    }

    private void drawFlow(GuiGraphics graphics, double value, double max, int x, int rowY, int color) {
        String text = compact(value);
        graphics.drawString(font, text, x + COLUMN_WIDTH - font.width(text), rowY + 3, value > 0 ? color : 0xFF707080);
        graphics.fill(x, rowY + 14, x + COLUMN_WIDTH, rowY + 16, TRACK_COLOR);
        if (value > 0) graphics.fill(x, rowY + 14, x + Math.max(1, (int) Math.round(value / max * COLUMN_WIDTH)), rowY + 16, color);
    }

    private void drawGraph(GuiGraphics graphics, int x, int y, int graphWidth, int graphHeight, int mouseX, int mouseY) {
        graphTooltip = null;
        graphics.fill(x, y, x + graphWidth, y + graphHeight, 0x50000000);
        int mid = y + graphHeight / 2;
        graphics.fill(x, mid, x + graphWidth, mid + 1, TRACK_COLOR);

        double max = 1;
        for (double value : netHistory) max = Math.max(max, Math.abs(value));
        int barSlot = graphWidth / HISTORY_SIZE;
        int index = 0;
        int count = netHistory.size();
        for (double value : netHistory) {
            int barHeight = (int) Math.round(Math.abs(value) / max * (graphHeight / 2 - 1));
            int barX = x + graphWidth - (count - index) * barSlot;
            if (value >= 0) graphics.fill(barX, mid - barHeight, barX + barSlot - 1, mid, 0xC055FF55);
            else graphics.fill(barX, mid + 1, barX + barSlot - 1, mid + 1 + barHeight, 0xC0FF5555);
            if (mouseX >= barX && mouseX < barX + barSlot && mouseY >= y && mouseY < y + graphHeight) {
                graphics.fill(barX, y, barX + barSlot - 1, y + graphHeight, HOVER_COLOR);
                graphTooltip = List.of(
                        Component.literal("Net " + (value >= 0 ? "+" : "-") + compact(Math.abs(value)) + " EMC/s").withStyle(value >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED),
                        Component.literal((count - 1 - index) + " s ago").withStyle(ChatFormatting.GRAY));
            }
            index++;
        }
        if (graphTooltip == null && mouseX >= x && mouseX < x + graphWidth && mouseY >= y && mouseY < y + graphHeight) {
            graphTooltip = List.of(Component.literal("Net EMC per second").withStyle(ChatFormatting.WHITE), Component.literal("one bar per second, last 60 s").withStyle(ChatFormatting.GRAY));
        }
    }

    private List<LinkSnapshot> sorted() {
        List<LinkSnapshot> links = ClientLinkData.getLinks();
        if (links == sortedSource && sort == sortedBy && descending == sortedDescending) return sortedCache;

        Comparator<LinkSnapshot> comparator = switch (sort) {
            case TIER -> Comparator.comparingInt(EmcLinksScreen::tierOrder);
            case ITEM -> Comparator.comparing(link -> link.itemStack().getHoverName().getString());
            case SPENT -> Comparator.comparingDouble(LinkSnapshot::emcSpent);
            case GAINED -> Comparator.comparingDouble(LinkSnapshot::emcGain);
        };
        if (descending) comparator = comparator.reversed();

        sortedCache = links.stream().sorted(comparator).toList();
        sortedSource = links;
        sortedBy = sort;
        sortedDescending = descending;
        return sortedCache;
    }

    private static double totalSpent(List<LinkSnapshot> links) {
        return links.stream().mapToDouble(LinkSnapshot::emcSpent).sum();
    }

    private static double totalGained(List<LinkSnapshot> links) {
        return links.stream().mapToDouble(LinkSnapshot::emcGain).sum();
    }

    private static BigInteger balance() {
        if (Minecraft.getInstance().player == null) return BigInteger.ZERO;
        return Minecraft.getInstance().player.getCapability(PECapabilities.KNOWLEDGE_CAPABILITY).map(IKnowledgeProvider::getEmc).orElse(BigInteger.ZERO);
    }

    private static int tierOrder(LinkSnapshot link) {
        try {
            return Matter.valueOf(link.tier().toUpperCase(Locale.ROOT)).ordinal();
        } catch (IllegalArgumentException e) {
            return Integer.MAX_VALUE;
        }
    }

    private int tierColor(String tier) {
        return tierColors.computeIfAbsent(tier, EmcLinksScreen::sampleTierColor);
    }

    /**
     * The colour of a tier, taken from its link texture as the game has it loaded right now. That follows
     * resource packs and KubeJS overrides, so it is right whether or not a pack recoloured the tiers.
     */
    private static int sampleTierColor(String tier) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath("projectexpansion", "textures/block/emc_link/" + tier + ".png");
        try (InputStream stream = Minecraft.getInstance().getResourceManager().open(texture); NativeImage image = NativeImage.read(stream)) {
            List<int[]> pixels = new ArrayList<>();
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    if ((abgr >>> 24) < 200) continue;
                    pixels.add(new int[]{abgr & 0xFF, (abgr >> 8) & 0xFF, (abgr >> 16) & 0xFF});
                }
            }
            if (pixels.isEmpty()) return fallbackTierColor(tier);

            // The most vivid quarter of the pixels carries the tier's colour; the rest is grey frame.
            pixels.sort(Comparator.<int[]>comparingDouble(EmcLinksScreen::vividness).reversed());
            int count = Math.max(1, pixels.size() / 4);
            int red = 0;
            int green = 0;
            int blue = 0;
            for (int i = 0; i < count; i++) {
                red += pixels.get(i)[0];
                green += pixels.get(i)[1];
                blue += pixels.get(i)[2];
            }
            red /= count;
            green /= count;
            blue /= count;

            // Keep dark colours readable on the dark panel.
            double lift = Math.max(1.0, 166.0 / Math.max(1, Math.max(red, Math.max(green, blue))));
            return 0xFF000000 | Math.min(255, (int) (red * lift)) << 16 | Math.min(255, (int) (green * lift)) << 8 | Math.min(255, (int) (blue * lift));
        } catch (IOException e) {
            return fallbackTierColor(tier);
        }
    }

    private static double vividness(int[] rgb) {
        return (Math.max(rgb[0], Math.max(rgb[1], rgb[2])) - Math.min(rgb[0], Math.min(rgb[1], rgb[2]))) / 255.0;
    }

    private static int fallbackTierColor(String tier) {
        try {
            Supplier<MapColor> color = Matter.valueOf(tier.toUpperCase(Locale.ROOT)).mapColor;
            if (color != null) return 0xFF000000 | color.get().col;
        } catch (IllegalArgumentException ignored) {
        }
        return 0xFFFFFFFF;
    }

    private static Component tierName(LinkSnapshot link) {
        try {
            BlockEMCLink block = Matter.valueOf(link.tier().toUpperCase(Locale.ROOT)).getEMCLink();
            if (block != null) return block.getName();
        } catch (IllegalArgumentException ignored) {
        }
        return Component.literal(link.tier());
    }

    private static String emc(BigDecimal value) {
        return EMCFormat.format(value);
    }

    private static String exact(double value) {
        return String.format(Locale.ROOT, "%,.1f", value);
    }

    /** 1.2K, 3.4M, ...; holding Shift shows the exact figure instead. */
    private static String compact(double value) {
        if (Screen.hasShiftDown()) return exact(value);

        String[] units = {"", "K", "M", "B", "T", "Qa"};
        double scaled = Math.abs(value);
        int unit = 0;
        while (scaled >= 1000 && unit < units.length - 1) {
            scaled /= 1000;
            unit++;
        }
        String sign = value < 0 ? "-" : "";
        String pattern = scaled >= 100 || (unit == 0 && scaled == Math.floor(scaled)) ? "%.0f" : "%.1f";
        return sign + String.format(Locale.ROOT, pattern, scaled) + units[unit];
    }

    private static String runway(double net, BigInteger balance) {
        if (net >= -0.5 || balance.signum() <= 0) return "stable";
        return duration(new BigDecimal(balance).doubleValue() / -net);
    }

    private static String duration(double seconds) {
        if (seconds >= 86400L * 3650) return "10+ years";
        long total = (long) seconds;
        if (total < 60) return total + "s";
        if (total < 3600) return total / 60 + "m " + total % 60 + "s";
        if (total < 86400) return total / 3600 + "h " + total % 3600 / 60 + "m";
        return total / 86400 + "d " + total % 86400 / 3600 + "h";
    }

}