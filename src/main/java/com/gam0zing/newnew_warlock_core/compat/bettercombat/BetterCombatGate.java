package com.gam0zing.newnew_warlock_core.compat.bettercombat;

import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Better Combat 可选依赖的运行时判定。
 * <p>类本身不在字段或签名里持有 Better Combat 类型，未安装该模组时
 * {@link #isWeapon(ItemStack)} 会在访问 {@code WeaponRegistry} 之前返回，
 * 不会触发目标类解析。</p>
 */
public final class BetterCombatGate {

    private static final boolean LOADED = ModList.get().isLoaded("bettercombat");

    /** 该物品是否由 Better Combat 接管（拥有 BC 武器数据）。 */
    public static boolean isWeapon(ItemStack stack) {
        if (!LOADED) {
            return false;
        }
        return WeaponRegistry.getAttributes(stack) != null;
    }

    private BetterCombatGate() {}
}
