package com.gam0zing.newnew_warlock_core.share;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 状态效果来源（业务区域）。
 * <p>效果实例通过 {@link WarlockEffectHost} 保存可为 null 的来源生物；这里统一提供四件事：</p>
 * <ul>
 *     <li>施加效果时写入来源（{@link #apply} / {@link #attachSource}）；</li>
 *     <li>自己喝药/进食期间把使用者作为来源（{@link #beginSelfConsumption}），
 *     使“自己喝下的药、吃下的食物”按自己的属性结算；</li>
 *     <li>效果结算期间（{@code MobEffectInstance#tick} 内产生的伤害与治疗）把当前效果来源暴露出来，
 *     供伤害入口与治疗入口读取；</li>
 *     <li>把来源注入 {@link DamageSource}。</li>
 * </ul>
 * <p>来源身份以 UUID 形式随效果实例写入实体存档（只走 {@code MobEffectInstance#save/load}，
 * 不进入物品组件、不改网络包），读档后由 {@link #resolvedSourceOf} 懒解析回活体：</p>
 * <ul>
 *     <li>自己喝/吃的效果，UUID 就是持有者本人 → 直接认作本人，重进地图/读档后不会丢失；</li>
 *     <li>外部来源（喷溅投掷者、药箭射手等）尽力解析：当前维度已加载实体 → 在线玩家 → 其它维度；
 *     来源未加载、离线或已死亡时解析失败，按无来源结算。</li>
 * </ul>
 * <p><b>已知边界（有意为之，不要在别处重复实现）</b></p>
 * <ul>
 *     <li>来源身份只在服务端使用：伤害/治疗结算都在服务端，客户端不需要也不应同步来源；
 *     物品组件同样不带来源——物品在被使用前本来就不存在“施加者”这一信息。</li>
 *     <li>UUID 解析是尽力而为：会遍历当前维度、在线玩家与其它维度，但未加载区块、离线玩家、
 *     已死亡实体都查不到，此时按无来源结算；UUID 仍保留在 NBT 中，来源重新加载后可再次解析成功。</li>
 *     <li>解析结果只在本次运行期缓存为引用；引用被移除（死亡/卸载）后自动退回按 UUID 重查。</li>
 *     <li>隐藏效果（{@code hiddenEffect}）只随外层效果实例保存一次，浮上来时沿用外层的来源字段。</li>
 *     <li>自己吃喝的来源上下文只在 {@code completeUsingItem} / {@code eat} 期间有效；绕过这两个入口
 *     直接施加的效果（如模组自定义瞬时效果、直接调用 {@code addEffect} 且不带实体）不会被归因。</li>
 *     <li>来源 UUID 存放在实体 NBT 中，可被手改存档伪造，风险等同原版 {@code AreaEffectCloud.Owner}。</li>
 *     <li>自己吃喝的上下文按栈配对弹出；若原版路径抛出异常则不会弹出（原版该路径基本不抛异常，风险极低）。</li>
 * </ul>
 */
public final class EffectSources {

    /** 正在结算的效果实例（按嵌套顺序压栈，支持效果结算内再次施加/结算效果） */
    private static final ThreadLocal<Deque<MobEffectInstance>> TICKING =
        ThreadLocal.withInitial(ArrayDeque::new);

    /** 正在自己吃喝（喝药/进食）的生物（按嵌套顺序压栈） */
    private static final ThreadLocal<Deque<LivingEntity>> SELF_CONSUMING =
        ThreadLocal.withInitial(ArrayDeque::new);

    private EffectSources() {}

    /**
     * 施加一个带来源的状态效果。
     * <p>等价于先 {@link #attachSource} 再调用 {@link LivingEntity#addEffect(MobEffectInstance)}，
     * 因此 {@code MobEffectEvent.Added} 等外部钩子照常触发。</p>
     *
     * @param target   被施加效果的目标
     * @param instance 效果实例
     * @param source   来源生物，可为 null
     * @return {@link LivingEntity#addEffect} 的结果（目标未受影响或效果未生效时为 false）
     */
    public static boolean apply(LivingEntity target, MobEffectInstance instance, @Nullable LivingEntity source) {
        attachSource(instance, source);
        return target.addEffect(instance);
    }

    /** 给效果实例写入来源生物与来源 UUID（实例为 null 时不做处理） */
    public static void attachSource(@Nullable MobEffectInstance instance, @Nullable LivingEntity source) {
        if (instance instanceof WarlockEffectHost host) {
            host.newnew$setEffectSource(source);
        }
    }

    /** {@return 效果实例的来源生物（运行时引用）；实例或来源缺失时为 null} */
    @Nullable
    public static LivingEntity sourceOf(@Nullable MobEffectInstance instance) {
        return instance instanceof WarlockEffectHost host ? host.newnew$effectSource() : null;
    }

    /** {@return 效果实例保存的来源 UUID；未记录时为 null} */
    @Nullable
    public static UUID sourceUuidOf(@Nullable MobEffectInstance instance) {
        return instance instanceof WarlockEffectHost host ? host.newnew$effectSourceUuid() : null;
    }

    /**
     * 解析效果实例的来源生物，供结算时使用。
     * <p>优先使用运行时引用；引用为空或已失效时按保存的 UUID 解析：</p>
     * <ul>
     *     <li>UUID 等于持有者本人 → 直接返回持有者（自己喝/吃的效果，读档后依然认得本人）；</li>
     *     <li>否则在当前维度、在线玩家、其它维度依次查找；解析成功后回填引用，失败返回 null。</li>
     * </ul>
     *
     * @param instance 效果实例，可为 null
     * @param target   效果持有者（被治疗/被伤害的生物）
     * @return 来源生物；解析失败时为 null
     */
    @Nullable
    public static LivingEntity resolvedSourceOf(@Nullable MobEffectInstance instance, LivingEntity target) {
        if (!(instance instanceof WarlockEffectHost host)) {
            return null;
        }

        LivingEntity source = host.newnew$effectSource();
        if (source != null && !source.isRemoved()) {
            return source;
        }

        UUID uuid = host.newnew$effectSourceUuid();
        if (uuid == null) {
            return null;
        }
        if (uuid.equals(target.getUUID())) {
            return target;
        }

        LivingEntity resolved = lookupServerSource(target, uuid);
        if (resolved != null) {
            host.newnew$setEffectSource(resolved);
        }
        return resolved;
    }

    /** 只在服务端解析：当前维度已加载实体 → 在线玩家 → 其它维度；客户端返回 null */
    @Nullable
    private static LivingEntity lookupServerSource(LivingEntity target, UUID uuid) {
        if (!(target.level() instanceof ServerLevel level)) {
            return null;
        }

        Entity entity = level.getEntity(uuid);
        if (entity instanceof LivingEntity living) {
            return living;
        }

        MinecraftServer server = level.getServer();
        if (server == null) {
            return null;
        }

        ServerPlayer player = server.getPlayerList().getPlayer(uuid);
        if (player != null) {
            return player;
        }

        for (ServerLevel other : server.getAllLevels()) {
            if (other == level) {
                continue;
            }
            Entity candidate = other.getEntity(uuid);
            if (candidate instanceof LivingEntity living) {
                return living;
            }
        }
        return null;
    }

    /**
     * 从原版施加效果时携带的 {@code Entity} 参数解析来源生物。
     * <p>投掷药水传入的是投掷者本人；滞留药水云传入的是云实体，需要再取它的 owner。</p>
     *
     * @param entity 施加效果时的来源实体，可为 null
     * @return 对应的来源生物；没有生物来源时为 null
     */
    @Nullable
    public static LivingEntity sourceEntity(@Nullable Entity entity) {
        if (entity instanceof LivingEntity living) {
            return living;
        }
        if (entity instanceof AreaEffectCloud cloud) {
            return cloud.getOwner();
        }
        return null;
    }

    /** 由“自己完成使用物品”入口在结算前调用，记录这次喝药/进食的使用者 */
    public static void beginSelfConsumption(LivingEntity consumer) {
        SELF_CONSUMING.get().push(consumer);
    }

    /** 由“自己完成使用物品”入口在结算后调用（与 {@link #beginSelfConsumption} 成对） */
    public static void endSelfConsumption() {
        Deque<LivingEntity> stack = SELF_CONSUMING.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    /**
     * {@return 当前自己吃喝的使用者，且仅在 {@code target} 就是使用者时返回，否则 null}
     * <p>限制在同一生物上，避免吃喝过程中顺带施加给其它生物的效果被误归因。</p>
     *
     * @param target 即将获得效果/治疗的生物
     */
    @Nullable
    public static LivingEntity currentSelfConsumptionSource(LivingEntity target) {
        LivingEntity consumer = SELF_CONSUMING.get().peek();
        return consumer == target ? consumer : null;
    }

    /** 由 {@code MobEffectInstance#tick} 在结算前调用，记录当前效果实例 */
    public static void beginTick(MobEffectInstance instance) {
        TICKING.get().push(instance);
    }

    /** 由 {@code MobEffectInstance#tick} 在结算后调用（与 {@link #beginTick} 成对） */
    public static void endTick() {
        Deque<MobEffectInstance> stack = TICKING.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    /** {@return 当前正在结算的效果实例；不在效果结算中时为 null} */
    @Nullable
    public static MobEffectInstance currentTickingEffect() {
        return TICKING.get().peek();
    }

    /**
     * {@return 当前正在结算的效果来源生物；不在效果结算中或解析失败时为 null}
     *
     * @param target 效果持有者（被治疗/被伤害的生物）
     */
    @Nullable
    public static LivingEntity currentTickingSource(LivingEntity target) {
        return attributionSourceOf(currentTickingEffect(), target);
    }

    /**
     * 解析用于结算归因的来源生物。
     * <p>容器模式下每份子实例保留各自的来源，归因规则：</p>
     * <ol>
     *     <li>子实例来源中有玩家 → 优先用玩家（就近取第一个能直接解析到的玩家）；</li>
     *     <li>没有玩家 → 用列表最后的来源（从尾部找第一个可解析的来源，即最近一次施加）；</li>
     *     <li>都没有 → 不归因。</li>
     * </ol>
     * <p>普通实例仍按自身来源解析。</p>
     *
     * @param instance 效果实例，可为 null
     * @param target   效果持有者
     * @return 用于归因的来源生物；无来源时为 null
     */
    @Nullable
    public static LivingEntity attributionSourceOf(@Nullable MobEffectInstance instance, LivingEntity target) {
        if (!(instance instanceof EffectStack stack)) {
            return resolvedSourceOf(instance, target);
        }

        List<MobEffectInstance> children = stack.children();
        if (children.isEmpty()) {
            return null;
        }

        // 1) 玩家优先：先走便宜的运行时引用检查，命中即返回
        for (MobEffectInstance child : children) {
            if (sourceOf(child) instanceof Player player) {
                return player;
            }
        }

        // 2) 没有玩家：用列表最后的来源（未读档时直接取运行时引用，O(1)）
        LivingEntity lastRef = sourceOf(children.get(children.size() - 1));
        if (lastRef != null) {
            return lastRef;
        }

        // 3) 读档后只剩 UUID：逐个解析，仍保持玩家优先，否则取尾部来源
        LivingEntity last = null;
        for (int i = children.size() - 1; i >= 0; i--) {
            LivingEntity source = resolvedSourceOf(children.get(i), target);
            if (i == children.size() - 1) {
                last = source;
            }
            if (source instanceof Player player) {
                return player;
            }
        }
        return last;
    }

    /**
     * 把“当前正在结算的效果来源”注入伤害来源。
     * <p>只在原伤害没有攻击者时注入，因此不会覆盖投掷药水的瞬时伤害等本来就带来源的情况；
     * 伤害类型、直接来源、来源坐标全部保持原样，仅补上 {@link DamageSource#getEntity()}。</p>
     *
     * @param source 进入伤害流程的伤害来源
     * @param target 被伤害的生物
     * @return 注入来源后的伤害来源（无需注入时原样返回）
     */
    public static DamageSource inheritTickingSource(DamageSource source, LivingEntity target) {
        LivingEntity effectSource = currentTickingSource(target);
        if (effectSource == null || source.getEntity() != null) {
            return source;
        }

        Entity direct = source.getDirectEntity();
        return new DamageSource(source.typeHolder(), direct, effectSource, source.sourcePositionRaw());
    }
}
