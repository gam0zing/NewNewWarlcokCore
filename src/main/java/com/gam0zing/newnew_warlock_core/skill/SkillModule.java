package com.gam0zing.newnew_warlock_core.skill;

import com.gam0zing.newnew_warlock_core.core.ModModule;
import com.gam0zing.newnew_warlock_core.skill.command.CommandDefinitionExporter;
import com.gam0zing.newnew_warlock_core.skill.command.PointAllocationCommands;
import com.gam0zing.newnew_warlock_core.skill.net.SkillTreeNetwork;
import com.gam0zing.newnew_warlock_core.skill.progress.SkillProgressTypes;
import com.gam0zing.newnew_warlock_core.skill.tree.SkillTreeLoader;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.common.NeoForge;

/**
 * 技能模块：点数分配命令、技能树数据、玩家进度附件与网络同步。
 */
public final class SkillModule implements ModModule {

    @Override
    public String id() {
        return "skill";
    }

    @Override
    public int priority() {
        return 60;
    }

    @Override
    public void init(IEventBus modEventBus, ModContainer modContainer) {
        // 点数分配命令类型（自定义注册表 newnew_warlock_core:point_allocation_command）
        modEventBus.addListener(PointAllocationCommands::createRegistry);
        PointAllocationCommands.COMMANDS.register(modEventBus);
        // 启动时导出命令定义表，供网页技能树工具导入
        CommandDefinitionExporter.register(modEventBus);

        // 技能树数据（data/<命名空间>/skill_trees/*.json）
        NeoForge.EVENT_BUS.addListener(SkillTreeLoader::onAddReloadListener);

        // 技能树：玩家进度附件、网络同步
        SkillProgressTypes.ATTACHMENT_TYPES.register(modEventBus);
        SkillTreeNetwork.register(modEventBus);
    }
}
