package com.gam0zing.newnew_warlock_core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import com.gam0zing.newnew_warlock_core.combat.sweep.SweepAttackTracker;
import com.gam0zing.newnew_warlock_core.combat.sweep.SweepDecision;
import com.gam0zing.newnew_warlock_core.combat.sweep.SweepDispatcher;

/**
 * 横扫伤害接管。
 * <p>原版 {@code attack} 先对主目标调用 {@code hurtOrSimulate}，再在 {@code doSweepAttack}
 * 里给周围实体补一次独立公式的伤害。这里把主目标那次调用替换为统一的伤害池分配，
 * 并取消原版横扫，避免重复结算。</p>
 */
@Mixin(Player.class)
public abstract class PlayerSweepAttackMixin {

    /** 本次攻击是否已经走统一分配器；决定原版横扫是否取消。 */
    @Unique
    private boolean newnew$sweepHandled;

    @WrapOperation(
        method = "attack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean newnew$dispatchSweep(Entity target, DamageSource source, float amount, Operation<Boolean> original) {
        Player player = (Player) (Object) this;
        if (player.level() instanceof ServerLevel && SweepAttackTracker.isRecording(player)) {
            // Better Combat 已接管本次攻击：原样结算，只记录实际伤害，不进入统一池
            SweepDecision.consume(player);
            newnew$sweepHandled = false;
            boolean hurt = original.call(target, source, amount);
            if (hurt && target instanceof LivingEntity living) {
                SweepAttackTracker.record(player, living, amount);
            }
            return hurt;
        }
        boolean sweeping = SweepDecision.consume(player);
        if (sweeping && target instanceof LivingEntity living && player.level() instanceof ServerLevel level) {
            AABB sweepBox = player.getWeaponItem().getSweepHitBox(player, living);
            newnew$sweepHandled = true;
            return SweepDispatcher.dispatch(level, player, living, sweepBox, amount, source);
        }
        newnew$sweepHandled = false;
        return original.call(target, source, amount);
    }

    /** 伤害已由 {@link SweepDispatcher} 统一结算，取消原版横扫。 */
    @Inject(
        method = "doSweepAttack(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;"
            + "FLnet/minecraft/world/phys/AABB;)V",
        at = @At("HEAD"),
        cancellable = true)
    private void newnew$cancelVanillaSweep(Entity target, float damage, DamageSource source, float scale,
        AABB box, CallbackInfo ci) {
        if (newnew$sweepHandled) {
            ci.cancel();
        }
    }
}
