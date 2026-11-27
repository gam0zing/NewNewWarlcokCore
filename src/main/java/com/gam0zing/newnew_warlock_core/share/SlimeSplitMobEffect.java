package com.gam0zing.newnew_warlock_core.share;

import com.gam0zing.newnew_warlock_core.mixin.MobEffectInstanceStackAccess;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginTuning;

import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 史莱姆分裂：倒计时结束的那一 tick 降一级并把时长还原为 5 分钟；I 级结束时正常移除。
 * <p>等级与剩余时长本身就是状态，不需要额外的玩家数据记录。</p>
 */
public final class SlimeSplitMobEffect extends MobEffect {

    public SlimeSplitMobEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration == 1;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (amplifier <= 0) {
            return false;
        }
        MobEffectInstance instance = entity.getEffect(ModMobEffects.SLIME_SPLIT);
        if (instance == null) {
            return false;
        }
        MobEffectInstanceStackAccess access = (MobEffectInstanceStackAccess) instance;
        access.newnew$setAmplifier(amplifier - 1);
        access.newnew$setDuration(OriginTuning.SLIME_SPLIT_DURATION_TICKS);
        if (entity instanceof ServerPlayer player) {
            // 原地改写了字段，需要主动同步一次，避免客户端一直显示旧等级。
            player.connection.send(new ClientboundUpdateMobEffectPacket(player.getId(), instance, false));
        }
        return true;
    }
}
