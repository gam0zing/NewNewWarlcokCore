package com.gam0zing.newnew_warlock_core.combat.effect;

import java.util.List;

import net.minecraft.world.effect.MobEffectInstance;

/**
 * 状态效果容器契约（框架层）。
 *
 * <p><b>放弃通用容器</b>：每个需要多实例合并的效果自己声明两段类型：
 * 一是该效果的子实例类型（如 {@link BleedingEffectInstance}），二是直接继承它的容器类型
 * （如 {@link BleedingEffectStack}）。容器对象本身就是子实例类型的实例，会被原样放进
 * {@code activeEffects}，因此迭代、{@code getEffect}、网络包与存档拿到的都是它 ——
 * 调用方可以安全转型回该效果的实例类型，并通过容器重写的方法读到聚合值。</p>
 *
 * <p>本接口只保留框架在“施加 / 读档重建 / 展平”三处需要的操作，不提供任何通用实现。</p>
 */
public interface EffectStack {

    /** 存档中保存子实例列表的键 */
    String CHILDREN_KEY = "newnew:stack_children";

    /** {@return 容器自身，必须是 {@link MobEffectInstance} 的子类实例} */
    MobEffectInstance self();

    /**
     * 并入一个子实例。
     * <p>不复制对象，默认所有权转移；同一实例进入多个容器属于调用方违约。
     * 传入另一个容器时展平其子实例。</p>
     *
     * @param child 待并入的实例
     * @return 是否真的并入了内容
     */
    boolean pushChild(MobEffectInstance child);

    /** {@return 只读的子实例列表} */
    List<MobEffectInstance> children();

    /**
     * {@return 全部子实例的等级合计值（1 级 = 1）}
     * <p>这是内部数据口径，<b>不做</b> {@code amplifier} 的 255 上限截断：
     * 255 只是 UI/原版字段的显示上限，不是系统的等级上限。
     * 效果逻辑需要真实等级时应读本方法，而不是读 {@code amplifier}。</p>
     */
    int totalLevel();
}
