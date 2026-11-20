package com.gam0zing.newnew_warlock_core.skill.unlock;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 一棵技能树的解锁条件。
 * <p>由数据包 JSON 的 {@code unlock} 对象构造；客户端也会解析它，
 * 因此实现只依赖原版类型与中性的起源 API。</p>
 */
public interface TreeUnlock {

    /** {@return 解锁方式的类型注册名} */
    Identifier type();

    /** {@return 当前玩家是否满足解锁条件} */
    boolean allows(ServerPlayer player);
}
