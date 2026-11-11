package com.gam0zing.newnew_warlock_core.effect;

import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

import com.gam0zing.newnew_warlock_core.mixin.LivingEntityEffectHooks;
import com.gam0zing.newnew_warlock_core.share.EffectSources;
import com.gam0zing.newnew_warlock_core.share.EffectStack;
import com.gam0zing.newnew_warlock_core.share.MobEffectStackFactory;
import com.gam0zing.newnew_warlock_core.share.MobEffectStackRegistry;

/**
 * 状态效果容器的施加入口逻辑。
 * <p>只有注册过工厂、且不是瞬时效果的状态效果才会进入容器；其余情况返回 null，
 * 由 mixin 放行给原版 {@code LivingEntity#addEffect}。</p>
 * <p>容器实例本身就是 {@link EffectStack} 的实现（且是具体子实例类型的子类），
 * 因此这里直接把 {@link EffectStack#self()} 放进 {@code activeEffects}，
 * 不再需要额外覆盖 {@code getEffect}。</p>
 */
public final class MobEffectStackHooks {

    private MobEffectStackHooks() {}

    /**
     * 尝试把一次施加并入容器。
     *
     * @param entity       被施加的目标
     * @param incoming     即将施加的实例
     * @param sourceEntity 施加来源实体，可为 null
     * @return null 表示不接管、交给原版；true/false 表示本次施加的最终结果
     */
    @Nullable
    public static Boolean tryApply(LivingEntity entity, MobEffectInstance incoming, @Nullable Entity sourceEntity) {
        if (entity.level().isClientSide()) {
            return null;
        }

        Optional<MobEffectStackFactory> factory = MobEffectStackRegistry.factoryFor(incoming.getEffect());
        if (factory.isEmpty()) {
            return null;
        }

        if (!CommonHooks.canMobEffectBeApplied(entity, incoming, sourceEntity)) {
            return false;
        }

        // 容器并入时会把普通实例转换成该效果的子实例类型（复制构造），
        // 因此先写入来源，避免依赖另一个 HEAD 注入（LivingEntityEffectSourceMixin）的执行顺序。
        attachSourceFromContext(entity, incoming, sourceEntity);

        Map<Holder<MobEffect>, MobEffectInstance> active = entity.getActiveEffectsMap();
        MobEffectInstance existing = active.get(incoming.getEffect());

        NeoForge.EVENT_BUS.post(new MobEffectEvent.Added(entity, existing, incoming, sourceEntity));

        EffectStack stack;
        boolean upgraded = false;
        if (existing instanceof EffectStack existingStack) {
            stack = existingStack;
        } else if (existing != null) {
            stack = factory.get().create(incoming.getEffect());
            stack.pushChild(existing);
            upgraded = true;
        } else {
            stack = factory.get().create(incoming.getEffect());
        }

        boolean changed = stack.pushChild(incoming);
        if (existing == null && !changed) {
            return false;
        }

        // 来源保留在各子实例上，容器自身不再持有来源；结算时按“玩家优先，否则最近一次”归因
        MobEffectInstance stackInstance = stack.self();

        LivingEntityEffectHooks hooks = (LivingEntityEffectHooks) (Object) entity;
        if (existing == null) {
            active.put(incoming.getEffect(), stackInstance);
            hooks.newnew$onEffectAdded(stackInstance, sourceEntity);
            // 原版只在首次获得该效果时调用 MobEffectInstance#onEffectAdded，
            // 更新已有槽位时仅调用 onEffectStarted；这里保持一致，避免重复触发（默认实现会播音效）。
            dispatchAdded(entity, incoming);
        } else {
            if (upgraded) {
                active.put(incoming.getEffect(), stackInstance);
            }
            if (changed || upgraded) {
                hooks.newnew$onEffectUpdated(stackInstance, true, sourceEntity);
            }
        }

        dispatchStarted(entity, incoming);
        return true;
    }

    private static void attachSourceFromContext(
        LivingEntity entity, MobEffectInstance incoming, @Nullable Entity sourceEntity) {
        LivingEntity source = EffectSources.sourceEntity(sourceEntity);
        if (source == null) {
            source = EffectSources.currentSelfConsumptionSource(entity);
        }
        if (source != null) {
            EffectSources.attachSource(incoming, source);
        }
    }

    private static void dispatchAdded(LivingEntity entity, MobEffectInstance incoming) {
        if (incoming instanceof EffectStack incomingStack) {
            for (MobEffectInstance child : incomingStack.children()) {
                child.onEffectAdded(entity);
            }
        } else {
            incoming.onEffectAdded(entity);
        }
    }

    private static void dispatchStarted(LivingEntity entity, MobEffectInstance incoming) {
        if (incoming instanceof EffectStack incomingStack) {
            for (MobEffectInstance child : incomingStack.children()) {
                child.onEffectStarted(entity);
            }
        } else {
            incoming.onEffectStarted(entity);
        }
    }
}
