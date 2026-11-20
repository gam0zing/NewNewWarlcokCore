package com.gam0zing.newnew_warlock_core.compat.neoorigins;

import net.neoforged.fml.ModList;

/**
 * NeoOrigins 可选依赖的运行时判定。
 * <p>NeoOrigins 只作为 {@code compileOnly} + {@code localRuntime} 的可选依赖存在。
 * 类本身不在字段或签名里持有 NeoOrigins 类型，未安装该模组时
 * {@link #isLoaded()} 为 {@code false}，调用方据此跳过全部兼容逻辑，
 * 不会触发目标类解析。</p>
 */
public final class NeoOriginsGate {

    private static final boolean LOADED = ModList.get().isLoaded("neoorigins");

    /** {@return NeoOrigins 是否已安装} */
    public static boolean isLoaded() {
        return LOADED;
    }

    private NeoOriginsGate() {}
}
