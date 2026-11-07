package com.gam0zing.newnew_warlock_core.enchant;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * 附魔全局基准表。
 * <p>启动/数据包重载后按附魔 {@link Identifier} 建立索引，并提供：
 * <ul>
 *     <li>{@link #get(Holder)} / {@link #entry(Identifier)}：有 {@code Holder} 的路径（附魔台、铁砧、战利品、村民）使用；</li>
 *     <li>{@link #get(Enchantment)}：没有 {@code Holder} 的路径（全局 {@code getMaxLevel} 注入）使用，
 *     依靠实例引用（{@link IdentityHashMap}）反查，数据包重载后需要重新 {@link #rebuild}。</li>
 * </ul>
 * 映射未建立时所有查询返回 {@code null}，调用方应回退到原版值，不允许抛异常。</p>
 * <p>该表只保存「基准值」，玩家相关的最终值由 {@link WarlockEnchantHooks#maxLevel} 在此基础上计算。</p>
 */
public final class EnchantBaseline {

    /** 原版等级硬上限（{@code EnchantmentDefinition} 编解码器与 {@code ItemEnchantments} 均为 255）。 */
    public static final int LEVEL_HARD_CAP = 255;

    /**
     * 一次重载后的完整索引快照。
     * <p>{@code byId} 与 {@code byInstance} 必须同时切换，否则重载瞬间会出现「新 id 表 + 旧实例表」的混合状态。</p>
     */
    private record Snapshot(Map<Identifier, Entry> byId, Map<Enchantment, Entry> byInstance) {
        static final Snapshot EMPTY = new Snapshot(Map.of(), Map.of());
    }

    private static volatile Snapshot snapshot = Snapshot.EMPTY;

    private EnchantBaseline() {}

    /**
     * 单条附魔的基准属性。
     * <p>字段与 {@link Enchantment.EnchantmentDefinition} 及原版附魔标签保持一致，
     * 后续新增的可配置属性继续往这个 record 里加字段即可（旧配置文件缺少的键读取时回退默认值）。</p>
     */
    public record Entry(
        Identifier id,
        int maxLevel,
        int weight,
        int minCostBase,
        int minCostPerLevelAboveFirst,
        int maxCostBase,
        int maxCostPerLevelAboveFirst,
        int anvilCost,
        boolean inTable,
        boolean treasure,
        boolean tradeable,
        boolean onRandomLoot,
        boolean onTradedEquipment) {

        /** 与原版 {@code Enchantment.Cost#calculate} 一致。 */
        public int minCost(int level) {
            return this.minCostBase + this.minCostPerLevelAboveFirst * (level - 1);
        }

        /** 与原版 {@code Enchantment.Cost#calculate} 一致。 */
        public int maxCost(int level) {
            return this.maxCostBase + this.maxCostPerLevelAboveFirst * (level - 1);
        }
    }

    /**
     * 用注册表里附魔的 {@code definition} 与原版标签生成默认条目。
     * <p>只在配置文件缺少该条目时使用；已存在的条目永不覆盖。</p>
     */
    public static Entry fromDefinition(Holder.Reference<Enchantment> holder) {
        Enchantment.EnchantmentDefinition definition = holder.value().definition();
        Enchantment.Cost minCost = definition.minCost();
        Enchantment.Cost maxCost = definition.maxCost();
        return new Entry(
            holder.key().identifier(),
            definition.maxLevel(),
            definition.weight(),
            minCost.base(),
            minCost.perLevelAboveFirst(),
            maxCost.base(),
            maxCost.perLevelAboveFirst(),
            definition.anvilCost(),
            holder.is(EnchantmentTags.IN_ENCHANTING_TABLE),
            holder.is(EnchantmentTags.TREASURE),
            holder.is(EnchantmentTags.TRADEABLE),
            holder.is(EnchantmentTags.ON_RANDOM_LOOT),
            holder.is(EnchantmentTags.ON_TRADED_EQUIPMENT));
    }

    /**
     * 重建索引。
     * <p>必须在服务端资源重载、世界启动、客户端世界载入时调用，否则实例引用表会指向旧注册表实例。</p>
     */
    public static synchronized void rebuild(RegistryAccess registries, Map<Identifier, Entry> entries) {
        Registry<Enchantment> registry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        Map<Enchantment, Entry> instanceMap = new IdentityHashMap<>(entries.size());
        entries.forEach((id, entry) ->
            registry.get(id).ifPresent(holder -> instanceMap.put(holder.value(), entry)));

        // 两个索引必须一次性发布，避免读到新旧混合的中间状态。
        snapshot = new Snapshot(Map.copyOf(entries), Collections.unmodifiableMap(instanceMap));
    }

    /** 数据包重载前的兜底：清空索引，所有查询都会回退到原版值。 */
    public static synchronized void clear() {
        snapshot = Snapshot.EMPTY;
    }

    @Nullable
    public static Entry get(Holder<Enchantment> holder) {
        return holder.unwrapKey().map(key -> snapshot.byId().get(key.identifier())).orElse(null);
    }

    @Nullable
    public static Entry get(Enchantment enchantment) {
        return snapshot.byInstance().get(enchantment);
    }

    @Nullable
    public static Entry entry(Identifier id) {
        return snapshot.byId().get(id);
    }

    /** 基准最大等级（无条目时回退原版值），结果 clamp 到 1~255。 */
    public static int maxLevel(Holder<Enchantment> holder, int vanillaMaxLevel) {
        Entry entry = get(holder);
        return Mth.clamp(entry == null ? vanillaMaxLevel : entry.maxLevel(), 1, LEVEL_HARD_CAP);
    }

    /** 基准最大等级（无条目时回退原版值），结果 clamp 到 1~255。 */
    public static int maxLevel(Enchantment enchantment, int vanillaMaxLevel) {
        Entry entry = get(enchantment);
        return Mth.clamp(entry == null ? vanillaMaxLevel : entry.maxLevel(), 1, LEVEL_HARD_CAP);
    }

    /** 当前基准表快照（只读）。 */
    public static Map<Identifier, Entry> entries() {
        return snapshot.byId();
    }
}
