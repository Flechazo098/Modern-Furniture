package com.flechazo.modernfurniture.block;

import com.flechazo.modernfurniture.block.entity.DisplayBlockEntity;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashSet;
import java.util.Set;

public abstract class AbstractDisplayBlock extends Block implements EntityBlock {
    public static final BooleanProperty POWERED = BooleanProperty.create("powered");
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;


    public AbstractDisplayBlock() {
        super(BlockBehaviour.Properties.of()
                .strength(2.0F, 6.0F)
                .noOcclusion()
                .sound(SoundType.METAL)
        );
        this.registerDefaultState(this.defaultBlockState()
                .setValue(POWERED, false)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED, FACING);
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
            boolean isOn = state.getValue(POWERED);
            level.setBlock(pos, state.setValue(POWERED, !isOn), 3);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DisplayBlockEntity(pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);

        VoxelShape baseFoot1 = Shapes.box(7.94198729810778/16, 0/16, 7.742227771688616/16, 14.94198729810778/16, 0.75/16, 8.492227771688617/16);
        VoxelShape baseFoot2 = Shapes.box(8.19198729810778/16, 0/16, 7.2622/16, 15.19198729810778/16, 0.75/16, 8.0122/16);
        VoxelShape centerPillar = Shapes.box(7.25/16, 0/16, 7.65/16, 8.75/16, 4.6499999999999995/16, 8.5/16);
        VoxelShape shieldMain = Shapes.box((double) - 2 /16, (double) 3 /16, 7.5/16, (double) 18 /16, (double) 7 /16, (double) 8 /16);
        VoxelShape shieldBack = Shapes.box(-3.25/16, 2.5/16, 7.25/16, 19.25/16, 14.75/16, 7.75/16);

        VoxelShape shape = Shapes.or(
                baseFoot1,
                baseFoot2,
                centerPillar,
                shieldMain,
                shieldBack
        );

        return rotateVoxelShape(shape, facing);
    }

    // 旋转 VoxelShape 以匹配方块朝向
    private VoxelShape rotateVoxelShape(VoxelShape shape, Direction facing) {
        Set<VoxelShape> rotatedShapes = new HashSet<>();

        // 对原始形状中的每个 AABB 应用旋转
        for (AABB aabb : shape.toAabbs()) {
            rotatedShapes.add(rotateAABB(aabb, facing));
        }

        // 组合所有旋转后的 AABB
        return rotatedShapes.stream()
                .reduce(Shapes.empty(), Shapes::or);
    }

    // 旋转单个 AABB 到指定朝向
    private VoxelShape rotateAABB(AABB aabb, Direction facing) {
        return switch (facing) {
            case NORTH -> Shapes.create(aabb);
            case SOUTH -> Shapes.create(  // 南向旋转 180°
                    1 - aabb.maxX, aabb.minY, 1 - aabb.maxZ,
                    1 - aabb.minX, aabb.maxY, 1 - aabb.minZ
            );
            case WEST -> Shapes.create(  // 西向旋转 90°
                    aabb.minZ, aabb.minY, 1 - aabb.maxX,
                    aabb.maxZ, aabb.maxY, 1 - aabb.minX
            );
            case EAST -> Shapes.create(  // 东向旋转 270°
                    1 - aabb.maxZ, aabb.minY, aabb.minX,
                    1 - aabb.minZ, aabb.maxY, aabb.maxX
            );
            default -> Shapes.create(aabb);
        };
    }
}