package com.gam0zing.newnew_warlock_core.combat.api;

import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

/**
 * 一条伤害处理器注册项。
 *
 * @param tag      绑定的伤害标签；{@code null} 表示兜底项
 * @param priority 优先级，数值越大越先被扫描
 * @param handler  处理器
 */
public record DamageHandlerRegistration(
    @Nullable TagKey<DamageType> tag,
    int priority,
    IDamageHandler handler) {

    public DamageHandlerRegistration {
        Objects.requireNonNull(handler, "handler");
    }

    /** {@return 绑定到伤害标签的注册项} */
    public static DamageHandlerRegistration tagged(TagKey<DamageType> tag, int priority, IDamageHandler handler) {
        return new DamageHandlerRegistration(Objects.requireNonNull(tag, "tag"), priority, handler);
    }

    /** {@return 未命中任何标签时使用的兜底注册项} */
    public static DamageHandlerRegistration fallback(int priority, IDamageHandler handler) {
        return new DamageHandlerRegistration(null, priority, handler);
    }

    public boolean isFallback() {
        return this.tag == null;
    }

    /** {@return 排序用的标签键；兜底项返回空串，保证排序稳定} */
    public String sortKey() {
        return this.tag == null ? "" : this.tag.location().toString();
    }
}
