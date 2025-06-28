package com.flechazo.modernfurniture.util.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * 电线连接的纯数据记录
 * 无状态，无Tick，只存引用
 */
public record WireConnection(BlockPos pos1, BlockPos pos2, ResourceLocation dimension) {
    public WireConnection(BlockPos pos1, BlockPos pos2, ResourceLocation dimension) {
        // 确保位置顺序一致，便于比较
        if (pos1.compareTo(pos2) <= 0) {
            this.pos1 = pos1;
            this.pos2 = pos2;
        } else {
            this.pos1 = pos2;
            this.pos2 = pos1;
        }
        this.dimension = dimension;
    }

    /**
     * 从NBT加载
     */
    public static WireConnection load(CompoundTag tag) {
        BlockPos pos1 = BlockPos.of(tag.getLong("pos1"));
        BlockPos pos2 = BlockPos.of(tag.getLong("pos2"));
        ResourceLocation dimension = ResourceLocation.parse(tag.getString("dimension"));
        return new WireConnection(pos1, pos2, dimension);
    }

    /**
     * 获取连接的另一端位置
     */
    public BlockPos getOtherEnd(BlockPos pos) {
        if (pos.equals(pos1)) {
            return pos2;
        } else if (pos.equals(pos2)) {
            return pos1;
        }
        return null;
    }

    /**
     * 检查连接是否包含指定位置
     */
    public boolean contains(BlockPos pos) {
        return pos1.equals(pos) || pos2.equals(pos);
    }

    /**
     * 保存到NBT
     */
    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("pos1", pos1.asLong());
        tag.putLong("pos2", pos2.asLong());
        tag.putString("dimension", dimension.toString());
        return tag;
    }

    @Override
    public String toString() {
        return String.format("WireConnection{%s <-> %s in %s}", pos1, pos2, dimension);
    }
}