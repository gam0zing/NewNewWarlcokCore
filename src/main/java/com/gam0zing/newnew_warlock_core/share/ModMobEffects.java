package com.gam0zing.newnew_warlock_core.share;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.share.BleedingMobEffect;
import com.gam0zing.newnew_warlock_core.share.FatalWoundMobEffect;
import com.gam0zing.newnew_warlock_core.share.HemorrhageMobEffect;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 伤害模块使用的状态效果注册表。
 * <p>持续时间、等级、触发概率等由身体部位数据文件（{@code newnew_body}）中的 {@code effects} 条目决定。</p>
 * <p>{@link #BLEEDING}、{@link #HEMORRHAGE}、{@link #FATAL_WOUND} 已实现生效逻辑；
 * 其余效果当前只完成注册（分类与颜色），生效逻辑后续实现。</p>
 */
public final class ModMobEffects {

    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, NewNewWarlockCore.MOD_ID);

    // ============================== 持续效果 ==============================

    /** 晕眩 */
    public static final DeferredHolder<MobEffect, MobEffect> DIZZINESS =
        register("dizziness", 0x9AA5B1);

    /** 出血：每 100 游戏刻按当前生命值百分比直接扣除生命 */
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING =
        MOB_EFFECTS.register("bleeding",
            () -> new BleedingMobEffect(MobEffectCategory.HARMFUL, 0xB11A1A));

    /** 失血 */
    public static final DeferredHolder<MobEffect, MobEffect> BLOOD_LOSS =
        register("blood_loss", 0x6B0B0B);

    /** 左臂骨折 */
    public static final DeferredHolder<MobEffect, MobEffect> LEFT_ARM_FRACTURE =
        register("left_arm_fracture", 0xE4DCC0);

    /** 右臂骨折 */
    public static final DeferredHolder<MobEffect, MobEffect> RIGHT_ARM_FRACTURE =
        register("right_arm_fracture", 0xDDD3B3);

    /** 腿部骨折 */
    public static final DeferredHolder<MobEffect, MobEffect> LEGS_FRACTURE =
        register("legs_fracture", 0xCEC4A4);

    /** 躯干骨折 */
    public static final DeferredHolder<MobEffect, MobEffect> TORSO_FRACTURE =
        register("torso_fracture", 0xBFB28E);

    /** 颈部骨折 */
    public static final DeferredHolder<MobEffect, MobEffect> NECK_FRACTURE =
        register("neck_fracture", 0xEDE6D0);

    // ============================== 增益效果 ==============================

    /** 不可阻挡：每级提供 20% 击退抗性 */
    public static final DeferredHolder<MobEffect, MobEffect> UNSTOPPABLE =
        MOB_EFFECTS.register("unstoppable",
            () -> new UnstoppableMobEffect(MobEffectCategory.BENEFICIAL, 0x8FA0B5));

    // ============================== 瞬时效果 ==============================

    /** 大出血：瞬时效果，施加时立即结算一次（随机施加出血） */
    public static final DeferredHolder<MobEffect, MobEffect> HEMORRHAGE =
        MOB_EFFECTS.register("hemorrhage",
            () -> new HemorrhageMobEffect(MobEffectCategory.HARMFUL, 0xB30000));

    /** 致命伤：持续效果，可叠加；扣血后生命值低于 15%×等级的最大生命值时由效果来源追加一次流血伤害 */
    public static final DeferredHolder<MobEffect, MobEffect> FATAL_WOUND =
        MOB_EFFECTS.register("fatal_wound",
            () -> new FatalWoundMobEffect(MobEffectCategory.HARMFUL, 0x990000));

    // ============================== 起源效果 ==============================

    /** 威慑：被猫威慑期间无法锁定目标，并持续远离威慑来源 */
    public static final DeferredHolder<MobEffect, MobEffect> INTIMIDATED =
        MOB_EFFECTS.register("intimidated", IntimidatedMobEffect::new);

    /** 史莱姆分裂：倒计时结束时降一级并重置为 5 分钟，I 级结束则移除 */
    public static final DeferredHolder<MobEffect, MobEffect> SLIME_SPLIT =
        MOB_EFFECTS.register("slime_split",
            () -> new SlimeSplitMobEffect(MobEffectCategory.BENEFICIAL, 0x63C64A));

    /** 史莱姆巨大化：界面提示，实际增益由起源能力维护 */
    public static final DeferredHolder<MobEffect, MobEffect> SLIME_GIANT =
        MOB_EFFECTS.register("slime_giant",
            () -> new SimpleEffect(MobEffectCategory.BENEFICIAL, 0x8FE06B));

    /** 史莱姆冰冻：寒冷群系的体感状态，到期时按环境升降级 */
    public static final DeferredHolder<MobEffect, MobEffect> SLIME_FROZEN =
        MOB_EFFECTS.register("slime_frozen", SlimeFrozenMobEffect::new);

    /** 史莱姆失水：炎热干燥群系的体感状态，到期时按环境升降级 */
    public static final DeferredHolder<MobEffect, MobEffect> SLIME_DEHYDRATED =
        MOB_EFFECTS.register("slime_dehydrated", SlimeDehydratedMobEffect::new);

    private static DeferredHolder<MobEffect, MobEffect> register(String name, int color) {
        return MOB_EFFECTS.register(name,
            () -> new SimpleEffect(MobEffectCategory.HARMFUL, color));
    }

    /** 仅暴露原版受保护构造器，供效果注册使用；生效逻辑后续按需覆写 */
    private static final class SimpleEffect extends MobEffect {
        private SimpleEffect(MobEffectCategory category, int color) {
            super(category, color);
        }
    }

    private ModMobEffects() {}
}
