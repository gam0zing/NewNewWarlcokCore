package com.gam0zing.newnew_warlock_core.config;

import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 网络同步相关配置。
 * <p>{@code custom_precision} 控制 {@code ClientboundSetEntityMotionPacket} 里速度分量的编码精度：
 * 精度为 {@code 1 / custom_precision}，单分量上限为 {@code 32767 / custom_precision}。
 * 原版固定为 8000（精度 0.000125，理论单分量上限约 4.096 block/tick，原版另钳到 3.9）。</p>
 * <p>服务端与客户端必须使用相同的值，否则客户端还原出来的速度会整体缩放。</p>
 */
public final class NetConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue CUSTOM_PRECISION;

    /** 运行时缓存，默认与原版一致。 */
    public static volatile double customPrecision = 8000.0D;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        CUSTOM_PRECISION = builder
            .comment(
                "实体速度分量在 ClientboundSetEntityMotionPacket 中的编码精度。",
                "精度 = 1 / 该值，单分量上限 = 32767 / 该值。",
                "原版为 8000（精度 0.000125，上限约 4.096 block/tick，实际另钳到 3.9）。",
                "调大可以提高精度但会降低可同步的最大速度；服务端与客户端必须一致。")
            .defineInRange("custom_precision", 8000.0D, 100.0D, 32767.0D);
        SPEC = builder.build();
    }

    private NetConfig() {}

    /** 配置加载/重载时刷新运行时缓存（由 mod 总线事件调用）。 */
    public static void onConfigChanged(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            customPrecision = CUSTOM_PRECISION.get();
        }
    }
}
