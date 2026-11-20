package com.gam0zing.newnew_warlock_core.origin;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.resources.Identifier;

/**
 * 本模组示例起源内容的注册名。
 * <p>正式内容替换这些示例时，只需要改数据文件与这里的常量。</p>
 */
public final class OriginIds {

    public static final Identifier RACE_SAMPLE = id("race/sample");
    public static final Identifier ESSENCE_SAMPLE = id("essence/sample");
    public static final Identifier BLESSING_NONE = id("blessing/none");
    public static final Identifier BLESSING_SAMPLE = id("blessing/sample");

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, path);
    }

    private OriginIds() {}
}
