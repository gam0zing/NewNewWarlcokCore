package com.gam0zing.newnew_warlock_core.enchant;

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
        if (entity instanceof Warden) {
            EnchantUnlockData.get(level).markWardenDefeated();
        } else if (entity instanceof WitherBoss) {
            EnchantUnlockData.get(level).markWitherDefeated();
        } else if (entity instanceof EnderDragon) {
            EnchantUnlockData.get(level).markDragonDefeated();
        }
    }

    private EnchantUnlockEvt() {}
}
