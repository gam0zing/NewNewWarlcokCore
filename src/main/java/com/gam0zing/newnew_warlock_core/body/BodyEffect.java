package com.gam0zing.newnew_warlock_core.body;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;

/**
 * 一次物理伤害最终会施加的一个受击效果。
 * <p>由身体部位的基础效果与武器 / 护甲档案的 {@link EffectDelta 增量} 合并而来，见 {@link #merge}。</p>
 *
 * @param effect      效果（注册名已解析为注册表持有者）
 * @param duration    持续时间，单位游戏刻（合并后必须为正）
 * @param amplifier   效果等级（原版 amplifier，0 表示 I 级；合并后必须 ≥ 0）
 * @param probability 触发概率基准，范围 0-1（合并后必须为正）
 */
public record BodyEffect(Holder<MobEffect> effect, int duration, int amplifier, float probability) {

    public BodyEffect {
        if (duration < 0) {
            duration = 0;
        }
        if (amplifier < 0) {
            amplifier = 0;
        }
        probability = Mth.clamp(probability, 0.0F, 1.0F);
    }

    /**
     * 合并身体部位的基础效果与武器 / 护甲档案的增量，得到本次实际参与判定的效果列表。
     *
     * <p>三者按效果 id 合并成一份：先放入部位的基础值（同名条目以最后一个为准），再依次叠加武器、
     * 护甲的 {@link EffectDelta}。部位里不存在的效果也会被加入，其基础值视为
     * “等级 0（amplifier = -1）、持续时间 0、概率 0”，因此 {@code delta_amplifier=1}
     * 恰好得到 1 级效果。</p>
     *
     * <p>合并后任意一项不满足“等级 ≥ 1（amplifier ≥ 0）、持续时间 > 0、概率 > 0”时会被跳过。</p>
     *
     * @param base   身体部位的基础效果
     * @param weapon 武器档案的效果增量
     * @param armor  护甲档案的效果增量
     * @return 合并后的效果列表（保持首次出现的顺序）
     */
    public static List<BodyEffect> merge(
        List<BodyEffect> base, List<EffectDelta> weapon, List<EffectDelta> armor) {
        if (base.isEmpty() && weapon.isEmpty() && armor.isEmpty()) {
            return List.of();
        }

        Map<Holder<MobEffect>, Accumulator> merged = new LinkedHashMap<>();
        for (BodyEffect effect : base) {
            Accumulator acc = merged.computeIfAbsent(effect.effect(), key -> new Accumulator());
            acc.duration = effect.duration();
            acc.amplifier = effect.amplifier();
            acc.probability = effect.probability();
        }
        applyDeltas(merged, weapon);
        applyDeltas(merged, armor);

        List<BodyEffect> result = new ArrayList<>(merged.size());
        for (Map.Entry<Holder<MobEffect>, Accumulator> entry : merged.entrySet()) {
            Accumulator acc = entry.getValue();
            // amplifier <= -1 等价于等级 <= 0
            if (acc.amplifier <= -1 || acc.duration <= 0 || !(acc.probability > 0.0F)) {
                continue;
            }
            result.add(new BodyEffect(entry.getKey(), acc.duration, acc.amplifier, acc.probability));
        }
        return List.copyOf(result);
    }

    private static void applyDeltas(
        Map<Holder<MobEffect>, Accumulator> merged, List<EffectDelta> deltas) {
        for (EffectDelta delta : deltas) {
            Accumulator acc = merged.computeIfAbsent(delta.effect(), key -> new Accumulator());
            acc.duration += delta.duration();
            acc.amplifier += delta.amplifier();
            acc.probability += delta.probability();
        }
    }

    /** 合并过程中的可变累加器；不存在的效果从“等级 0、时长 0、概率 0”开始 */
    private static final class Accumulator {
        int duration;
        int amplifier = -1;
        float probability;
    }

    /**
     * 按“本次实际扣除的生命值”折算触发概率。
     * <p>数据里的 {@code probability} 是“一次扣除量正好等于 {@code (当前生命值 + 最大生命值) / 2}”时的概率；
     * 扣除量更低时按比例线性缩放，扣除量更高（已足以致死）时保持基准概率。</p>
     *
     * @param damage        本次从目标身上扣除的生命值
     * @param currentHealth 目标在扣除之前的当前生命值
     * @param maxHealth     目标的最大生命值
     * @return 折算后的实际触发概率，范围 0-1
     */
    public float probabilityFor(float damage, float currentHealth, float maxHealth) {
        if (!(this.probability > 0.0F) || !(damage > 0.0F)) {
            return 0.0F;
        }
        float denominator = Math.max(0.0F, currentHealth) + Math.max(0.0F, maxHealth);
        if (!(denominator > 0.0F)) {
            // 目标没有有效生命值，视为“扣血等于生命值基准”
            return this.probability;
        }
        float ratio = Math.min(1.0F, 2.0F * damage / denominator);
        return this.probability * ratio;
    }
}
