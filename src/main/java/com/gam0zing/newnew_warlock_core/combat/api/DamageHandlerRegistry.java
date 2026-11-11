package com.gam0zing.newnew_warlock_core.combat.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * 伤害标签 → 处理器的注册表。
 * <p>伤害开始处理时，按优先级从高到低扫描所有绑定标签的注册项，第一个命中的标签对应的处理器接管本次流程；
 * 没有任何标签命中时使用兜底处理器。</p>
 * <p>同优先级按“标签 ID → 处理器 ID”排序，保证结果与注册顺序无关、跨启动稳定。</p>
 * <p>注册在模组初始化阶段完成，随后 {@link #freeze()} 固化；固化后不允许再注册，
 * 从而保证运行期不会出现“某个存档里多了一个处理器”的隐式差异。</p>
 */
public final class DamageHandlerRegistry {

    /** 优先级降序；同优先级按标签 ID、再按处理器 ID 升序 */
    private static final Comparator<DamageHandlerRegistration> ORDER =
        Comparator.comparingInt(DamageHandlerRegistration::priority).reversed()
            .thenComparing(DamageHandlerRegistration::sortKey)
            .thenComparing(registration -> registration.handler().id().toString());

    private static final List<DamageHandlerRegistration> TAGGED = new ArrayList<>();
    private static final List<DamageHandlerRegistration> FALLBACKS = new ArrayList<>();
    private static final Set<TagKey<DamageType>> REGISTERED_TAGS = new HashSet<>();

    private static List<DamageHandlerRegistration> taggedOrder = List.of();
    private static List<DamageHandlerRegistration> fallbackOrder = List.of();
    private static boolean frozen;

    /**
     * 注册一个“伤害标签 → 处理器”的绑定。
     *
     * @throws IllegalStateException 注册表已固化，或该标签已经注册过
     */
    public static synchronized void register(TagKey<DamageType> tag, int priority, IDamageHandler handler) {
        requireUnfrozen();
        if (!REGISTERED_TAGS.add(tag)) {
            throw new IllegalStateException("伤害标签重复注册：" + tag.location());
        }
        TAGGED.add(DamageHandlerRegistration.tagged(tag, priority, handler));
    }

    /**
     * 注册兜底处理器：所有没有命中任何已注册标签的伤害都由它接管。
     *
     * @throws IllegalStateException 注册表已固化
     */
    public static synchronized void registerFallback(int priority, IDamageHandler handler) {
        requireUnfrozen();
        FALLBACKS.add(DamageHandlerRegistration.fallback(priority, handler));
    }

    /**
     * 固化注册表。必须至少注册一个兜底处理器，否则所有未打标签的伤害都会无处理器可用。
     *
     * @throws IllegalStateException 没有兜底处理器
     */
    public static synchronized void freeze() {
        if (frozen) {
            return;
        }
        if (FALLBACKS.isEmpty()) {
            throw new IllegalStateException("伤害处理器注册表缺少兜底处理器");
        }
        taggedOrder = List.copyOf(sorted(TAGGED));
        fallbackOrder = List.copyOf(sorted(FALLBACKS));
        frozen = true;
    }

    /**
     * 为一次伤害选出处理器。
     * <p>顺序扫描已注册标签，第一个命中者获胜；均未命中时返回兜底处理器。</p>
     */
    public static IDamageHandler resolve(DamageSource source) {
        if (!frozen) {
            throw new IllegalStateException("伤害处理器注册表尚未固化");
        }
        for (DamageHandlerRegistration registration : taggedOrder) {
            if (source.is(registration.tag())) {
                return registration.handler();
            }
        }
        return fallbackOrder.get(0).handler();
    }

    /** {@return 已固化的标签注册项（按扫描顺序）} 仅供诊断使用。 */
    public static List<DamageHandlerRegistration> taggedOrder() {
        return taggedOrder;
    }

    /** {@return 已固化的兜底注册项（按扫描顺序）} 仅供诊断使用。 */
    public static List<DamageHandlerRegistration> fallbackOrder() {
        return fallbackOrder;
    }

    private static List<DamageHandlerRegistration> sorted(List<DamageHandlerRegistration> source) {
        List<DamageHandlerRegistration> copy = new ArrayList<>(source);
        copy.sort(ORDER);
        return copy;
    }

    private static void requireUnfrozen() {
        if (frozen) {
            throw new IllegalStateException("伤害处理器注册表已固化，不能再注册");
        }
    }

    private DamageHandlerRegistry() {}
}
