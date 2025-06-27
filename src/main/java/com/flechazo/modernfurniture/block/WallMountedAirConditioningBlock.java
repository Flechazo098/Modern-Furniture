package com.flechazo.modernfurniture.block;

import com.flechazo.modernfurniture.block.entity.WallMountedAirConditioningBlockEntity;
import com.flechazo.modernfurniture.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.stream.Stream;

public class WallMountedAirConditioningBlock extends AbstractAirConditioningBlock {
    private static final VoxelShape BASE_SHAPE = Stream.of(
            Block.box(-4, 10.11864, 12.4122, 20, 11.11864, 12.6622),  // 叶片部分
            Block.box(-4, 10.5, 11.75, 20, 16, 12.5),                 // 前面板
            Block.box(-4, 10, 12.5, 20, 16, 16)                       // 主体
    ).reduce((v1, v2) -> Shapes.join(v1, v2, BooleanOp.OR)).get();

    @Override
    protected VoxelShape getAirConditioningShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return VoxelShapeUtils.rotateShape(BASE_SHAPE, facing);
    }

    @Override
    protected void addParticleEffects(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction facing = state.getValue(FACING);
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.3;
        double z = pos.getZ() + 0.5;

        switch (facing) {
            case NORTH -> z -= 0.4;
            case SOUTH -> z += 0.4;
            case WEST -> x -= 0.4;
            case EAST -> x += 0.4;
        }

        for (int i = 0; i < 3; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 0.3;
            double offsetY = (random.nextDouble() - 0.5) * 0.2;
            double offsetZ = (random.nextDouble() - 0.5) * 0.3;

            level.addParticle(ParticleTypes.SNOWFLAKE,
                    x + offsetX, y + offsetY, z + offsetZ,
                    0.0, -0.1, 0.0);
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WallMountedAirConditioningBlockEntity(pos, state);
    }
}