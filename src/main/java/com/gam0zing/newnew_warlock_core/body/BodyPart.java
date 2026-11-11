package com.gam0zing.newnew_warlock_core.body;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.body.hit.HitFace;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;

/**
 * 生物的一个自定义受击部位（合并后的最终数据）。
 * <p>部位在某个受击面上占据一块矩形区域，坐标与 {@link com.gam0zing.newnew_warlock_core.body.hit.HitLocation}
 * 一致：以“从体外正对该面看”的左下角为 (0, 0)、右上角为 (1, 1)。</p>
 *
 * @param name        可读名称（数据文件里的 {@code part_name}），便于查找与调试
 * @param surface     受击面；{@link #DEFAULT} 为 null（没有面，永远不命中）
 * @param u0          区域横坐标下限，已归一化到 [0, 1]
 * @param v0          区域纵坐标下限，已归一化到 [0, 1]
 * @param u1          区域横坐标上限，已归一化到 [0, 1]
 * @param v1          区域纵坐标上限，已归一化到 [0, 1]
 * @param damageScale 该部位的受伤倍率（缺省 1，负值按 0 处理，非有限值按 1 处理）
 * @param rigidity    该部位的刚性：物理伤害据此选择武器的高/中/低刚性倍率，进而决定肉伤
 * @param armorSlot   该部位由哪个装备槽的护甲保护；null 表示该部位不受护甲保护
 * @param effects     命中该部位时可能施加的受击效果
 */
public record BodyPart(
    String name,
    @Nullable HitFace surface,
    float u0,
    float v0,
    float u1,
    float v1,
    float damageScale,
    float rigidity,
    @Nullable EquipmentSlot armorSlot,
    List<BodyEffect> effects) {

    /** 身体部位刚性的缺省值（中刚性档） */
    public static final float DEFAULT_RIGIDITY = 0.5F;

    /** 身体部位受伤倍率的缺省值 */
    public static final float DEFAULT_DAMAGE_SCALE = 1.0F;

    /** 全局默认部位：未命中任何部位时返回，无受击效果、伤害倍率 1、刚性 0.5、无护甲、无面（永远不命中） */
    public static final BodyPart DEFAULT = new BodyPart(
        "default", null, 0.0F, 0.0F, 1.0F, 1.0F, DEFAULT_DAMAGE_SCALE, DEFAULT_RIGIDITY, null, List.of());

    public BodyPart {
        name = name == null ? "" : name;
        // 区域自动归一化：两个角点不要求是有序的
        float minU = Math.min(u0, u1);
        float maxU = Math.max(u0, u1);
        float minV = Math.min(v0, v1);
        float maxV = Math.max(v0, v1);
        u0 = Mth.clamp(minU, 0.0F, 1.0F);
        u1 = Mth.clamp(maxU, 0.0F, 1.0F);
        v0 = Mth.clamp(minV, 0.0F, 1.0F);
        v1 = Mth.clamp(maxV, 0.0F, 1.0F);
        damageScale = Float.isFinite(damageScale) ? Math.max(0.0F, damageScale) : DEFAULT_DAMAGE_SCALE;
        rigidity = Float.isFinite(rigidity) ? Math.max(0.0F, rigidity) : DEFAULT_RIGIDITY;
        effects = List.copyOf(effects);
    }

    /** {@return 该部位是否是全局默认部位} */
    public boolean isDefault() {
        return this == DEFAULT;
    }

    /**
     * 判断一次受击是否落在该部位上（面相同且坐标落在区域内，边界含端点）。
     *
     * @param face 受击面
     * @param x    面内横坐标
     * @param y    面内纵坐标
     */
    public boolean matches(HitFace face, float x, float y) {
        return this.surface == face && x >= this.u0 && x <= this.u1 && y >= this.v0 && y <= this.v1;
    }
}
