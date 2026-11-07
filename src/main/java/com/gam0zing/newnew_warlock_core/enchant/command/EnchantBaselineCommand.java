package com.gam0zing.newnew_warlock_core.enchant.command;

import java.util.Comparator;
import java.util.List;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.enchant.EnchantBaseline;
import com.gam0zing.newnew_warlock_core.enchant.config.EnchantBaselineConfig;

/**
 * {@code /newnew enchant baseline reload|regenerate|dump}。
 * <ul>
 *     <li><b>reload</b>：从磁盘重新读取当前生效表（改完 TOML 不用重启）；</li>
 *     <li><b>regenerate</b>：按当前注册表强制重写全部条目（会覆盖手动修改，需 op）；</li>
 *     <li><b>dump</b>：在聊天栏显示摘要，并把完整条目写进日志。</li>
 * </ul>
 */
public final class EnchantBaselineCommand {

    private EnchantBaselineCommand() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(EnchantBaselineCommand::onRegisterCommands);
    }

    private static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("newnew")
            .then(Commands.literal("enchant")
                .then(Commands.literal("baseline")
                    .then(Commands.literal("reload")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .executes(EnchantBaselineCommand::reload))
                    .then(Commands.literal("regenerate")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .executes(EnchantBaselineCommand::regenerate))
                    .then(Commands.literal("dump")
                        .executes(EnchantBaselineCommand::dump)))));
    }

    private static int reload(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        EnchantBaselineConfig.reloadFromDisk(server.registryAccess());

        int count = EnchantBaseline.entries().size();
        source.sendSuccess(() -> Component.literal(
            "[附魔] 已从磁盘重载基准表：" + count + " 条，来源 " + EnchantBaselineConfig.describeSource()), true);
        return count;
    }

    private static int regenerate(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        MinecraftServer server = source.getServer();
        int count = EnchantBaselineConfig.regenerate(server.registryAccess());

        source.sendSuccess(() -> Component.literal(
            "[附魔] 已按当前注册表重写基准表：" + count + " 条，来源 " + EnchantBaselineConfig.describeSource()), true);
        return count;
    }

    private static int dump(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        EnchantBaselineConfig.GlobalSettings settings = EnchantBaselineConfig.settings();
        List<EnchantBaseline.Entry> entries = EnchantBaseline.entries().values().stream()
            .sorted(Comparator.comparing(entry -> entry.id().toString()))
            .toList();

        source.sendSuccess(() -> Component.literal(
            "[附魔] 基准表 " + entries.size() + " 条，来源 " + EnchantBaselineConfig.describeSource()), false);
        source.sendSuccess(() -> Component.literal(
            "[附魔] table: max_power=" + settings.maxPower()
                + ", xp_cost_scale=" + settings.xpCostScale()
                + ", extra_power=" + settings.extraPower()
                + "; anvil: max_cost=" + settings.anvilMaxCost()), false);

        for (EnchantBaseline.Entry entry : entries) {
            NewNewWarlockCore.LOGGER.info(
                "[enchant] dump {} max={} weight={} min={}+{} max={}+{} anvil={} in_table={} treasure={} tradeable={} loot={} traded={}",
                entry.id(), entry.maxLevel(), entry.weight(),
                entry.minCostBase(), entry.minCostPerLevelAboveFirst(),
                entry.maxCostBase(), entry.maxCostPerLevelAboveFirst(),
                entry.anvilCost(), entry.inTable(), entry.treasure(), entry.tradeable(),
                entry.onRandomLoot(), entry.onTradedEquipment());
        }

        source.sendSuccess(() -> Component.literal("[附魔] 完整条目已写入日志（搜索 \"[enchant] dump\"）"), false);
        return entries.size();
    }
}
