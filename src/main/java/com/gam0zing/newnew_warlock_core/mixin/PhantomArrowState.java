package com.gam0zing.newnew_warlock_core.mixin;

import java.util.function.Consumer;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 幻影箭矢的两种标记。
 * <p>1）线程内标记：弩的箭矢在 {@code performShooting -> shoot -> createProjectile -> customArrow} 之间
 * 隔着几层调用，用线程内标记把「本次射击是幻影箭矢」传给箭矢创建处，在那里统一做减伤与不可拾取。</p>
 * <p>2）物品标记：弩在装填时判定幻影，命中后箭矢不入包；这层信息要跨越「装填 → 射击」的间隔，
 * 因此写在弩的 {@code CUSTOM_DATA} 上，射击时读取并清理（玩家一般看不见这个标记）。</p>
 */
public final class PhantomArrowState {

    private static final ThreadLocal<Boolean> ACTIVE = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** 弩上的幻影标记键 */
    private static final String CROSSBOW_MARK = "newnew_phantom_arrow";

    /** 标记本次射击为幻影箭矢 */
    public static void begin() {
        ACTIVE.set(Boolean.TRUE);
    }

    public static boolean isActive() {
        return ACTIVE.get();
    }

    /** 结束标记，必须在射击流程退出时调用 */
    public static void end() {
        ACTIVE.remove();
    }

    /** 在弩上写入幻影标记 */
    public static void markCrossbow(ItemStack weapon) {
        update(weapon, tag -> tag.putBoolean(CROSSBOW_MARK, true));
    }

    public static boolean isCrossbowMarked(ItemStack weapon) {
        CustomData data = weapon.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().getBooleanOr(CROSSBOW_MARK, false);
    }

    /** 清理弩自身的幻影标记 */
    public static void clearCrossbowMark(ItemStack weapon) {
        if (!isCrossbowMarked(weapon)) {
            return;
        }
        update(weapon, tag -> tag.remove(CROSSBOW_MARK));
    }

    private static void update(ItemStack weapon, Consumer<CompoundTag> action) {
        CustomData.update(DataComponents.CUSTOM_DATA, weapon, action);
    }

    private PhantomArrowState() {}
}
