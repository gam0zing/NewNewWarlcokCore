package com.gam0zing.newnew_warlock_core.combat.standard;

import com.gam0zing.newnew_warlock_core.combat.spine.DamageCategory;
import com.gam0zing.newnew_warlock_core.combat.spine.DamageContext;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * 闪避判定。
 * <p>在伤害流程最早期、内部伤害短路之后执行：命中后整次伤害计算被跳过，
 * 不进入预处理、格挡、护甲/附魔减免、伤害事件与扣血。</p>
 * <p>只有物理、魔法两个伤害分类参与判定，且爆炸伤害始终无法闪避；
 * 模组内部伤害（{@code IDamageHandler#isInner}）在此之前已经直接结算，同样不会走到这里。</p>
 */
public final class DamageDodge {

    /**
     * 按受击者的闪避概率决定是否跳过本次伤害。
     *
     * @return {@code true} 表示本次伤害被闪避
     */
    public static boolean tryDodge(DamageContext ctx) {
        DamageCategory category = ctx.getCategory();
        if (category != DamageCategory.PHYSICS && category != DamageCategory.MAGIC) {
            return false;
        }

        DamageSource source = ctx.getSource();
        if (source.is(DamageTypeTags.IS_EXPLOSION)) {
            return false;
        }

        LivingEntity target = ctx.getTarget();
        double dodge = ModAttributes.getValue(target, ModAttributes.DODGE);
        return dodge > 0.0D && target.getRandom().nextDouble() < dodge;
    }

    private DamageDodge() {}
}
