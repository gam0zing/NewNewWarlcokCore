package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.LivingEntity;

import com.gam0zing.newnew_warlock_core.mob.skill.MobSkills;

/**
 * 生物技能驱动：{@link LivingEntity} 每 tick 调用一次 {@link MobSkills#updateSkill(LivingEntity)}。
 * <p>技能列表与冷却/持续状态都保存在服务端的生物 NBT 上且不做客户端同步，
 * 因此只在服务端驱动，避免客户端把空状态当成“冷却结束”而每 tick 触发 invoke。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMobSkillMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void newnew$updateMobSkills(CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide()) {
            return;
        }
        MobSkills.updateSkill(self);
    }
}
