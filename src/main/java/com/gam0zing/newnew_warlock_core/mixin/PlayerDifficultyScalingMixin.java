package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.damagesource.IScalingFunction;

/**
 * 玩家难度缩放接管。
 * <p>原版 {@code Player#hurt} 在创建 {@code DamageContainer} 之前，会通过
 * {@link IScalingFunction#scaleDamage} 按难度缩放伤害（和平 0、简单减半、困难 ×1.5 等）。
 * 该缩放发生在伤害管线看到数值之前，无法在管线内部抵消，所以单独拦截这一处调用。</p>
 * <p>当前返回原值，等同于剔除原版难度缩放；自定义难度规则留待后续设计。</p>
 */
@Mixin(Player.class)
public abstract class PlayerDifficultyScalingMixin {

    @Redirect(
        method = "hurtServer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/damagesource/IScalingFunction;"
                + "scaleDamage(Lnet/minecraft/world/damagesource/DamageSource;"
                + "Lnet/minecraft/world/entity/player/Player;FLnet/minecraft/world/Difficulty;)F"))
    private float newnew$scaleDamageForDifficulty(IScalingFunction scalingFunction, DamageSource source,
        Player target, float amount, Difficulty difficulty) {
        // TODO: 业务区域 —— 玩家受伤的难度缩放
        // 可用数据：source（伤害来源）、target（受伤玩家）、amount（缩放前的伤害）、difficulty（当前难度）
        // 目前返回原值，即不进行任何难度缩放。
        return amount;
    }
}
