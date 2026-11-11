package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import com.gam0zing.newnew_warlock_core.combat.spine.AttackParticleType;
import com.gam0zing.newnew_warlock_core.combat.standard.VanillaCombatViewHelper;

/**
 * 服务端玩家攻击粒子（暴击 / 附魔暴击）的断流。
 * <p>原版 {@link ServerPlayer#crit} 与 {@link ServerPlayer#magicCrit} 会广播
 * {@code ClientboundAnimatePacket}（4 / 5），由客户端生成暴击/附魔粒子。
 * 这里整体转到 {@link VanillaCombatViewHelper}，当前默认放行原版粒子。</p>
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerAttackFeedbackMixin {

    @Inject(method = "crit(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void newnew$crit(Entity target, CallbackInfo ci) {
        if (!VanillaCombatViewHelper.handleAttackParticle((Player) (Object) this, AttackParticleType.CRIT)) {
            ci.cancel();
        }
    }

    @Inject(method = "magicCrit(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void newnew$magicCrit(Entity target, CallbackInfo ci) {
        if (!VanillaCombatViewHelper.handleAttackParticle((Player) (Object) this, AttackParticleType.MAGIC_CRIT)) {
            ci.cancel();
        }
    }
}
