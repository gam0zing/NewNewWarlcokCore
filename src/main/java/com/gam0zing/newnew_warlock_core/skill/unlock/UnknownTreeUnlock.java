package com.gam0zing.newnew_warlock_core.skill.unlock;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 未知解锁方式：数据包引用了未注册的类型时降级使用，始终视为锁定。
 * <p>这样客户端解析不会因为缺少某个可选模组的解锁方式而整棵树失败。</p>
 */
public final class UnknownTreeUnlock implements TreeUnlock {

    private final Identifier type;

    public UnknownTreeUnlock(Identifier type) {
        this.type = type;
    }

    @Override
    public Identifier type() {
        return this.type;
    }

    @Override
    public boolean allows(ServerPlayer player) {
        return false;
    }
}
