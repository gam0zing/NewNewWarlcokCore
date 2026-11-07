package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import com.gam0zing.newnew_warlock_core.rebuild.NewNewRebuildHost;
import com.gam0zing.newnew_warlock_core.registry.ModAttributes;

/**
 * 为 {@link LivingEntity} 织入 {@link NewNewRebuildHost#newnewRebuild()}。
 * <p>只新增方法，不改动原版逻辑。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityNewNewRebuildMixin implements NewNewRebuildHost {

    @Override
    public void newnewRebuild() {
        LivingEntity self = (LivingEntity) (Object) this;

        // 重建标记只在服务端写入，客户端不执行
        if (self.level().isClientSide()) {
            return;
        }

        AttributeInstance instance = self.getAttribute(ModAttributes.NEWNEW_MOB_REBUILD);
        // 没有重建标识（玩家、傀儡生物等）或已经重建过：直接跳过
        if (instance == null || instance.getBaseValue() != 0.0D) {
            return;
        }

        instance.setBaseValue(1.0D);

        // TODO: rebuild 逻辑
        // 注意：本方法会由 EntityJoinLevelEvent 在实体加入世界时调用，此时区块可能尚未 FULL。
        // 涉及世界交互（方块、掉落物、区块查询等）的重建步骤应自行延后到下一 tick 执行。
    }
}
