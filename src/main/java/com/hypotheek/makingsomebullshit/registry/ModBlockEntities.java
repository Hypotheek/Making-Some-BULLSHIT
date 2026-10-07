package com.hypotheek.makingsomebullshit.registry;

import com.hypotheek.makingsomebullshit.MakingSomeBullshit;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MakingSomeBullshit.MOD_ID);

    public static final RegistryObject<BlockEntityType<LinkTerminalBlockEntity>> LINK_TERMINAL =
            BLOCK_ENTITIES.register("link_terminal", () ->
                    BlockEntityType.Builder.of(LinkTerminalBlockEntity::new, ModBlocks.LINK_TERMINAL.get()).build(null));
}
