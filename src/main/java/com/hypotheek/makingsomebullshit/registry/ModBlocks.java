package com.hypotheek.makingsomebullshit.registry;

import com.hypotheek.makingsomebullshit.MakingSomeBullshit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, MakingSomeBullshit.MOD_ID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, MakingSomeBullshit.MOD_ID);

    public static final RegistryObject<Block> LINK_TERMINAL = BLOCKS.register("link_terminal", () -> new
            LinkTerminalBlock(Block.Properties.of().strength(2.0f).requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> state.getValue(LinkTerminalBlock.LIT) ? 15 : 0)));

    public static final RegistryObject<Item> LINK_TERMINAL_ITEM = ITEMS.register("link_terminal", () -> new
            BlockItem(LINK_TERMINAL.get(), new Item.Properties()));
}
