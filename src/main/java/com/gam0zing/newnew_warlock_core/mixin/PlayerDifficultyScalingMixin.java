package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.damagesource.IScalingFunction;

/**
 * 玩家难度缩放接管。
 * <p>26.3 中，伤害的难度增幅发生在 {@code Player#hurtServer}：
 * {@code source.type().scaling().getScalingFunction().scaleDamage(source, this, damage, this.level().getDifficulty())}。
 * 原版默认缩放在困难 ×1.5、简单减半、和平归零。</p>
 * <p>这里把传入的难度参数固定为 {@link Difficulty#NORMAL}，让所有伤害（包括其它模组扩展的
 * {@link IScalingFunction}）都按普通难度计算。难度加成由 mob points 模块统一接管，
 * 不再叠加原版难度缩放。</p>
 */
@Mixin(Player.class)
public abstract class PlayerDifficultyScalingMixin {

    @ModifyArg(
        method = "hurtServer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/neoforged/neoforge/common/damagesource/IScalingFunction;"
                + "scaleDamage(Lnet/minecraft/world/damagesource/DamageSource;"
                + "Lnet/minecraft/world/entity/player/Player;FLnet/minecraft/world/Difficulty;)F"),
        index = 3)
    private Difficulty newnew$forceNormalDifficulty(Difficulty difficulty) {
        return Difficulty.NORMAL;
    }
}
