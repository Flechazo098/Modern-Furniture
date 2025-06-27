package com.flechazo.modernfurniture.block;

import com.flechazo.modernfurniture.block.entity.DisplayBlockEntity;
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
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

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

        VoxelShape shape = VoxelShapeUtils.createMultiBox(
                new double[]{7.94198729810778, 0, 7.742227771688616, 14.94198729810778, 0.75, 8.492227771688617},
                new double[]{8.19198729810778, 0, 7.2622, 15.19198729810778, 0.75, 8.0122},
                new double[]{7.25, 0, 7.65, 8.75, 4.6499999999999995, 8.5},
                new double[]{-2, 3, 7.5, 18, 7, 8},
                new double[]{-3.25, 2.5, 7.25, 19.25, 14.75, 7.75}
        );

        return VoxelShapeUtils.rotateShape(shape, facing);
    }
}