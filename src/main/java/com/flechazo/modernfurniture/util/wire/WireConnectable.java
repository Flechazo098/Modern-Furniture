package com.flechazo.modernfurniture.util.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * 需要电线连接的设备必须实现此接口
 * 电线连接是激活桥梁，断开或区块未加载则设备不工作
 */
public interface WireConnectable {

    /**
     * 当连接被激活时调用
     *
     * @param level        世界
     * @param pos          设备位置
     * @param connectedPos 连接的另一端位置
     */
    void onConnectionActivated(Level level, BlockPos pos, BlockPos connectedPos);

    /**
     * 当连接被停用时调用
     *
     * @param level        世界
     * @param pos          设备位置
     * @param connectedPos 连接的另一端位置
     */
    void onConnectionDeactivated(Level level, BlockPos pos, BlockPos connectedPos);

    /**
     * 检查设备是否可以与指定位置的设备连接
     *
     * @param level     世界
     * @param pos       本设备位置
     * @param targetPos 目标设备位置
     * @return 是否可以连接
     */
    boolean canConnectTo(Level level, BlockPos pos, BlockPos targetPos);

    /**
     * 获取设备的连接类型
     *
     * @return 连接类型
     */
    WireConnectionType getConnectionType();

    /**
     * 检查设备当前是否处于工作状态
     *
     * @param level 世界
     * @param pos   设备位置
     * @return 是否在工作
     */
    boolean isWorking(Level level, BlockPos pos);
}