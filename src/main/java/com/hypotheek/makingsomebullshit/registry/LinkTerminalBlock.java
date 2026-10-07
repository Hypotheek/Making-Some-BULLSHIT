package com.hypotheek.makingsomebullshit.registry;

import com.hypotheek.makingsomebullshit.net.Network;
import com.hypotheek.makingsomebullshit.net.OpenLinksScreenPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;

public class LinkTerminalBlock extends Block {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public LinkTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    // The model's screen is on its north side. The base is the same every way round; the body behind it is turned to each facing.
    private static final VoxelShape BASE = Block.box(1, 0, 1, 15, 3, 15);
    private static final VoxelShape SHAPE_NORTH = Shapes.or(BASE, Block.box(2, 3, 1, 14, 16, 13));
    private static final VoxelShape SHAPE_EAST = Shapes.or(BASE, Block.box(3, 3, 2, 15, 16, 14));
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(BASE, Block.box(2, 3, 3, 14, 16, 15));
    private static final VoxelShape SHAPE_WEST = Shapes.or(BASE, Block.box(1, 3, 2, 13, 16, 14));
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final int AWAKE_TICKS = 100;

    private static final Map<GlobalPos, Long> LAST_WOKEN = new HashMap<>();

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case WEST -> SHAPE_WEST;
            case NORTH -> SHAPE_NORTH;
            case EAST -> SHAPE_EAST;
            default -> SHAPE_SOUTH;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if(level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        if(player instanceof ServerPlayer serverPlayer) {
            wake(serverPlayer.serverLevel(), pos);
            Network.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer), new OpenLinksScreenPacket());
        }
        return InteractionResult.SUCCESS;
    }

    public static void wake(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if(!(state.getBlock() instanceof LinkTerminalBlock)) return;

        LAST_WOKEN.put(GlobalPos.of(level.dimension(), pos), level.getGameTime());
        if(!state.getValue(LIT)) {
            level.setBlock(pos, state.setValue(LIT, true), Block.UPDATE_ALL);
            level.scheduleTick(pos, state.getBlock(), 20);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource source) {
        GlobalPos key = GlobalPos.of(level.dimension(), pos);
        Long woken = LAST_WOKEN.get(key);

        if(woken != null && level.getGameTime() - woken < AWAKE_TICKS) {
            level.scheduleTick(pos, state.getBlock(), 20);
        } else {
            LAST_WOKEN.remove(key);
            level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
        }
    }

}
