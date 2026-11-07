package com.gam0zing.newnew_warlock_core.body;

import java.util.List;

/**
 * 一个完整的身体定义：一组按顺序排列的受击部位。
 * <p>{@link Bodies} 中的内置 Body 常量可视为 {@code priority = 0} 的数据来源：
 * 与数据文件一起参与合并时，顺序在 {@code priority} 小于 0 的文件之后、大于 0 的文件之前。</p>
 *
 * @param name   可读名称，例如 {@code human_like}
 * @param parts  部位列表（不可变，顺序即“按顺序取第一个命中”的顺序）
 * @param weight 身体重量（默认 1，非有限值或非正数按默认值处理）
 */
public record Body(String name, List<BodyPart> parts, double weight) {

    /** 身体重量缺省值 */
    public static final double DEFAULT_WEIGHT = 1.0D;

    public Body {
        parts = List.copyOf(parts);
        weight = Double.isFinite(weight) && weight > 0.0D ? weight : DEFAULT_WEIGHT;
    }
}
