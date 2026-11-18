package com.gam0zing.newnew_warlock_core.mixin.compat.bettercombat;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.bettercombat.api.AttackHand;
import net.bettercombat.api.WeaponAttributes;
import net.bettercombat.logic.PlayerAttackHelper;
import net.bettercombat.network.Packets;
import net.bettercombat.network.ServerNetwork;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import com.gam0zing.newnew_warlock_core.combat.sweep.SweepAttackTracker;

/**
 * Better Combat 出手入口。
 * <p>锚定在 {@code executeIfPossible} 调用点：此时「维度有效 / 攻击未被禁用 / 能解析出攻击动作」
 * 都已通过，且不依赖 Better Combat 的 {@code lambda$...} 合成方法名。</p>
 * <p>Better Combat 的空挥同样会发送该请求，因此这里也是空挥波的唯一入口。</p>
 */
@Mixin(ServerNetwork.class)
public abstract class ServerNetworkMixin {

    @Inject(
        method = "handleAttackRequest",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/MinecraftServer;executeIfPossible(Ljava/lang/Runnable;)V"),
        require = 0)
    private static void newnew$beginBetterCombatAttack(Packets.C2S_AttackRequest packet,
        MinecraftServer server, ServerPlayer player, ServerGamePacketListenerImpl handler, CallbackInfo ci) {
        AttackHand hand = PlayerAttackHelper.getCurrentAttack(player, packet.comboCount());
        WeaponAttributes.Attack attack = hand == null ? null : hand.attack();
        // 只有能横扫的 BC 武器（挥砍弧度 > 0）才生成横扫之波；突刺类武器彻底不参与
        if (attack == null || attack.angle() <= 0.0D) {
            return;
        }
        SweepAttackTracker.begin(player, packet.cursorTarget(), attack.damageMultiplier());
    }
}
