package com.gam0zing.newnew_warlock_core.body;

import java.util.ArrayList;
import java.util.List;

import com.gam0zing.newnew_warlock_core.body.hit.HitFace;

import net.minecraft.world.entity.EquipmentSlot;

/**
 * 内置身体的几何定义。
 * <p>每个身体都是<b>独立完整地</b>写出来的，不使用“类型开关”之类的共享模板：
 * 后面出现千奇百怪的身体时直接新增一个方法即可，不会受已有形态的假设影响。</p>
 * <p>唯一的共用部分是坐标助手 {@link #band} / {@link #full}，它们只是“面上一条横带 / 整个面”的写法糖，
 * 不包含任何形态语义，也不替调用方决定任何数值：受伤倍率、刚性、护甲槽、受击效果都在每个调用点显式传入，
 * 这样每个部位都能单独调整，不会牵动其它部位。</p>
 * <p>坐标约定与 {@link com.gam0zing.newnew_warlock_core.body.hit.HitLocation} 相同：
 * 从体外正对该面看，左下角 (0,0)、右上角 (1,1)。</p>
 * <p>此类的静态初始化只依赖 {@link BodyPart}，不触碰注册表，可离线验证。</p>
 */
public final class BodyLayouts {

    private BodyLayouts() {}

    /** 脚部在侧面上的高度：底面全部 + 四个侧面最下方 7.5% 的区域 */
    private static final float FEET_LINE = 0.075F;

    /**
     * 类人身体：两足直立、有双臂、头部占比 25%。
     * <p>侧面自下而上：feet 7.5% → legs 30% → waist / belly / back_down 20% →
     * left_arm / right_arm / chest / back_up 17.5% → head 25%。</p>
     */
    public static Body humanLike() {
        float headLine = 1.0F - 0.25F;
        float armLine = headLine - 0.175F;
        float waistLine = armLine - 0.20F;

        List<BodyPart> parts = new ArrayList<>();

        // head：顶面全部 + 左/右/前三个侧面上方
        full(parts, "head", HitFace.TOP,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.LEFT, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.RIGHT, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.FRONT, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());

        // back_head：后面上方
        band(parts, "back_head", HitFace.BACK, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());

        // 手臂：左右面的上层
        band(parts, "left_arm", HitFace.LEFT, armLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "right_arm", HitFace.RIGHT, armLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());

        // 胸 / 腹：前面的上层与中层
        band(parts, "chest", HitFace.FRONT, armLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "belly", HitFace.FRONT, waistLine, armLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());

        // 背：后面的上层与中层
        band(parts, "back_up", HitFace.BACK, armLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "back_down", HitFace.BACK, waistLine, armLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());

        // 腰：左右面的中层
        band(parts, "waist", HitFace.LEFT, waistLine, armLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "waist", HitFace.RIGHT, waistLine, armLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());

        // feet：底面全部 + 四个侧面最下方 7.5%
        full(parts, "feet", HitFace.BOTTOM,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());
        band(parts, "feet", HitFace.LEFT, 0.0F, FEET_LINE,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());
        band(parts, "feet", HitFace.RIGHT, 0.0F, FEET_LINE,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());
        band(parts, "feet", HitFace.FRONT, 0.0F, FEET_LINE,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());
        band(parts, "feet", HitFace.BACK, 0.0F, FEET_LINE,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());

        // legs：四个侧面 7.5% 以上至 waistLine（底面已归 feet）
        band(parts, "legs", HitFace.LEFT, FEET_LINE, waistLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "legs", HitFace.RIGHT, FEET_LINE, waistLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "legs", HitFace.FRONT, FEET_LINE, waistLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "legs", HitFace.BACK, FEET_LINE, waistLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());

        return new Body("human_like", parts, Body.DEFAULT_WEIGHT);
    }

    /**
     * 骷髅型身体：几何与 {@link #humanLike()} 完全相同（两足直立、有双臂、头部占比 25%）。
     * <p>单独成型是因为骷髅的受伤倍率与受击效果和类人身体差别很大，未来只会改这里，
     * 不会牵连 {@link #humanLike()}；当前全部为默认值（倍率 1、无效果）。</p>
     */
    public static Body skeleton() {
        float headLine = 1.0F - 0.25F;
        float armLine = headLine - 0.175F;
        float waistLine = armLine - 0.20F;

        List<BodyPart> parts = new ArrayList<>();

        // head：顶面全部 + 左/右/前三个侧面上方
        full(parts, "head", HitFace.TOP,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.LEFT, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.RIGHT, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.FRONT, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());

        // back_head：后面上方
        band(parts, "back_head", HitFace.BACK, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());

        // 手臂：左右面的上层
        band(parts, "left_arm", HitFace.LEFT, armLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "right_arm", HitFace.RIGHT, armLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());

        // 胸 / 腹：前面的上层与中层
        band(parts, "chest", HitFace.FRONT, armLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "belly", HitFace.FRONT, waistLine, armLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());

        // 背：后面的上层与中层
        band(parts, "back_up", HitFace.BACK, armLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "back_down", HitFace.BACK, waistLine, armLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());

        // 腰：左右面的中层
        band(parts, "waist", HitFace.LEFT, waistLine, armLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "waist", HitFace.RIGHT, waistLine, armLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());

        // feet：底面全部 + 四个侧面最下方 7.5%
        full(parts, "feet", HitFace.BOTTOM,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());
        band(parts, "feet", HitFace.LEFT, 0.0F, FEET_LINE,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());
        band(parts, "feet", HitFace.RIGHT, 0.0F, FEET_LINE,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());
        band(parts, "feet", HitFace.FRONT, 0.0F, FEET_LINE,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());
        band(parts, "feet", HitFace.BACK, 0.0F, FEET_LINE,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());

        // legs：四个侧面 7.5% 以上至 waistLine（底面已归 feet）
        band(parts, "legs", HitFace.LEFT, FEET_LINE, waistLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "legs", HitFace.RIGHT, FEET_LINE, waistLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "legs", HitFace.FRONT, FEET_LINE, waistLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "legs", HitFace.BACK, FEET_LINE, waistLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());

        return new Body("skeleton", parts, Body.DEFAULT_WEIGHT);
    }

    /**
     * 僵尸型身体：几何与 {@link #humanLike()} 完全相同（两足直立、有双臂、头部占比 25%）。
     * <p>单独成型的原因同 {@link #skeleton()}：僵尸类的受伤倍率与受击效果会单独调整，
     * 当前全部为默认值（倍率 1、无效果）。</p>
     */
    public static Body zombie() {
        float headLine = 1.0F - 0.25F;
        float armLine = headLine - 0.175F;
        float waistLine = armLine - 0.20F;

        List<BodyPart> parts = new ArrayList<>();

        // head：顶面全部 + 左/右/前三个侧面上方
        full(parts, "head", HitFace.TOP,
            3, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.LEFT, headLine, 1.0F,
            2, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.RIGHT, headLine, 1.0F,
            2, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.FRONT, headLine, 1.0F,
            2, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());

        // back_head：后面上方
        band(parts, "back_head", HitFace.BACK, headLine, 1.0F,
            3, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());

        // 手臂：左右面的上层
        band(parts, "left_arm", HitFace.LEFT, armLine, headLine,
            0.5F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "right_arm", HitFace.RIGHT, armLine, headLine,
            0.5F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());

        // 胸 / 腹：前面的上层与中层
        band(parts, "chest", HitFace.FRONT, armLine, headLine,
            1, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "belly", HitFace.FRONT, waistLine, armLine,
            0.8F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());

        // 背：后面的上层与中层
        band(parts, "back_up", HitFace.BACK, armLine, headLine,
            1.5F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "back_down", HitFace.BACK, waistLine, armLine,
            1.75F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());

        // 腰：左右面的中层
        band(parts, "waist", HitFace.LEFT, waistLine, armLine,
            0.7F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "waist", HitFace.RIGHT, waistLine, armLine,
            0.7F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());

        // feet：底面全部 + 四个侧面最下方 7.5%
        full(parts, "feet", HitFace.BOTTOM,
            0.2F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());
        band(parts, "feet", HitFace.LEFT, 0.0F, FEET_LINE,
            0.2F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());
        band(parts, "feet", HitFace.RIGHT, 0.0F, FEET_LINE,
            0.2F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());
        band(parts, "feet", HitFace.FRONT, 0.0F, FEET_LINE,
            0.2F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());
        band(parts, "feet", HitFace.BACK, 0.0F, FEET_LINE,
            0.2F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.FEET, List.of());

        // legs：四个侧面 7.5% 以上至 waistLine（底面已归 feet）
        band(parts, "legs", HitFace.LEFT, FEET_LINE, waistLine,
            0.5F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "legs", HitFace.RIGHT, FEET_LINE, waistLine,
            0.5F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "legs", HitFace.FRONT, FEET_LINE, waistLine,
            0.5F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "legs", HitFace.BACK, FEET_LINE, waistLine,
            0.5F, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());

        return new Body("zombie", parts, Body.DEFAULT_WEIGHT);
    }

    /**
     * 烈焰人型身体：无手臂、无腿的“头 + 两段身体”，四个部位。
     * <p>侧面自下而上：body_down 35% → body_up 40% → head 25%（后面上方 25% 为 back_head）。</p>
     */
    public static Body blaze() {
        float headLine = 1.0F - 0.25F;
        float bodyLine = headLine - 0.40F;

        List<BodyPart> parts = new ArrayList<>();

        // head：顶面全部 + 左/右/前三个侧面上方 25%
        full(parts, "head", HitFace.TOP,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.LEFT, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.RIGHT, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.FRONT, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());

        // back_head：后面上方 25%
        band(parts, "back_head", HitFace.BACK, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());

        // body_up：四个侧面的上层 40%
        band(parts, "body_up", HitFace.LEFT, bodyLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "body_up", HitFace.RIGHT, bodyLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "body_up", HitFace.FRONT, bodyLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "body_up", HitFace.BACK, bodyLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());

        // body_down：底面全部 + 四个侧面的下层 35%
        full(parts, "body_down", HitFace.BOTTOM,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "body_down", HitFace.LEFT, 0.0F, bodyLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "body_down", HitFace.RIGHT, 0.0F, bodyLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "body_down", HitFace.FRONT, 0.0F, bodyLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "body_down", HitFace.BACK, 0.0F, bodyLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());

        return new Body("blaze", parts, Body.DEFAULT_WEIGHT);
    }

    /**
     * 苦力怕型身体：四足直立、无手臂、头部占比 30%。
     * <p>侧面自下而上：legs 25% → waist / belly / back_down 25% →
     * chest（左右面也用 chest，原手臂位置同样归入胸）/ back_up 20% → head 30%。</p>
     */
    public static Body creeper() {
        float headLine = 1.0F - 0.30F;
        float chestLine = headLine - 0.20F;
        float waistLine = chestLine - 0.25F;

        List<BodyPart> parts = new ArrayList<>();

        // head：顶面全部 + 左/右/前三个侧面上方
        full(parts, "head", HitFace.TOP,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.LEFT, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.RIGHT, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());
        band(parts, "head", HitFace.FRONT, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());

        // back_head：后面上方
        band(parts, "back_head", HitFace.BACK, headLine, 1.0F,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.HEAD, List.of());

        // 胸：没有手臂，前/左/右三个面的上层都归入胸
        band(parts, "chest", HitFace.FRONT, chestLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "chest", HitFace.LEFT, chestLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "chest", HitFace.RIGHT, chestLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());

        // 腹 / 下背：中层
        band(parts, "belly", HitFace.FRONT, waistLine, chestLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "back_down", HitFace.BACK, waistLine, chestLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());

        // 上背 / 腰：后面的上层与左右面的中层
        band(parts, "back_up", HitFace.BACK, chestLine, headLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.CHEST, List.of());
        band(parts, "waist", HitFace.LEFT, waistLine, chestLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "waist", HitFace.RIGHT, waistLine, chestLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());

        // legs：底面全部 + 四个侧面最下方
        full(parts, "legs", HitFace.BOTTOM,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "legs", HitFace.LEFT, 0.0F, waistLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "legs", HitFace.RIGHT, 0.0F, waistLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "legs", HitFace.FRONT, 0.0F, waistLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());
        band(parts, "legs", HitFace.BACK, 0.0F, waistLine,
            BodyPart.DEFAULT_DAMAGE_SCALE, BodyPart.DEFAULT_RIGIDITY, EquipmentSlot.LEGS, List.of());

        return new Body("creeper", parts, Body.DEFAULT_WEIGHT);
    }

    /**
     * 面上的一条横带：x 恒为 0-1，y 为给定区间。
     * <p>所有部位数值（受伤倍率、刚性、护甲槽、受击效果）都由调用方显式传入，本方法不再提供任何缺省。</p>
     */
    private static void band(
        List<BodyPart> parts, String name, HitFace face, float y0, float y1,
        float damageScale, float rigidity, EquipmentSlot armorSlot, List<BodyEffect> effects) {
        parts.add(new BodyPart(
            name, face, 0.0F, y0, 1.0F, y1, damageScale, rigidity, armorSlot, effects));
    }

    /** 整个面：x 与 y 都是 0-1；其余数值同样由调用方显式传入 */
    private static void full(
        List<BodyPart> parts, String name, HitFace face,
        float damageScale, float rigidity, EquipmentSlot armorSlot, List<BodyEffect> effects) {
        band(parts, name, face, 0.0F, 1.0F, damageScale, rigidity, armorSlot, effects);
    }
}
