package com.gam0zing.newnew_warlock_core.combat.physics;

import java.util.Map;
import java.util.function.DoubleBinaryOperator;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * 防护力曲线。
 * <p>护甲档案里的 {@code defence_func} 是一个注册名，映射到“（耐久比例, 盔甲值）→ 防护力”的曲线。
 * 防护力直接由曲线给出，不再额外乘一次厚度，因此曲线输出不必 ≤ 1
 * （例如当前通用曲线是 {@code 盔甲值 × 耐久比例^0.5}）。
 * 这里的“盔甲值”指物品的<b>原版护甲值</b>（{@code ArmorItem#getDefense()}）。</p>
 * <p>内置曲线（命名空间 {@code newnew_warlock_core}）：</p>
 * <ul>
 *     <li>{@code general}：通用曲线 {@code 盔甲值 × √耐久比例}（默认）</li>
 * </ul>
 * <p>未知 id 回退到 {@code general}。</p>
 */
public final class PhysicsDefenceFunctions {

    /** 通用防护曲线：{@code 盔甲值 × 耐久比例^0.5} */
    public static final Identifier GENERAL = id("general");

    private static final Map<Identifier, DoubleBinaryOperator> FUNCTIONS = Map.of(
        GENERAL, (ratio, armorValue) -> armorValue * Math.sqrt(ratio));

    private PhysicsDefenceFunctions() {}

    /**
     * 按曲线 id 求当前防护力。
     *
     * @param id              曲线注册名，可为 null 或未注册名（回退 {@link #GENERAL}）
     * @param durabilityRatio 耐久比例，自动截断到 [0, 1]
     * @param armorValue      盔甲值（物品的原版护甲值，非负）
     * @return 防护力，非负（不一定 ≤ 1）
     */
    public static double apply(Identifier id, double durabilityRatio, double armorValue) {
        double ratio = Mth.clamp(durabilityRatio, 0.0D, 1.0D);
        double armor = Math.max(0.0D, armorValue);
        DoubleBinaryOperator curve = id == null ? null : FUNCTIONS.get(id);
        if (curve == null) {
            curve = FUNCTIONS.get(GENERAL);
        }
        return Math.max(0.0D, curve.applyAsDouble(ratio, armor));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, path);
    }
}
