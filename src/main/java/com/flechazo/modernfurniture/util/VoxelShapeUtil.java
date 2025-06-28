package com.flechazo.modernfurniture.util;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Arrays;

public class VoxelShapeUtil {

    /**
     * 根据朝向旋转 VoxelShape
     *
     * @param shape  原始形状
     * @param facing 目标朝向
     * @return 旋转后的形状
     */
    public static VoxelShape rotateShape(VoxelShape shape, Direction facing) {
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

    /**
     * 创建多个 box 并合并
     *
     * @param boxes box坐标数组，每个数组包含6个double值 [minX, minY, minZ, maxX, maxY, maxZ]
     * @return 合并后的VoxelShape
     */
    public static VoxelShape createMultiBox(double[]... boxes) {
        VoxelShape result = Shapes.empty();
        for (double[] box : boxes) {
            if (box.length == 6) {
                result = Shapes.or(result, Shapes.box(
                        box[0] / 16.0, box[1] / 16.0, box[2] / 16.0,
                        box[3] / 16.0, box[4] / 16.0, box[5] / 16.0
                ));
            }
        }
        return result;
    }
}