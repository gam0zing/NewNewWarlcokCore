package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.entity.player.Player;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 经验获取倍率。
 * <p>所有经验最终都通过 {@code Player#giveExperiencePoints} 进入经验条（经验球、任务奖励、命令等），
 * 因此在这里按玩家持有的 {@code exp_receive_scale} 缩放。</p>
 */
@Mixin(Player.class)
public abstract class PlayerExperienceMixin {

    @ModifyVariable(method = "giveExperiencePoints", at = @At("HEAD"), argsOnly = true)
    private int newnew$scaleExperience(int amount) {
        if (amount == 0) {
            return amount;
        }

        float scale = ModAttributes.getScale((Player) (Object) this, ModAttributes.EXP_RECEIVE_SCALE);
        if (scale == 1.0F) {
            return amount;
        }

        long scaled = Math.round(amount * (double) scale);
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, scaled));
    }
}
