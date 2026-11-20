package com.gam0zing.newnew_warlock_core.skill.unlock;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 默认解锁：树始终对玩家开放。
 */
public final class DefaultTreeUnlock implements TreeUnlock {

    public static final DefaultTreeUnlock INSTANCE = new DefaultTreeUnlock();

    private DefaultTreeUnlock() {}

    @Override
    public Identifier type() {
        return TreeUnlocks.DEFAULT_ID;
    }

    @Override
    public boolean allows(ServerPlayer player) {
        return true;
    }
}
