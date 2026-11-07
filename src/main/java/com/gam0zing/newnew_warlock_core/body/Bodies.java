package com.gam0zing.newnew_warlock_core.body;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.resources.Identifier;

/**
 * 内置 Body 常量。
 * <p>每个常量都是一套完整的受击部位定义，伤害倍率全部为 1、不带受击效果；坐标使用与
 * {@link com.gam0zing.newnew_warlock_core.combat.hit.HitLocation} 相同的面内坐标系：
 * 从体外正对该面看，左下角为 (0,0)、右上角为 (1,1)，x 恒为 0-1，y 为竖直方向占比（0 为脚、1 为头）。</p>
 *
 * <p>内置 Body 用带命名空间的 id 引用（例如 {@code newnew_warlock_core:human_like}），
 * 数据文件可通过 {@code template} 字段引用（见 {@link BodyPartFile#templateBody}）；
 * 与数据文件一起合并时等价于 {@code priority = 0} 的数据来源，见 {@link #PRIORITY}。</p>
 */
public final class Bodies {

    /** 内置 Body 在合并顺序中的位置：等价于 {@code priority = 0} */
    public static final int PRIORITY = 0;

    // ----------------------------------------------------------------------------------
    // 常量声明顺序很重要：静态初始化自上而下执行，id 必须先于对应的 Body，Body 必须先于 BY_MOB
    // ----------------------------------------------------------------------------------

    /** {@link #HUMAN_LIKE} 的注册名 */
    public static final Identifier HUMAN_LIKE_ID = id("human_like");

    /**
     * 类人身体（玩家/僵尸/村民等两足直立生物）。
     * <p>几何见 {@link BodyLayouts#humanLike()}。</p>
     */
    public static final Body HUMAN_LIKE = BodyLayouts.humanLike();

    /** {@link #CREEPER} 的注册名 */
    public static final Identifier CREEPER_ID = id("creeper");

    /**
     * 苦力怕型身体（四足直立、无手臂、大头）。
     * <p>几何见 {@link BodyLayouts#creeper()}。</p>
     */
    public static final Body CREEPER = BodyLayouts.creeper();

    /** {@link #SKELETON} 的注册名 */
    public static final Identifier SKELETON_ID = id("skeleton");

    /**
     * 骷髅型身体：几何与 {@link #HUMAN_LIKE} 相同，但受伤倍率与受击效果会单独调整。
     * <p>几何见 {@link BodyLayouts#skeleton()}。</p>
     */
    public static final Body SKELETON = BodyLayouts.skeleton();

    /** {@link #ZOMBIE} 的注册名 */
    public static final Identifier ZOMBIE_ID = id("zombie");

    /**
     * 僵尸型身体：几何与 {@link #HUMAN_LIKE} 相同，但受伤倍率与受击效果会单独调整。
     * <p>几何见 {@link BodyLayouts#zombie()}。</p>
     */
    public static final Body ZOMBIE = BodyLayouts.zombie();

    /** {@link #BLAZE} 的注册名 */
    public static final Identifier BLAZE_ID = id("blaze");

    /**
     * 烈焰人型身体（头 + 上下两段身体，共 4 个部位）。
     * <p>几何见 {@link BodyLayouts#blaze()}。</p>
     */
    public static final Body BLAZE = BodyLayouts.blaze();

    /** 所有内置 Body */
    public static final List<Body> ALL = List.of(HUMAN_LIKE, CREEPER, SKELETON, ZOMBIE, BLAZE);

    private static final Map<Identifier, Body> BY_ID = Map.of(
        HUMAN_LIKE_ID, HUMAN_LIKE,
        CREEPER_ID, CREEPER,
        SKELETON_ID, SKELETON,
        ZOMBIE_ID, ZOMBIE,
        BLAZE_ID, BLAZE);

    /** 默认套用 {@link #HUMAN_LIKE} 的生物（非骷髅、非僵尸的两足直立生物） */
    private static final List<String> HUMAN_LIKE_MOBS = List.of(
        "player",
        "villager", "wandering_trader",
        "pillager", "vindicator", "evoker", "illusioner", "witch",
        "piglin", "piglin_brute");

    /** 默认套用 {@link #SKELETON} 的生物 */
    private static final List<String> SKELETON_MOBS = List.of(
        "skeleton", "stray", "bogged", "wither_skeleton");

    /** 默认套用 {@link #ZOMBIE} 的生物 */
    private static final List<String> ZOMBIE_MOBS = List.of(
        "zombie", "husk", "drowned", "zombie_villager", "zombified_piglin");

    /**
     * 内置的 {@code mob_id → Body} 映射。
     * <p>在数据合并里等价于 {@code priority = 0} 的数据来源（排在同样 priority 0 的数据文件之前），
     * 数据文件仍可在此基础上追加或用同名部位整体替换。</p>
     */
    public static final Map<Identifier, Body> BY_MOB = buildMobMap();

    private Bodies() {}

    /**
     * 按注册名查找内置 Body。
     *
     * @param id 带命名空间的 id，例如 {@code newnew_warlock_core:human_like}；可为 null
     * @return 对应的内置 Body；id 为空或不存在时返回 {@link Optional#empty()}
     */
    public static Optional<Body> byId(@Nullable Identifier id) {
        return id == null ? Optional.empty() : Optional.ofNullable(BY_ID.get(id));
    }

    private static Map<Identifier, Body> buildMobMap() {
        Map<Identifier, Body> map = new HashMap<>();
        putAll(map, HUMAN_LIKE_MOBS, HUMAN_LIKE);
        putAll(map, SKELETON_MOBS, SKELETON);
        putAll(map, ZOMBIE_MOBS, ZOMBIE);
        map.put(Identifier.withDefaultNamespace("creeper"), CREEPER);
        map.put(Identifier.withDefaultNamespace("blaze"), BLAZE);
        return Map.copyOf(map);
    }

    private static void putAll(Map<Identifier, Body> map, List<String> mobs, Body body) {
        for (String path : mobs) {
            map.put(Identifier.withDefaultNamespace(path), body);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, path);
    }
}
