package com.gam0zing.newnew_warlock_core.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.body.BodyEffect;
import com.gam0zing.newnew_warlock_core.body.BodyPart;
import com.gam0zing.newnew_warlock_core.body.EffectDelta;
import com.gam0zing.newnew_warlock_core.combat.damage.DamageContext;
import com.gam0zing.newnew_warlock_core.combat.effect.BleedingEffectInstance;
import com.gam0zing.newnew_warlock_core.combat.effect.BleedingEffectStack;
import com.gam0zing.newnew_warlock_core.combat.effect.BleedingMobEffect;
import com.gam0zing.newnew_warlock_core.combat.effect.EffectPersistence;
import com.gam0zing.newnew_warlock_core.combat.effect.EffectSources;
import com.gam0zing.newnew_warlock_core.combat.hit.HitFace;
import com.gam0zing.newnew_warlock_core.combat.physics.PhysicsContext;
import com.gam0zing.newnew_warlock_core.combat.physics.PhysicsDamage;
import com.gam0zing.newnew_warlock_core.combat.physics.PhysicsWeapon;
import com.gam0zing.newnew_warlock_core.registry.ModMobEffects;

/**
 * 部位受伤效果的数据验证：概率折算、效果合并、物理命中落地、出血容器聚合与逐刻伤害。
 *
 * <p>这一组只关心数值，不关心 UI/表现。所有断言都打印实际值，方便直接从 gametest 日志抄数据。</p>
 *
 * <p>关键口径：</p>
 * <ul>
 *     <li>触发概率 = 基准概率 × clamp(2 × 实际扣血 / (扣血前生命 + 最大生命), 0, 1)；</li>
 *     <li>出血每 {@link BleedingMobEffect#INTERVAL_TICKS} 刻结算一次，
 *     单次伤害 = max(当前生命 × 5% × 等级, 0.2 × 等级)；</li>
 *     <li>容器展示等级 = 所有子实例等级之和（1 级 = amplifier 0），展示时长 = 子实例最大值。</li>
 * </ul>
 */
public final class BodyEffectDataGameTests {

    private static final float EPS = 1.0E-4F;

    private BodyEffectDataGameTests() {}

    public static void register(RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment) {
        register(event, environment, "probability_curve", 100,
            BodyEffectDataGameTests::probabilityCurveIsCalibrated);
        register(event, environment, "body_effect_merge", 100,
            BodyEffectDataGameTests::bodyEffectMergeCombinesSources);
        register(event, environment, "physics_body_effects", 100,
            BodyEffectDataGameTests::physicsBodyEffectsFeedBleedingStack);
        register(event, environment, "bleeding_stack_dot", 500,
            BodyEffectDataGameTests::bleedingStackDamageOverTime);
        register(event, environment, "merged_level_single_hit", 200,
            BodyEffectDataGameTests::mergedLevelDealsSingleHit);
        register(event, environment, "infinite_child", 100,
            BodyEffectDataGameTests::infiniteChildKeepsStackAlive);
        register(event, environment, "level_saturation", 100,
            BodyEffectDataGameTests::displayLevelSaturatesAtMaxAmplifier);
        register(event, environment, "attribution", 100,
            BodyEffectDataGameTests::attributionPrefersPlayerThenLastSource);
        register(event, environment, "save_load_round_trip", 100,
            BodyEffectDataGameTests::bleedingStackSaveLoadRoundTrip);
    }

    private static void register(
        RegisterGameTestsEvent event, Holder<TestEnvironmentDefinition<?>> environment,
        String name, int maxTicks, java.util.function.Consumer<GameTestHelper> function) {
        event.registerTest(
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "body_effect/" + name),
            new SimpleTestInstance(ModGameTests.info(environment, ModGameTests.emptyStructure(), maxTicks), function));
    }

    // ============================================================
    // 1. 概率折算
    // ============================================================

    /** 概率折算曲线：半血基准、线性缩放、致死上限、零伤害与低血量口径。 */
    public static void probabilityCurveIsCalibrated(GameTestHelper helper) {
        BodyEffect full = effect(1.0F);
        BodyEffect half = effect(0.5F);

        // 基准点：扣血正好等于 (当前 + 最大) / 2 → 原样返回基准概率
        assertClose(helper, "扣血=半血基准 1.0", full.probabilityFor(20.0F, 20.0F, 20.0F), 1.0F);
        assertClose(helper, "扣血=半血基准 0.5", half.probabilityFor(20.0F, 20.0F, 20.0F), 0.5F);

        // 线性缩放：扣血为基准的一半 → 概率减半
        assertClose(helper, "扣血=基准一半", full.probabilityFor(10.0F, 20.0F, 20.0F), 0.5F);
        assertClose(helper, "扣血=基准四分之一", full.probabilityFor(5.0F, 20.0F, 20.0F), 0.25F);

        // 超过基准（已足以致死）不再放大
        assertClose(helper, "扣血超过基准", full.probabilityFor(100.0F, 20.0F, 20.0F), 1.0F);

        // 零伤害不触发
        assertClose(helper, "零伤害", full.probabilityFor(0.0F, 20.0F, 20.0F), 0.0F);
        // 基准概率为 0 时不触发
        assertClose(helper, "基准 0", effect(0.0F).probabilityFor(20.0F, 20.0F, 20.0F), 0.0F);

        // 低血量口径：2 点血、20 上限、扣 1 点 → 2×1/22 = 0.0909（旧公式会得到 0.5）
        float lowHealth = full.probabilityFor(1.0F, 2.0F, 20.0F);
        float oldFormula = 1.0F / 2.0F;
        assertClose(helper, "低血量折算", lowHealth, 2.0F / 22.0F);
        helper.assertTrue(Math.abs(lowHealth - oldFormula) > 0.3F,
            "低血量口径仍未修正：new=" + lowHealth + " old=" + oldFormula);

        NewNewWarlockCore.LOGGER.info(
            "[data][probability] base=1.0 dmg=20 -> {} | dmg=10 -> {} | dmg=5 -> {} | dmg=1/life2 -> {}",
            1.0F, 0.5F, 0.25F, lowHealth);
        helper.succeed();
    }

    // ============================================================
    // 2. 效果合并
    // ============================================================

    /** 部位基础值 + 武器/护甲增量按 id 合并，非法条目被剔除，概率最终 clamp 到 1。 */
    public static void bodyEffectMergeCombinesSources(GameTestHelper helper) {
        Holder<MobEffect> bleeding = ModMobEffects.BLEEDING;
        Holder<MobEffect> bloodLoss = ModMobEffects.BLOOD_LOSS;

        List<BodyEffect> base = List.of(new BodyEffect(bleeding, 200, 0, 0.4F));
        List<EffectDelta> weapon = List.of(new EffectDelta(bleeding, 100, 1, 0.3F));
        List<EffectDelta> armor = List.of(new EffectDelta(bleeding, 0, 0, -0.1F));

        List<BodyEffect> merged = BodyEffect.merge(base, weapon, armor);
        helper.assertTrue(merged.size() == 1, "合并结果数量应为 1，实际 " + merged.size());
        BodyEffect result = merged.get(0);
        helper.assertTrue(result.duration() == 300, "时长应为 200+100=300，实际 " + result.duration());
        helper.assertTrue(result.amplifier() == 1, "等级应为 0+1=1，实际 " + result.amplifier());
        assertClose(helper, "概率合并", result.probability(), 0.6F);

        // 概率上限：0.9 + 0.5 必须收敛到 1
        List<BodyEffect> clamped = BodyEffect.merge(
            List.of(new BodyEffect(bleeding, 200, 0, 0.9F)),
            List.of(new EffectDelta(bleeding, 0, 0, 0.5F)), List.of());
        assertClose(helper, "概率上限 clamp", clamped.get(0).probability(), 1.0F);

        // 只在增量里出现、但等级没有提升的效果（amplifier = -1）应被剔除
        List<BodyEffect> dropped = BodyEffect.merge(
            List.of(), List.of(new EffectDelta(bloodLoss, 200, 0, 0.5F)), List.of());
        helper.assertTrue(dropped.isEmpty(), "amplifier=-1 的条目应被剔除，实际 " + dropped.size());

        // 只靠增量提升 1 级 → amplifier = -1 + 1 = 0，保留
        List<BodyEffect> promoted = BodyEffect.merge(
            List.of(), List.of(new EffectDelta(bloodLoss, 200, 1, 0.5F)), List.of());
        helper.assertTrue(promoted.size() == 1, "提升 1 级的条目应保留，实际 " + promoted.size());
        helper.assertTrue(promoted.get(0).amplifier() == 0,
            "提升后等级应为 0，实际 " + promoted.get(0).amplifier());
        helper.assertTrue(promoted.get(0).duration() == 200,
            "提升后时长应为 200，实际 " + promoted.get(0).duration());

        NewNewWarlockCore.LOGGER.info(
            "[data][merge] duration={} amplifier={} probability={}", result.duration(),
            result.amplifier(), result.probability());
        helper.succeed();
    }

    // ============================================================
    // 3. 物理落地：部位效果 → 出血容器
    // ============================================================

    /** 物理伤害落地后按部位效果施加出血；再次命中应并入同一容器而不是新建效果槽位。 */
    public static void physicsBodyEffectsFeedBleedingStack(GameTestHelper helper) {
        LivingEntity target = spawnTarget(helper);
        DamageContext ctx = physicsContext(helper, target, new BodyPart(
            "test_chest", HitFace.FRONT, 0.0F, 0.0F, 1.0F, 1.0F,
            1.0F, BodyPart.DEFAULT_RIGIDITY, null,
            List.of(new BodyEffect(ModMobEffects.BLEEDING, 200, 0, 1.0F))));

        // 扣血 20 / 生命 20 / 上限 20 → 折算系数 1.0，保证概率分支必定命中，否则测试会随机失败
        PhysicsDamage.applyBodyEffects(ctx, 20.0F, 20.0F);

        MobEffectInstance first = target.getEffect(ModMobEffects.BLEEDING);
        helper.assertTrue(first instanceof BleedingEffectStack,
            "首次施加应得到出血容器，实际 " + (first == null ? "null" : first.getClass().getSimpleName()));
        BleedingEffectStack stack = (BleedingEffectStack) first;
        helper.assertTrue(stack.children().size() == 1,
            "首次施加后子实例应为 1，实际 " + stack.children().size());
        helper.assertTrue(stack.getAmplifier() == 0, "1 级出血的 amplifier 应为 0，实际 " + stack.getAmplifier());
        helper.assertTrue(stack.getDuration() == 200, "时长应为 200，实际 " + stack.getDuration());
        helper.assertTrue(stack.children().get(0) instanceof BleedingEffectInstance,
            "子实例类型应为 BleedingEffectInstance");

        // 同一部位再次命中：必须并入已有容器
        PhysicsDamage.applyBodyEffects(ctx, 20.0F, 20.0F);
        MobEffectInstance second = target.getEffect(ModMobEffects.BLEEDING);
        helper.assertTrue(second == stack, "二次命中创建了新对象，说明没有并入已有容器");
        helper.assertTrue(stack.children().size() == 2,
            "二次命中后子实例应为 2，实际 " + stack.children().size());
        helper.assertTrue(stack.getAmplifier() == 1,
            "两个 1 级出血的展示 amplifier 应为 1，实际 " + stack.getAmplifier());
        helper.assertTrue(target.getActiveEffects().size() == 1,
            "activeEffects 中应只有 1 条出血，实际 " + target.getActiveEffects().size());

        NewNewWarlockCore.LOGGER.info(
            "[data][apply] children={} amplifier={} duration={}",
            stack.children().size(), stack.getAmplifier(), stack.getDuration());
        helper.succeed();
    }

    // ============================================================
    // 4. 出血容器逐刻伤害
    // ============================================================

    /**
     * 两个子实例（I 级 200 刻 + II 级 300 刻）在真实 tick 下的血量曲线。
     * <p>合并结算：等级 = 1 + 2 = 3，每个间隔只打一次；相位取子实例最大剩余时长。</p>
     * <ul>
     *     <li>t=0（剩余 300）：III 级打 20×15%=3.0 → 17.0；</li>
     *     <li>t=100（剩余 200）：打 17×15%=2.55 → 14.45；</li>
     *     <li>t=200（I 级到期，剩余 II 级 → 等级 2）：打 14.45×10%=1.445 → 13.005；</li>
     *     <li>t=300：II 级到期，效果整体移除。</li>
     * </ul>
     */
    public static void bleedingStackDamageOverTime(GameTestHelper helper) {
        LivingEntity target = spawnTarget(helper);

        BleedingEffectStack stack = new BleedingEffectStack(ModMobEffects.BLEEDING);
        stack.pushChild(new BleedingEffectInstance(ModMobEffects.BLEEDING, 200, 0));
        stack.pushChild(new BleedingEffectInstance(ModMobEffects.BLEEDING, 300, 1));
        target.addEffect(stack);

        MobEffectInstance applied = target.getEffect(ModMobEffects.BLEEDING);
        helper.assertTrue(applied instanceof BleedingEffectStack, "施加后应存在出血容器");
        helper.assertTrue(applied.getAmplifier() == 2,
            "1+2 级的展示 amplifier 应为 2，实际 " + applied.getAmplifier());
        helper.assertTrue(applied.getDuration() == 300,
            "展示时长应为 300，实际 " + applied.getDuration());

        float startHealth = target.getHealth();

        helper.runAfterDelay(5, () -> {
            float health = target.getHealth();
            NewNewWarlockCore.LOGGER.info("[data][dot] t=5 health={} (expected 17.0)", health);
            assertClose(helper, "t=5 血量", health, 17.0F);

            helper.runAfterDelay(100, () -> {
                float health100 = target.getHealth();
                NewNewWarlockCore.LOGGER.info("[data][dot] t=105 health={} (expected 14.45)", health100);
                assertClose(helper, "t=105 血量", health100, 14.45F);

                helper.runAfterDelay(100, () -> {
                    float health200 = target.getHealth();
                    MobEffectInstance remaining = target.getEffect(ModMobEffects.BLEEDING);
                    int children = remaining instanceof BleedingEffectStack remainingStack
                        ? remainingStack.children().size() : -1;
                    NewNewWarlockCore.LOGGER.info(
                        "[data][dot] t=205 health={} (expected 13.005) children={} amplifier={}",
                        health200, children, remaining == null ? -1 : remaining.getAmplifier());
                    assertClose(helper, "t=205 血量", health200, 13.005F);
                    helper.assertTrue(children == 1, "I 级子实例应已到期，剩余子实例数 " + children);
                    helper.assertTrue(remaining != null && remaining.getAmplifier() == 1,
                        "剩余 II 级的展示 amplifier 应为 1（等级缓存需已刷新）");

                    helper.runAfterDelay(110, () -> {
                        MobEffectInstance gone = target.getEffect(ModMobEffects.BLEEDING);
                        NewNewWarlockCore.LOGGER.info(
                            "[data][dot] t=315 health={} bleeding={}",
                            target.getHealth(), gone == null ? "removed" : "alive");
                        helper.assertTrue(gone == null, "两个子实例都到期后出血应被整体移除");
                        helper.assertTrue(target.getHealth() < startHealth, "出血没有造成任何伤害");
                        helper.assertTrue(!target.isDeadOrDying(), "目标不应被出血打死");
                        helper.succeed();
                    });
                });
            });
        });
    }

    /**
     * 合并生效：两份 I 级出血必须等于一份 II 级，只结算一次。
     * <p>t=0 预期掉 20×10% = 2.0（而不是两份 I 级各 5% 的 1.9+1.0）。</p>
     */
    public static void mergedLevelDealsSingleHit(GameTestHelper helper) {
        LivingEntity target = spawnTarget(helper);

        BleedingEffectStack stack = new BleedingEffectStack(ModMobEffects.BLEEDING);
        stack.pushChild(new BleedingEffectInstance(ModMobEffects.BLEEDING, 200, 0));
        stack.pushChild(new BleedingEffectInstance(ModMobEffects.BLEEDING, 200, 0));
        target.addEffect(stack);

        helper.assertTrue(target.getEffect(ModMobEffects.BLEEDING).getAmplifier() == 1,
            "两份 I 级应聚合为 II 级");

        helper.runAfterDelay(5, () -> {
            float health = target.getHealth();
            NewNewWarlockCore.LOGGER.info("[data][merge-hit] t=5 health={} (expected 18.0)", health);
            assertClose(helper, "合并结算血量", health, 18.0F);
            helper.succeed();
        });
    }

    /** 无限时长子实例：展示无限、永不自然到期，有限子实例照常递减。 */
    public static void infiniteChildKeepsStackAlive(GameTestHelper helper) {
        BleedingEffectStack stack = new BleedingEffectStack(ModMobEffects.BLEEDING);
        stack.pushChild(new BleedingEffectInstance(
            ModMobEffects.BLEEDING, MobEffectInstance.INFINITE_DURATION, 0));
        stack.pushChild(new BleedingEffectInstance(ModMobEffects.BLEEDING, 100, 0));

        helper.assertTrue(stack.isInfiniteDuration(), "含无限子实例时容器应为无限时长");
        helper.assertTrue(stack.getDuration() == MobEffectInstance.INFINITE_DURATION,
            "无限子实例应让展示时长变为无限，实际 " + stack.getDuration());
        helper.assertTrue(stack.getAmplifier() == 1,
            "两个 1 级子实例的展示 amplifier 应为 1，实际 " + stack.getAmplifier());

        helper.succeed();
    }

    /** 展示等级饱和：子实例数量无上限，但展示 amplifier 必须封顶在原版 255。 */
    public static void displayLevelSaturatesAtMaxAmplifier(GameTestHelper helper) {
        BleedingEffectStack stack = new BleedingEffectStack(ModMobEffects.BLEEDING);
        stack.pushChild(new BleedingEffectInstance(ModMobEffects.BLEEDING, 200, 0));
        // 先读一次让缓存落地，再追加子实例，验证缓存会失效
        helper.assertTrue(stack.getAmplifier() == 0, "单个 I 级的 amplifier 应为 0");
        stack.pushChild(new BleedingEffectInstance(ModMobEffects.BLEEDING, 200, 0));
        helper.assertTrue(stack.getAmplifier() == 1,
            "追加后缓存应失效并重算到 1，实际 " + stack.getAmplifier());

        for (int i = 2; i < 300; i++) {
            stack.pushChild(new BleedingEffectInstance(ModMobEffects.BLEEDING, 200, 0));
        }

        NewNewWarlockCore.LOGGER.info(
            "[data][saturate] children={} totalLevel={} amplifier={}",
            stack.children().size(), stack.totalLevel(), stack.getAmplifier());
        helper.assertTrue(stack.children().size() == 300,
            "子实例目前不设上限，应有 300 个，实际 " + stack.children().size());
        helper.assertTrue(stack.totalLevel() == 300,
            "内部等级合计不应被截断，应为 300，实际 " + stack.totalLevel());
        helper.assertTrue(stack.getAmplifier() == 255,
            "对外 amplifier 应封顶 255，实际 " + stack.getAmplifier());
        helper.succeed();
    }

    /** 来源归因：子实例来源中有玩家则用玩家，否则用列表最后的来源。 */
    public static void attributionPrefersPlayerThenLastSource(GameTestHelper helper) {
        Villager target = spawnTarget(helper);
        Villager first = helper.spawn(EntityTypes.VILLAGER, 0, 1, 1);
        Villager last = helper.spawn(EntityTypes.VILLAGER, 2, 1, 1);

        BleedingEffectStack stack = new BleedingEffectStack(ModMobEffects.BLEEDING);
        BleedingEffectInstance a = new BleedingEffectInstance(ModMobEffects.BLEEDING, 200, 0);
        BleedingEffectInstance b = new BleedingEffectInstance(ModMobEffects.BLEEDING, 200, 0);
        EffectSources.attachSource(a, first);
        EffectSources.attachSource(b, last);
        stack.pushChild(a);
        stack.pushChild(b);

        helper.assertTrue(EffectSources.attributionSourceOf(stack, target) == last,
            "无玩家来源时应取列表最后的来源");

        Player player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        EffectSources.attachSource(a, player);
        helper.assertTrue(EffectSources.attributionSourceOf(stack, target) == player,
            "存在玩家来源时应优先用玩家");

        helper.assertTrue(EffectSources.attributionSourceOf(b, target) == last,
            "普通实例应解析自身来源，不受容器规则影响");

        NewNewWarlockCore.LOGGER.info("[data][source] no-player -> last, with-player -> player");
        helper.succeed();
    }

    // ============================================================
    // 5. 存档往返
    // ============================================================

    /** 容器存档后读档必须还原成容器，且子实例、展示等级、展示时长完整。 */
    public static void bleedingStackSaveLoadRoundTrip(GameTestHelper helper) {
        BleedingEffectStack original = new BleedingEffectStack(ModMobEffects.BLEEDING);
        original.pushChild(new BleedingEffectInstance(ModMobEffects.BLEEDING, 200, 0));
        original.pushChild(new BleedingEffectInstance(ModMobEffects.BLEEDING, 300, 1));

        Map<Holder<MobEffect>, MobEffectInstance> live = new HashMap<>();
        live.put(ModMobEffects.BLEEDING, original);
        List<EffectPersistence.SavedEffect> saved = EffectPersistence.collect(live);
        helper.assertTrue(saved.size() == 1, "应收集到 1 条出血附加数据，实际 " + saved.size());

        Map<Holder<MobEffect>, MobEffectInstance> loadedMap = new HashMap<>();
        loadedMap.put(ModMobEffects.BLEEDING, new MobEffectInstance(ModMobEffects.BLEEDING, 300, 0));
        EffectPersistence.apply(loadedMap, saved);
        MobEffectInstance loaded = loadedMap.get(ModMobEffects.BLEEDING);

        helper.assertTrue(loaded instanceof BleedingEffectStack,
            "读档应重建容器，实际 " + (loaded == null ? "null" : loaded.getClass().getSimpleName()));
        BleedingEffectStack rebuilt = (BleedingEffectStack) loaded;
        helper.assertTrue(rebuilt.children().size() == 2,
            "读档后子实例应为 2，实际 " + rebuilt.children().size());
        helper.assertTrue(rebuilt.getAmplifier() == 2,
            "读档后展示 amplifier 应为 2，实际 " + rebuilt.getAmplifier());
        helper.assertTrue(rebuilt.getDuration() == 300,
            "读档后展示时长应为 300，实际 " + rebuilt.getDuration());

        helper.assertTrue(rebuilt.children().get(0).getDuration() == 200,
            "第 1 个子实例时长应为 200，实际 " + rebuilt.children().get(0).getDuration());
        helper.assertTrue(rebuilt.children().get(1).getAmplifier() == 1,
            "第 2 个子实例 amplifier 应为 1，实际 " + rebuilt.children().get(1).getAmplifier());

        NewNewWarlockCore.LOGGER.info(
            "[data][save] children={} amplifier={} duration={}",
            rebuilt.children().size(), rebuilt.getAmplifier(), rebuilt.getDuration());
        helper.succeed();
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private static BodyEffect effect(float probability) {
        return new BodyEffect(ModMobEffects.BLEEDING, 200, 0, probability);
    }

    /** 生成一个静止、无 AI 的村民作为数据载体（20 点生命，不受日晒影响）。 */
    private static Villager spawnTarget(GameTestHelper helper) {
        for (int x = 0; x < 3; x++) {
            for (int z = 0; z < 3; z++) {
                helper.setBlock(x, 0, z, net.minecraft.world.level.block.Blocks.STONE);
            }
        }
        Villager villager = helper.spawn(EntityTypes.VILLAGER, 1, 1, 1);
        villager.setNoAi(true);
        villager.setPersistenceRequired();
        villager.setHealth(villager.getMaxHealth());
        return villager;
    }

    /** 构造一个只带物理上下文、不真正走 hurt 的伤害上下文，供 applyBodyEffects 直接调用。 */
    private static DamageContext physicsContext(
        GameTestHelper helper, LivingEntity target, BodyPart bodyPart) {
        var source = target.damageSources().generic();
        DamageContainer container = new DamageContainer(source, 20.0F);
        DamageContext ctx = new DamageContext(source, target, container);
        ctx.setPhysics(new PhysicsContext(PhysicsWeapon.DEFAULT, bodyPart, null, false, 1.0D));
        return ctx;
    }

    private static void assertClose(GameTestHelper helper, String what, float actual, float expected) {
        helper.assertTrue(Math.abs(actual - expected) <= EPS,
            what + " 期望 " + expected + "，实际 " + actual);
    }
}
