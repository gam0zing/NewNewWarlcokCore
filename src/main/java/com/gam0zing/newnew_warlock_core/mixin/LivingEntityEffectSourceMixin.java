package com.gam0zing.newnew_warlock_core.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.gam0zing.newnew_warlock_core.share.EffectSources;

/**
 * 状态效果来源的伤害归因入口。
 * <p>效果结算（{@code MobEffectInstance#tick} → {@code MobEffect#applyEffectTick}）内产生的伤害，
 * 原版伤害来源（中毒、凋零、瞬时伤害等）往往没有攻击者。这里在 {@code hurt} 的最前端
 * 把当前结算效果的来源生物补进 {@link DamageSource}，于是：</p>
 * <ul>
 *     <li>{@code LivingIncomingDamageEvent}、{@code LivingDamageEvent.Pre/Post} 与自定义伤害管线
 *     都能读到攻击者；</li>
 *     <li>伤害强度（{@code damage_scale}、{@code <tag>_damage_scale}）、死亡消息、击杀归属随之生效。</li>
 * </ul>
 * <p>同时把原版已经带来源的施加途径（{@code addEffect}/{@code forceAddEffect} 的 {@code Entity} 参数，
 * 如投掷药水、滞留药水云）写入效果实例；没有实体来源时，仅在自己喝药/进食期间把使用者当作来源
 * （“自己喝下的药、吃下的食物吃自己属性”），其余无来源途径保持 null。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectSourceMixin {

    /** 效果结算内造成的伤害继承效果来源（原伤害已有攻击者时不覆盖；读档后按 UUID 懒解析） */
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private DamageSource newnew$inheritEffectSource(DamageSource source) {
        return EffectSources.inheritTickingSource(source, (LivingEntity) (Object) this);
    }

    /** 原版带来源施加效果时写入来源；自己喝药时回落到饮用者 */
    @Inject(
        method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
        at = @At("HEAD"))
    private void newnew$attachAddedEffectSource(MobEffectInstance instance, @Nullable Entity sourceEntity,
        CallbackInfoReturnable<Boolean> cir) {
        LivingEntity source = EffectSources.sourceEntity(sourceEntity);
        if (source == null) {
            source = EffectSources.currentSelfConsumptionSource((LivingEntity) (Object) this);
        }
        if (source != null) {
            EffectSources.attachSource(instance, source);
        }
    }

    /** 强制施加效果（登录同步、命令等）同样保留已有来源 */
    @Inject(method = "forceAddEffect", at = @At("HEAD"))
    private void newnew$attachForcedEffectSource(MobEffectInstance instance, @Nullable Entity sourceEntity,
        CallbackInfo ci) {
        LivingEntity source = EffectSources.sourceEntity(sourceEntity);
        if (source != null) {
            EffectSources.attachSource(instance, source);
        }
    }
}
