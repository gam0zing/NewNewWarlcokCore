package com.gam0zing.newnew_warlock_core.mob;

import com.gam0zing.newnew_warlock_core.core.ModModule;
import com.gam0zing.newnew_warlock_core.mob.config.MobDifficultyConfig;
import com.gam0zing.newnew_warlock_core.mob.config.MobPointsConfig;
import com.gam0zing.newnew_warlock_core.mob.net.DifficultySync;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 生物模块：属性点分配参数与生物重建的难度来源配置。
 */
public final class MobModule implements ModModule {

    @Override
    public String id() {
        return "mob";
    }

    @Override
    public int priority() {
        return 50;
    }

    @Override
    public void init(IEventBus modEventBus, ModContainer modContainer) {
        // 生物属性点分配参数（common + 存档 server 两层配置）
        MobPointsConfig.register();
        // 生物重建的难度来源（三份 common + server 两层配置）
        MobDifficultyConfig.register();
        // 当前位置总难度的服务端权威计算与客户端同步（HUD 显示用）
        DifficultySync.register(modEventBus);
        // 被玩家击杀时按该生物的 points 缩放经验掉落
        NeoForge.EVENT_BUS.addListener(MobExperienceEvt::onLivingExperienceDrop);
    }
}
