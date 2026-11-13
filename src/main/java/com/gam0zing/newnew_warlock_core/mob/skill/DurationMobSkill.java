package com.gam0zing.newnew_warlock_core.mob.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * 带持续时间的技能基类：在普通冷却技能的基础上插入一个“施放中”阶段。
 * <p>该技能自己的状态包含 {@code cooldown} 和 {@code duration}，以 {@link DurationState} 的 Codec
 * 存进技能条目的 {@code custom} 里，与等级是同一条数据。同一时刻最多只有一个大于 0，
 * 大于 0 的那个就代表当前阶段：</p>
 * <ul>
 *     <li><b>持续阶段</b>（{@code duration > 0}）：每次 tick 减 1 并调用
 *     {@link #onDuration(LivingEntity, int)}；减到 0 的那个 tick 调用
 *     {@link #endInvoke(LivingEntity, int)}，并把 cooldown 从 0 设为 {@link #maxCooldown()}；</li>
 *     <li><b>冷却阶段</b>（{@code duration == 0 && cooldown > 0}）：每次 tick 减 1 并调用
 *     {@link #onCooldown(LivingEntity, int)}；</li>
 *     <li><b>施放</b>（两者都为 0）：调用 {@link #invoke(LivingEntity, int)}，然后把 duration 从 0 设为
 *     {@link #maxDuration()}，cooldown 保持 0。</li>
 * </ul>
 * <p>因此一个阶段结束才会把另一个设为最大值：持续时间结束时才进入冷却，冷却结束时才再次施放。</p>
 * <p>具体技能若需要额外状态，可定义自己的 MapCodec，并通过
 * {@link MobSkills#state} / {@link MobSkills#setState} 读写 custom 中的其他键，不会覆盖 cooldown/duration。</p>
 */
public abstract class DurationMobSkill extends CooldownMobSkill {

    /** 持续技能在 custom 中保存的状态。 */
    public record DurationState(int cooldown, int duration) {

        public static final MapCodec<DurationState> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("cooldown", 0).forGetter(DurationState::cooldown),
            Codec.INT.optionalFieldOf("duration", 0).forGetter(DurationState::duration))
            .apply(instance, DurationState::new));

        public static final DurationState EMPTY = new DurationState(0, 0);
    }

    private final int maxDuration;

    /**
     * @param id          技能注册名
     * @param price       每级价值（points），不能为负数
     * @param maxCooldown 冷却时长（tick），必须为正数
     * @param maxDuration 持续时长（tick），必须为正数
     */
    protected DurationMobSkill(Identifier id, float price, int maxCooldown, int maxDuration) {
        super(id, price, maxCooldown);
        if (maxDuration <= 0) {
            throw new IllegalArgumentException("maxDuration 必须为正数：" + id + " -> " + maxDuration);
        }
        this.maxDuration = maxDuration;
    }

    /** {@return 持续时长（tick）} */
    public final int maxDuration() {
        return this.maxDuration;
    }

    /** 持续阶段的每个 tick 调用（duration 已减 1）。 */
    public abstract void onDuration(LivingEntity self, int level);

    /** 持续阶段结束的那个 tick 调用一次，随后进入冷却阶段。 */
    public abstract void endInvoke(LivingEntity self, int level);

    @Override
    public void onTick(LivingEntity self, int level) {
        Identifier skillId = id();
        DurationState state = MobSkills.state(self, skillId, DurationState.MAP_CODEC, DurationState.EMPTY);

        if (state.duration() > 0) {
            int remaining = state.duration() - 1;
            if (remaining > 0) {
                MobSkills.setState(self, skillId, DurationState.MAP_CODEC,
                    new DurationState(state.cooldown(), remaining));
                onDuration(self, level);
            } else {
                // 持续阶段结束：同一 tick 切入冷却阶段
                MobSkills.setState(self, skillId, DurationState.MAP_CODEC,
                    new DurationState(maxCooldown(), 0));
                onDuration(self, level);
                endInvoke(self, level);
            }
            return;
        }

        if (state.cooldown() > 0) {
            MobSkills.setState(self, skillId, DurationState.MAP_CODEC,
                new DurationState(state.cooldown() - 1, 0));
            onCooldown(self, level);
            return;
        }

        // 初始状态或冷却结束：先检查触发条件，满足才施放并进入持续阶段
        if (!getTrigger(self, level)) {
            return;
        }
        invoke(self, level);
        MobSkills.setState(self, skillId, DurationState.MAP_CODEC, new DurationState(0, this.maxDuration));
    }
}
