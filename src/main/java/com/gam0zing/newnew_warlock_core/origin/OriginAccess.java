package com.gam0zing.newnew_warlock_core.origin;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

/**
 * 起源后端的内部接口，由兼容层实现。
 * <p>方法签名只使用原版类型，避免公共 API 泄露 NeoOrigins 的类型；
 * 未安装 NeoOrigins 时 {@link WarlockOrigins} 不会持有任何实现。</p>
 */
public interface OriginAccess {

    /** {@return 玩家当前各层的起源，键为层注册名} */
    Map<Identifier, Identifier> snapshot(ServerPlayer player);

    /** 设置某一层的起源。 */
    OriginSetResult setOrigin(ServerPlayer player, Identifier layer, Identifier origin);

    /** 清除某一层的起源。 */
    boolean clearOrigin(ServerPlayer player, Identifier layer);

    /**
     * 打开单层重选界面。
     *
     * @param layers      允许重选的层
     * @param consumeItem 提交时消耗的物品，可为 null
     * @param messageKey  打开界面时发送的提示翻译键
     * @return 是否成功打开
     */
    boolean openPicker(
        ServerPlayer player,
        List<Identifier> layers,
        @Nullable Item consumeItem,
        String messageKey);
}
