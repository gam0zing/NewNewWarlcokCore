package com.gam0zing.newnew_warlock_core.combat.spine;

import java.util.ArrayDeque;
import java.util.Deque;

import org.jetbrains.annotations.Nullable;

/**
 * 记录每次 {@code LivingEntity#hurt} 调用的状态，用于在方法返回处
 * 1. 还原被我们阻塞后丢失的返回值；
 * 2. 清理因事件取消而未弹出的 {@link net.neoforged.neoforge.common.damagesource.DamageContainer}。
 * <p>使用栈以支持嵌套调用（例如伤害流程中再次造成伤害）。</p>
 */
public final class PipelineState {
    private static final ThreadLocal<Deque<Invocation>> INVOCATIONS = ThreadLocal.withInitial(ArrayDeque::new);

    private PipelineState() {}

    /** {@code LivingEntity#hurt} 入口调用 */
    public static void begin(int containerBaseline) {
        INVOCATIONS.get().push(new Invocation(containerBaseline));
    }

    /** {@code LivingEntity#hurt} 返回时调用，返回当前调用记录（栈已元素时返回 null） */
    @Nullable
    public static Invocation end() {
        return INVOCATIONS.get().poll();
    }

    /** 由伤害管线写入本次 hurt 调用的返回值 */
    public static void setCurrentResult(boolean result) {
        Invocation invocation = INVOCATIONS.get().peek();
        if (invocation != null) {
            invocation.result = result;
        }
    }

    public static final class Invocation {
        /** 进入 hurt 时容器栈的深度，用于清理残留容器 */
        public final int containerBaseline;
        /** 管线给出的返回值；null 表示本次调用没有接管（例如被其他模组提前取消） */
        @Nullable
        public Boolean result;

        Invocation(int containerBaseline) {
            this.containerBaseline = containerBaseline;
        }
    }
}
