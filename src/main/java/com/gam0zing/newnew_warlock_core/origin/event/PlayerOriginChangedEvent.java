package com.gam0zing.newnew_warlock_core.origin.event;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.origin.OriginSlot;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.Event;

/**
 * 某一个起源槽位发生变化时发布的事件。
 * <p>一次更换可能同时改变多个槽位，因此每个槽位单独发布一次；旧值或新值可以为 null
 * （表示该槽位原本为空或被清除）。事件只用于通知，不可取消。</p>
 */
public final class PlayerOriginChangedEvent extends Event {

    private final ServerPlayer player;
    private final OriginSlot slot;
    @Nullable
    private final Identifier oldOrigin;
    @Nullable
    private final Identifier newOrigin;

    public PlayerOriginChangedEvent(
        ServerPlayer player,
        OriginSlot slot,
        @Nullable Identifier oldOrigin,
        @Nullable Identifier newOrigin) {
        this.player = player;
        this.slot = slot;
        this.oldOrigin = oldOrigin;
        this.newOrigin = newOrigin;
    }

    public ServerPlayer player() {
        return this.player;
    }

    public OriginSlot slot() {
        return this.slot;
    }

    @Nullable
    public Identifier oldOrigin() {
        return this.oldOrigin;
    }

    @Nullable
    public Identifier newOrigin() {
        return this.newOrigin;
    }
}
