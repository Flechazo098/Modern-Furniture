package com.flechazo.modernfurniture.block;

import com.flechazo.modernfurniture.block.entity.AbstractAirConditioningBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.shapes.VoxelShape;

public abstract class AbstractAirConditioningBlock extends Block implements EntityBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public AbstractAirConditioningBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.0F, 4.0F)
                .noOcclusion()
                .isViewBlocking((state, world, pos) -> false)
                .sound(SoundType.METAL)
        );
        this.registerDefaultState(this.defaultBlockState()
                .setValue(OPEN, false)
                .setValue(FACING, Direction.NORTH));
    }

    protected abstract VoxelShape getAirConditioningShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context);

    protected abstract void addParticleEffects(BlockState state, Level level, BlockPos pos, RandomSource random);

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
            if (blockEntity instanceof AbstractAirConditioningBlockEntity acEntity) {
                acEntity.triggerAnimation(!isOpen);

                // 当空调被打开时，开始房间检测和计时
                if (!isOpen) {
                    acEntity.startCooling();
                } else {
                    acEntity.stopCooling();
                }
            }
            level.setBlock(pos, state.setValue(OPEN, !isOpen), 3);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(OPEN)) {
            addParticleEffects(state, level, pos, random);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return (lvl, pos, st, be) -> {
                if (be instanceof AbstractAirConditioningBlockEntity acEntity) {
                    acEntity.clientTick();
                }
            };
        } else {
            return (lvl, pos, st, be) -> {
                if (be instanceof AbstractAirConditioningBlockEntity acEntity) {
                    acEntity.serverTick();
                }
            };
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getAirConditioningShape(state, level, pos, context);
    }
}