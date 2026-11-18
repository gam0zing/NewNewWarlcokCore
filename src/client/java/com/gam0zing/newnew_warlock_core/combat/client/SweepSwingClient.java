package com.gam0zing.newnew_warlock_core.combat.client;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.combat.sweep.net.SweepSwingPayload;

import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * 空挥横扫的客户端入口。
 * <p>左键点击空气（原版不会触发攻击）时把请求上报服务端；是否真的触发横扫由服务端按原版条件判定。</p>
 */
@EventBusSubscriber(modid = NewNewWarlockCore.MOD_ID, value = Dist.CLIENT)
public final class SweepSwingClient {

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        Player player = event.getEntity();
        if (!player.getMainHandItem().canPerformAction(ItemAbilities.SWORD_SWEEP)) {
            return;
        }
        // 不能在这里判断蓄力：原版在触发本事件之前已经重置了客户端的蓄力计时器
        ClientPacketDistributor.sendToServer(new SweepSwingPayload());
    }

    private SweepSwingClient() {}
}
