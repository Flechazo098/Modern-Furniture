package com.flechazo.modernfurniture.client;

import com.flechazo.modernfurniture.util.wire.WireConnection;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端电线连接管理器
 * 用于存储从服务端同步的连接数据
 */
public class ClientWireManager {
    private static final Map<ResourceLocation, Set<WireConnection>> connections = new ConcurrentHashMap<>();
    
    /**
     * 更新连接数据
     */
    public static void updateConnections(ResourceLocation dimension, Set<WireConnection> newConnections) {
        Set<WireConnection> concurrentSet = ConcurrentHashMap.newKeySet();
        concurrentSet.addAll(newConnections);
        connections.put(dimension, concurrentSet);
    }
    
    /**
     * 添加单个连接
     */
    public static void addConnection(WireConnection connection) {
        connections.computeIfAbsent(connection.dimension(), k -> ConcurrentHashMap.newKeySet()).add(connection);
    }
    
    /**
     * 移除单个连接
     */
    public static void removeConnection(WireConnection connection) {
        Set<WireConnection> dimConnections = connections.get(connection.dimension());
        if (dimConnections != null) {
            dimConnections.remove(connection);
            if (dimConnections.isEmpty()) {
                connections.remove(connection.dimension());
            }
        }
    }
    
    /**
     * 获取指定维度的所有连接
     */
    public static Set<WireConnection> getConnections(ResourceLocation dimension) {
        Set<WireConnection> dimConnections = connections.get(dimension);
        return dimConnections != null ? Set.copyOf(dimConnections) : Collections.emptySet();
    }
    
    /**
     * 清除所有连接
     */
    public static void clear() {
        connections.clear();
    }
    
    /**
     * 清除指定维度的连接
     */
    public static void clearDimension(ResourceLocation dimension) {
        connections.remove(dimension);
    }
}