package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;

import com.gam0zing.newnew_warlock_core.mob.ExplorerBonus;

/**
 * 探险家：吸收经验球时按概率使经验数值翻倍。
 * <p>作用点是经验球把剩余经验（经验修补消耗之后）交给玩家的那一刻，
 * 因此命令、任务等其它经验来源不受影响，也不会干扰经验修补的结算。</p>
 */
@Mixin(ExperienceOrb.class)
public abstract class ExperienceOrbMixin {

    @Redirect(
        method = "playerTouch",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;giveExperiencePoints(I)V"))
    private void newnew$explorerDoubleXp(Player player, int amount) {
        player.giveExperiencePoints(ExplorerBonus.doubleOrbXp(player, amount));
    }
}
