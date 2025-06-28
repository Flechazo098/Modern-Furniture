package com.flechazo.modernfurniture.util.wire;

/**
 * 电线连接类型
 */
public enum WireConnectionType {
    /**
     * 空调室内机 - 需要外机提供动力
     */
    AIR_CONDITIONING_INDOOR,

    /**
     * 空调室外机 - 为室内机提供动力
     */
    AIR_CONDITIONING_OUTDOOR;

    /**
     * 检查两种类型是否可以连接
     */
    public boolean canConnectTo(WireConnectionType other) {
        return (this == AIR_CONDITIONING_INDOOR && other == AIR_CONDITIONING_OUTDOOR) ||
                (this == AIR_CONDITIONING_OUTDOOR && other == AIR_CONDITIONING_INDOOR);
    }
}