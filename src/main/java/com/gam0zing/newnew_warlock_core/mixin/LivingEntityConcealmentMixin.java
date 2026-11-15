package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 隐蔽与灵性：在 {@link LivingEntity#getVisibilityPercent(ServerLevel, Entity)} 的出口结算索敌范围。
 * <p>原版该方法内已结算完隐身、潜行、护甲覆盖与 NeoForge 可见性钩子，这里在此基础上：</p>
 * <ul>
 *     <li>隐蔽（玩家专属）：被发现范围 = 原本值 × (1 - 隐蔽)；</li>
 *     <li>灵性（索敌方）：相对基准 10 每点，若范围已被削减（有缺口）则补回缺口的 10%（最多补满原本值）；
 *     没有缺口时改为把索敌范围放大 5%。</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityConcealmentMixin {

    /** 四维基准值：10 点视为原版水平。 */
    private static final double SPIRITUALITY_BASELINE = 10.0D;
    /** 灵性每点补回被削减索敌范围的百分比。 */
    private static final double GAP_FILL_PER_POINT = 0.10D;
    /** 灵性每点放大索敌范围的百分比。 */
    private static final double RANGE_BONUS_PER_POINT = 0.05D;

    @Inject(method = "getVisibilityPercent", at = @At("RETURN"), cancellable = true)
    private void newnew$applyVisibilityAttributes(
        ServerLevel level, Entity targetingEntity, CallbackInfoReturnable<Double> cir) {
        double base = cir.getReturnValueD();
        double result = base;

        if ((Object) this instanceof Player player) {
            double concealment = ModAttributes.getValue(player, ModAttributes.CONCEALMENT);
            if (concealment != 0.0D) {
                result = base * (1.0D - concealment);
            }
        }

        if (targetingEntity instanceof LivingEntity seeker) {
            double spirituality = ModAttributes.getValue(seeker, ModAttributes.NEWNEW_SPIRITUALITY)
                - SPIRITUALITY_BASELINE;
            if (spirituality > 0.0D) {
                double deficit = base - result;
                if (deficit > 0.0D) {
                    result += deficit * Math.min(1.0D, spirituality * GAP_FILL_PER_POINT);
                } else {
                    result *= 1.0D + spirituality * RANGE_BONUS_PER_POINT;
                }
            }
        }

        if (result != base) {
            cir.setReturnValue(result);
        }
    }
}
