package com.gam0zing.newnew_warlock_core;

import org.slf4j.Logger;

import com.gam0zing.newnew_warlock_core.body.BodyPartLoader;
import com.gam0zing.newnew_warlock_core.combat.effect.BleedingEffectStack;
import com.gam0zing.newnew_warlock_core.combat.effect.MobEffectStackRegistry;
import com.gam0zing.newnew_warlock_core.config.NetConfig;
import com.gam0zing.newnew_warlock_core.enchant.EnchantBaselineLoader;
import com.gam0zing.newnew_warlock_core.enchant.command.EnchantBaselineCommand;
import com.gam0zing.newnew_warlock_core.enchant.config.EnchantBaselineConfig;
import com.gam0zing.newnew_warlock_core.enchant.net.EnchantBaselineSync;
import com.gam0zing.newnew_warlock_core.gametest.ModGameTests;
import com.gam0zing.newnew_warlock_core.gametest.SimpleTestInstance;
import com.gam0zing.newnew_warlock_core.event.AttributeEvt;
import com.gam0zing.newnew_warlock_core.event.PhysicsDefaultComponents;
import com.gam0zing.newnew_warlock_core.registry.ModAttributes;
import com.gam0zing.newnew_warlock_core.registry.ModDataComponents;
import com.gam0zing.newnew_warlock_core.registry.ModMobEffects;
import com.gam0zing.newnew_warlock_core.skill.command.CommandDefinitionExporter;
import com.gam0zing.newnew_warlock_core.skill.command.PointAllocationCommands;
import com.gam0zing.newnew_warlock_core.skill.net.SkillTreeNetwork;
import com.gam0zing.newnew_warlock_core.skill.progress.SkillProgressTypes;
import com.gam0zing.newnew_warlock_core.skill.tree.SkillTreeLoader;
import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(NewNewWarlockCore.MOD_ID)
public class NewNewWarlockCore {
    public static final String MOD_ID = "newnew_warlock_core";
    public static final Logger LOGGER = LogUtils.getLogger();

    public NewNewWarlockCore(IEventBus modEventBus, ModContainer modContainer) {
        ModAttributes.ATTRIBUTES.register(modEventBus);
        modEventBus.addListener(AttributeEvt::onEntityAttributeModification);
        modEventBus.addListener(ModGameTests::register);
        modEventBus.addListener(SimpleTestInstance::registerType);

        // 伤害模块使用的状态效果（仅注册，逻辑后续实现）
        ModMobEffects.MOB_EFFECTS.register(modEventBus);

        // 点数分配命令类型（自定义注册表 newnew_warlock_core:point_allocation_command）
        modEventBus.addListener(PointAllocationCommands::createRegistry);
        PointAllocationCommands.COMMANDS.register(modEventBus);
        // 启动时导出命令定义表，供网页技能树工具导入
        CommandDefinitionExporter.register(modEventBus);

        // 出血按“多来源、多等级、多时长”方式合并，因此注册为容器管理效果
        MobEffectStackRegistry.register(ModMobEffects.BLEEDING, BleedingEffectStack::new);

        // 物理武器/护甲数据组件与默认档案
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        modEventBus.addListener(PhysicsDefaultComponents::onModifyDefaultComponents);

        // 实体速度同步包精度（common 配置）
        modContainer.registerConfig(ModConfig.Type.LOCAL, NetConfig.SPEC);
        modEventBus.addListener((ModConfigEvent.Loading event) -> NetConfig.onConfigChanged(event));
        modEventBus.addListener((ModConfigEvent.Reloading event) -> NetConfig.onConfigChanged(event));

        // 自定义受击部位数据（data/<命名空间>/newnew_body/*.json）
        NeoForge.EVENT_BUS.addListener(BodyPartLoader::onAddReloadListener);

        // 技能树数据（data/<命名空间>/skill_trees/*.json）
        NeoForge.EVENT_BUS.addListener(SkillTreeLoader::onAddReloadListener);

        // 技能树：玩家进度附件、网络同步
        SkillProgressTypes.ATTACHMENT_TYPES.register(modEventBus);
        SkillTreeNetwork.register(modEventBus);

        // 附魔全局基准表（common 配置在这里加载；附魔条目在注册表可用后补齐）
        EnchantBaselineConfig.init();
        EnchantBaselineLoader.register();

        // 村民附魔交易：在 VillagerTradesEvent 里替换成统一接口（不使用 Mixin）

        // 服务端基准快照同步（方案 B）与 /newnew enchant baseline 命令
        EnchantBaselineSync.register(modEventBus);
        EnchantBaselineCommand.register();
    }
}
