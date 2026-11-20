package com.gam0zing.newnew_warlock_core.skill.unlock;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 不解锁：树始终锁定，只能由后续系统主动开放。
 */
public final class LockedTreeUnlock implements TreeUnlock {

    public static final LockedTreeUnlock INSTANCE = new LockedTreeUnlock();

    private LockedTreeUnlock() {}

    @Override
    public Identifier type() {
        return TreeUnlocks.LOCKED_ID;
    }

    @Override
    public boolean allows(ServerPlayer player) {
        return false;
    }
}
