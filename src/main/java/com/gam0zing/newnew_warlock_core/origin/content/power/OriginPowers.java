package com.gam0zing.newnew_warlock_core.origin.content.power;

import com.cyberday1.neoorigins.api.power.CodePowerOptions;
import com.cyberday1.neoorigins.api.power.PowerConfiguration;
import com.cyberday1.neoorigins.api.power.PowerDisplay;
import com.cyberday1.neoorigins.api.power.PowerType;
import com.cyberday1.neoorigins.data.PowerDataManager;
import com.cyberday1.neoorigins.power.registry.PowerTypes;
import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 第一批起源的自定义能力注册入口。
 * <p>单效果能力由代码实现；合并型能力使用 {@code newnew_warlock_core:composite}
 * 数据包能力承载多个子能力。数据包与代码使用同一 id 时，数据包版本会覆盖代码版本。</p>
 */
public final class OriginPowers {

    /** 自定义能力类型注册表；由兼容模块在 NeoOrigins 存在时注册到 mod 总线。 */
    public static final DeferredRegister<PowerType<?>> POWER_TYPES =
        DeferredRegister.create(PowerTypes.REGISTRY_KEY, NewNewWarlockCore.MOD_ID);

    static {
        POWER_TYPES.register("composite", CompositePower::new);
    }

    /** 【史莱姆生命】：体型、攻击力、药水时长、进食与消化速度统一由最大生命值驱动 */
    public static final Identifier SLIME_KIN_IGNORE = id("slime_kin_ignore");
    public static final Identifier SLIME_LIFE = id("slime_life");
    public static final Identifier SLIME_WATER_BREATHING = id("slime_water_breathing");
    public static final Identifier SLIME_SPLIT = id("slime_split");
    public static final Identifier SLIME_ENVIRONMENT_SENSITIVITY = id("slime_environment_sensitivity");
    public static final Identifier SLIME_DEVOUR = id("slime_devour");
    public static final Identifier SLIME_ACTIVE_SPLIT = id("slime_active_split");

    public static final Identifier CAT_SPRINT = id("cat_sprint");
    public static final Identifier CAT_AQUAPHOBIC = id("cat_aquaphobic");
    /** 【毛茸茸】：猫与天堂鸟共用 */
    public static final Identifier FLUFFY = id("fluffy");
    public static final Identifier CAT_NATURAL_ENEMY = id("cat_natural_enemy");
    public static final Identifier CAT_CLIMB = id("cat_climb");
    public static final Identifier CAT_SCRATCH = id("cat_scratch");
    public static final Identifier CAT_INTIMIDATE = id("cat_intimidate");
    public static final Identifier CAT_CARNIVORE = id("cat_carnivore");

    public static final Identifier GOLEM_MOVEMENT = id("stone_golem_movement");
    public static final Identifier GOLEM_SUNLIGHT = id("stone_golem_sunlight");
    public static final Identifier GOLEM_FOOD_RULES = id("stone_golem_food_rules");
    public static final Identifier GOLEM_POTION_IMMUNITY = id("stone_golem_potion_immunity");

    public static final Identifier BIRD_SLEEP_ALTITUDE = id("paradise_bird_sleep_altitude");
    public static final Identifier BIRD_FOOD_RULES = id("paradise_bird_food_rules");
    public static final Identifier BIRD_NO_SPRINT = id("bird_no_sprint");
    public static final Identifier BIRD_DASH = id("bird_dash");

    public static void register() {
        register(SLIME_KIN_IGNORE, new PassivePower(), NoConfig.INSTANCE);
        register(SLIME_LIFE, new SlimeLifePower(), NoConfig.INSTANCE);
        register(SLIME_WATER_BREATHING, new WaterBreathingPower(), NoConfig.INSTANCE);
        register(SLIME_SPLIT, new SlimeSplitPower(), NoConfig.INSTANCE);
        register(SLIME_ENVIRONMENT_SENSITIVITY, new BiomeHazardPower(), NoConfig.INSTANCE);
        register(SLIME_DEVOUR, new PassivePower(), NoConfig.INSTANCE);
        register(SLIME_ACTIVE_SPLIT, new SlimeActiveSplitPower(), SlimeActiveSplitPower.Config.INSTANCE);

        register(CAT_SPRINT, new CatSprintPower(), NoConfig.INSTANCE);
        register(CAT_AQUAPHOBIC, new MovementPower(MovementPower.Flag.CAT_OXYGEN), NoConfig.INSTANCE);
        register(FLUFFY, new PassivePower(), NoConfig.INSTANCE);
        register(CAT_NATURAL_ENEMY, new CreeperFearPower(), NoConfig.INSTANCE);
        register(CAT_CLIMB, new MovementPower(MovementPower.Flag.CAT_WALL), NoConfig.INSTANCE);
        register(CAT_SCRATCH, new BareHandPower(), NoConfig.INSTANCE);
        register(CAT_INTIMIDATE, new IntimidatePower(), IntimidatePower.Config.INSTANCE);
        register(CAT_CARNIVORE, new PassivePower(), NoConfig.INSTANCE);

        register(GOLEM_MOVEMENT, new PassivePower(), NoConfig.INSTANCE);
        register(GOLEM_SUNLIGHT, new SunlightPower(), NoConfig.INSTANCE);
        register(GOLEM_FOOD_RULES, new PassivePower(), NoConfig.INSTANCE);
        register(GOLEM_POTION_IMMUNITY, new PassivePower(), NoConfig.INSTANCE);

        register(BIRD_SLEEP_ALTITUDE, new SleepAltitudePower(), NoConfig.INSTANCE);
        register(BIRD_FOOD_RULES, new PassivePower(), NoConfig.INSTANCE);
        register(BIRD_NO_SPRINT, new PassivePower(), NoConfig.INSTANCE);
        register(BIRD_DASH, new BirdDashPower(), BirdDashPower.Config.INSTANCE);
    }

    private static <C extends PowerConfiguration> void register(Identifier id, PowerType<C> type, C config) {
        PowerDataManager.INSTANCE.registerCodePower(
            id,
            type,
            config,
            PowerDisplay.of(
                Component.translatable("power." + id.getNamespace() + "." + id.getPath() + ".name"),
                Component.translatable("power." + id.getNamespace() + "." + id.getPath() + ".description")),
            CodePowerOptions.NONE);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, path);
    }

    private OriginPowers() {}
}
