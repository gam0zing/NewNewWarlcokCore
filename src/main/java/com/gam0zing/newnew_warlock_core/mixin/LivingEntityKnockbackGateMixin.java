package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

import com.gam0zing.newnew_warlock_core.combat.standard.VanillaDamageUtils;

/**
 * 击退断流。
 * <p>26.3 的 {@link LivingEntity#knockback(double, double, double, net.minecraft.world.damagesource.DamageSource,
 * float, boolean)} 是所有击退的统一入口（玩家近战与横扫、生物攻击、山羊冲撞、末影龙等最终都会走到这里），
 * 同时也是 {@code LivingKnockBackEvent} 的发出点。</p>
 * <p>这里把原版实现整体替换为：先照常发出 {@link LivingKnockBackEvent}（其他模组仍可取消或改写数值），
 * 再由 {@link VanillaDamageUtils#handleKnockback} 按原版位移规则推动实体。
 * 位移规则集中在那一个方法里，按伤害类型区分击退时只需要换调用目标。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityKnockbackGateMixin {

    @Inject(
        method = "knockback(DDDLnet/minecraft/world/damagesource/DamageSource;FZ)V",
        at = @At("HEAD"),
        cancellable = true)
    private void newnew$routeKnockback(double strength, double x, double z, DamageSource source, float damage,
        boolean flag, CallbackInfo ci) {
        // 原版实现整体不执行，等价于“事件之后的部分被剔除”
        ci.cancel();

        LivingEntity self = (LivingEntity) (Object) this;

        // 外部钩子：LivingKnockBackEvent（保持与其他模组的兼容）
        LivingKnockBackEvent event = CommonHooks.onLivingKnockBack(self, (float) strength, x, z);
        if (event.isCanceled()) {
            return;
        }

        // 位移：原版规则（按伤害类型区分时替换这里）
        VanillaDamageUtils.handleKnockback(self, event);
    }
}
