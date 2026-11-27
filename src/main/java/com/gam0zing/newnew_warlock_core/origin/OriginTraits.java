package com.gam0zing.newnew_warlock_core.origin;

import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.combat.api.DamageTags;
import com.gam0zing.newnew_warlock_core.combat.spine.WarlockHealingHost;
import com.gam0zing.newnew_warlock_core.mixin.MobEffectInstanceStackAccess;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginPowers;
import com.gam0zing.newnew_warlock_core.origin.content.power.OriginTuning;
import com.gam0zing.newnew_warlock_core.origin.content.power.SlimeSplitPower;
import com.gam0zing.newnew_warlock_core.origin.content.power.SlimeState;
import com.gam0zing.newnew_warlock_core.share.EffectSources;
import com.gam0zing.newnew_warlock_core.share.ModMobEffects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.cubemob.Slime;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PotionItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 种族机制的全局钩子。
 * <p>NeoOrigins 的能力 API 没有覆盖死亡、进食、药水施加等入口，这些条目集中在这里处理。
 * 所有分支都以具体能力 id（{@link #hasPower}）门控，效果归属于能力本身；未安装起源模组时不会生效。</p>
 */
public final class OriginTraits {

    private static final TagKey<Item> RAW_MEAT = itemTag("raw_meat");
    private static final TagKey<Item> VEGETABLE = itemTag("vegetable");
    private static final TagKey<Item> MEAT = itemTag("meat");

    /**
     * 客户端本地预测使用的能力查询，由物理客户端兼容层安装。
     * <p>服务端逻辑一律走 {@link WarlockOrigins}；这里只用于让疾跑/进食等客户端预测
     * 与服务端规则保持一致，否则服务端未发生变化时不会下发纠正数据。</p>
     */
    @Nullable
    private static Predicate<Player> clientSelfCheck;

    @Nullable
    private static Predicate<Identifier> clientPowerCheck;

    /** 由物理客户端在 NeoOrigins 加载后安装；第一个参数判断是否为本地玩家。 */
    public static void installClientPowerCheck(Predicate<Player> selfCheck, Predicate<Identifier> powerCheck) {
        clientSelfCheck = selfCheck;
        clientPowerCheck = powerCheck;
    }

    private static boolean clientIsSelf(Player player) {
        Predicate<Player> check = clientSelfCheck;
        return check != null && check.test(player);
    }

    /**
     * 玩家当前是否拥有某项能力。
     * <p>服务端走 {@link WarlockOrigins#hasPower}；客户端只对本地玩家走同步下来的能力快照，
     * 保证疾跑、进食等本地预测与服务端规则一致。</p>
     */
    public static boolean hasPower(Player player, Identifier power) {
        if (player instanceof ServerPlayer serverPlayer) {
            return WarlockOrigins.hasPower(serverPlayer, power);
        }
        if (!clientIsSelf(player)) {
            return false;
        }
        Predicate<Identifier> check = clientPowerCheck;
        return check != null && check.test(power);
    }

    // ============================== 种族判断 ==============================

    /**
     * 起源状态效果是否禁止被统一清除（牛奶、{@code /effect clear} 等走
     * {@link LivingEntity#removeAllEffects()} 的入口）。
     * <p>分裂等级与巨大化时长就是这两个效果本身；被清除会直接丢失起源状态。</p>
     */
    public static boolean isProtectedEffect(MobEffectInstance instance) {
        MobEffect effect = instance.getEffect().value();
        return effect == ModMobEffects.SLIME_SPLIT.get()
            || effect == ModMobEffects.SLIME_GIANT.get()
            || effect == ModMobEffects.SLIME_FROZEN.get()
            || effect == ModMobEffects.SLIME_DEHYDRATED.get();
    }

    /** 是否处于分裂状态：分裂直接使用原版效果，双端判断一致。 */
    public static boolean hasSlimeSplit(Player player) {
        return player.hasEffect(ModMobEffects.SLIME_SPLIT);
    }

    // ============================== 药水 / 食物 ==============================

    /**
     * 史莱姆的药水亲和：效果时长按最大生命值变化（基底 0.5，最大生命 4 点 5 倍、20 点 1 倍、40 点 0.5 倍），
     * 起源机制自带的效果（分裂、巨大化、凝固、失水）不受影响；石巨人免疫一切效果。
     *
     * @return {@code false} 表示这次施加应被放弃
     */
    public static boolean adjustPotion(LivingEntity target, MobEffectInstance instance) {
        if (!(target instanceof ServerPlayer player)) {
            return true;
        }
        if (isProtectedEffect(instance)) {
            return true;
        }
        if (hasPower(player, OriginPowers.GOLEM_POTION_IMMUNITY)) {
            return false;
        }
        if (!hasPower(player, OriginPowers.SLIME_LIFE)) {
            return true;
        }
        int duration = instance.getDuration();
        if (duration > 0) {
            double multiplier = OriginTuning.SLIME_POTION_DURATION_BASE
                * OriginTuning.SLIME_POTION_DURATION_HEALTH_REFERENCE
                / Math.max(1.0D, player.getMaxHealth());
            int scaled = Math.max(1, (int) Math.floor(duration * multiplier));
            if (scaled != duration) {
                ((MobEffectInstanceStackAccess) instance).newnew$setDuration(scaled);
            }
        }
        return true;
    }

    /** 【无需进食】：石巨人无法进食。 */
    public static boolean blocksEating(ServerPlayer player) {
        return hasPower(player, OriginPowers.GOLEM_FOOD_RULES);
    }

    /**
     * 该消耗品是否被能力规则完全禁止食用。
     * <p>石巨人的【无需进食】禁止一切食物，【药水免疫】禁止饮用一切药水；
     * 天堂鸟的【无法消化肉类】禁止所有肉类（直接阻止进食动作，而不是只把营养清零）。
     * 猫的素食规则只降低饱和度，因此不受影响。</p>
     */
    public static boolean blocksConsumable(Player player, ItemStack stack) {
        if (stack.has(DataComponents.FOOD) && hasPower(player, OriginPowers.GOLEM_FOOD_RULES)) {
            return true;
        }
        if (stack.getItem() instanceof PotionItem
            && hasPower(player, OriginPowers.GOLEM_POTION_IMMUNITY)) {
            return true;
        }
        if (!stack.has(DataComponents.FOOD)) {
            return false;
        }
        return hasPower(player, OriginPowers.BIRD_FOOD_RULES) && stack.is(MEAT);
    }

    /** 按种族规则替换食物的营养与饱和度。 */
    public static FoodProperties modifyFood(Player player, ItemStack stack, FoodProperties food) {
        if (hasPower(player, OriginPowers.BIRD_FOOD_RULES) && stack.is(MEAT)) {
            return copy(food, 0, 0.0F);
        }
        if (hasPower(player, OriginPowers.CAT_CARNIVORE)) {
            if (stack.is(RAW_MEAT)) {
                return copy(food, food.nutrition() * 2, food.saturation() * 2.0F);
            }
            if (stack.is(VEGETABLE)) {
                return copy(food, food.nutrition(), 0.0F);
            }
        }
        return food;
    }

    private static FoodProperties copy(FoodProperties food, int nutrition, float saturation) {
        return new FoodProperties(nutrition, saturation, food.canAlwaysEat());
    }

    // ============================== 跳跃 ==============================

    /**
     * 贴墙下落时的重力系数：0 表示完全抵消重力，1 表示不减速。
     * <p>非冰面：饥饿值大于 6 时完全抵消，否则只减免 60%；
     * 冰面上只有饥饿值大于 6 才能减免 40%。</p>
     */
    public static double catWallSlideFactor(Player player) {
        boolean enoughFood = player.getFoodData().getFoodLevel() > 6;
        if (isIceSurface(player)) {
            return enoughFood ? OriginTuning.CAT_WALL_SLIDE_ICE_FACTOR : 1.0D;
        }
        return enoughFood
            ? OriginTuning.CAT_WALL_SLIDE_FACTOR
            : OriginTuning.CAT_WALL_SLIDE_WEAK_FACTOR;
    }

    /** {@return 脚下地面或正前方贴着的方块是否为冰类} */
    public static boolean isIceSurface(Player player) {
        if (isIce(player.level().getBlockState(player.blockPosition().below()))) {
            return true;
        }
        BlockPos ahead = blockAhead(player);
        return isIce(player.level().getBlockState(ahead));
    }

    private static boolean isIce(BlockState state) {
        return state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE)
            || state.is(Blocks.BLUE_ICE) || state.is(Blocks.FROSTED_ICE);
    }

    /** {@return 朝向水平方向、胸口高度处是否紧贴一个可碰撞方块（与俯仰角无关）} */
    public static boolean isWallAhead(Player player) {
        BlockPos pos = blockAhead(player);
        return !player.level().getBlockState(pos).getCollisionShape(player.level(), pos).isEmpty();
    }

    private static BlockPos blockAhead(Player player) {
        double yaw = Math.toRadians(player.getYRot());
        double distance = player.getBbWidth() * 0.5D + 0.15D;
        return BlockPos.containing(
            player.getX() - Math.sin(yaw) * distance,
            player.getY() + player.getBbHeight() * 0.5D,
            player.getZ() + Math.cos(yaw) * distance);
    }

    public static boolean blocksJump(ServerPlayer player) {
        return hasPower(player, OriginPowers.GOLEM_MOVEMENT);
    }

    public static boolean blocksSprint(Player player) {
        if (hasPower(player, OriginPowers.BIRD_NO_SPRINT)) {
            return true;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            return SlimeState.isGiant(serverPlayer);
        }
        // 客户端只对本地玩家生效；巨大化史莱姆会附带可同步的 SLIME_GIANT 提示效果
        return clientIsSelf(player) && player.hasEffect(ModMobEffects.SLIME_GIANT);
    }

    // ============================== 事件 ==============================

    /** 史莱姆不会受到史莱姆的伤害；猫受到的火焰伤害提高；受到非内部伤害会解除威慑。 */
    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.hasEffect(ModMobEffects.INTIMIDATED)
            && !event.getSource().is(DamageTags.INNER_DAMAGE)) {
            victim.removeEffect(ModMobEffects.INTIMIDATED);
        }
        if (event.getEntity() instanceof ServerPlayer player) {
            if (hasPower(player, OriginPowers.SLIME_KIN_IGNORE)
                && event.getSource().getEntity() instanceof Slime) {
                event.setCanceled(true);
            } else if (hasPower(player, OriginPowers.FLUFFY)
                && event.getSource().is(DamageTypeTags.IS_FIRE)) {
                event.setAmount(event.getAmount() * OriginTuning.CAT_FIRE_DAMAGE_MULTIPLIER);
            }
        }
    }

    /** 生物史莱姆不会把史莱姆玩家设为目标；被威慑的生物无法锁定目标；苦力怕不会索敌猫。 */
    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getEntity().hasEffect(ModMobEffects.INTIMIDATED)) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof Creeper creeper
            && event.getNewAboutToBeSetTarget() instanceof ServerPlayer cat
            && hasPower(cat, OriginPowers.CAT_NATURAL_ENEMY) && creeper.getLastHurtByMob() != cat) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof Slime
            && event.getNewAboutToBeSetTarget() instanceof ServerPlayer player
            && hasPower(player, OriginPowers.SLIME_KIN_IGNORE)) {
            event.setCanceled(true);
        }
    }

    /** 史莱姆致命伤害分裂；猫空手命中施加「大出血」。 */
    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (target instanceof ServerPlayer player
            && player.getHealth() <= 0.0F && hasPower(player, OriginPowers.SLIME_SPLIT)) {
            // 内部伤害（冰冻、失水、出血等）的致死不允许触发分裂
            if (!event.getSource().is(DamageTags.INNER_DAMAGE)) {
                SlimeSplitPower.trySplit(player);
            }
            return;
        }
        if (event.getSource().getEntity() instanceof ServerPlayer cat && cat != target
            && hasPower(cat, OriginPowers.CAT_SCRATCH) && cat.getMainHandItem().isEmpty()
            && target.getRandom().nextFloat() < OriginTuning.CAT_BLEEDING_CHANCE) {
            EffectSources.apply(target, new MobEffectInstance(ModMobEffects.HEMORRHAGE, 1, 0), cat);
        }
    }

    /** 石巨人的饥饿值锁定；猫免疫细雪的冰冻。 */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (hasPower(player, OriginPowers.FLUFFY)) {
            if (player.getTicksFrozen() > 0) {
                player.setTicksFrozen(0);
            }
            return;
        }
        if (!hasPower(player, OriginPowers.GOLEM_FOOD_RULES) || player.tickCount % 20 != 0) {
            return;
        }
        player.getFoodData().setFoodLevel(OriginTuning.GOLEM_LOCKED_FOOD);
        player.getFoodData().setSaturation(0.0F);
    }

    // ============================== 史莱姆消耗品 ==============================

    /** 【吞噬】能否把该物品当作消耗品长按使用。 */
    public static boolean isSlimeConsumable(Item item, Player player) {
        if (!hasPower(player, OriginPowers.SLIME_DEVOUR)) {
            return false;
        }
        if (item == Items.SLIME_BALL) {
            return true;
        }
        return item == Items.SLIME_BLOCK && !hasSlimeSplit(player);
    }

    /** 【吞噬】专属消耗品（粘液球/粘液块）的使用时长；-1 表示不接管。 */
    public static int slimeConsumableDuration(ItemStack stack, LivingEntity entity) {
        if (!(entity instanceof Player player) || !hasPower(player, OriginPowers.SLIME_DEVOUR)) {
            return -1;
        }
        return slimeSpecialDuration(stack, player);
    }

    /**
     * 史莱姆所有进食与饮用的使用时长。
     * <p>粘液球/粘液块由【吞噬】接管并使用专属基准值；其余食物与药水由【史莱姆生命】
     * 按原版时长除以体型。</p>
     *
     * @param vanilla 原版返回的使用时长
     * @return -1 表示保持原版
     */
    public static int slimeUseDuration(ItemStack stack, LivingEntity entity, int vanilla) {
        if (!(entity instanceof Player player)) {
            return -1;
        }
        if (hasPower(player, OriginPowers.SLIME_DEVOUR)) {
            int special = slimeSpecialDuration(stack, player);
            if (special >= 0) {
                return special;
            }
        }
        if (!hasPower(player, OriginPowers.SLIME_LIFE) || vanilla <= 0) {
            return -1;
        }
        boolean consumable = stack.has(DataComponents.FOOD)
            || stack.getUseAnimation() == ItemUseAnimation.DRINK;
        if (!consumable) {
            return -1;
        }
        return Math.max(1, (int) Math.round(vanilla / slimeBodyScale(player)));
    }

    /** 史莱姆的消化速度倍率：基底 25% + 0.5 × 体型（1 体型 75%、2 体型 125%，线性；3 体型 175%）。 */
    public static double slimeDigestRate(Player player) {
        return OriginTuning.SLIME_DIGEST_BASE_RATE
            + OriginTuning.SLIME_DIGEST_RATE_PER_SCALE * slimeBodyScale(player);
    }

    private static int slimeSpecialDuration(ItemStack stack, Player player) {
        double scale = slimeBodyScale(player);
        if (stack.is(Items.SLIME_BALL)) {
            return (int) Math.round(OriginTuning.SLIME_BALL_USE_TICKS / scale);
        }
        if (stack.is(Items.SLIME_BLOCK)) {
            return (int) Math.round(OriginTuning.SLIME_BLOCK_USE_TICKS / scale);
        }
        return -1;
    }

    /** 体型统一由最大生命值换算（20 最大生命值 = 1 体型），进食/饮用时长与消化速度都以此为准。 */
    private static double slimeBodyScale(Player player) {
        return Math.max(0.05D, player.getMaxHealth() / OriginTuning.SLIME_HEALTH_PER_SCALE);
    }

    /**
     * 完成一次史莱姆消耗品使用。
     * <p>返回扣减后的物品栈；效果只在服务端结算，客户端仅做与之一致的消耗预测。</p>
     */
    public static ItemStack finishSlimeConsumable(Player player, ItemStack stack) {
        ItemStack result = stack.copy();
        if (!player.getAbilities().instabuild) {
            result.shrink(1);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            // 与 vanilla 的 Consumable#onConsume 一致：吃的人由本地播放收尾音效
            player.playSound(SoundEvents.GENERIC_EAT.value(), 1.0F, 1.0F);
            return result;
        }
        long now = serverPlayer.level().getGameTime();
        WarlockHealingHost healing = (WarlockHealingHost) serverPlayer;
        if (stack.is(Items.SLIME_BALL)) {
            boolean wasFull = serverPlayer.getHealth() >= serverPlayer.getMaxHealth();
            healing.healByLiving(serverPlayer, OriginTuning.SLIME_BALL_HEAL);
            if (wasFull) {
                SlimeSplitPower.shortenSplit(serverPlayer, OriginTuning.SLIME_BALL_SPLIT_REDUCTION);
            }
        } else if (stack.is(Items.SLIME_BLOCK)) {
            SlimeState.setGiantUntil(serverPlayer, now + OriginTuning.SLIME_GIANT_DURATION_TICKS);
            // 先让最大生命值提升到账，再按新上限结算 30 点治疗
            SlimeSplitPower.refreshNow(serverPlayer);
            healing.healByLiving(serverPlayer, OriginTuning.SLIME_GIANT_HEAL);
        }
        // 除吃的人以外由服务端广播（吃的人已在客户端本地播放，避免双声）
        serverPlayer.level().playSound(serverPlayer, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
            SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0F, 1.0F);
        return result;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, path);
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, id(path));
    }

    private OriginTraits() {}
}
