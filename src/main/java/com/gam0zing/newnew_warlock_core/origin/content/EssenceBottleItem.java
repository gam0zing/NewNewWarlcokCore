package com.gam0zing.newnew_warlock_core.origin.content;

import com.gam0zing.newnew_warlock_core.origin.OriginSetResult;
import com.gam0zing.newnew_warlock_core.origin.OriginSlot;
import com.gam0zing.newnew_warlock_core.origin.WarlockOrigins;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 灵质瓶：直接替换为构造时绑定的灵质，成功后消耗。
 * <p>每个灵质注册一个物品实例；替换绕开选择界面，因此也可以用于无法直接选择的灵质。</p>
 */
public final class EssenceBottleItem extends Item {

    private final Identifier essence;

    public EssenceBottleItem(Properties properties, Identifier essence) {
        super(properties);
        this.essence = essence;
    }

    public Identifier essence() {
        return this.essence;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer server)) {
            return InteractionResult.PASS;
        }
        if (!WarlockOrigins.isAvailable()) {
            server.sendOverlayMessage(Component.translatable("origin.newnew_warlock_core.unavailable"));
            return InteractionResult.FAIL;
        }

        OriginSetResult result = WarlockOrigins.setOrigin(server, OriginSlot.ESSENCE, this.essence);
        if (result == OriginSetResult.UNCHANGED) {
            server.sendOverlayMessage(Component.translatable("origin.newnew_warlock_core.essence.unchanged"));
            return InteractionResult.SUCCESS;
        }
        if (result != OriginSetResult.SUCCESS) {
            server.sendOverlayMessage(Component.translatable("origin.newnew_warlock_core.set.failed"));
            return InteractionResult.FAIL;
        }

        ItemStack stack = player.getItemInHand(hand);
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
        }
        server.sendOverlayMessage(Component.translatable("origin.newnew_warlock_core.essence.replaced"));
        return InteractionResult.CONSUME;
    }
}
