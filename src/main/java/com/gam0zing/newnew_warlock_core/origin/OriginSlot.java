package com.gam0zing.newnew_warlock_core.origin;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.resources.Identifier;

/**
 * 玩家身上的三种起源槽位。
 * <p>每个槽位对应 NeoOrigins 的一个起源层；界面与业务层只使用这个枚举，
 * 不直接依赖 NeoOrigins 的层 id 常量。</p>
 */
public enum OriginSlot {

    /** 种族层，复用 NeoOrigins 自带的 {@code neoorigins:origin} 层。 */
    RACE("race", Identifier.fromNamespaceAndPath("neoorigins", "origin")),

    /** 灵质层，由本模组提供。 */
    ESSENCE("essence", Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "essence")),

    /** 加护层，由本模组提供；对玩家隐藏，只能通过 API 或方块赋予。 */
    BLESSING("blessing", Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "blessing"));

    private final String key;
    private final Identifier layerId;

    OriginSlot(String key, Identifier layerId) {
        this.key = key;
        this.layerId = layerId;
    }

    /** {@return 用于翻译键与调试的短名称} */
    public String key() {
        return this.key;
    }

    /** {@return NeoOrigins 中对应的起源层注册名} */
    public Identifier layerId() {
        return this.layerId;
    }

    /** 按层注册名反查槽位；未知层返回 null。 */
    public static OriginSlot byLayer(Identifier layerId) {
        for (OriginSlot slot : values()) {
            if (slot.layerId.equals(layerId)) {
                return slot;
            }
        }
        return null;
    }
}
