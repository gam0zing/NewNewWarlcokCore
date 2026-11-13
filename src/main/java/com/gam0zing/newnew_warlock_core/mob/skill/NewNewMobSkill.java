package com.gam0zing.newnew_warlock_core.mob.skill;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * 生物技能。
 * <p>技能是全局单例（注册进 {@link MobSkills}），除注册名外，全部行为由实现类负责；
 * 每次 tick 由 {@link MobSkills#updateSkill(LivingEntity)} 驱动。</p>
 * <p>技能自身需要的状态（冷却、计数器等）必须按生物保存在该生物自己的 NBT 上，
 * 不允许存放在技能实例字段里，否则多个生物会互相污染。</p>
 */
public interface NewNewMobSkill {

    /** {@return 注册名，同时也是技能列表 NBT 里使用的技能 id} */
    Identifier id();

    /**
     * {@return 该技能每级的价值（points，只读）}
     * <p>由具体技能在构造时传入，保存在全局单例里。</p>
     */
    float price();

    /**
     * {@return 指定等级的总价值}
     * <p>默认是“等级 × {@link #price()}”，子类可以按等级做非线性定价。</p>
     */
    default float getTotalPrice(int level) {
        return level * price();
    }

    /**
     * 每 tick 驱动一次。
     *
     * @param self  技能持有者
     * @param level 该生物拥有的该技能等级（合并后的等级，至少为 1）
     */
    void onTick(LivingEntity self, int level);
}
