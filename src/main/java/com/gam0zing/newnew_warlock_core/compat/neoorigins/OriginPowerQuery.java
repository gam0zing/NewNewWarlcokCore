package com.gam0zing.newnew_warlock_core.compat.neoorigins;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.jetbrains.annotations.Nullable;

import com.cyberday1.neoorigins.api.power.EmbeddedPower;
import com.cyberday1.neoorigins.api.power.PowerHolder;
import com.cyberday1.neoorigins.power.builtin.base.AbstractTogglePower;
import com.cyberday1.neoorigins.service.ActiveOriginService;
import com.gam0zing.newnew_warlock_core.origin.content.power.CompositePower;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 玩家的“有效能力”查询：把复合能力展开成子能力后再匹配。
 * <p>NeoOrigins 自带的类型查询只遍历玩家直接持有的 power，
 * 看不到 {@link CompositePower} 的子能力；这里提供递归版本，
 * 并通过 mixin 桥接回 NeoOrigins 的查询入口。</p>
 *
 * <p>展开过程不评估能力条件，条件只在真正匹配时沿父链检查。这样条件谓词内部再调用
 * {@link #hasPower} 也不会递归，且查询可以在命中后立即返回。</p>
 *
 * <p>客户端不同步子能力 id，因此本类只用于服务端。</p>
 */
public final class OriginPowerQuery {

    private static final int MAX_DEPTH = 16;

    /**
     * 能力树的一个节点。
     *
     * @param holder 能力本身
     * @param parent 父复合能力，直接持有的能力为 {@code null}
     * @param depth  从直接持有的能力算起的层数
     */
    private record Node(PowerHolder<?> holder, @Nullable Node parent, int depth) {

        /** {@return 自身与父链上所有能力的条件是否都满足} */
        boolean isUsableBy(ServerPlayer player) {
            for (Node node = this; node != null; node = node.parent()) {
                if (!node.holder().isConditionSatisfied(player)) {
                    return false;
                }
            }
            return true;
        }
    }

    /** {@return 玩家是否直接或通过复合能力间接拥有该能力} */
    public static boolean hasPower(ServerPlayer player, Identifier power) {
        return findFirst(player, node -> node.holder().id().equals(power)) != null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static boolean has(ServerPlayer player, Class<?> type, Predicate<?> predicate) {
        return findFirst(player, node -> type.isInstance(node.holder().type())
            && node.isUsableBy(player)
            && ((Predicate) predicate).test(node.holder().config())) != null;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void forEachOfType(ServerPlayer player, Class<?> type, Consumer<?> consumer) {
        forEachUsable(player, type, node -> ((Consumer) consumer).accept(node.holder().config()));
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void forEachOfTypeActive(ServerPlayer player, Class<?> type, Consumer<?> consumer) {
        forEachUsable(player, type, node -> {
            if (node.holder().type() instanceof AbstractTogglePower toggle
                && toggle.isToggledOff(player, node.holder().config(), node.holder().id())) {
                return;
            }
            ((Consumer) consumer).accept(node.holder().config());
        });
    }

    /** 对每个类型匹配且条件满足的节点执行动作；节点可能在父链上重复出现，由遍历去重。 */
    private static void forEachUsable(ServerPlayer player, Class<?> type, Consumer<Node> action) {
        walkAll(player, node -> {
            if (type.isInstance(node.holder().type()) && node.isUsableBy(player)) {
                action.accept(node);
            }
        });
    }

    /** 遍历整棵能力树。 */
    private static void walkAll(ServerPlayer player, Consumer<Node> action) {
        findFirst(player, node -> {
            action.accept(node);
            return false;
        });
    }

    /**
     * 广度优先展开能力树，返回第一个匹配的节点。
     * <p>展开过程不调用 {@link PowerHolder#isConditionSatisfied}，因此匹配谓词里可以安全地再次查询。</p>
     */
    @Nullable
    private static Node findFirst(ServerPlayer player, Predicate<Node> match) {
        Set<Identifier> visited = new HashSet<>();
        Deque<Node> queue = new ArrayDeque<>();
        for (PowerHolder<?> holder : ActiveOriginService.allPowers(player)) {
            queue.add(new Node(holder, null, 0));
        }
        while (!queue.isEmpty()) {
            Node node = queue.poll();
            PowerHolder<?> holder = node.holder();
            if (!visited.add(holder.id())) {
                continue;
            }
            if (match.test(node)) {
                return node;
            }
            if (node.depth() >= MAX_DEPTH || !(holder.type() instanceof CompositePower composite)) {
                continue;
            }
            for (EmbeddedPower child : composite.children(holder.config())) {
                try {
                    queue.add(new Node(child.holder(), node, node.depth() + 1));
                } catch (RuntimeException ignored) {
                    // 数据重载瞬间子能力可能尚未注册。
                }
            }
        }
        return null;
    }

    private OriginPowerQuery() {}
}
