package com.flechazo.modernfurniture.block;

import com.flechazo.modernfurniture.block.entity.LaptopBlockEntity;
import com.flechazo.modernfurniture.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class LaptopBlock extends Block implements EntityBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape CLOSED_SHAPE = Shapes.or(
            Shapes.box(1 / 16.0, 0 / 16.0, 0.8 / 16.0, 15 / 16.0, 0.5 / 16.0, 10.8 / 16.0),
            Shapes.box(1 / 16.0, 0.5 / 16.0, 0.8 / 16.0, 15 / 16.0, 0.75 / 16.0, 10.8 / 16.0)
    );

    private static final VoxelShape OPEN_SHAPE = Shapes.or(
            Shapes.box(1 / 16.0, 0 / 16.0, 0.8 / 16.0, 15 / 16.0, 0.5 / 16.0, 10.8 / 16.0),
            Shapes.box(1 / 16.0, 0.425 / 16.0, 10.475 / 16.0, 15 / 16.0, 10.425 / 16.0, 10.725 / 16.0)
    );

    public LaptopBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(1.5F, 3.0F)
                .noOcclusion()
                .sound(SoundType.METAL)
        );
        this.registerDefaultState(this.defaultBlockState()
                .setValue(OPEN, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN, FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            boolean isOpen = state.getValue(OPEN);
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof LaptopBlockEntity laptopEntity) {
                laptopEntity.triggerAnimation(!isOpen);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaptopBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean isOpen = state.getValue(OPEN);
        Direction facing = state.getValue(FACING);

        VoxelShape shape = isOpen ? OPEN_SHAPE : CLOSED_SHAPE;

        return VoxelShapeUtils.rotateShape(shape, facing);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return (lvl, pos, st, be) -> {
                if (be instanceof LaptopBlockEntity laptopEntity) {
                    laptopEntity.clientTick();
                }
            };
        }
        return null;
    }
}