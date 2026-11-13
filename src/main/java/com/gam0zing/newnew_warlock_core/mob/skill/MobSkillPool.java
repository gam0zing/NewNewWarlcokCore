package com.gam0zing.newnew_warlock_core.mob.skill;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * 内置技能池实现：持有一组候选技能，按生物剩余的 points 做概率抽取。
 * <p>抽取规则：</p>
 * <ol>
 *     <li>以传入 points 的 {@code × 2%} 为成功概率（上限 100%）判定一次，失败立即结束；</li>
 *     <li>判定成功后，从池内随机挑 3 个技能，按随机顺序取第一个当前 points 付得起的加入结果，
 *     并扣减它的总价值（{@link NewNewMobSkill#getTotalPrice(int)}）；</li>
 *     <li>同一个技能被重复抽中时，每重复一次等级 +1；</li>
 *     <li>最多判定 10 次，避免 points 很少时反复空转。</li>
 * </ol>
 * <p>池的候选内容可以在注册后继续修改：{@link #addSkill}（累加等级）、{@link #setSkill}（覆盖）、
 * {@link #removeSkill}、{@link #clear}；已注册的池可通过 {@link MobSkillPools#getPool(Identifier)} 取回。
 * 没有候选技能时行为等同于空池，直接返回空列表。</p>
 */
public final class MobSkillPool implements NewNewMobSkillPool {

    /** 单次 rebuild 内最多判定次数 */
    private static final int MAX_ROLLS = 10;
    /** 每次判定成功后随机挑选的候选数量 */
    private static final int CANDIDATE_COUNT = 3;
    /** 每点 points 提供的判定成功率 */
    private static final double CHANCE_PER_POINT = 0.02D;

    private final Identifier id;
    private final int priority;
    private final List<MobSkillEntry> entries = new ArrayList<>();

    public MobSkillPool(Identifier id, int priority) {
        this.id = id;
        this.priority = priority;
    }

    @Override
    public Identifier id() {
        return this.id;
    }

    @Override
    public int priority() {
        return this.priority;
    }

    /** {@return 当前候选技能的不可变快照} */
    public synchronized List<MobSkillEntry> skills() {
        return List.copyOf(this.entries);
    }

    /**
     * 添加候选技能；同 id 已存在时等级相加（饱和在 {@link Integer#MAX_VALUE}）。
     * <p>需要覆盖已有等级时用 {@link #setSkill(Identifier, int)}。</p>
     */
    public synchronized MobSkillPool addSkill(Identifier skillId, int level) {
        requirePositive(level);
        for (int index = 0; index < this.entries.size(); index++) {
            MobSkillEntry entry = this.entries.get(index);
            if (entry.id().equals(skillId)) {
                this.entries.set(index,
                    new MobSkillEntry(skillId, addLevels(entry.level(), level), entry.custom()));
                return this;
            }
        }
        this.entries.add(new MobSkillEntry(skillId, level));
        return this;
    }

    /** 设置候选技能的等级：已存在则直接覆盖等级，不存在则添加。 */
    public synchronized MobSkillPool setSkill(Identifier skillId, int level) {
        requirePositive(level);
        for (int index = 0; index < this.entries.size(); index++) {
            MobSkillEntry entry = this.entries.get(index);
            if (entry.id().equals(skillId)) {
                this.entries.set(index, new MobSkillEntry(skillId, level, entry.custom()));
                return this;
            }
        }
        this.entries.add(new MobSkillEntry(skillId, level));
        return this;
    }

    /** 移除候选技能；不存在时不做任何事。 */
    public synchronized MobSkillPool removeSkill(Identifier skillId) {
        this.entries.removeIf(entry -> entry.id().equals(skillId));
        return this;
    }

    /** 清空全部候选技能（池本身仍然注册着）。 */
    public synchronized MobSkillPool clear() {
        this.entries.clear();
        return this;
    }

    @Override
    public synchronized List<MobSkillEntry> getRandom(LivingEntity self, double remainingPoints) {
        if (this.entries.isEmpty() || remainingPoints <= 0.0D) {
            return List.of();
        }

        List<MobSkillEntry> snapshot = List.copyOf(this.entries);
        Map<Identifier, Integer> baseLevels = new HashMap<>(snapshot.size());
        for (MobSkillEntry entry : snapshot) {
            baseLevels.put(entry.id(), entry.level());
        }

        RandomSource random = self.getRandom();
        double points = remainingPoints;
        // 技能 id -> 被抽中的次数
        Map<Identifier, Integer> picked = new LinkedHashMap<>();
        List<MobSkillEntry> candidates = new ArrayList<>(snapshot.size());

        for (int roll = 0; roll < MAX_ROLLS; roll++) {
            double chance = Math.min(points * CHANCE_PER_POINT, 1.0D);
            if (chance <= 0.0D || random.nextDouble() >= chance) {
                break;
            }

            candidates.clear();
            candidates.addAll(snapshot);
            shuffle(candidates, random);

            boolean paid = false;
            for (int index = 0; index < Math.min(CANDIDATE_COUNT, candidates.size()); index++) {
                MobSkillEntry candidate = candidates.get(index);
                NewNewMobSkill skill = MobSkills.get(candidate.id());
                if (skill == null) {
                    continue;
                }
                int pickedTimes = picked.getOrDefault(candidate.id(), 0);
                int level = addLevels(candidate.level(), pickedTimes);
                float cost = skill.getTotalPrice(level);
                if (cost > points) {
                    continue;
                }
                points -= cost;
                picked.put(candidate.id(), pickedTimes + 1);
                paid = true;
                break;
            }
            if (!paid) {
                break;
            }
        }

        List<MobSkillEntry> result = new ArrayList<>(picked.size());
        for (Map.Entry<Identifier, Integer> entry : picked.entrySet()) {
            int baseLevel = baseLevels.getOrDefault(entry.getKey(), 1);
            result.add(new MobSkillEntry(entry.getKey(), addLevels(baseLevel, entry.getValue() - 1)));
        }
        return List.copyOf(result);
    }

    private static void shuffle(List<MobSkillEntry> list, RandomSource random) {
        for (int index = list.size() - 1; index > 0; index--) {
            int swap = random.nextInt(index + 1);
            MobSkillEntry previous = list.set(index, list.get(swap));
            list.set(swap, previous);
        }
    }

    private static int addLevels(int first, int second) {
        long sum = (long) first + second;
        return sum >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    private static void requirePositive(int level) {
        if (level <= 0) {
            throw new IllegalArgumentException("技能等级必须为正数：" + level);
        }
    }
}
