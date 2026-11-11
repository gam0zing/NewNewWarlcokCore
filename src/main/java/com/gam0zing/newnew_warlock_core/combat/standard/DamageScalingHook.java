package com.gam0zing.newnew_warlock_core.combat.standard;

import com.gam0zing.newnew_warlock_core.combat.spine.DamageCategory;
import com.gam0zing.newnew_warlock_core.combat.spine.DamageScaling;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

/**
 * 伤害强度修正的挂载点。
 * <p>按攻击者持有的 {@code damage_scale}（通用）与分类伤害强度（{@code <tag>_damage_scale}）修正伤害。</p>
 * <p>这些属性是“造成伤害”的属性，因此只读取伤害来源生物（{@link DamageSource#getEntity()}）；
 * 环境伤害等没有攻击者的伤害不受影响。</p>
 */
public final class DamageScalingHook {

    /** 由 {@code LivingDamageEvt} 在 LOW 优先级转发。 */
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        float original = event.getNewDamage();
        float scaled = DamageScaling.apply(attacker, DamageCategory.resolve(source), original);
        if (scaled != original) {
            event.setNewDamage(scaled);
        }
    }

    private DamageScalingHook() {}
}
