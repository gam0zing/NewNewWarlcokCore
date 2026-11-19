package com.gam0zing.newnew_warlock_core.compat.bettercombat;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;

/**
 * 矛挑飞的伤害类型。
 * <p>该类型归入 {@code physics} 标签，但不属于 {@code no_knockback}，因此会进入本模组的伤害管线，
 * 并由 {@code VanillaDamageHandler} 分流到 45° 上抬的挑飞位移。</p>
 */
public final class SpearLift {

    public static final ResourceKey<DamageType> KEY = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "spear_lift"));

    /** {@return 以攻击者自身位置为来源坐标的挑飞伤害源；伤害类型未加载时返回 {@code null}} */
    @Nullable
    public static DamageSource createSource(LivingEntity attacker) {
        Holder<DamageType> holder = attacker.level().damageSources().damageTypes.get(KEY).orElse(null);
        if (holder == null) {
            return null;
        }
        return new DamageSource(holder, attacker, attacker, attacker.position());
    }

    private SpearLift() {}
}
