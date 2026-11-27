package com.gam0zing.newnew_warlock_core.origin;

import com.cyberday1.neoorigins.client.ClientPlayerPowers;
import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.compat.neoorigins.NeoOriginsGate;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * 客户端能力查询：把 NeoOrigins 同步到客户端的能力快照接到 {@link OriginTraits}。
 * <p>只用于本地预测（疾跑、进食）。服务端拒绝本地预测时不会主动下发纠正，
 * 因此客户端必须按同样的规则自行判断。</p>
 * <p>未安装 NeoOrigins 时查询恒为 false，不会解析 {@link ClientPlayerPowers}。</p>
 */
@EventBusSubscriber(modid = NewNewWarlockCore.MOD_ID, value = Dist.CLIENT)
public final class ClientOriginTraits {

    static {
        OriginTraits.installClientPowerCheck(
            player -> player == Minecraft.getInstance().player,
            ClientOriginTraits::hasPower);
    }

    private static boolean hasPower(Identifier power) {
        if (!NeoOriginsGate.isLoaded()) {
            return false;
        }
        var player = Minecraft.getInstance().player;
        return player != null && ClientPlayerPowers.hasPower(player.getUUID(), power);
    }

    private ClientOriginTraits() {}
}
