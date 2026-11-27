package com.gam0zing.newnew_warlock_core.origin.content.power;

import com.cyberday1.neoorigins.api.power.PowerConfiguration;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

/**
 * 无参数代码能力的配置占位。
 * <p>本批种族能力的数值集中在 {@link OriginTuning}，因此能力本身不需要数据包配置；
 * 仍然实现 {@link PowerConfiguration} 以满足 NeoOrigins 的能力注册接口。</p>
 */
public enum NoConfig implements PowerConfiguration {
    INSTANCE;

    public static final Codec<NoConfig> CODEC = MapCodec.unitCodec(INSTANCE);
}
