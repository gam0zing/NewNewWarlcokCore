package com.gam0zing.newnew_warlock_core.rebuild;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * 生物重建的挂点。
 * <p>{@link EntityJoinLevelEvent} 同时覆盖两类生物，保证每只生物至少进入一次 {@link NewNewRebuildHost#newnewRebuild()}：</p>
 * <ul>
 *     <li>新生成/召唤的生物：{@code ServerLevel#addFreshEntity} → 实体加入世界时触发；</li>
 *     <li>旧存档中的生物：区块实体读档加载（{@code PersistentEntitySectionManager#processPendingLoads}）后加入世界时触发。</li>
 * </ul>
 * <p>是否已经重建过由 {@code newnew_mob_rebuild} 的 base 值判定（会持久化），因此重复加入世界不会重复重建。</p>
 * <p>只在服务端处理；客户端只接收属性同步，不执行重建逻辑。</p>
 */
@EventBusSubscriber(modid = NewNewWarlockCore.MOD_ID)
public final class NewNewRebuildEvt {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof NewNewRebuildHost host) {
            host.newnewRebuild();
        }
    }

    private NewNewRebuildEvt() {}
}
