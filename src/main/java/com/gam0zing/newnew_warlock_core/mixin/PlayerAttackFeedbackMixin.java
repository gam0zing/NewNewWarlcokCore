package com.gam0zing.newnew_warlock_core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;

import com.gam0zing.newnew_warlock_core.combat.damage.AttackParticleType;
import com.gam0zing.newnew_warlock_core.combat.damage.DamagePipeline;

/**
 * 玩家攻击表现的断流。
 * <p>26.3 把攻击音效统一收敛到 {@code Player#playServerSideSound}（攻击、横扫、暴击、命中反馈），
 * 横扫粒子在 {@code doSweepAttack}，受击指示粒子在 {@code damageStatsAndHearts}。
 * 这些表现统一转到业务插槽 {@link DamagePipeline#handleAttackSound} /
 * {@link DamagePipeline#handleAttackParticle}，插槽默认都不放行，因此默认没有任何攻击音效与粒子。</p>
 */
@Mixin(Player.class)
public abstract class PlayerAttackFeedbackMixin {

    /** 攻击相关音效（攻击、横扫、暴击、命中反馈）。 */
    @Inject(method = "playServerSideSound", at = @At("HEAD"), cancellable = true)
    private void newnew$attackSound(SoundEvent sound, CallbackInfo ci) {
        if (!DamagePipeline.handleAttackSound((Player) (Object) this, sound)) {
            ci.cancel();
        }
    }

    /** 横扫粒子：原版在 {@code Player#doSweepAttack(Entity, float, DamageSource, float, AABB)} 里发送。 */
    @WrapOperation(
        method = "doSweepAttack(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;"
            + "FLnet/minecraft/world/phys/AABB;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
    private int newnew$sweepParticles(
        ServerLevel level, ParticleOptions particle, double x, double y, double z, int count,
        double xOffset, double yOffset, double zOffset, double speed, Operation<Integer> original) {
        if (!DamagePipeline.handleAttackParticle((Player) (Object) this, AttackParticleType.SWEEP)) {
            return 0;
        }
        return original.call(level, particle, x, y, z, count, xOffset, yOffset, zOffset, speed);
    }

    /** 受击指示粒子：原版在 {@code Player#damageStatsAndHearts} 里对目标发送 {@code DAMAGE_INDICATOR}。 */
    @WrapOperation(
        method = "damageStatsAndHearts(Lnet/minecraft/world/entity/Entity;F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I"))
    private int newnew$damageIndicatorParticles(
        ServerLevel level, ParticleOptions particle, double x, double y, double z, int count,
        double xOffset, double yOffset, double zOffset, double speed, Operation<Integer> original) {
        if (!DamagePipeline.handleAttackParticle((Player) (Object) this, AttackParticleType.DAMAGE_INDICATOR)) {
            return 0;
        }
        return original.call(level, particle, x, y, z, count, xOffset, yOffset, zOffset, speed);
    }
}
