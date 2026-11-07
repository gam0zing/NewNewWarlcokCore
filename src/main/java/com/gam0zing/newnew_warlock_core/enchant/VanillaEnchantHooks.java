package com.gam0zing.newnew_warlock_core.enchant;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import net.minecraft.util.Util;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantable;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import com.gam0zing.newnew_warlock_core.enchant.config.EnchantBaselineConfig;

/**
 * 原版行为的默认实现。
 * <p>附魔台部分直接调用原版方法，保证替换 Mixin 后不改变任何结果；
 * 战利品/村民部分在复刻原版公式时使用 {@link EnchantBaseline} 的全局基准最大等级，
 * 当基准表与注册表默认值一致时结果与原版完全相同。</p>
 */
public enum VanillaEnchantHooks implements WarlockEnchantHooks {
    INSTANCE;

    /** 原版附魔台的 power 硬上限（{@code EnchantmentHelper#getEnchantmentCost} 中的 15）。 */
    public static final int VANILLA_POWER_CAP = 15;

    /**
     * 把「书架扫描得到的 power」换算成实际使用的 power。
     * <p>先把书架 power 截断到 {@code max_power}（原版在 {@code getEnchantmentCost} 里写死的 15），
     * 再叠加 {@code extra_power}——叠加发生在截断之后，因此 {@code extra_power} 不会被 {@code max_power} 吃掉。</p>
     */
    public static int effectivePower(int vanillaPower, int maxPower, int extraPower) {
        return Math.max(0, Math.min(Math.max(0, vanillaPower), maxPower) + extraPower);
    }

    /** 实际 power 的硬上限：{@code max_power + extra_power}；默认配置（15 / 0）下等于原版 15。 */
    public static int effectivePowerCap(int maxPower, int extraPower) {
        return Math.max(1, maxPower + extraPower);
    }

    /** {@return 物品的附魔能力；26.3 起由 {@code enchantable} 数据组件提供} */
    private static int enchantabilityOf(ItemStack stack) {
        Enchantable enchantable = stack.get(DataComponents.ENCHANTABLE);
        return enchantable == null ? 0 : enchantable.value();
    }

    @Override
    public int tablePower(EnchantContext ctx, ItemStack stack, int vanillaPower) {
        EnchantBaselineConfig.GlobalSettings settings = EnchantBaselineConfig.settings();
        return effectivePower(vanillaPower, settings.maxPower(), settings.extraPower());
    }

    @Override
    public int tablePowerCap(EnchantContext ctx) {
        EnchantBaselineConfig.GlobalSettings settings = EnchantBaselineConfig.settings();
        return effectivePowerCap(settings.maxPower(), settings.extraPower());
    }

    /**
     * 复刻原版 {@code EnchantmentHelper#getEnchantmentCost} 的公式，但 power 的上限改由
     * {@link #tablePowerCap} 决定（原版写死 15），并额外乘 {@code table.xp_cost_scale}。
     * <p>{@code power} 已由调用方按上限截断，这里不再二次截断，否则 {@code extra_power} 会被吃掉。
     * 默认配置（15 / 1.0 / 0）下随机数消耗与结果和原版完全一致。</p>
     */
    @Override
    public int tableSlotCost(EnchantContext ctx, int slot, int power, ItemStack stack, RandomSource random) {
        int enchantability = enchantabilityOf(stack);
        if (enchantability <= 0) {
            return 0;
        }

        int effectivePower = Math.max(0, power);
        int roll = random.nextInt(8) + 1 + (effectivePower >> 1) + random.nextInt(effectivePower + 1);
        int cost = slot == 0
            ? Math.max(roll / 3, 1)
            : (slot == 1 ? roll * 2 / 3 + 1 : Math.max(roll, effectivePower * 2));

        double scale = EnchantBaselineConfig.settings().xpCostScale();
        return scale == 1.0D ? cost : Math.max(0, (int) Math.round(cost * scale));
    }

    /**
     * 复刻原版 {@code EnchantmentHelper#selectEnchantment} + {@code getAvailableEnchantmentResults}，
     * 但把「最大等级 / 成本曲线 / 权重」的来源从附魔自身的 {@code definition} 换成 {@link EnchantBaseline}
     * （最大等级再经过 {@link WarlockEnchanting#hooks()} 做玩家相关修正）。
     * <p>基准表与注册表默认值一致时，随机数消耗与结果和原版完全相同。</p>
     */
    @Override
    public List<EnchantmentInstance> tableCandidates(
        EnchantContext ctx, RegistryAccess registries, ItemStack stack, int slot, int cost, RandomSource random) {
        Optional<HolderSet.Named<Enchantment>> tag =
            registries.lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
        if (tag.isEmpty()) {
            return List.of();
        }

        List<EnchantmentInstance> result = this.selectTableEnchantments(ctx, random, stack, cost, tag.get().stream());
        // 原版怪癖：附魔书有多个候选时随机移除一条。
        if (stack.is(Items.BOOK) && result.size() > 1) {
            result.remove(random.nextInt(result.size()));
        }
        return result;
    }

    /** 原版 {@code EnchantmentHelper#selectEnchantment} 的等价实现。 */
    private List<EnchantmentInstance> selectTableEnchantments(
        EnchantContext ctx, RandomSource random, ItemStack stack, int level, Stream<Holder<Enchantment>> candidates) {
        List<EnchantmentInstance> result = new ArrayList<>();
        int enchantability = enchantabilityOf(stack);
        if (enchantability <= 0) {
            return result;
        }

        level += 1 + random.nextInt(enchantability / 4 + 1) + random.nextInt(enchantability / 4 + 1);
        float variance = (random.nextFloat() + random.nextFloat() - 1.0F) * 0.15F;
        level = Mth.clamp(Math.round(level + level * variance), 1, Integer.MAX_VALUE);

        List<EnchantmentInstance> available = this.availableTableEnchantments(ctx, level, stack, candidates);
        if (available.isEmpty()) {
            return result;
        }

        this.pickTableEnchantment(random, available).ifPresent(result::add);
        while (random.nextInt(50) <= level) {
            if (!result.isEmpty()) {
                EnchantmentInstance last = result.get(result.size() - 1);
                available.removeIf(instance -> !Enchantment.areCompatible(last.enchantment(), instance.enchantment()));
            }
            if (available.isEmpty()) {
                break;
            }

            this.pickTableEnchantment(random, available).ifPresent(result::add);
            level /= 2;
        }
        return result;
    }

    /** 原版 {@code EnchantmentHelper#getAvailableEnchantmentResults} 的等价实现，成本曲线与上限来自基准表。 */
    private List<EnchantmentInstance> availableTableEnchantments(
        EnchantContext ctx, int level, ItemStack stack, Stream<Holder<Enchantment>> candidates) {
        List<EnchantmentInstance> result = new ArrayList<>();
        candidates.filter(holder -> this.isInTable(holder)).filter(stack::isPrimaryItemFor).forEach(holder -> {
            Enchantment enchantment = holder.value();
            EnchantBaseline.Entry entry = EnchantBaseline.get(holder);
            int maxLevel = EnchantBaseline.maxLevel(holder, enchantment.getMaxLevel());
            maxLevel = Mth.clamp(
                WarlockEnchanting.hooks().maxLevel(ctx, enchantment, maxLevel), 1, EnchantBaseline.LEVEL_HARD_CAP);
            int minLevel = enchantment.getMinLevel();

            for (int candidateLevel = maxLevel; candidateLevel >= minLevel; candidateLevel--) {
                int minCost = entry == null ? enchantment.getMinCost(candidateLevel) : entry.minCost(candidateLevel);
                int maxCost = entry == null ? enchantment.getMaxCost(candidateLevel) : entry.maxCost(candidateLevel);
                if (level >= minCost && level <= maxCost) {
                    result.add(new EnchantmentInstance(holder, candidateLevel));
                    break;
                }
            }
        });
        return result;
    }

    /** 按基准表权重加权抽取；权重与原版一致时随机数消耗与原版相同。 */
    private Optional<EnchantmentInstance> pickTableEnchantment(
        RandomSource random, List<EnchantmentInstance> candidates) {
        int total = 0;
        for (EnchantmentInstance instance : candidates) {
            total += this.baselineWeight(instance);
        }
        if (total <= 0) {
            return Optional.empty();
        }

        int target = random.nextInt(total);
        for (EnchantmentInstance instance : candidates) {
            target -= this.baselineWeight(instance);
            if (target < 0) {
                return Optional.of(instance);
            }
        }
        return Optional.empty();
    }

    private int baselineWeight(EnchantmentInstance instance) {
        EnchantBaseline.Entry entry = EnchantBaseline.get(instance.enchantment());
        return entry == null ? instance.weight() : Math.max(1, entry.weight());
    }

    /** 基准表的 {@code in_table} 开关；基准缺失时视为原版标签内（调用方只从标签流里取候选）。 */
    private boolean isInTable(Holder<Enchantment> holder) {
        EnchantBaseline.Entry entry = EnchantBaseline.get(holder);
        return entry == null || entry.inTable();
    }

    /** 基准表的 {@code tradeable} 开关；基准缺失时视为可交易。 */
    private boolean isTradeable(Holder<Enchantment> holder) {
        EnchantBaseline.Entry entry = EnchantBaseline.get(holder);
        return entry == null || entry.tradeable();
    }

    @Override
    public int maxLevel(EnchantContext ctx, Enchantment enchantment, int baselineMaxLevel) {
        return Mth.clamp(baselineMaxLevel, 1, EnchantBaseline.LEVEL_HARD_CAP);
    }

    @Override
    public ItemStack randomLootEnchant(
        EnchantContext ctx, ItemStack stack, RandomSource random,
        Optional<HolderSet<Enchantment>> options, boolean onlyCompatible) {
        RegistryAccess registries = ctx.registryAccess();
        if (registries == null) {
            return stack;
        }

        boolean plainBook = stack.is(Items.BOOK);
        // 村民交易书的 gives 直接就是附魔书，因此两种书都按「书」处理：不做兼容性过滤。
        boolean isBook = plainBook || stack.is(Items.ENCHANTED_BOOK);
        boolean filterCompatible = !isBook && onlyCompatible;
        // 村民交易的书沿用原版 #tradeable 标签，这里再叠加基准表的 tradeable 开关。
        boolean filterTradeable = ctx.scene() == EnchantScene.TRADE_BOOK;
        Registry<Enchantment> registry = registries.lookupOrThrow(Registries.ENCHANTMENT);

        Stream<Holder<Enchantment>> stream = options
            .map(HolderSet::stream)
            .orElseGet(() -> registry.listElements().map(holder -> (Holder<Enchantment>) holder))
            .filter(holder -> !filterCompatible || stack.supportsEnchantment(holder))
            .filter(holder -> !filterTradeable || this.isTradeable(holder));

        List<Holder<Enchantment>> candidates = stream.toList();
        Optional<Holder<Enchantment>> picked = Util.getRandomSafe(candidates, random);
        if (picked.isEmpty()) {
            return stack;
        }

        Holder<Enchantment> holder = picked.get();
        Enchantment enchantment = holder.value();
        int minLevel = enchantment.getMinLevel();
        int maxLevel = EnchantBaseline.maxLevel(holder, enchantment.getMaxLevel());
        maxLevel = Mth.clamp(
            WarlockEnchanting.hooks().maxLevel(ctx, enchantment, maxLevel), 1, EnchantBaseline.LEVEL_HARD_CAP);
        int level = Mth.nextInt(random, minLevel, Math.max(minLevel, maxLevel));

        ItemStack result = plainBook ? new ItemStack(Items.ENCHANTED_BOOK) : stack;
        result.enchant(holder, level);
        return result;
    }

    @Override
    public ItemStack leveledLootEnchant(
        EnchantContext ctx, ItemStack stack, int level, RandomSource random,
        Optional<? extends HolderSet<Enchantment>> options) {
        RegistryAccess registries = ctx.registryAccess();
        if (registries == null) {
            return stack;
        }
        return EnchantmentHelper.enchantItem(random, stack, level, registries, options);
    }

    /** 复刻原版 {@code EnchantRandomlyFunction#enchantItem} 的附加价格公式。 */
    @Override
    public int tradeBookCost(EnchantContext ctx, ItemStack book, int level, RandomSource random) {
        if (level <= 0) {
            return 1;
        }
        return Math.min(2 + random.nextInt(5 + level * 10) + 3 * level, 64);
    }

    /** 复刻原版 {@code EnchantWithLevelsFunction#run} 的附加价格公式。 */
    @Override
    public int tradeItemCost(EnchantContext ctx, ItemStack stack, int level, RandomSource random) {
        return Mth.clamp(level, 1, 64);
    }

    @Override
    public MerchantOffer enchantBookOffer(
        EnchantContext ctx, Entity trader, RandomSource random,
        TagKey<Enchantment> tradeableTag, int minLevel, int maxAllowedLevel, int villagerXp) {
        Registry<Enchantment> registry = trader.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        List<Holder<Enchantment>> tradeable = registry.get(tradeableTag)
            .map(tag -> tag.stream().filter(holder -> this.isTradeable(holder)).toList())
            .orElse(List.of());
        Optional<Holder<Enchantment>> optional = Util.getRandomSafe(tradeable, random);

        int emeraldCost;
        ItemStack result;
        if (optional.isPresent()) {
            Holder<Enchantment> holder = optional.get();
            Enchantment enchantment = holder.value();
            int min = Math.max(enchantment.getMinLevel(), minLevel);
            int baselineMax = EnchantBaseline.maxLevel(holder, enchantment.getMaxLevel());
            int resolvedMax = Mth.clamp(
                WarlockEnchanting.hooks().maxLevel(ctx, enchantment, baselineMax), 1, EnchantBaseline.LEVEL_HARD_CAP);
            int max = Math.min(resolvedMax, maxAllowedLevel);
            // 基准表把上限调到低于交易下限时，退化为单点等级，避免原版 nextInt 抛出空区间异常。
            max = Math.max(min, max);
            int level = Mth.nextInt(random, min, max);

            result = EnchantmentHelper.createBook(new EnchantmentInstance(holder, level));
            emeraldCost = 2 + random.nextInt(5 + level * 10) + 3 * level;
            if (holder.is(EnchantmentTags.DOUBLE_TRADE_PRICE)) {
                emeraldCost *= 2;
            }
            emeraldCost = Math.min(emeraldCost, 64);
        } else {
            emeraldCost = 1;
            result = new ItemStack(Items.BOOK);
        }

        return new MerchantOffer(
            new ItemCost(Items.EMERALD, emeraldCost), Optional.of(new ItemCost(Items.BOOK)),
            result, 12, villagerXp, 0.2F);
    }

    @Override
    public MerchantOffer enchantedItemOffer(
        EnchantContext ctx, Entity trader, RandomSource random, ItemStack item,
        TagKey<Enchantment> equipmentTag, int baseEmeraldCost, int maxUses, int villagerXp, float priceMultiplier) {
        int level = 5 + random.nextInt(15);
        RegistryAccess registries = trader.level().registryAccess();
        Optional<HolderSet.Named<Enchantment>> tag =
            registries.lookupOrThrow(Registries.ENCHANTMENT).get(equipmentTag);

        ItemStack result = EnchantmentHelper.enchantItem(random, item.copy(), level, registries, tag);
        int emeraldCost = Math.min(baseEmeraldCost + level, 64);
        return new MerchantOffer(
            new ItemCost(Items.EMERALD, emeraldCost), result, maxUses, villagerXp, priceMultiplier);
    }
}
