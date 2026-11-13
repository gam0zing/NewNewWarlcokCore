package com.gam0zing.newnew_warlock_core.mob.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * 带冷却的技能基类。
 * <p>该技能自己的状态只包含 {@code cooldown}，以 {@link CooldownState} 的 Codec 存进技能条目的
 * {@code custom} 里，与等级是同一条数据：</p>
 * <ul>
 *     <li>{@code cooldown > 0}：每次 tick 减 1，并调用 {@link #onCooldown(LivingEntity, int)}；</li>
 *     <li>{@code cooldown == 0}：调用 {@link #invoke(LivingEntity, int)}，然后把 cooldown 设为
 *     {@link #maxCooldown()}。</li>
 * </ul>
 * <p>具体技能若需要额外状态，可定义自己的 MapCodec，并通过
 * {@link MobSkills#state} / {@link MobSkills#setState} 读写 custom 中的其他键，不会覆盖 cooldown。</p>
 */
public abstract class CooldownMobSkill implements NewNewMobSkill {

    /** 冷却技能在 custom 中保存的状态。 */
    public record CooldownState(int cooldown) {

        public static final MapCodec<CooldownState> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("cooldown", 0).forGetter(CooldownState::cooldown))
            .apply(instance, CooldownState::new));

        public static final CooldownState EMPTY = new CooldownState(0);
    }

    private final Identifier id;
    private final float price;
    private final int maxCooldown;

    /**
     * @param id          技能注册名
     * @param price       每级价值（points），不能为负数
     * @param maxCooldown 冷却时长（tick），必须为正数
     */
    protected CooldownMobSkill(Identifier id, float price, int maxCooldown) {
        if (price < 0.0F) {
            throw new IllegalArgumentException("price 不能为负数：" + id + " -> " + price);
        }
        if (maxCooldown <= 0) {
            throw new IllegalArgumentException("maxCooldown 必须为正数：" + id + " -> " + maxCooldown);
        }
        this.id = id;
        this.price = price;
        this.maxCooldown = maxCooldown;
    }

    @Override
    public final Identifier id() {
        return this.id;
    }

    @Override
    public final float price() {
        return this.price;
    }

    /** {@return 冷却时长（tick）} */
    public final int maxCooldown() {
        return this.maxCooldown;
    }

    /** 冷却结束、可以施放时调用。 */
    public abstract void invoke(LivingEntity self, int level);

    /** 冷却期内的每个 tick 调用（cooldown 已减 1）。 */
    public abstract void onCooldown(LivingEntity self, int level);

    /**
     * 冷却完毕时的触发条件。
     * <p>返回 {@code true} 才会 {@link #invoke} 并重新进入冷却；返回 {@code false} 时保持待机，
     * 下个 tick 继续检查。</p>
     */
    public abstract boolean getTrigger(LivingEntity self, int level);

    @Override
    public void onTick(LivingEntity self, int level) {
        Identifier skillId = id();
        CooldownState state = MobSkills.state(self, skillId, CooldownState.MAP_CODEC, CooldownState.EMPTY);

        if (state.cooldown() > 0) {
            MobSkills.setState(self, skillId, CooldownState.MAP_CODEC,
                new CooldownState(state.cooldown() - 1));
            onCooldown(self, level);
            return;
        }

        if (!getTrigger(self, level)) {
            return;
        }

        invoke(self, level);
        MobSkills.setState(self, skillId, CooldownState.MAP_CODEC, new CooldownState(this.maxCooldown));
    }
}
