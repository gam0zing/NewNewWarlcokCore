package com.gam0zing.newnew_warlock_core.mixin;

import java.util.List;
import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import com.gam0zing.newnew_warlock_core.combat.effect.EffectPersistence;

/**
 * 状态效果附加数据的实体存档。
 *
 * <p>26.3 的实体存档改用 {@link ValueOutput}/{@link ValueInput}，效果实例本身不再暴露
 * {@code save/load}，因此来源 UUID 与容器子实例改由这里在原生效果数据之后追加一份附带记录。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityEffectPersistenceMixin {

    @Shadow
    @Final
    private Map<Holder<MobEffect>, MobEffectInstance> activeEffects;

    /** 存档：把来源 UUID 与容器子实例额外写到效果数据之后 */
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void newnew$saveEffectExtras(ValueOutput output, CallbackInfo ci) {
        List<EffectPersistence.SavedEffect> saved = EffectPersistence.collect(this.activeEffects);
        if (!saved.isEmpty()) {
            output.store(EffectPersistence.KEY, EffectPersistence.CODEC, saved);
        }
    }

    /** 读档：把来源 UUID 与容器子实例写回已加载的效果槽位 */
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void newnew$loadEffectExtras(ValueInput input, CallbackInfo ci) {
        input.read(EffectPersistence.KEY, EffectPersistence.CODEC)
            .ifPresent(saved -> EffectPersistence.apply(this.activeEffects, saved));
    }
}
