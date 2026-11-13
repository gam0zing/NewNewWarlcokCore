package com.gam0zing.newnew_warlock_core.mob.skill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

/**
 * 生物技能池注册表与“生物 id → 技能池 id 列表”的绑定表。
 * <p>绑定来源分三层，依次累积：</p>
 * <ul>
 *     <li><b>默认绑定</b>：所有参与重建（即使用 points）的生物绑定通用池；其中敌对生物再按攻击方式
 *     绑定近战池或远程池（见 {@link #applyDefaultBindings}）；</li>
 *     <li><b>内置绑定</b>：代码里通过 {@link #bind(Identifier, Identifier...)} 注册，先于数据文件生效；</li>
 *     <li><b>数据文件</b>：{@code data/<命名空间>/mob_skill_pool/*.json}（见 {@link MobSkillPoolFile}），
 *     按文件 id 升序应用；{@code over_write} 为 true 时先清空已累积的列表再追加。</li>
 * </ul>
 * <p>每个生物的技能池列表内同一个技能池 id 只出现一次。抽取时按技能池的
 * {@link NewNewMobSkillPool#priority()} 从高到低调用 {@code getRandom}，优先级相同时保持绑定顺序，
 * 因此给生物追加的专属技能池可以先于通用池抽取。</p>
 */
public final class MobSkillPools {

    private static final Identifier GENERAL_POOL = poolId("general");
    private static final Identifier MELEE_POOL = poolId("melee");
    private static final Identifier RANGED_POOL = poolId("ranged");

    /**
     * 敌对生物（{@link MobCategory#MONSTER}）中按远程处理的类型：这些生物不绑近战池。
     * <p>末影龙按远程处理；凋灵同时具备近战与远程，单独放在 {@link #HYBRID_MONSTERS}。</p>
     */
    private static final Set<Identifier> RANGED_MONSTERS = Set.of(
        Identifier.withDefaultNamespace("skeleton"),
        Identifier.withDefaultNamespace("stray"),
        Identifier.withDefaultNamespace("bogged"),
        Identifier.withDefaultNamespace("blaze"),
        Identifier.withDefaultNamespace("breeze"),
        Identifier.withDefaultNamespace("ghast"),
        Identifier.withDefaultNamespace("shulker"),
        Identifier.withDefaultNamespace("guardian"),
        Identifier.withDefaultNamespace("elder_guardian"),
        Identifier.withDefaultNamespace("witch"),
        Identifier.withDefaultNamespace("evoker"),
        Identifier.withDefaultNamespace("illusioner"),
        Identifier.withDefaultNamespace("pillager"),
        Identifier.withDefaultNamespace("ender_dragon"));

    /** 同时具备近战与远程手段的敌对生物：两个池都绑定。 */
    private static final Set<Identifier> HYBRID_MONSTERS = Set.of(
        Identifier.withDefaultNamespace("wither"));

    private static final Map<Identifier, NewNewMobSkillPool> REGISTRY = new HashMap<>();
    private static final Map<Identifier, List<Identifier>> BASE_BINDINGS = new HashMap<>();

    /** 当前生效的生物绑定（数据重载时整体替换，读取无锁） */
    private static volatile Map<Identifier, List<Identifier>> bindings = Map.of();

    private MobSkillPools() {}

    private static Identifier poolId(String path) {
        return Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, path);
    }

    /**
     * 注册一个技能池单例。
     *
     * @throws IllegalStateException 同一个 id 重复注册
     */
    public static synchronized void register(NewNewMobSkillPool pool) {
        Identifier id = pool.id();
        if (REGISTRY.putIfAbsent(id, pool) != null) {
            throw new IllegalStateException("生物技能池重复注册：" + id);
        }
    }

    /** {@return 指定 id 的技能池单例；未注册时返回 {@code null}} */
    @Nullable
    public static NewNewMobSkillPool get(Identifier id) {
        return REGISTRY.get(id);
    }

    /**
     * {@return 指定 id 的内置技能池；未注册或不是 {@link MobSkillPool} 时返回 {@code null}}
     * <p>注册后想继续修改池内容时用这个方法取回，例如
     * {@code MobSkillPools.getPool(id).addSkill(skillId, 1)}。</p>
     */
    @Nullable
    public static MobSkillPool getPool(Identifier id) {
        return get(id) instanceof MobSkillPool pool ? pool : null;
    }

    /**
     * 为生物追加内置绑定的技能池 id（重复 id 只保留首次出现）。
     * <p>内置绑定是该生物的基准列表，之后的数据文件可以在它上面覆盖或追加。</p>
     */
    public static synchronized void bind(Identifier mobId, Identifier... poolIds) {
        List<Identifier> list = BASE_BINDINGS.computeIfAbsent(mobId, id -> new ArrayList<>());
        for (Identifier poolId : poolIds) {
            if (poolId != null && !list.contains(poolId)) {
                list.add(poolId);
            }
        }
    }

    /** {@return 该生物当前绑定的技能池 id 列表（不可变；没有绑定时为空列表）} */
    public static List<Identifier> poolsFor(Identifier mobId) {
        return bindings.getOrDefault(mobId, List.of());
    }

    /** {@return 该生物当前绑定的技能池 id 列表} */
    public static List<Identifier> poolsFor(LivingEntity entity) {
        return poolsFor(EntityType.getKey(entity.getType()));
    }

    /**
     * 用数据文件整体重建绑定表（由 {@link MobSkillPoolLoader} 在数据重载完成时调用）。
     * <p>合并顺序：默认绑定（见 {@link #applyDefaultBindings}）→ 内置绑定 → 数据文件（按文件 id 升序）。
     * 每个文件先按 {@code over_write} 决定是否清空，再追加自己的技能池；未注册的技能池与未注册的生物
     * 都会记录日志并跳过。</p>
     */
    static void replaceWith(Map<Identifier, MobSkillPoolFile> files) {
        Map<Identifier, List<Identifier>> merged = new HashMap<>();
        applyDefaultBindings(merged);
        BASE_BINDINGS.forEach((mobId, poolIds) -> {
            List<Identifier> list = merged.computeIfAbsent(mobId, id -> new ArrayList<>());
            for (Identifier poolId : poolIds) {
                if (!list.contains(poolId)) {
                    list.add(poolId);
                }
            }
        });

        List<Map.Entry<Identifier, MobSkillPoolFile>> ordered = new ArrayList<>(files.entrySet());
        ordered.sort(Comparator.comparing(entry -> entry.getKey().toString()));

        for (Map.Entry<Identifier, MobSkillPoolFile> entry : ordered) {
            Identifier fileId = entry.getKey();
            MobSkillPoolFile file = entry.getValue();
            if (!BuiltInRegistries.ENTITY_TYPE.containsKey(file.mobId())) {
                NewNewWarlockCore.LOGGER.warn(
                    "[mob_skill_pool] {}: mob_id={} 未注册，该数据不会生效（可能是对应模组未安装）",
                    fileId, file.mobId());
            }

            List<Identifier> poolIds = merged.computeIfAbsent(file.mobId(), id -> new ArrayList<>());
            if (file.overWrite()) {
                poolIds.clear();
            }
            for (Identifier poolId : file.pools()) {
                if (!REGISTRY.containsKey(poolId)) {
                    NewNewWarlockCore.LOGGER.warn(
                        "[mob_skill_pool] {}: 技能池 {} 未注册，已跳过该条目", fileId, poolId);
                    continue;
                }
                if (!poolIds.contains(poolId)) {
                    poolIds.add(poolId);
                }
            }
        }

        Map<Identifier, List<Identifier>> snapshot = new HashMap<>(merged.size());
        merged.forEach((mobId, poolIds) -> snapshot.put(mobId, List.copyOf(poolIds)));
        bindings = Map.copyOf(snapshot);
        NewNewWarlockCore.LOGGER.info("[mob_skill_pool] 已加载 {} 份数据，共绑定 {} 种生物的技能池",
            files.size(), snapshot.size());
    }

    /**
     * 生成默认绑定：所有持有重建标识（即 rebuild 时会使用 points）的生物绑定通用池；
     * 其中 {@link MobCategory#MONSTER} 按攻击方式再绑定近战池或远程池：
     * {@link #RANGED_MONSTERS} 只绑远程，{@link #HYBRID_MONSTERS} 两个都绑，其余敌对生物绑近战。
     * <p>按实体注册表动态生成，因此其它模组的生物也会绑上通用池；未知的敌对生物默认按近战处理，
     * 需要时可通过数据文件的 {@code over_write} 改写。</p>
     */
    private static void applyDefaultBindings(Map<Identifier, List<Identifier>> merged) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (!hasRebuildAttribute(type)) {
                continue;
            }
            Identifier mobId = EntityType.getKey(type);
            List<Identifier> poolIds = merged.computeIfAbsent(mobId, id -> new ArrayList<>());
            poolIds.add(GENERAL_POOL);
            if (type.getCategory() != MobCategory.MONSTER) {
                continue;
            }
            boolean ranged = RANGED_MONSTERS.contains(mobId);
            boolean hybrid = HYBRID_MONSTERS.contains(mobId);
            if (hybrid || !ranged) {
                poolIds.add(MELEE_POOL);
            }
            if (hybrid || ranged) {
                poolIds.add(RANGED_POOL);
            }
        }
    }

    /** {@return 该实体类型是否持有重建标识（玩家、傀儡等不持有，也就不会使用 points）} */
    @SuppressWarnings("unchecked")
    private static boolean hasRebuildAttribute(EntityType<?> type) {
        if (!DefaultAttributes.hasSupplier(type)) {
            return false;
        }
        AttributeSupplier supplier = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
        return supplier != null && supplier.hasAttribute(ModAttributes.NEWNEW_MOB_REBUILD);
    }
}
