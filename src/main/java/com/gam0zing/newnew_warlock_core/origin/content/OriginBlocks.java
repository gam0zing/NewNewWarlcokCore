package com.gam0zing.newnew_warlock_core.origin.content;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.origin.OriginIds;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 起源内容的方块注册。
 */
public final class OriginBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
        DeferredRegister.createBlocks(NewNewWarlockCore.MOD_ID);

    /** 示例雕像：交互后赋予 {@link OriginIds#BLESSING_SAMPLE} */
    public static final DeferredBlock<FixedBlessingStatueBlock> SAMPLE_STATUE =
        BLOCKS.registerBlock(
            "sample_statue",
            properties -> new FixedBlessingStatueBlock(properties, OriginIds.BLESSING_SAMPLE),
            () -> BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops());

    private OriginBlocks() {}
}
