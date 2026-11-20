package com.gam0zing.newnew_warlock_core.origin.content;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 固定赋予某个加护的雕像实现，用于示例与简单内容。
 */
public final class FixedBlessingStatueBlock extends DivineStatueBlock {

    private final Identifier blessing;

    public FixedBlessingStatueBlock(BlockBehaviour.Properties properties, Identifier blessing) {
        super(properties);
        this.blessing = blessing;
    }

    @Override
    protected Identifier blessing(ServerPlayer player) {
        return this.blessing;
    }
}
