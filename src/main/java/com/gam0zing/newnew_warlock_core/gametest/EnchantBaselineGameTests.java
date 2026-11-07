package com.gam0zing.newnew_warlock_core.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.netty.buffer.Unpooled;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.EnchantWithLevelsFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.providers.number.ints.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.enchant.EnchantBaseline;
import com.gam0zing.newnew_warlock_core.enchant.EnchantContext;
import com.gam0zing.newnew_warlock_core.enchant.EnchantScene;
import com.gam0zing.newnew_warlock_core.enchant.VanillaEnchantHooks;
import com.gam0zing.newnew_warlock_core.enchant.WarlockEnchanting;
import com.gam0zing.newnew_warlock_core.enchant.config.EnchantBaselineConfig;
import com.gam0zing.newnew_warlock_core.enchant.net.EnchantBaselineSyncPayload;

/**
 * 附魔系统的 gametest：基准表、默认实现、菜单端到端、战利品、村民交易、网络快照、命令。
 * <p>绝大多数用例是同步的一次性断言，使用 3x3x3 的空结构 {@code run/gameteststructures/empty.snbt}；
 * 附魔台端到端需要摆书架，用 5x3x5 的空结构 {@code run/gameteststructures/enchant_table.snbt}。</p>
 * <p>模板命名空间故意使用模组 ID：{@code neoforge.enabledGameTestNamespaces} 只放行模组命名空间，
 * 而 SNBT 结构加载只看路径、不看命名空间。</p>
 */
public final class EnchantBaselineGameTests {

    private static final Identifier SHARPNESS_ID = Identifier.parse("minecraft:sharpness");

    private EnchantBaselineGameTests() {}

    public static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        register(event, environment, "baseline_matches_registry", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::baselineMatchesRegistry);
        register(event, environment, "global_max_level", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::globalMaxLevelInjectionIsClamped);
        register(event, environment, "table_candidates", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::tableCandidatesMatchVanilla);
        register(event, environment, "table_slot_cost", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::tableSlotCostMatchesVanilla);
        register(event, environment, "extra_power", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::extraPowerStacksAfterCap);
        register(event, environment, "hook_max_level", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::hookMaxLevelIsClamped);
        register(event, environment, "sync_payload", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::syncPayloadRoundTrips);
        register(event, environment, "empty_snapshot", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::emptySnapshotFallsBackToLocal);
        register(event, environment, "baseline_commands", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::baselineCommandsWork);
        register(event, environment, "book_offer", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::enchantBookOfferIsValid);
        register(event, environment, "enchanting_table", ModGameTests.enchantTableStructure(), 100,
            EnchantBaselineGameTests::enchantingTableMenuMatchesVanilla);
        register(event, environment, "anvil_merge", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::anvilMergeRespectsBaselineMaxLevel);
        register(event, environment, "random_loot", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::randomLootEnchantProducesValidResult);
        register(event, environment, "leveled_loot", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::leveledLootEnchantProducesValidResult);
        register(event, environment, "item_offer", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::enchantedItemOfferProducesValidOffer);
        register(event, environment, "trade_loot_cost", ModGameTests.emptyStructure(), 100,
            EnchantBaselineGameTests::tradeLootWritesAdditionalCost);
    }

    private static void register(
        RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
        String name, Identifier structure, int maxTicks,
        java.util.function.Consumer<GameTestHelper> function) {
        event.registerTest(
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "enchant/" + name),
            new SimpleTestInstance(ModGameTests.info(environment, structure, maxTicks), function));
    }

    // ============================================================
    // 基准表
    // ============================================================

    /** 基准表已按注册表生成，且所有条目都在合法区间内。 */
    public static void baselineMatchesRegistry(GameTestHelper helper) {
        RegistryAccess registries = registries(helper);
        Registry<Enchantment> registry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        Map<Identifier, EnchantBaseline.Entry> entries = EnchantBaseline.entries();

        helper.assertTrue(!entries.isEmpty(), "基准表为空，说明数据包重载后没有重建索引");
        helper.assertTrue(
            entries.size() <= registry.keySet().size(),
            "基准表条目 " + entries.size() + " 多于注册表 " + registry.keySet().size());

        for (EnchantBaseline.Entry entry : entries.values()) {
            helper.assertTrue(entry.maxLevel() >= 1 && entry.maxLevel() <= EnchantBaseline.LEVEL_HARD_CAP,
                "最大等级越界：" + entry.id() + " = " + entry.maxLevel());
            helper.assertTrue(entry.weight() >= 1 && entry.weight() <= 1024,
                "权重越界：" + entry.id() + " = " + entry.weight());
            helper.assertTrue(entry.anvilCost() >= 0, "铁砧费用为负：" + entry.id() + " = " + entry.anvilCost());
            helper.assertTrue(entry.minCost(1) >= 0 && entry.maxCost(1) >= 0,
                "成本曲线为负：" + entry.id());
        }

        helper.succeed();
    }

    /** 全局 {@code getMaxLevel} 注入：改基准后不带 Holder 的调用路径也会跟着变，并 clamp 到 255。 */
    public static void globalMaxLevelInjectionIsClamped(GameTestHelper helper) {
        RegistryAccess registries = registries(helper);
        Registry<Enchantment> registry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        Enchantment sharpness = registry.get(SHARPNESS_ID).map(Holder.Reference::value).orElse(null);
        helper.assertTrue(sharpness != null, "注册表里没有 minecraft:sharpness");

        Map<Identifier, EnchantBaseline.Entry> original = new HashMap<>(EnchantBaseline.entries());
        EnchantBaseline.Entry base = original.get(SHARPNESS_ID);
        helper.assertTrue(base != null, "基准表缺少 minecraft:sharpness");

        try {
            EnchantBaseline.rebuild(registries, replace(original, withMaxLevel(base, 30)));
            helper.assertTrue(sharpness.getMaxLevel() == 30,
                "getMaxLevel 未被基准表接管：期望 30，实际 " + sharpness.getMaxLevel());

            EnchantBaseline.rebuild(registries, replace(original, withMaxLevel(base, 999)));
            helper.assertTrue(sharpness.getMaxLevel() == EnchantBaseline.LEVEL_HARD_CAP,
                "999 级未被 clamp 到 255，实际 " + sharpness.getMaxLevel());
            helper.assertTrue(
                EnchantBaseline.maxLevel(sharpness, sharpness.definition().maxLevel()) == EnchantBaseline.LEVEL_HARD_CAP,
                "EnchantBaseline.maxLevel 没有 clamp 到 255");
        } finally {
            EnchantBaseline.rebuild(registries, original);
        }

        helper.assertTrue(sharpness.getMaxLevel() == sharpness.definition().maxLevel(),
            "测试结束后基准表没有恢复");
        helper.succeed();
    }

    /** 默认实现的 100% 兼容性：同种子下附魔台候选与原版 selectEnchantment 完全一致。 */
    public static void tableCandidatesMatchVanilla(GameTestHelper helper) {
        RegistryAccess registries = registries(helper);
        Registry<Enchantment> registry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        HolderSet.Named<Enchantment> tag = registry.get(EnchantmentTags.IN_ENCHANTING_TABLE)
            .orElseThrow(() -> new IllegalStateException("注册表缺少 in_enchanting_table 标签"));

        int[] costs = {1, 5, 10, 20, 30, 40};
        for (int cost : costs) {
            for (long seed = 0; seed < 8; seed++) {
                RandomSource vanillaRandom = RandomSource.create(seed);
                List<EnchantmentInstance> vanilla = EnchantmentHelper.selectEnchantment(
                    vanillaRandom, stack, cost, tag.stream());

                RandomSource hookRandom = RandomSource.create(seed);
                List<EnchantmentInstance> ours = VanillaEnchantHooks.INSTANCE.tableCandidates(
                    EnchantContext.table(null, null, stack, hookRandom), registries, stack, 0, cost, hookRandom);

                helper.assertTrue(ours.size() == vanilla.size(),
                    "cost=" + cost + " seed=" + seed + " 候选数量不一致："
                        + ours.size() + " vs " + vanilla.size());
                for (int index = 0; index < ours.size(); index++) {
                    EnchantmentInstance expected = vanilla.get(index);
                    EnchantmentInstance actual = ours.get(index);
                    helper.assertTrue(actual.enchantment().value() == expected.enchantment().value(),
                        "cost=" + cost + " seed=" + seed + " 第 " + index + " 条附魔不同");
                    helper.assertTrue(actual.level() == expected.level(),
                        "cost=" + cost + " seed=" + seed + " 第 " + index + " 条等级不同："
                            + actual.level() + " vs " + expected.level());
                }
            }
        }

        helper.succeed();
    }

    /** 默认实现的附魔台费用公式与原版一致（power 由调用方按上限截断，这里不再二次截断）。 */
    public static void tableSlotCostMatchesVanilla(GameTestHelper helper) {
        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        int power = Math.min(15, EnchantBaselineConfig.settings().maxPower());

        for (int slot = 0; slot < 3; slot++) {
            for (long seed = 0; seed < 8; seed++) {
                int vanilla = EnchantmentHelper.getEnchantmentCost(RandomSource.create(seed), slot, power, stack);
                RandomSource hookRandom = RandomSource.create(seed);
                int ours = VanillaEnchantHooks.INSTANCE.tableSlotCost(
                    EnchantContext.table(null, null, stack, hookRandom), slot, power, stack, hookRandom);

                helper.assertTrue(ours == vanilla,
                    "slot=" + slot + " seed=" + seed + " 费用不一致：" + ours + " vs " + vanilla);
            }
        }

        // extra_power 默认 0、xp_cost_scale 默认 1.0，因此上面的比较等价于原版。
        helper.assertTrue(EnchantBaselineConfig.settings().extraPower() >= 0, "extra_power 不应为负");
        helper.succeed();
    }

    /** {@code extra_power} 必须叠加在 {@code max_power} 截断之后，且硬上限要包含它（否则配置值会被 cap 吃掉）。 */
    public static void extraPowerStacksAfterCap(GameTestHelper helper) {
        // 默认配置（max_power=15 / extra_power=0）必须与原版完全一致。
        helper.assertTrue(VanillaEnchantHooks.effectivePower(15, 15, 0) == 15,
            "默认配置下 power 应与原版一致：" + VanillaEnchantHooks.effectivePower(15, 15, 0));
        helper.assertTrue(VanillaEnchantHooks.effectivePowerCap(15, 0) == 15,
            "默认配置下硬上限应为 15：" + VanillaEnchantHooks.effectivePowerCap(15, 0));

        // 书架 power 先截到 max_power，再叠加 extra_power。
        int stacked = VanillaEnchantHooks.effectivePower(30, 15, 5);
        helper.assertTrue(stacked == 20, "extra_power 未叠加在截断之后：" + stacked);
        helper.assertTrue(VanillaEnchantHooks.effectivePowerCap(15, 5) == 20,
            "硬上限未包含 extra_power：" + VanillaEnchantHooks.effectivePowerCap(15, 5));

        // 书架 power 低于上限时不应被抬高到 max_power。
        int low = VanillaEnchantHooks.effectivePower(3, 15, 5);
        helper.assertTrue(low == 8, "低书架 power 被错误抬高：" + low);

        // 负值/零值不得产生非法 power。
        helper.assertTrue(VanillaEnchantHooks.effectivePower(-1, 15, 0) == 0, "负书架 power 应归零");
        helper.assertTrue(VanillaEnchantHooks.effectivePowerCap(0, 0) >= 1, "硬上限至少为 1");
        helper.succeed();
    }

    /** 附魔等级的最终上限必须受 255 硬上限保护。 */
    public static void hookMaxLevelIsClamped(GameTestHelper helper) {
        RegistryAccess registries = registries(helper);
        Enchantment sharpness = registries.lookupOrThrow(Registries.ENCHANTMENT)
            .get(SHARPNESS_ID).map(Holder.Reference::value).orElse(null);
        helper.assertTrue(sharpness != null, "注册表里没有 minecraft:sharpness");

        int resolved = WarlockEnchanting.hooks().maxLevel(
            EnchantContext.of(EnchantScene.OTHER), sharpness,
            EnchantBaseline.maxLevel(sharpness, sharpness.definition().maxLevel()));
        helper.assertTrue(resolved >= 1 && resolved <= EnchantBaseline.LEVEL_HARD_CAP,
            "hook 返回的等级越界：" + resolved);

        resolved = WarlockEnchanting.hooks().maxLevel(EnchantContext.of(EnchantScene.OTHER), sharpness, 999);
        helper.assertTrue(resolved == EnchantBaseline.LEVEL_HARD_CAP,
            "hook 没有把 999 clamp 到 255，实际 " + resolved);
        helper.succeed();
    }

    // ============================================================
    // 网络快照
    // ============================================================

    /** 快照 payload 的编解码往返必须无损（含全局设置与 13 个条目字段）。 */
    public static void syncPayloadRoundTrips(GameTestHelper helper) {
        List<EnchantBaseline.Entry> entries = List.copyOf(EnchantBaseline.entries().values());
        helper.assertTrue(!entries.isEmpty(), "基准表为空，无法测试快照");

        EnchantBaselineConfig.GlobalSettings settings = EnchantBaselineConfig.settings();
        EnchantBaselineSyncPayload payload = new EnchantBaselineSyncPayload(
            entries, settings, EnchantBaselineSyncPayload.SCHEMA_VERSION);

        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            EnchantBaselineSyncPayload.STREAM_CODEC.encode(buffer, payload);
            EnchantBaselineSyncPayload decoded = EnchantBaselineSyncPayload.STREAM_CODEC.decode(buffer);

            helper.assertTrue(decoded.schemaVersion() == payload.schemaVersion(), "schemaVersion 不一致");
            helper.assertTrue(decoded.settings().equals(payload.settings()), "全局设置不一致");
            helper.assertTrue(decoded.entries().size() == payload.entries().size(),
                "条目数量不一致：" + decoded.entries().size() + " vs " + payload.entries().size());
            for (int index = 0; index < entries.size(); index++) {
                EnchantBaseline.Entry expected = payload.entries().get(index);
                EnchantBaseline.Entry actual = decoded.entries().get(index);
                helper.assertTrue(actual.equals(expected), "第 " + index + " 条快照不一致：" + actual);
            }
        } finally {
            buffer.release();
        }

        helper.succeed();
    }

    /** 空快照不能把基准表锁成空：必须解锁并回退到本地表。 */
    public static void emptySnapshotFallsBackToLocal(GameTestHelper helper) {
        RegistryAccess registries = registries(helper);
        helper.assertTrue(!EnchantBaseline.entries().isEmpty(), "测试前基准表不应为空");

        EnchantBaselineConfig.applySnapshot(registries, List.of(), EnchantBaselineConfig.settings());

        helper.assertTrue(!EnchantBaseline.entries().isEmpty(), "收到空快照后基准表被清空了");
        helper.assertTrue(EnchantBaseline.entry(SHARPNESS_ID) != null,
            "收到空快照后找不到 minecraft:sharpness，说明没有回退到本地表");
        helper.succeed();
    }

    // ============================================================
    // 命令 / 村民交易
    // ============================================================

    /** {@code /newnew enchant baseline dump|reload|regenerate} 必须能被执行并返回条数。 */
    public static void baselineCommandsWork(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        helper.assertTrue(server != null, "测试世界没有服务端实例");
        CommandDispatcher<CommandSourceStack> dispatcher = server.getCommands().getDispatcher();
        CommandSourceStack source = server.createCommandSourceStack();

        int dumped = execute(helper, dispatcher, source, "newnew enchant baseline dump");
        helper.assertTrue(dumped > 0, "dump 返回 0 条，基准表可能为空");

        int reloaded = execute(helper, dispatcher, source, "newnew enchant baseline reload");
        helper.assertTrue(reloaded > 0, "reload 返回 0 条");

        // regenerate 走的是「按标签重新生成全部条目」的路径，顺便验证生成出来的开关正确。
        int regenerated = execute(helper, dispatcher, source, "newnew enchant baseline regenerate");
        helper.assertTrue(regenerated > 0, "regenerate 返回 0 条");

        EnchantBaseline.Entry sharpness = EnchantBaseline.entry(SHARPNESS_ID);
        helper.assertTrue(sharpness != null, "regenerate 后基准表缺少 minecraft:sharpness");
        helper.assertTrue(sharpness.inTable(), "regenerate 后 sharpness 的 in_table 应为 true");
        EnchantBaseline.Entry swiftSneak = EnchantBaseline.entry(Identifier.parse("minecraft:swift_sneak"));
        helper.assertTrue(swiftSneak != null, "regenerate 后基准表缺少 minecraft:swift_sneak");
        helper.assertTrue(!swiftSneak.inTable() && swiftSneak.treasure(),
            "迅捷潜行应保留「宝藏附魔、不进附魔台」的标签语义");

        helper.succeed();
    }

    /** 村民附魔书交易：等级在交易区间内，价格合法，结果是非空的附魔书。 */
    public static void enchantBookOfferIsValid(GameTestHelper helper) {
        Villager villager = new Villager(EntityTypes.VILLAGER, helper.getLevel());
        RandomSource random = RandomSource.create(1234L);
        MerchantOffer offer = VanillaEnchantHooks.INSTANCE.enchantBookOffer(
            EnchantContext.trade(EnchantScene.TRADE_BOOK, villager, random),
            villager, random, EnchantmentTags.TRADEABLE, 1, 3, 2);

        helper.assertTrue(offer != null, "村民附魔书交易为空");
        helper.assertTrue(offer.getCostA().getCount() >= 1 && offer.getCostA().getCount() <= 64,
            "交易价格越界：" + offer.getCostA().getCount());

        ItemStack result = offer.getResult();
        helper.assertTrue(result.is(Items.ENCHANTED_BOOK) || result.is(Items.BOOK),
            "交易结果不是附魔书：" + result);
        if (result.is(Items.ENCHANTED_BOOK)) {
            ItemEnchantments enchantments = result.getOrDefault(
                DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
            helper.assertTrue(!enchantments.isEmpty(), "附魔书没有任何附魔");
            for (Holder<Enchantment> holder : enchantments.keySet()) {
                int level = enchantments.getLevel(holder);
                helper.assertTrue(level >= 1 && level <= 3, "交易附魔等级越界：" + level);
            }
        }

        helper.succeed();
    }

    // ============================================================
    // 附魔台（端到端）
    // ============================================================

    /**
     * 用真实的 {@link EnchantmentMenu} 走一遍「摆好书架 → 放物品 → 生成费用与线索 → 点击附魔」。
     * <ol>
     *     <li>三行费用必须与同种子的原版 {@link EnchantmentHelper#getEnchantmentCost} 完全一致（书架 power 截到 15）；</li>
     *     <li>线索必须与同种子的候选结果一致；</li>
     *     <li>点击后物品实际拿到的附魔里必须包含线索那一条。</li>
     * </ol>
     */
    public static void enchantingTableMenuMatchesVanilla(GameTestHelper helper) {
        RegistryAccess registries = registries(helper);
        Registry<Enchantment> registry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        IdMap<Holder<Enchantment>> ids = registry.asHolderIdMap();

        helper.setBlock(2, 0, 2, Blocks.ENCHANTING_TABLE);
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            helper.setBlock(2 + offset.getX(), offset.getY(), 2 + offset.getZ(), Blocks.BOOKSHELF);
        }

        BlockPos tablePos = helper.absolutePos(new BlockPos(2, 0, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        EnchantmentMenu menu = new EnchantmentMenu(
            0, player.getInventory(), ContainerLevelAccess.create(helper.getLevel(), tablePos));

        ItemStack stack = new ItemStack(Items.DIAMOND_SWORD);
        menu.getSlot(0).set(stack);
        menu.getSlot(1).set(new ItemStack(Items.LAPIS_LAZULI, 64));

        // 1. 费用：书架摆满后 power 超过 15，必须按原版截断，随机序列也必须与原版一致。
        int seed = menu.getEnchantmentSeed();
        RandomSource expectedRandom = RandomSource.create(seed);
        for (int slot = 0; slot < 3; slot++) {
            int expected = EnchantmentHelper.getEnchantmentCost(expectedRandom, slot, 15, stack);
            if (expected < slot + 1) {
                expected = 0;
            }
            helper.assertTrue(menu.costs[slot] == expected,
                "slot=" + slot + " 菜单费用 " + menu.costs[slot] + " 与原版 " + expected + " 不一致");
        }

        // 2. 线索：必须来自「enchantmentSeed + slot」这个种子下的候选列表。
        for (int slot = 0; slot < 3; slot++) {
            if (menu.costs[slot] <= 0) {
                helper.assertTrue(menu.enchantClue[slot] == -1 && menu.levelClue[slot] == -1,
                    "slot=" + slot + " 没有费用却显示了线索");
                continue;
            }

            RandomSource random = RandomSource.create(seed + slot);
            List<EnchantmentInstance> candidates = VanillaEnchantHooks.INSTANCE.tableCandidates(
                EnchantContext.table(player, tablePos, stack, random),
                registries, stack, slot, menu.costs[slot], random);
            helper.assertTrue(!candidates.isEmpty(), "slot=" + slot + " 没有候选附魔");

            EnchantmentInstance clue = candidates.get(random.nextInt(candidates.size()));
            helper.assertTrue(menu.enchantClue[slot] == ids.getId(clue.enchantment()),
                "slot=" + slot + " 线索附魔与同种子候选不一致");
            helper.assertTrue(menu.levelClue[slot] == clue.level(),
                "slot=" + slot + " 线索等级 " + menu.levelClue[slot] + " 与候选 " + clue.level() + " 不一致");
        }

        // 3. 点击：结果里必须包含线索对应的那条附魔（同样的种子必须给出同样的结果）。
        helper.assertTrue(menu.costs[0] > 0, "第 0 行费用为 0，无法验证点击流程");
        int clueId = menu.enchantClue[0];
        int clueLevel = menu.levelClue[0];

        player.giveExperienceLevels(100);
        helper.assertTrue(menu.clickMenuButton(player, 0), "点击附魔按钮失败");

        ItemEnchantments enchantments = menu.getSlot(0).getItem()
            .getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        helper.assertTrue(!enchantments.isEmpty(), "点击后物品没有得到附魔");

        boolean matched = false;
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (ids.getId(holder) == clueId && enchantments.getLevel(holder) == clueLevel) {
                matched = true;
            }
        }
        helper.assertTrue(matched, "点击结果与线索不一致：clue=" + clueId + "/" + clueLevel);

        menu.getSlot(0).set(ItemStack.EMPTY);
        menu.getSlot(1).set(ItemStack.EMPTY);
        helper.succeed();
    }

    // ============================================================
    // 铁砧（端到端）
    // ============================================================

    /**
     * 两本 5 级锋利书在铁砧合并：
     * <ul>
     *     <li>默认基准（锋利最大 5）时不能升到 6；</li>
     *     <li>把基准改成 10 后必须升到 6，证明铁砧读的是基准表的 {@code max_level}；</li>
     *     <li>费用保持合法区间（1~39）。</li>
     * </ul>
     */
    public static void anvilMergeRespectsBaselineMaxLevel(GameTestHelper helper) {
        RegistryAccess registries = registries(helper);
        Registry<Enchantment> registry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> holder = registry.get(SHARPNESS_ID).orElse(null);
        helper.assertTrue(holder != null, "注册表里没有 minecraft:sharpness");

        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);

        menu.getSlot(0).set(enchantedBook(holder, 5));
        menu.getSlot(1).set(enchantedBook(holder, 5));

        ItemStack defaultResult = menu.getSlot(2).getItem();
        helper.assertTrue(!defaultResult.isEmpty(), "默认基准下铁砧没有产出合并结果");
        helper.assertTrue(storedLevel(defaultResult, holder) == 5,
            "基准最大等级 5 时不应合并到 6，实际 " + storedLevel(defaultResult, holder));
        helper.assertTrue(menu.getCost() >= 1 && menu.getCost() <= 39,
            "铁砧费用越界：" + menu.getCost());

        Map<Identifier, EnchantBaseline.Entry> original = new HashMap<>(EnchantBaseline.entries());
        EnchantBaseline.Entry base = original.get(SHARPNESS_ID);
        helper.assertTrue(base != null, "基准表里没有 minecraft:sharpness");
        try {
            EnchantBaseline.rebuild(registries, replace(original, withMaxLevel(base, 10)));
            menu.getSlot(0).set(enchantedBook(holder, 5));

            ItemStack raisedResult = menu.getSlot(2).getItem();
            helper.assertTrue(!raisedResult.isEmpty(), "基准最大等级改成 10 后铁砧没有产出结果");
            helper.assertTrue(storedLevel(raisedResult, holder) == 6,
                "基准最大等级 10 时应合并到 6，实际 " + storedLevel(raisedResult, holder));
        } finally {
            EnchantBaseline.rebuild(registries, original);
        }

        helper.succeed();
    }

    // ============================================================
    // 村民交易 / 战利品
    // ============================================================

    /** 战利品随机附魔：附魔书 → 1 条合法等级附魔；装备 → 只出兼容附魔。 */
    public static void randomLootEnchantProducesValidResult(GameTestHelper helper) {
        RandomSource random = RandomSource.create(42L);

        ItemStack book = new ItemStack(Items.BOOK);
        ItemStack enchantedBook = VanillaEnchantHooks.INSTANCE.randomLootEnchant(
            lootContext(helper, EnchantScene.LOOT_BOOK, book, random), book, random, Optional.empty(), false);

        helper.assertTrue(enchantedBook.is(Items.ENCHANTED_BOOK), "附魔书没有被转换成附魔书：" + enchantedBook);
        ItemEnchantments stored = enchantedBook.getOrDefault(
            DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
        helper.assertTrue(stored.size() == 1, "附魔书应恰好 1 条附魔，实际 " + stored.size());
        assertLevelsWithinBaseline(helper, stored, "战利品附魔书");

        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        ItemStack enchantedSword = VanillaEnchantHooks.INSTANCE.randomLootEnchant(
            lootContext(helper, EnchantScene.LOOT_TOOL, sword, RandomSource.create(43L)),
            sword, RandomSource.create(43L), Optional.empty(), true);

        helper.assertTrue(enchantedSword.is(Items.DIAMOND_SWORD), "装备被换成了其他物品：" + enchantedSword);
        ItemEnchantments onSword = enchantedSword.getOrDefault(
            DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        helper.assertTrue(!onSword.isEmpty(), "装备没有得到附魔");
        for (Holder<Enchantment> holder : onSword.keySet()) {
            helper.assertTrue(holder.value().canEnchant(enchantedSword),
                "onlyCompatible 时出现了不兼容附魔：" + holder.value().description().getString());
        }

        helper.succeed();
    }

    /** 战利品定级附魔：给定等级必须给物品加上合法区间的附魔。 */
    public static void leveledLootEnchantProducesValidResult(GameTestHelper helper) {
        RandomSource random = RandomSource.create(44L);
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);

        ItemStack result = VanillaEnchantHooks.INSTANCE.leveledLootEnchant(
            lootContext(helper, EnchantScene.LOOT_TOOL, sword, random),
            sword, 30, random, Optional.empty());

        helper.assertTrue(result.is(Items.DIAMOND_SWORD), "定级附魔把物品换成了其他东西：" + result);
        ItemEnchantments enchantments = result.getOrDefault(
            DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        helper.assertTrue(!enchantments.isEmpty(), "定级战利品附魔没有产出附魔");
        assertLevelsWithinBaseline(helper, enchantments, "定级战利品");

        helper.succeed();
    }

    /** 村民附魔物品交易：结果必须是附魔好的目标物品，价格在 [基础价, 64]。 */
    public static void enchantedItemOfferProducesValidOffer(GameTestHelper helper) {
        Villager villager = new Villager(EntityTypes.VILLAGER, helper.getLevel());
        RandomSource random = RandomSource.create(99L);

        MerchantOffer offer = VanillaEnchantHooks.INSTANCE.enchantedItemOffer(
            EnchantContext.trade(EnchantScene.TRADE_ITEM, villager, random),
            villager, random, new ItemStack(Items.DIAMOND_SWORD),
            EnchantmentTags.ON_TRADED_EQUIPMENT, 5, 3, 2, 0.05F);

        helper.assertTrue(offer != null, "附魔物品交易返回空");
        helper.assertTrue(offer.getResult().is(Items.DIAMOND_SWORD),
            "交易结果不是钻石剑：" + offer.getResult());
        ItemEnchantments enchantments = offer.getResult().getOrDefault(
            DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        helper.assertTrue(!enchantments.isEmpty(), "交易结果没有附魔");
        assertLevelsWithinBaseline(helper, enchantments, "村民附魔物品");

        int price = offer.getCostA().getCount();
        helper.assertTrue(price >= 5 && price <= 64, "交易价格越界：" + price);

        helper.succeed();
    }

    /**
     * 26.3 的村民交易由 {@code given_item_modifier} 生成，价格来自 {@code ADDITIONAL_TRADE_COST}。
     * <p>组件缺失会让 {@code VillagerTrade#getOffer} 得到 0 价并丢弃整笔交易（表现为图书管理员没有附魔书交易），
     * 因此这里直接构造交易用的 {@link LootContext} 跑两个原版函数，验证价格会被写回。</p>
     */
    public static void tradeLootWritesAdditionalCost(GameTestHelper helper) {
        RegistryAccess registries = registries(helper);
        Registry<Enchantment> registry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        HolderSet.Named<Enchantment> tradeable = registry.get(EnchantmentTags.TRADEABLE)
            .orElseThrow(() -> new IllegalStateException("注册表缺少 tradeable 标签"));
        HolderSet.Named<Enchantment> equipment = registry.get(EnchantmentTags.ON_TRADED_EQUIPMENT)
            .orElseThrow(() -> new IllegalStateException("注册表缺少 on_traded_equipment 标签"));

        Villager villager = new Villager(EntityTypes.VILLAGER, helper.getLevel());
        LootContext context = tradeLootContext(helper, villager);

        // 1. 附魔书（图书管理员的 gives 直接是附魔书）。
        ItemStack book = EnchantRandomlyFunction.randomEnchantment()
            .withOneOf(tradeable)
            .includeAdditionalCostComponent()
            .build()
            .apply(new ItemStack(Items.ENCHANTED_BOOK), context);

        helper.assertTrue(book.is(Items.ENCHANTED_BOOK), "交易结果不是附魔书：" + book);
        helper.assertTrue(!book.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty(),
            "交易附魔书没有附魔，会被 filtered 丢弃");
        Integer bookCost = book.get(DataComponents.ADDITIONAL_TRADE_COST);
        helper.assertTrue(bookCost != null && bookCost >= 1 && bookCost <= 64,
            "附魔书附加价格非法：" + bookCost);

        // 2. 附魔装备：附加价格等于本次附魔等级。
        ItemStack boots = new EnchantWithLevelsFunction.Builder(
                Holder.<ContextIntProvider>direct(new ConstantValue(10)))
            .withOptions(equipment)
            .includeAdditionalCostComponent()
            .build()
            .apply(new ItemStack(Items.DIAMOND_BOOTS), context);

        helper.assertTrue(boots.is(Items.DIAMOND_BOOTS), "交易结果不是钻石靴：" + boots);
        helper.assertTrue(!boots.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty(),
            "交易装备没有附魔");
        Integer itemCost = boots.get(DataComponents.ADDITIONAL_TRADE_COST);
        helper.assertTrue(itemCost != null && itemCost == 10,
            "装备附加价格应为附魔等级 10，实际 " + itemCost);

        helper.succeed();
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private static RegistryAccess registries(GameTestHelper helper) {
        return helper.getLevel().registryAccess();
    }

    /** 直接经 Brigadier 执行命令并拿到返回值（{@code Commands#performPrefixedCommand} 是 void）。 */
    private static int execute(
        GameTestHelper helper, CommandDispatcher<CommandSourceStack> dispatcher,
        CommandSourceStack source, String command) {
        try {
            return dispatcher.execute(command, source);
        } catch (CommandSyntaxException exception) {
            helper.fail("命令执行失败：" + command + " -> " + exception.getMessage());
            return -1;
        }
    }

    private static EnchantBaseline.Entry withMaxLevel(EnchantBaseline.Entry entry, int maxLevel) {
        return new EnchantBaseline.Entry(
            entry.id(),
            maxLevel,
            entry.weight(),
            entry.minCostBase(),
            entry.minCostPerLevelAboveFirst(),
            entry.maxCostBase(),
            entry.maxCostPerLevelAboveFirst(),
            entry.anvilCost(),
            entry.inTable(),
            entry.treasure(),
            entry.tradeable(),
            entry.onRandomLoot(),
            entry.onTradedEquipment());
    }

    private static Map<Identifier, EnchantBaseline.Entry> replace(
        Map<Identifier, EnchantBaseline.Entry> base, EnchantBaseline.Entry replaced) {

        Map<Identifier, EnchantBaseline.Entry> copy = new HashMap<>(base);
        copy.put(replaced.id(), replaced);
        return copy;
    }

    /** 构造一个带关卡（从而带 registry access）的战利品上下文，避免依赖 {@code LootContext} 的复杂构建。 */
    private static EnchantContext lootContext(
        GameTestHelper helper, EnchantScene scene, ItemStack stack, RandomSource random) {
        return new EnchantContext(scene, null, null, null, helper.getLevel(), null, stack, random);
    }

    /** 复刻 {@code AbstractVillager#addOffersFromTradeSet} 的交易上下文：交易实体 + 允许附加价格。 */
    private static LootContext tradeLootContext(GameTestHelper helper, Villager villager) {
        LootParams params = new LootParams.Builder(helper.getLevel())
            .withParameter(LootContextParams.ORIGIN, villager.position())
            .withParameter(LootContextParams.THIS_ENTITY, villager)
            .withParameter(LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, Unit.INSTANCE)
            .create(LootContextParamSets.VILLAGER_TRADE);
        return new LootContext.Builder(params).create(Optional.<Identifier>empty());
    }

    private static ItemStack enchantedBook(Holder<Enchantment> enchantment, int level) {
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
        mutable.set(enchantment, level);
        book.set(DataComponents.STORED_ENCHANTMENTS, mutable.toImmutable());
        return book;
    }

    private static int storedLevel(ItemStack stack, Holder<Enchantment> enchantment) {
        return stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(enchantment);
    }

    /** 断言每条附魔的等级都落在「1 ~ 基准最大等级」内。 */
    private static void assertLevelsWithinBaseline(
        GameTestHelper helper, ItemEnchantments enchantments, String scene) {
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            int level = enchantments.getLevel(holder);
            int max = EnchantBaseline.maxLevel(holder, holder.value().definition().maxLevel());
            helper.assertTrue(level >= 1 && level <= max,
                scene + " 的 " + holder.value().description().getString() + " 等级越界：" + level + " > " + max);
        }
    }
}
