package com.gam0zing.newnew_warlock_core.mob;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

/**
 * 记录本存档是否成形过下界/末地传送门，供难度加成使用。
 * <p>下界门成形（点火瞬间，框架图案被检测到）由 {@link BlockEvent.PortalSpawnEvent} 给出；
 * 末地门成形没有现成事件，见 mixin 包里的 {@code EnderEyeItemMixin}。
 * 实体跨维度旅行作为兜底，覆盖命令/其他模组直接放置传送门方块等绕过成形检测的情形。
 * 标记保存在主世界的 {@link WorldProgressData}，因此重启后依然有效。</p>
 */
@EventBusSubscriber(modid = NewNewWarlockCore.MOD_ID)
public final class WorldProgressEvt {

    /**
     * 下界门成形即计入。
     * <p>事件在框架图案匹配成功、放置传送门方块之前触发且可取消；
     * {@link EventPriority#LOWEST} 保证绝大多数模组已经决定过是否取消，取消时不误计。</p>
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPortalSpawn(BlockEvent.PortalSpawnEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            WorldProgressData.get(level).markNetherOpened();
        }
    }

    /** 兜底：命令/模组直接放置的传送门方块不经过成形检测，实体穿过时补记。 */
    @SubscribeEvent
    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        ResourceKey<Level> dimension = event.getDimension();
        if (dimension.equals(Level.NETHER)) {
            WorldProgressData.get(level).markNetherOpened();
        } else if (dimension.equals(Level.END)) {
            WorldProgressData.get(level).markEndOpened();
        }
    }

    private WorldProgressEvt() {}
}
