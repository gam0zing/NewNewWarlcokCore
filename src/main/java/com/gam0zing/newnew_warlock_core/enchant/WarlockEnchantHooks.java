package com.gam0zing.newnew_warlock_core.enchant;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.MerchantOffer;

/**
 * 附魔系统的统一改造接口。
 * <p>原版六个入口（附魔台、铁砧、战利品附魔书、战利品工具附魔、村民附魔书、村民附魔物品）
 * 都会被 Mixin 替换成这里的方法调用；默认实现 {@link VanillaEnchantHooks} 保持原版行为，
 * 通过 {@link WarlockEnchanting#setHooks(WarlockEnchantHooks)} 替换成自己的实现。</p>
 * <p>所有返回等级/费用的方法都应在实现内自行 clamp 到合法区间（最大等级 1~255）。</p>
 */
public interface WarlockEnchantHooks {

    // ============================================================
    // 附魔台
    // ============================================================

    /**
     * 计算附魔台的附魔能力（power）。
     *
     * @param ctx         附魔台上下文（含玩家、坐标、随机源）
     * @param stack       放入附魔槽的物品
     * @param vanillaPower 原版书架扫描得到的 power（默认最大 15）
     * @return 改造后的 power
     */
    int tablePower(EnchantContext ctx, ItemStack stack, int vanillaPower);

    /**
     * 附魔台 power 的硬上限。
     * <p>原版在 {@code EnchantmentHelper#getEnchantmentCost} 中把 power 截断为 15；该方法取代这个截断。
     * 默认实现返回 {@code table.max_power + table.extra_power}，因为默认的 {@link #tablePower} 是
     * 「书架 power 先截到 {@code max_power}，再叠加 {@code extra_power}」。</p>
     */
    int tablePowerCap(EnchantContext ctx);

    /**
     * 计算附魔台第 {@code slot} 行（0~2）要消耗的经验等级。
     *
     * @param power 已经过 {@link #tablePower} 计算、并按 {@link #tablePowerCap} 截断的 power
     */
    int tableSlotCost(EnchantContext ctx, int slot, int power, ItemStack stack, RandomSource random);

    /**
     * 生成附魔台某一行的候选附魔列表。
     * <p>原版逻辑：按 {@code in_enchanting_table} 标签过滤 → {@code selectEnchantment} 按 weight 加权抽取 →
     * 附魔书移除其中一条。调用方会在每次调用前用 {@code enchantmentSeed + slot} 重置随机种子，
     * 因此该方法必须只使用传入的 {@code random}，保证三个线索与实际结果一致。</p>
     *
     * @param cost 该行的经验等级（同时作为原版附魔强度）
     * @return 候选列表；返回 {@code null} 会被调用方当作空列表处理（但仍建议直接返回空列表）
     */
    List<EnchantmentInstance> tableCandidates(
        EnchantContext ctx, RegistryAccess registries, ItemStack stack, int slot, int cost, RandomSource random);

    // ============================================================
    // 铁砧 / 全局兜底
    // ============================================================

    /**
     * 计算附魔的最终最大等级。
     * <p>调用方已经先读取了全局基准表（{@link EnchantBaseline}），并把结果作为 {@code baselineMaxLevel} 传入；
     * 实现可以在此基础上按玩家、物品、场景继续加成。</p>
     *
     * @param enchantment      附魔实例（没有 {@code Holder} 的场景，例如铁砧与全局兜底，只能用实例）
     * @param baselineMaxLevel 全局基准表里的最大等级（无基准时是原版值）
     */
    int maxLevel(EnchantContext ctx, Enchantment enchantment, int baselineMaxLevel);

    // ============================================================
    // 战利品
    // ============================================================

    /**
     * 战利品随机附魔（{@code EnchantRandomlyFunction}）。
     * <p>原版对附魔书与装备分别走这条逻辑：附魔书直接生成附魔书；装备按 {@code onlyCompatible} 过滤兼容性。
     * 返回 {@code null} 会被调用方当作「保持原物品」处理。</p>
     */
    ItemStack randomLootEnchant(
        EnchantContext ctx, ItemStack stack, RandomSource random,
        Optional<HolderSet<Enchantment>> options, boolean onlyCompatible);

    /**
     * 战利品定级附魔（{@code EnchantWithLevelsFunction}）。
     * <p>返回 {@code null} 会被调用方当作「保持原物品」处理。</p>
     */
    ItemStack leveledLootEnchant(
        EnchantContext ctx, ItemStack stack, int level, RandomSource random,
        Optional<? extends HolderSet<Enchantment>> options);

    // ============================================================
    // 村民交易
    // ============================================================

    /**
     * 计算村民附魔书交易的附加价格，写入 {@code ADDITIONAL_TRADE_COST} 数据组件。
     * <p>26.3 的交易价格由 {@code VillagerTrade#getOffer} 合成：
     * {@code wants.toItemCost(context, 组件值)}；组件缺失或为 0 时价格就是 {@code wants} 的原始数量，
     * 最终数量小于 1 会导致整笔交易不生成。因此这里必须返回至少 1。</p>
     * <p>默认公式复刻原版 {@code EnchantRandomlyFunction}：
     * {@code 2 + rand(5 + 10*level) + 3*level}，上限 64。
     * {@code double_trade_price} 的翻倍由 {@code VillagerTrade#getOffer} 读取组件后处理，这里不要重复翻倍。</p>
     *
     * @param book  生成的附魔书
     * @param level 书中的附魔等级（多个附魔时取最高等级）
     * @return 附加绿宝石价格，至少 1
     */
    int tradeBookCost(EnchantContext ctx, ItemStack book, int level, RandomSource random);

    /**
     * 计算村民附魔装备交易的附加价格，写入 {@code ADDITIONAL_TRADE_COST} 数据组件。
     * <p>默认公式复刻原版 {@code EnchantWithLevelsFunction}：附加价格等于附魔等级，上限 64。</p>
     *
     * @param stack 已附魔的装备
     * @param level 本次附魔使用的等级
     * @return 附加绿宝石价格，至少 1
     */
    int tradeItemCost(EnchantContext ctx, ItemStack stack, int level, RandomSource random);

    /**
     * 生成村民附魔书交易。
     * <p>原版公式：等级 {@code [max(1,minLevel), min(基准最大等级,maxLevel)]}；价格
     * {@code 2 + rand(5+10*level) + 3*level}，{@code double_trade_price} 翻倍，上限 64。</p>
     */
    MerchantOffer enchantBookOffer(
        EnchantContext ctx, Entity trader, RandomSource random,
        TagKey<Enchantment> tradeableTag, int minLevel, int maxLevel, int villagerXp);

    /**
     * 生成村民附魔物品交易。
     * <p>原版公式：附魔等级 {@code 5 + rand(15)}；价格 {@code min(baseEmeraldCost + level, 64)}。</p>
     */
    MerchantOffer enchantedItemOffer(
        EnchantContext ctx, Entity trader, RandomSource random, ItemStack item,
        TagKey<Enchantment> equipmentTag, int baseEmeraldCost, int maxUses, int villagerXp, float priceMultiplier);
}
