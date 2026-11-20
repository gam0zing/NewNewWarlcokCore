package com.gam0zing.newnew_warlock_core.origin.content;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.origin.OriginSetResult;
import com.gam0zing.newnew_warlock_core.origin.OriginSlot;
import com.gam0zing.newnew_warlock_core.origin.WarlockOrigins;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 神之雕像的抽象基类。
 * <p>子类决定给予哪个加护，并可覆写 {@link #canGrant(ServerPlayer)} 增加条件。
 * 加护层对玩家隐藏，所以这里直接通过 API 写入，不打开选择界面。</p>
 */
public abstract class DivineStatueBlock extends Block {

    protected DivineStatueBlock(Properties properties) {
        super(properties);
    }

    /** {@return 本次交互要赋予的加护；返回 null 表示不处理} */
    @Nullable
    protected abstract Identifier blessing(ServerPlayer player);

    /** 交互条件；默认无条件。 */
    protected boolean canGrant(ServerPlayer player) {
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(
        BlockState state,
        Level level,
        BlockPos pos,
        Player player,
        BlockHitResult hit) {
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
        if (!canGrant(server)) {
            server.sendOverlayMessage(Component.translatable("origin.newnew_warlock_core.statue.denied"));
            return InteractionResult.FAIL;
        }

        Identifier blessing = blessing(server);
        if (blessing == null) {
            return InteractionResult.PASS;
        }

        OriginSetResult result = WarlockOrigins.setOrigin(server, OriginSlot.BLESSING, blessing);
        if (result == OriginSetResult.SUCCESS) {
            server.sendOverlayMessage(Component.translatable("origin.newnew_warlock_core.statue.granted"));
            return InteractionResult.CONSUME;
        }
        if (result == OriginSetResult.UNCHANGED) {
            return InteractionResult.SUCCESS;
        }
        server.sendOverlayMessage(Component.translatable("origin.newnew_warlock_core.set.failed"));
        return InteractionResult.FAIL;
    }
}
