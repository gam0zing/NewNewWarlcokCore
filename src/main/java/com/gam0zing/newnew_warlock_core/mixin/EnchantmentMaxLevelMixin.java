package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.item.enchantment.Enchantment;

import com.gam0zing.newnew_warlock_core.enchant.EnchantBaseline;
import com.gam0zing.newnew_warlock_core.enchant.EnchantUnlockData;

/**
 * 最大等级的全局兜底。
 * <p>所有直接调用 {@code Enchantment#getMaxLevel} 的代码路径（原版没被单独接管的、其他模组的、指令、
 * 创造模式物品栏等）都会在返回值处被替换成 {@link EnchantBaseline} 的基准值。
 * 这里只读全局基准，不做玩家相关修正；玩家相关修正必须由各入口把 {@link com.gam0zing.newnew_warlock_core.enchant.EnchantContext}
 * 传进 {@link com.gam0zing.newnew_warlock_core.enchant.WarlockEnchanting#hooks()}。</p>
 * <p>基准表为空（未加载/数据包重载中）时直接返回原版值，因此不会改变默认行为。</p>
 * <p>兼容性：神化（Apotheosis）、Quark 等模组也会改写 {@code getMaxLevel}。本 Mixin 的优先级设为
 * {@code 1100}（高于默认 {@code 1000}），保证在它们之后应用，使最终返回值仍经过
 * {@link EnchantBaseline#LEVEL_HARD_CAP} 的 255 上限保护；本模组只被动的兜底，不会主动抹掉
 * 其他模组给出的更高等级（基准表里该附魔的最大等级仍可由服主自行调高）。</p>
 */
@Mixin(value = Enchantment.class, priority = 1100)
public abstract class EnchantmentMaxLevelMixin {

    @Inject(method = "getMaxLevel", at = @At("RETURN"), cancellable = true)
    private void warlock$getMaxLevel(CallbackInfoReturnable<Integer> cir) {
        Enchantment self = (Enchantment) (Object) this;
        if (EnchantBaseline.get(self) != null) {
            // 走基准表的统一入口，让「首次击杀末影龙 → 基准最大等级 +1」在这条全局路径也生效。
            cir.setReturnValue(EnchantBaseline.maxLevel(self, cir.getReturnValue()));
        }
    }

    /**
     * 铁砧每级附魔费用的全局兜底，来源同 {@link EnchantBaseline}；
     * 首次击杀监守者后在此处减半（对所有铁砧实现生效，因此不再在 {@code AnvilMenuMixin} 里重复处理）。
     */
    @Inject(method = "getAnvilCost", at = @At("RETURN"), cancellable = true)
    private void warlock$getAnvilCost(CallbackInfoReturnable<Integer> cir) {
        EnchantBaseline.Entry entry = EnchantBaseline.get((Enchantment) (Object) this);
        int cost = entry == null ? cir.getReturnValue() : Math.max(0, entry.anvilCost());
        if (cost > 0 && EnchantUnlockData.wardenDefeated()) {
            cost = Math.max(1, cost / 2);
        }
        cir.setReturnValue(cost);
    }
}
