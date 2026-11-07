package com.gam0zing.newnew_warlock_core.combat.hit;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

/**
 * 受击面。
 * <p>语义是<b>生物自身的相对方向</b>（上/下/左/右/前/后），与面在世界中的实际朝向无关：
 * {@link #FRONT} 永远是生物面朝的一侧，{@link #RIGHT} 永远是生物自己的右手侧，生物转身后这些语义跟着转。</p>
 * <p>数据文件里用序列名书写（{@code top}、{@code bottom}、{@code left}、{@code right}、{@code front}、{@code back}），
 * 解析见 {@link #byName(String)}。</p>
 */
public enum HitFace {
    /** 0：上 */
    TOP(0),
    /** 1：下 */
    BOTTOM(1),
    /** 2：左（生物自己的左手侧） */
    LEFT(2),
    /** 3：右（生物自己的右手侧） */
    RIGHT(3),
    /** 4：前（生物面朝的一侧） */
    FRONT(4),
    /** 5：后 */
    BACK(5);

    private static final HitFace[] BY_INDEX = values();

    /** 六个面（上/下/左/右/前/后），按枚举顺序排列 */
    public static final List<HitFace> ALL = List.of(BY_INDEX);

    private static final Map<String, HitFace> BY_NAME = Arrays.stream(BY_INDEX)
        .collect(Collectors.toUnmodifiableMap(HitFace::serializedName, face -> face));

    private final int index;

    HitFace(int index) {
        this.index = index;
    }

    /** {@return 该面在网络/存档中使用的数值（0-5）} */
    public int index() {
        return this.index;
    }

    /** {@return 数据文件里使用的序列名，例如 {@code top}、{@code front}} */
    public String serializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    /** {@return 该面是否属于侧面（左/右/前/后）} */
    public boolean isSide() {
        return this != TOP && this != BOTTOM;
    }

    public static HitFace byIndex(int index) {
        if (index < 0 || index >= BY_INDEX.length) {
            throw new IllegalArgumentException("Unknown hit face index: " + index);
        }
        return BY_INDEX[index];
    }

    /**
     * 按数据文件里的序列名解析（大小写与首尾空格不敏感）。
     *
     * @param name 序列名，例如 {@code top}；可为 null
     * @return 对应的面；名字为空或未知时返回 {@link Optional#empty()}
     */
    public static Optional<HitFace> byName(@Nullable String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NAME.get(name.trim().toLowerCase(Locale.ROOT)));
    }
}
