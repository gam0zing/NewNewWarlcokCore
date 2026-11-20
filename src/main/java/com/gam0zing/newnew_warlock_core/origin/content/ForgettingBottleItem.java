package com.gam0zing.newnew_warlock_core.origin.content;

import java.util.List;

import com.gam0zing.newnew_warlock_core.origin.OriginSlot;
import com.gam0zing.newnew_warlock_core.origin.WarlockOrigins;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * 遗忘之瓶：只重选灵质层。
 * <p>物品在提交时消耗、取消时保留，由 NeoOrigins 的单层选择会话处理。</p>
 */
public final class ForgettingBottleItem extends Item {

    public ForgettingBottleItem(Properties properties) {
        super(properties);
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

        boolean opened = WarlockOrigins.openPicker(
            server,
            List.of(OriginSlot.ESSENCE),
            this,
            "item.newnew_warlock_core.forgetting_bottle.select");
        return opened ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }
}
