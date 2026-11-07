package com.gam0zing.newnew_warlock_core.enchant;

import java.util.Objects;

/**
 * 附魔改造接口的静态门面。
 * <p>所有 Mixin 只调用这里；默认使用 {@link VanillaEnchantHooks}，因此未替换实现时完全保持原版行为。
 * 需要自定义规则时，在模组初始化阶段调用 {@link #setHooks(WarlockEnchantHooks)} 注册自己的实现（一般保留一个
 * {@code VanillaEnchantHooks} 作为回退分支即可）。</p>
 */
public final class WarlockEnchanting {

    private static volatile WarlockEnchantHooks hooks = VanillaEnchantHooks.INSTANCE;

    private WarlockEnchanting() {}

    public static WarlockEnchantHooks hooks() {
        return hooks;
    }

    public static void setHooks(WarlockEnchantHooks implementation) {
        hooks = Objects.requireNonNull(implementation, "implementation");
    }

    public static void resetHooks() {
        hooks = VanillaEnchantHooks.INSTANCE;
    }
}
