package com.gam0zing.newnew_warlock_core.skill.unlock;

import com.gam0zing.newnew_warlock_core.origin.WarlockOrigins;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

/**
 * 起源解锁：玩家任意一层起源等于参数中的注册名时开放。
 * <p>未安装 NeoOrigins 时 {@link WarlockOrigins#hasOrigin} 永远为 false，树保持锁定。</p>
 */
public final class OriginTreeUnlock implements TreeUnlock {

    private final Identifier origin;

    public OriginTreeUnlock(Identifier origin) {
        this.origin = origin;
    }

    public Identifier origin() {
        return this.origin;
    }

    @Override
    public Identifier type() {
        return TreeUnlocks.ORIGIN_ID;
    }

    @Override
    public boolean allows(ServerPlayer player) {
        return WarlockOrigins.hasOrigin(player, this.origin);
    }
}
