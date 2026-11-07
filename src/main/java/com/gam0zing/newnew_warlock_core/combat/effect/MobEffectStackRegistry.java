package com.gam0zing.newnew_warlock_core.combat.effect;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * 状态效果容器注册表：效果注册名 → 容器工厂。
 * <p><b>按需注册</b>，未注册的效果完全走原版 {@code activeEffects} 行为。</p>
 */
public final class MobEffectStackRegistry {

    private static final Map<Identifier, MobEffectStackFactory> FACTORIES = new ConcurrentHashMap<>();

    private MobEffectStackRegistry() {}

    /**
     * 注册容器工厂。
     *
     * @param effect  效果类型
     * @param factory 容器工厂
     * @throws IllegalStateException 同一效果重复注册时抛出，属于开发期错误
     */
    public static void register(Holder<MobEffect> effect, MobEffectStackFactory factory) {
        Objects.requireNonNull(effect, "effect");
        Objects.requireNonNull(factory, "factory");
        Identifier id = idOf(effect);
        MobEffectStackFactory existing = FACTORIES.putIfAbsent(id, factory);
        if (existing != null) {
            throw new IllegalStateException("Duplicate mob effect stack factory for " + id);
        }
    }

    /** {@return 该效果对应的容器工厂；未注册或为瞬时效果时为空} */
    public static Optional<MobEffectStackFactory> factoryFor(Holder<MobEffect> effect) {
        if (effect.value().isInstantaneous()) {
            return Optional.empty();
        }
        return Optional.ofNullable(FACTORIES.get(idOf(effect)));
    }

    /**
     * 读档重建：如果存档里带有子实例列表且该效果已注册工厂，则把普通实例升级为容器。
     *
     * @param base     原版读取出的普通实例
     * @param children 子实例列表
     * @return 重建后的容器实例；不满足条件或解析失败时为 null
     */
    @Nullable
    public static MobEffectInstance tryRebuild(MobEffectInstance base, List<MobEffectInstance> children) {
        if (base.getEffect().value().isInstantaneous()) {
            return null;
        }

        MobEffectStackFactory factory = FACTORIES.get(idOf(base.getEffect()));
        if (factory == null || children.isEmpty()) {
            return null;
        }

        EffectStack stack = factory.create(base.getEffect());
        for (MobEffectInstance child : children) {
            if (child != null && child.getEffect().equals(base.getEffect())) {
                stack.pushChild(child);
            }
        }

        return stack.children().isEmpty() ? null : stack.self();
    }

    private static Identifier idOf(Holder<MobEffect> effect) {
        return effect.unwrapKey()
            .map(key -> key.identifier())
            .orElseGet(() -> Objects.requireNonNull(BuiltInRegistries.MOB_EFFECT.getKey(effect.value())));
    }
}
