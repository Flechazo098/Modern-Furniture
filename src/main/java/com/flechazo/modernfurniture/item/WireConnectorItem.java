package com.flechazo.modernfurniture.item;

import com.flechazo.modernfurniture.util.wire.WireConnectable;
import com.flechazo.modernfurniture.util.wire.WireNetworkManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;

public class WireConnectorItem extends Item {
    private static final String FIRST_POS_TAG = "FirstPos";
    private static final String FIRST_DIM_TAG = "FirstDim";

    public WireConnectorItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();

        if (player == null) return InteractionResult.FAIL;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof WireConnectable)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.translatable("item.modern_furniture.wire_connector.not_connectable")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        CompoundTag tag = stack.getOrCreateTag();

        // 检查是否已有第一个位置
        if (tag.contains(FIRST_POS_TAG)) {
            // 获取第一个位置
            long firstPosLong = tag.getLong(FIRST_POS_TAG);
            String firstDim = tag.getString(FIRST_DIM_TAG);
            BlockPos firstPos = BlockPos.of(firstPosLong);

            // 检查维度是否相同
            if (!level.dimension().location().toString().equals(firstDim)) {
                player.sendSystemMessage(Component.translatable("item.modern_furniture.wire_connector.different_dimension")
                        .withStyle(ChatFormatting.RED));
                tag.remove(FIRST_POS_TAG);
                tag.remove(FIRST_DIM_TAG);
                return InteractionResult.FAIL;
            }

            // 检查是否是同一个位置
            if (firstPos.equals(pos)) {
                player.sendSystemMessage(Component.translatable("item.modern_furniture.wire_connector.same_position")
                        .withStyle(ChatFormatting.RED));
                return InteractionResult.FAIL;
            }

            // 尝试建立连接
            WireNetworkManager manager = WireNetworkManager.get((ServerLevel) level);
            if (manager.addConnection(level, firstPos, pos)) {
                player.sendSystemMessage(Component.translatable("item.modern_furniture.wire_connector.connected")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                player.sendSystemMessage(Component.translatable("item.modern_furniture.wire_connector.connection_failed")
                        .withStyle(ChatFormatting.RED));
            }

            // 清除第一个位置
            tag.remove(FIRST_POS_TAG);
            tag.remove(FIRST_DIM_TAG);
        } else {
            // 设置第一个位置
            tag.putLong(FIRST_POS_TAG, pos.asLong());
            tag.putString(FIRST_DIM_TAG, level.dimension().location().toString());

            player.sendSystemMessage(Component.translatable("item.modern_furniture.wire_connector.first_selected")
                    .withStyle(ChatFormatting.YELLOW));
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);

        tooltip.add(Component.translatable("item.modern_furniture.wire_connector.tooltip1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.modern_furniture.wire_connector.tooltip2")
                .withStyle(ChatFormatting.GRAY));

        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(FIRST_POS_TAG)) {
            BlockPos firstPos = BlockPos.of(tag.getLong(FIRST_POS_TAG));
            tooltip.add(Component.translatable("item.modern_furniture.wire_connector.first_pos",
                            firstPos.getX(), firstPos.getY(), firstPos.getZ())
                    .withStyle(ChatFormatting.AQUA));
        }
    }
}