package com.gam0zing.newnew_warlock_core.enchant;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.warden.Warden;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

/**
 * 记录 Boss 的首次击杀，并管理 {@link EnchantUnlockData} 的静态快照。
 * <p>三个 Boss 都没有重写 {@code die}，因此 {@link LivingDeathEvent} 在服务端一定可用；
 * 袭击胜利没有现成事件，见 mixin 包里的 {@code RaidVictoryMixin}。</p>
 */
@EventBusSubscriber(modid = NewNewWarlockCore.MOD_ID)
public final class EnchantUnlockEvt {

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        EnchantUnlockData.activate(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        EnchantUnlockData.deactivate();
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }

        Entity entity = event.getEntity();
        EnchantUnlockData data = EnchantUnlockData.get(level);
        if (entity instanceof Warden) {
            if (data.markWardenDefeated()) {
                announceUnlock(level, "unlock.newnew_warlock_core.warden");
            }
        } else if (entity instanceof WitherBoss) {
            if (data.markWitherDefeated()) {
                announceUnlock(level, "unlock.newnew_warlock_core.wither");
            }
        } else if (entity instanceof EnderDragon) {
            if (data.markDragonDefeated()) {
                announceUnlock(level, "unlock.newnew_warlock_core.dragon");
            }
        }
    }

    /**
     * 向全服玩家聊天栏广播一条解锁提示（青色）。
     * <p>一次性解锁是世界级的，因此提示发给所有在线玩家；{@code overlay = false} 表示走聊天栏而非动作栏。</p>
     */
    public static void announceUnlock(ServerLevel level, String translationKey) {
        level.getServer().getPlayerList().broadcastSystemMessage(
            Component.translatable(translationKey).withStyle(ChatFormatting.AQUA), false);
    }

    private EnchantUnlockEvt() {}
}
