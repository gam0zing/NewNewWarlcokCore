package com.gam0zing.newnew_warlock_core.effect;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.mixin.MobEffectInstanceStackAccess;
import com.gam0zing.newnew_warlock_core.share.EffectSources;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 药水属性的施加修正。
 * <p>在效果真正进入目标的效果容器之前，按「来源生物」持有的属性修正这份效果实例：</p>
 * <ul>
 *     <li>{@code potion_level}：等级加上该值（向下取整），降到 0 以下则放弃施加；</li>
 *     <li>{@code potion_time}：持续时间乘以该值；</li>
 * </ul>
 * <p>实例是原地修改的，因此 {@link EffectSources} 写入的来源、隐藏效果等附加信息都不会丢失。</p>
 */
public final class PotionScaling {

    /**
     * @param target       被施加效果的目标
     * @param instance     即将施加的效果实例（原地修改）
     * @param sourceEntity 原版施加入口携带的来源实体，可为 null
     * @return {@code false} 表示应当放弃这次施加
     */
    public static boolean apply(LivingEntity target, MobEffectInstance instance, @Nullable Entity sourceEntity) {
        LivingEntity source = EffectSources.sourceEntity(sourceEntity);
        if (source == null) {
            source = EffectSources.currentSelfConsumptionSource(target);
        }
        if (source == null) {
            return true;
        }

        double levelBonus = ModAttributes.getValue(source, ModAttributes.POTION_LEVEL);
        if (levelBonus != 0.0D) {
            int amplifier = instance.getAmplifier() + (int) Math.floor(levelBonus);
            if (amplifier < 0) {
                return false;
            }
            int clamped = Mth.clamp(amplifier, 0, 255);
            if (clamped != instance.getAmplifier()) {
                ((MobEffectInstanceStackAccess) instance).newnew$setAmplifier(clamped);
            }
        }

        float timeScale = ModAttributes.getScale(source, ModAttributes.POTION_TIME);
        int duration = instance.getDuration();
        if (timeScale != 1.0F && duration > 0) {
            int scaled = (int) Math.floor(duration * (double) timeScale);
            if (scaled <= 0) {
                return false;
            }
            if (scaled != duration) {
                ((MobEffectInstanceStackAccess) instance).newnew$setDuration(scaled);
            }
        }

        return true;
    }

    private PotionScaling() {}
}
