package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.world.item.component.BlocksAttacks;

/**
 * 盾牌格挡时扣除的耐久提升至 3 倍。
 * <p>26.3 的格挡耐久走 {@code BlocksAttacks#hurtBlockingItem}，耐久公式为
 * {@code itemDamage.apply(damage)}；这里把传入的格挡伤害放大 3 倍，耐久消耗随之变为 3 倍，
 * 不影响使用统计（{@code Stats#ITEM_USED} 仍只记一次）。</p>
 */
@Mixin(BlocksAttacks.class)
public abstract class BlocksAttacksMixin {

    @ModifyVariable(method = "hurtBlockingItem", at = @At("HEAD"), argsOnly = true)
    private float newnew$tripleShieldDurabilityCost(float damage) {
        return damage * 3.0F;
    }
}
