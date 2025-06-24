package com.flechazo.modernfurniture.block;

import com.flechazo.modernfurniture.block.entity.AirConditioningBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;
import java.util.stream.Stream;

public class AirConditioningBlock extends Block implements EntityBlock {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape BASE_SHAPE = Stream.of(
            Block.box(-4, 10.11864, 12.4122, 20, 11.11864, 12.6622),  // 叶片部分
            Block.box(-4, 10.5, 11.75, 20, 16, 12.5),                 // 前面板
            Block.box(-4, 10, 12.5, 20, 16, 16)                       // 主体
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    public AirConditioningBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.0F, 4.0F)
                .noOcclusion()
                .isViewBlocking((state, world, pos) -> false) // 确保不阻挡视野
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
            if (blockEntity instanceof AirConditioningBlockEntity acEntity) {
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
            // 发射霜降粒子效果
            Direction facing = state.getValue(FACING);
            double x = pos.getX() + 0.5;
            double y = pos.getY() + 0.3;
            double z = pos.getZ() + 0.5;

            // 根据朝向调整粒子发射位置
            switch (facing) {
                case NORTH -> z -= 0.4;
                case SOUTH -> z += 0.4;
                case WEST -> x -= 0.4;
                case EAST -> x += 0.4;
            }

            // 发射多个粒子
            for (int i = 0; i < 3; i++) {
                double offsetX = (random.nextDouble() - 0.5) * 0.3;
                double offsetY = (random.nextDouble() - 0.5) * 0.2;
                double offsetZ = (random.nextDouble() - 0.5) * 0.3;

                level.addParticle(ParticleTypes.SNOWFLAKE,
                        x + offsetX, y + offsetY, z + offsetZ,
                        0.0, -0.1, 0.0);
            }
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return (lvl, pos, st, be) -> {
                if (be instanceof AirConditioningBlockEntity acEntity) {
                    acEntity.clientTick();
                }
            };
        } else {
            // 服务端ticker用于处理制冷逻辑
            return (lvl, pos, st, be) -> {
                if (be instanceof AirConditioningBlockEntity acEntity) {
                    acEntity.serverTick();
                }
            };
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AirConditioningBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return rotateShape(BASE_SHAPE, facing);
    }


    private VoxelShape rotateShape(VoxelShape shape, Direction facing) {
        return switch (facing) {
            case SOUTH -> {
                VoxelShape[] rotatedShapes = shape.toAabbs().stream()
                        .map(aabb -> Shapes.create(
                                1 - aabb.maxX, aabb.minY, 1 - aabb.maxZ,
                                1 - aabb.minX, aabb.maxY, 1 - aabb.minZ
                        ))
                        .toArray(VoxelShape[]::new);
                yield rotatedShapes.length > 0 ?
                        Arrays.stream(rotatedShapes).reduce(Shapes.empty(), Shapes::or) : Shapes.empty();
            }
            case WEST -> {
                VoxelShape[] rotatedShapes = shape.toAabbs().stream()
                        .map(aabb -> Shapes.create(
                                aabb.minZ, aabb.minY, 1 - aabb.maxX,
                                aabb.maxZ, aabb.maxY, 1 - aabb.minX
                        ))
                        .toArray(VoxelShape[]::new);
                yield rotatedShapes.length > 0 ?
                        Arrays.stream(rotatedShapes).reduce(Shapes.empty(), Shapes::or) : Shapes.empty();
            }
            case EAST -> {
                VoxelShape[] rotatedShapes = shape.toAabbs().stream()
                        .map(aabb -> Shapes.create(
                                1 - aabb.maxZ, aabb.minY, aabb.minX,
                                1 - aabb.minZ, aabb.maxY, aabb.maxX
                        ))
                        .toArray(VoxelShape[]::new);
                yield rotatedShapes.length > 0 ?
                        Arrays.stream(rotatedShapes).reduce(Shapes.empty(), Shapes::or) : Shapes.empty();
            }
            default -> shape;
        };
    }
}