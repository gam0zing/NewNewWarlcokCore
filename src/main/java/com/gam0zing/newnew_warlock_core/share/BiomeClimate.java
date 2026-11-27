package com.gam0zing.newnew_warlock_core.share;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;

/**
 * 史莱姆的体感环境，由温度与湿度共同决定。
 * <p>低温：基础温度低于 0.15；高温干燥：基础温度高于 1.0 且群系没有降水。
 * 其余情况（包括炎热但潮湿的丛林）都算合适温度。</p>
 */
public enum BiomeClimate {

    COLD,
    NORMAL,
    HOT_DRY;

    /** 低温阈值 */
    public static final float COLD_THRESHOLD = 0.15F;
    /** 高温阈值 */
    public static final float HOT_THRESHOLD = 1.0F;

    /** {@return 生物当前所在位置的环境体感} */
    public static BiomeClimate at(LivingEntity entity) {
        return at(entity.level().getBiome(entity.blockPosition()).value());
    }

    /** {@return 群系对应的环境体感} */
    public static BiomeClimate at(Biome biome) {
        float temperature = biome.getBaseTemperature();
        if (temperature < COLD_THRESHOLD) {
            return COLD;
        }
        if (temperature > HOT_THRESHOLD && !biome.hasPrecipitation()) {
            return HOT_DRY;
        }
        return NORMAL;
    }
}
