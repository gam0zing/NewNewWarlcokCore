package com.gam0zing.newnew_warlock_core.compat.bettercombat;

import net.bettercombat.logic.WeaponRegistry;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.fml.ModList;

/**
 * Better Combat 可选依赖的运行时判定。
 * <p>类本身不在字段或签名里持有 Better Combat 类型，未安装该模组时
 * {@link #isWeapon(ItemStack)} 会在访问 {@code WeaponRegistry} 之前返回，
 * 不会触发目标类解析。</p>
 */
public final class BetterCombatGate {

    private static final boolean LOADED = ModList.get().isLoaded("bettercombat");

    /** {@return Better Combat 是否已安装} */
    public static boolean isLoaded() {
        return LOADED;
    }

    /** 该物品是否由 Better Combat 接管（拥有 BC 武器数据）。 */
    public static boolean isWeapon(ItemStack stack) {
        if (!LOADED) {
            return false;
        }
        return WeaponRegistry.getAttributes(stack) != null;
    }

    /** 该物品是否被 Better Combat 归类为矛。 */
    public static boolean isSpear(ItemStack stack) {
        if (!LOADED) {
            return false;
        }
        net.bettercombat.api.WeaponAttributes attributes = WeaponRegistry.getAttributes(stack);
        return attributes != null && "spear".equals(attributes.category());
    }

    /**
     * {@return 向前方向、正处于冲锋状态且手持带【突进】的矛时，突进等级；否则为 0}
     * <p>方向编码：0 前、1 后、2 左、3 右。服务端与客户端共用同一份判定，
     * 客户端用它决定是否预测戳刺特效。</p>
     */
    public static int lungeLevel(Player player, int direction) {
        if (!LOADED || direction != 0 || !player.isUsingItem()) {
            return 0;
        }
        ItemStack useItem = player.getUseItem();
        if (!isSpear(useItem)) {
            return 0;
        }
        Holder<Enchantment> lunge = player.registryAccess()
            .lookupOrThrow(Registries.ENCHANTMENT)
            .getOrThrow(Enchantments.LUNGE);
        return EnchantmentHelper.getItemEnchantmentLevel(lunge, useItem);
    }

    private BetterCombatGate() {}
}
