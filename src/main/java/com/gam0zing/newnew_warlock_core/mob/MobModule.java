package com.gam0zing.newnew_warlock_core.mob;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.core.ModModule;
import com.gam0zing.newnew_warlock_core.mob.config.MobDifficultyConfig;
import com.gam0zing.newnew_warlock_core.mob.config.MobPointsConfig;
import com.gam0zing.newnew_warlock_core.mob.net.DifficultySync;
import com.gam0zing.newnew_warlock_core.mob.skill.MobSkillPool;
import com.gam0zing.newnew_warlock_core.mob.skill.MobSkillPoolLoader;
import com.gam0zing.newnew_warlock_core.mob.skill.MobSkillPools;
import com.gam0zing.newnew_warlock_core.mob.skill.MobSkills;
import com.gam0zing.newnew_warlock_core.mob.skill.impl.BerserkSkill;
import com.gam0zing.newnew_warlock_core.mob.skill.impl.DecaySkill;
import com.gam0zing.newnew_warlock_core.mob.skill.impl.HardeningSkill;
import com.gam0zing.newnew_warlock_core.mob.skill.impl.RapidRegenerationSkill;
import com.gam0zing.newnew_warlock_core.mob.skill.impl.RocketJumpSkill;

import net.minecraft.resources.Identifier;
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
        // 生物技能列表附件（技能 id + 等级 + 技能自定义状态，持久化在生物自己的 NBT 上）
        MobSkills.register(modEventBus);
        // 内置技能
        MobSkills.register(new BerserkSkill());
        MobSkills.register(new HardeningSkill());
        MobSkills.register(new RapidRegenerationSkill());
        MobSkills.register(new RocketJumpSkill());
        MobSkills.register(new DecaySkill());
        // 内置技能池；优先级高的先抽取（通用 0 < 近战/远程 10），远程池暂时为空
        MobSkillPool general = new MobSkillPool(id("general"), 0);
        general.addSkill(id("hardening"), 1);
        general.addSkill(id("rapid_regeneration"), 1);
        general.addSkill(id("decay"), 1);
        MobSkillPools.register(general);

        MobSkillPool melee = new MobSkillPool(id("melee"), 10);
        melee.addSkill(id("berserk"), 1);
        melee.addSkill(id("rocket_jump"), 1);
        MobSkillPools.register(melee);

        MobSkillPools.register(new MobSkillPool(id("ranged"), 10));
        // 生物 id → 技能池 id 的绑定数据（data/<命名空间>/mob_skill_pool/*.json）
        NeoForge.EVENT_BUS.addListener(MobSkillPoolLoader::onAddReloadListener);
        // 被玩家击杀时按该生物的 points 缩放经验掉落
        NeoForge.EVENT_BUS.addListener(MobExperienceEvt::onLivingExperienceDrop);
        // 被玩家击杀时按 base_points × 缩放 提高玩家的属性点上限
        NeoForge.EVENT_BUS.addListener(MobKillPointsEvt::onLivingDeath);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, path);
    }
}
