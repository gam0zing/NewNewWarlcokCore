package com.gam0zing.newnew_warlock_core.origin.content.power;

/**
 * 第一批起源的全部可调数值。
 * <p>文档只给出机制、未给出具体数值的条目，默认值集中在这里，方便后续统一调整。</p>
 */
public final class OriginTuning {

    // ============================== 史莱姆 ==============================

    /** 体型/攻击倍率的基准值（0 生命时为 0，再被下限截断） */
    public static final double SLIME_SIZE_BASE = 0.0D;
    /** 每点最大生命提供的倍率：每 4 点生命 +0.2 尺寸，20 点生命为 1.0 */
    public static final double SLIME_SIZE_PER_HEALTH = 0.05D;
    /** 体型/攻击倍率的下限 */
    public static final double SLIME_SIZE_MIN = 0.125D;
    public static final double SLIME_SIZE_MAX = 3.0D;
    /** 体型换算：最大生命值 / 该值 = 体型（20 最大生命值 = 1 体型），进食与消化速度都以它为准 */
    public static final double SLIME_HEALTH_PER_SCALE = 20.0D;

    /** 分裂状态持续时间（5 分钟） */
    public static final int SLIME_SPLIT_DURATION_TICKS = 20 * 60 * 5;
    /** 分裂等级上限（II 级），达到后再次致命伤害会真正死亡 */
    public static final int SLIME_SPLIT_MAX_STACKS = 2;
    /** 每级分裂的最大生命倍率 */
    public static final double SLIME_SPLIT_HEALTH_FACTOR = 0.5D;

    /** 粘液球：基础使用时长（tick），实际时长 = 该值 / 体型 */
    public static final int SLIME_BALL_USE_TICKS = 60;
    /** 粘液球治疗量 */
    public static final float SLIME_BALL_HEAL = 4.0F;
    /** 满血使用粘液球时缩短的分裂剩余时间（30 秒） */
    public static final int SLIME_BALL_SPLIT_REDUCTION = 20 * 30;

    /** 粘液块使用时长（20 秒） */
    public static final int SLIME_BLOCK_USE_TICKS = 20 * 20;
    /** 巨大化持续时间（5 分钟） */
    public static final int SLIME_GIANT_DURATION_TICKS = 20 * 60 * 5;
    /** 巨大化最大生命加成（+100%） */
    public static final double SLIME_GIANT_HEALTH_BONUS = 1.0D;
    /** 巨大化生命上限提升到账后提供的治疗量 */
    public static final float SLIME_GIANT_HEAL = 30.0F;

    /** 原版一份食物的进食时长（tick），仅用于第一人称「抬到嘴边」动画的节奏基准 */
    public static final int VANILLA_EAT_DURATION_TICKS = 32;
    /** 咀嚼动画与咀嚼音效的开始时刻（已用 tick） */
    public static final int SLIME_CHEW_START_TICKS = 7;

    /** 消化速度的基底加成：所有体型都在此基础上叠加 */
    public static final double SLIME_DIGEST_BASE_RATE = 0.25D;
    /** 消化速度随体型的系数：消化速度 = 基底 + 该值 × 体型（1 体型 75%、2 体型 125%，线性） */
    public static final double SLIME_DIGEST_RATE_PER_SCALE = 0.5D;

    /** 药水时长的统一基底倍率 */
    public static final double SLIME_POTION_DURATION_BASE = 0.5D;
    /** 药水时长的生命值基准：倍率 = 基底 × 该值 / 最大生命值（40 生命 → 0.5 倍，20 生命 → 1 倍，4 生命 → 5 倍） */
    public static final double SLIME_POTION_DURATION_HEALTH_REFERENCE = 40.0D;

    /** 主动分裂冷却（10 秒） */
    public static final int SLIME_ACTIVE_SPLIT_COOLDOWN = 200;

    /** 环境效果（冰冻/失水）默认的每轮持续时长（2 分钟） */
    public static final int SLIME_CLIMATE_DURATION_TICKS = 20 * 60 * 2;
    /** 处于分裂 II 时环境效果的每轮时长（30 秒） */
    public static final int SLIME_CLIMATE_DURATION_SPLIT_TWO_TICKS = 20 * 30;
    /** 处于分裂 I 时环境效果的每轮时长（60 秒） */
    public static final int SLIME_CLIMATE_DURATION_SPLIT_ONE_TICKS = 20 * 60;
    /** 处于巨大化时环境效果的每轮时长（4 分钟） */
    public static final int SLIME_CLIMATE_DURATION_GIANT_TICKS = 20 * 60 * 4;
    /** 环境效果的周期伤害间隔（5 秒） */
    public static final int SLIME_CLIMATE_DAMAGE_INTERVAL = 100;
    /** 环境效果每级的周期伤害 */
    public static final float SLIME_CLIMATE_DAMAGE_PER_LEVEL = 2.0F;
    /** 冰冻：削减/受伤加成 = 基准 + 每级 × 等级 */
    public static final float SLIME_FROZEN_REDUCTION_BASE = -0.20F;
    public static final float SLIME_FROZEN_REDUCTION_PER_LEVEL = 0.30F;
    /** 失水：每级削减的总移动速度与跳跃初速 */
    public static final float SLIME_DEHYDRATED_REDUCTION_PER_LEVEL = 0.10F;

    // ============================== 猫 ==============================

    /** 跑速加成（+50%） */
    public static final double CAT_SPEED_BONUS = 0.5D;
    /** 奔跑时额外饥饿消耗比例 */
    public static final double CAT_SPRINT_EXHAUSTION_EXTRA = 0.5D;
    /** 猫受到火焰伤害的倍率 */
    public static final float CAT_FIRE_DAMAGE_MULTIPLIER = 1.5F;
    /** 威慑效果持续时间：3 秒 */
    public static final int CAT_INTIMIDATE_DURATION = 60;
    /** 威慑逃跑范围：距威慑来源超过该距离即停止逃跑 */
    public static final double CAT_INTIMIDATE_FLEE_RANGE = 8.0D;
    /** 天敌：苦力怕感知并远离猫的范围 */
    public static final double CAT_CREEPER_FEAR_RANGE = 8.0D;
    /** 安全落距加成 */
    public static final double CAT_SAFE_FALL_BONUS = 5.0D;
    /** 空手攻击加成 */
    public static final double CAT_BARE_HAND_DAMAGE = 2.0D;
    /** 空手大出血概率 */
    public static final float CAT_BLEEDING_CHANCE = 0.08F;
    /** 贴墙下落减速（有饥饿时） */
    public static final double CAT_WALL_SLIDE_FACTOR = 0.0D;
    /** 贴墙下落减速（饥饿不足时） */
    public static final double CAT_WALL_SLIDE_WEAK_FACTOR = 0.4D;
    /** 贴墙下落减速（冰面且饥饿充足时，仅 40% 减免） */
    public static final double CAT_WALL_SLIDE_ICE_FACTOR = 0.6D;
    /** 贴墙每秒消耗的饥饿值 */
    public static final float CAT_WALL_EXHAUSTION_PER_SECOND = 0.5F;
    /** 蹬墙跳初速 */
    public static final double CAT_WALL_JUMP_VELOCITY = 0.42D;
    /** 蹬墙跳饥饿消耗 */
    public static final float CAT_WALL_JUMP_EXHAUSTION = 2.0F;
    /** 主动恐吓冷却（10 秒） */
    public static final int CAT_INTIMIDATE_COOLDOWN = 200;
    /** 主动恐吓半径 */
    public static final double CAT_INTIMIDATE_RADIUS = 12.0D;

    // ============================== 石巨人 ==============================

    /** 饥饿锁定值 */
    public static final int GOLEM_LOCKED_FOOD = 6;

    // ============================== 天堂鸟 ==============================

    /** 【云层之上】无法入睡的最高高度 */
    public static final double BIRD_MIN_SLEEP_Y = 196.0D;

    /** 振翅冲刺速度 */
    public static final double BIRD_DASH_POWER = 2.0D;

    /** 振翅冷却（游戏刻） */
    public static final int BIRD_DASH_COOLDOWN = 10;

    private OriginTuning() {}
}
