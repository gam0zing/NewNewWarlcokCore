package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;

import com.gam0zing.newnew_warlock_core.combat.damage.PeriodicDamageGate;

/**
 * 为 {@link PeriodicDamageGate} 提供每个生物自己的结算时刻。
 * <p>字段只在内存中，不随存档持久化：实体重新加载后按“从未结算”处理，
 * 因此不会因为 {@code tickCount} 归零而被长时间卡住。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityPeriodicDamageMixin implements PeriodicDamageGate {

    /** 从未结算过的哨兵；取 {@code Integer.MIN_VALUE / 2} 以避免与 tickCount 相减时溢出 */
    @Unique
    private static final int NEWNEW$NEVER = Integer.MIN_VALUE / 2;

    @Unique
    private int newnew$lastDrownTick = NEWNEW$NEVER;

    @Unique
    private int newnew$lastDryOutTick = NEWNEW$NEVER;

    @Override
    public boolean newnew$allowPeriodicDamage(DamageSource source) {
        int now = ((LivingEntity) (Object) this).tickCount;
        if (source.is(DamageTypes.DROWN)) {
            if (now - this.newnew$lastDrownTick < VANILLA_INTERVAL_TICKS) {
                return false;
            }
            this.newnew$lastDrownTick = now;
            return true;
        }
        if (source.is(DamageTypes.DRY_OUT)) {
            if (now - this.newnew$lastDryOutTick < VANILLA_INTERVAL_TICKS) {
                return false;
            }
            this.newnew$lastDryOutTick = now;
            return true;
        }
        return true;
    }
}
