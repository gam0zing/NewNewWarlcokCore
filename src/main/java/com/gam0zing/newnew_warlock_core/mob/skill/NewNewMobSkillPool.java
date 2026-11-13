package com.gam0zing.newnew_warlock_core.mob.skill;

import java.util.List;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;

/**
 * 生物技能池。
 * <p>技能池是全局单例（注册进 {@link MobSkillPools}），生物通过生物 id 绑定若干技能池 id，
 * 重建前期依次调用每个技能池的 {@link #getRandom(LivingEntity, double)}，
 * 把返回的技能合并进生物自己的技能列表，并按技能价值扣除生物剩余的 points。</p>
 * <p>随机源使用 {@code self.getRandom()}；返回的技能不需要去重，合并阶段会处理，
 * 但等级必须为正数，非法条目会被跳过并记录日志。</p>
 * <p>技能池有优先级：同一生物绑定的多个技能池按 {@link #priority()} 从高到低依次抽取，
 * 优先级相同时保持绑定的先后顺序。因此可以为生物追加专属技能池并让其先于通用池抽取。</p>
 */
public interface NewNewMobSkillPool {

    /** {@return 注册名，同时也是生物绑定与数据文件里使用的技能池 id} */
    Identifier id();

    /**
     * {@return 抽取优先级，数值越大越先抽取}
     * <p>默认 0；实现类通过构造参数提供，注册后随单例固定。</p>
     */
    default int priority() {
        return 0;
    }

    /**
     * 为指定生物随机出技能。
     *
     * @param self           技能持有者
     * @param remainingPoints 当前剩余的可分配 points，技能池应只返回总价值不超过它的技能
     * @return 技能 id - 等级列表；可以为空列表，不应返回 {@code null}
     */
    List<MobSkillEntry> getRandom(LivingEntity self, double remainingPoints);
}
