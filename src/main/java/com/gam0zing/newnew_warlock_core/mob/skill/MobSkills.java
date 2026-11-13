package com.gam0zing.newnew_warlock_core.mob.skill;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.mojang.serialization.MapCodec;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 生物技能注册表与技能列表的运行时操作。
 * <ul>
 *     <li>技能注册：{@link #register(NewNewMobSkill)}，按 {@link NewNewMobSkill#id()} 建立全局单例索引；</li>
 *     <li>技能列表：以附件形式持久化在生物自己的 NBT 上，每个条目包含“技能 id + 等级 + 技能自定义状态 custom”；</li>
 *     <li>重建分配：{@link #assignFromPools(LivingEntity)} 遍历该生物绑定的技能池，把随机结果合并进技能列表；</li>
 *     <li>tick 驱动：{@link #updateSkill(LivingEntity)} 按列表里的 id 找到技能单例并调用
 *     {@link NewNewMobSkill#onTick(LivingEntity, int)}。</li>
 * </ul>
 * <p>合并规则：同 id 技能已存在时等级相加（饱和在 {@link Integer#MAX_VALUE}）。</p>
 * <p>技能列表内同一个技能 id 只出现一次；技能池返回的重复条目也按上面的规则合并。</p>
 * <p>等级与技能状态是同一个 {@link MobSkillEntry}。状态放在条目的 {@code custom} 里，
 * 由技能实现类用自己的 {@link MapCodec} 解释；{@link #state} / {@link #setState} 负责按 Codec 读写，
 * 且只覆盖 Codec 声明的键，技能在 custom 里额外保存的字段不会被清掉。</p>
 */
public final class MobSkills {

    /** 技能列表的 NBT 结构：{@code {skills: [{id: "...", level: 1, custom: {...}}]}} */
    public static final MapCodec<List<MobSkillEntry>> SKILL_LIST_CODEC =
        MobSkillEntry.CODEC.listOf().fieldOf("skills");

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NewNewWarlockCore.MOD_ID);

    /** 生物技能列表附件；没有技能的生物不会创建该附件，默认值为空列表。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<List<MobSkillEntry>>> SKILLS =
        ATTACHMENTS.register("mob_skills", () -> AttachmentType
            .builder(() -> List.<MobSkillEntry>of())
            .serialize(SKILL_LIST_CODEC)
            .build());

    private static final Map<Identifier, NewNewMobSkill> REGISTRY = new HashMap<>();
    /** 只报告一次的诊断信息，避免每 tick 刷屏 */
    private static final Set<Identifier> MISSING_REPORTED = new HashSet<>();
    private static final Set<Identifier> FAILURE_REPORTED = new HashSet<>();

    private MobSkills() {}

    /** 由模块初始化调用：把技能列表附件注册到模组总线。 */
    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }

    /**
     * 注册一个技能单例。
     *
     * @throws IllegalStateException 同一个 id 重复注册
     */
    public static synchronized void register(NewNewMobSkill skill) {
        Identifier id = skill.id();
        if (REGISTRY.putIfAbsent(id, skill) != null) {
            throw new IllegalStateException("生物技能重复注册：" + id);
        }
    }

    /** {@return 指定 id 的技能单例；未注册时返回 {@code null}} */
    @Nullable
    public static NewNewMobSkill get(Identifier id) {
        return REGISTRY.get(id);
    }

    /** {@return 该生物当前的技能列表（不可变；没有技能时为空列表）} */
    public static List<MobSkillEntry> skills(LivingEntity self) {
        List<MobSkillEntry> data = self.getExistingDataOrNull(SKILLS.get());
        return data != null ? data : List.of();
    }

    /** {@return 该生物技能列表里指定技能的条目；没有该技能时返回 {@code null}} */
    @Nullable
    public static MobSkillEntry skill(LivingEntity self, Identifier skillId) {
        for (MobSkillEntry entry : skills(self)) {
            if (entry.id().equals(skillId)) {
                return entry;
            }
        }
        return null;
    }

    /**
     * 按技能自己的 MapCodec 读取它在 custom 中的状态。
     *
     * @param fallback 技能不存在、尚未写入状态或解析失败时返回的值
     */
    public static <S> S state(LivingEntity self, Identifier skillId, MapCodec<S> codec, S fallback) {
        MobSkillEntry entry = skill(self, skillId);
        if (entry == null) {
            return fallback;
        }
        return entry.custom().read(codec).orElse(fallback);
    }

    /**
     * 按技能自己的 MapCodec 写入它在 custom 中的状态，等级与 custom 里的其他字段保持不变。
     * <p>技能不在列表里时不做事；只有 Codec 声明的键会被覆盖。</p>
     */
    public static <S> void setState(LivingEntity self, Identifier skillId, MapCodec<S> codec, S state) {
        List<MobSkillEntry> entries = skills(self);
        for (int index = 0; index < entries.size(); index++) {
            MobSkillEntry entry = entries.get(index);
            if (!entry.id().equals(skillId)) {
                continue;
            }
            CompoundTag custom = entry.custom().copy();
            custom.store(codec, state);
            List<MobSkillEntry> updated = new ArrayList<>(entries);
            updated.set(index, new MobSkillEntry(entry.id(), entry.level(), custom));
            self.setData(SKILLS.get(), List.copyOf(updated));
            return;
        }
    }

    /** 清空该生物的全部技能数据（含冷却/持续状态），重建开始时调用。 */
    public static void clear(LivingEntity self) {
        self.setData(SKILLS.get(), List.of());
    }

    /**
     * 重建前期的技能分配：遍历该生物绑定的技能池，依次随机并合并进自己的技能列表。
     * <p>每抽到一个技能就按下式扣除剩余 points：{@code 等级 × 技能价值}（见
     * {@link NewNewMobSkill#getTotalPrice(int)}）；价值超过剩余 points 的技能不会被抽取。
     * 未知技能池、未注册技能、返回 {@code null} 或非法条目只跳过自身并记录日志。</p>
     *
     * @param points 正态分布发放的 points
     * @return 扣除技能价值后剩余的 points
     */
    public static double assignFromPools(LivingEntity self, double points) {
        double remaining = Math.max(points, 0.0D);
        List<Identifier> poolIds =
            new ArrayList<>(MobSkillPools.poolsFor(EntityType.getKey(self.getType())));
        if (poolIds.isEmpty()) {
            return remaining;
        }

        // 优先级高的技能池先抽取（points 先到先得）；同优先级保持绑定顺序，List.sort 是稳定排序
        poolIds.sort(Comparator.comparingInt((Identifier poolId) -> {
            NewNewMobSkillPool pool = MobSkillPools.get(poolId);
            return pool != null ? pool.priority() : Integer.MIN_VALUE;
        }).reversed());

        List<MobSkillEntry> merged = new ArrayList<>(skills(self));
        boolean changed = false;
        for (Identifier poolId : poolIds) {
            NewNewMobSkillPool pool = MobSkillPools.get(poolId);
            if (pool == null) {
                NewNewWarlockCore.LOGGER.warn("[mob_skill] 技能池 {} 未注册，已跳过（生物：{}）",
                    poolId, EntityType.getKey(self.getType()));
                continue;
            }

            List<MobSkillEntry> rolled;
            try {
                rolled = pool.getRandom(self, remaining);
            } catch (RuntimeException exception) {
                NewNewWarlockCore.LOGGER.error("[mob_skill] 技能池 {} 随机失败（生物：{}），已跳过该技能池",
                    poolId, EntityType.getKey(self.getType()), exception);
                continue;
            }
            if (rolled == null) {
                NewNewWarlockCore.LOGGER.warn("[mob_skill] 技能池 {} 返回了 null，已跳过", poolId);
                continue;
            }

            for (MobSkillEntry entry : rolled) {
                if (entry == null || entry.id() == null || entry.level() <= 0) {
                    NewNewWarlockCore.LOGGER.warn("[mob_skill] 技能池 {} 返回了非法条目 {}，已跳过", poolId, entry);
                    continue;
                }
                NewNewMobSkill skill = REGISTRY.get(entry.id());
                if (skill == null) {
                    reportMissing(entry.id());
                    continue;
                }
                float price = skill.getTotalPrice(entry.level());
                if (price > remaining) {
                    continue;
                }
                remaining -= price;
                merge(merged, entry);
                changed = true;
            }
        }

        if (changed) {
            self.setData(SKILLS.get(), List.copyOf(merged));
        }
        return remaining;
    }

    /**
     * 每 tick 驱动该生物的全部技能。
     * <p>未注册的技能 id 只报告一次；单个技能抛出的异常不会影响其他技能。</p>
     */
    public static void updateSkill(LivingEntity self) {
        List<MobSkillEntry> entries = skills(self);
        if (entries.isEmpty()) {
            return;
        }

        for (MobSkillEntry entry : entries) {
            NewNewMobSkill skill = REGISTRY.get(entry.id());
            if (skill == null) {
                reportMissing(entry.id());
                continue;
            }
            try {
                skill.onTick(self, entry.level());
            } catch (RuntimeException exception) {
                reportFailure(entry.id(), exception);
            }
        }
    }

    /**
     * 把一个技能条目合并进列表：同 id 已存在时等级相加（饱和在 {@link Integer#MAX_VALUE}），
     * 并保留原有的冷却/持续状态。
     */
    private static void merge(List<MobSkillEntry> entries, MobSkillEntry incoming) {
        for (int index = 0; index < entries.size(); index++) {
            MobSkillEntry existing = entries.get(index);
            if (!existing.id().equals(incoming.id())) {
                continue;
            }
            long sum = (long) existing.level() + incoming.level();
            int level = sum >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
            entries.set(index,
                new MobSkillEntry(existing.id(), level, existing.custom()));
            return;
        }
        entries.add(incoming);
    }

    private static synchronized void reportMissing(Identifier id) {
        if (MISSING_REPORTED.add(id)) {
            NewNewWarlockCore.LOGGER.warn("[mob_skill] 生物持有未注册的技能 {}，该技能不会生效", id);
        }
    }

    private static synchronized void reportFailure(Identifier id, RuntimeException exception) {
        if (FAILURE_REPORTED.add(id)) {
            NewNewWarlockCore.LOGGER.error("[mob_skill] 技能 {} 的 onTick 抛出异常（同一技能只报告一次）", id, exception);
        }
    }
}
