package com.hypotheek.makingsomebullshit.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LinkTerminalBlockEntity extends BlockEntity {
    public LinkTerminalBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LINK_TERMINAL.get(), pos, state);
    }
}
