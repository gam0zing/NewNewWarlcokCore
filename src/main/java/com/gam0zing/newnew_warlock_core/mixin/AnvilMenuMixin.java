package com.gam0zing.newnew_warlock_core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.enchantment.Enchantment;

import com.gam0zing.newnew_warlock_core.enchant.EnchantBaseline;
import com.gam0zing.newnew_warlock_core.enchant.EnchantContext;
import com.gam0zing.newnew_warlock_core.enchant.WarlockEnchanting;
import com.gam0zing.newnew_warlock_core.enchant.config.EnchantBaselineConfig;

/**
 * 铁砧的最大等级接管。
 * <p>原版 {@code AnvilMenu#createResult} 在合并附魔时用 {@code Enchantment#getMaxLevel} 截断等级
 * （同一表达式里调用两次）。这里把返回值换成「全局基准 → 玩家相关修正」的结果。</p>
 * <p>注意必须取 {@code max(现有等级, 上限)} 才不会让高等级物品在合并时降级——原版已经按
 * {@code j2 = i2 == j2 ? j2 + 1 : Math.max(j2, i2)} 保证现有等级不会被压低。</p>
 * <p>另外把铁砧的费用上限（原版写死的 40 / 39）接到 {@code anvil.max_cost}：
 * 原版顺序为 {@code i = 40}（叠加物品惩罚封顶，不改）、{@code cost >= 40 → cost = 39}、
 * {@code cost >= 40 && !instabuild → 结果清空}，因此 40 的前两个有效序号分别是「截断阈值」与「结果清空阈值」。</p>
 * <p><b>版本约束</b>：下面三处 {@code @Constant} 的 ordinal 是硬编码的字节码顺序，已针对 1.21.1
 * （{@code AnvilMenu.createResult} 第 217 / 249 / 253 行）核对过。升级 Minecraft 版本后必须用
 * {@code mixin.debug.export} + {@code javap} 复核；若不匹配，Mixin 不会报错，只会静默地不再替换。</p>
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    /** {@code this.cost.set(39)}：超过上限时显示的费用。 */
    @ModifyConstant(method = "createResultInternal", constant = @Constant(intValue = 39))
    private int warlock$clampAnvilDisplayCost(int original) {
        return Math.max(0, EnchantBaselineConfig.settings().anvilMaxCost() - 1);
    }

    /** {@code this.cost.get() >= 40}：把费用截断到上限。 */
    @ModifyConstant(method = "createResultInternal", constant = @Constant(intValue = 40, ordinal = 1))
    private int warlock$anvilCostThreshold(int original) {
        return Math.max(1, EnchantBaselineConfig.settings().anvilMaxCost());
    }

    /** {@code this.cost.get() >= 40 && !instabuild}：超过上限视为「过于昂贵」。 */
    @ModifyConstant(method = "createResultInternal", constant = @Constant(intValue = 40, ordinal = 2))
    private int warlock$anvilTooExpensiveThreshold(int original) {
        return Math.max(1, EnchantBaselineConfig.settings().anvilMaxCost());
    }

    @WrapOperation(
        method = "createResultInternal",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/Enchantment;getMaxLevel()I"))
    private int warlock$maxLevel(Enchantment enchantment, Operation<Integer> original) {
        ItemCombinerMenu menu = (ItemCombinerMenu) (Object) this;
        int baseline = EnchantBaseline.maxLevel(enchantment, original.call(enchantment));
        int resolved = WarlockEnchanting.hooks().maxLevel(
            EnchantContext.anvil(menu.player, menu.inputSlots.getItem(0)), enchantment, baseline);
        return Mth.clamp(resolved, 1, EnchantBaseline.LEVEL_HARD_CAP);
    }
}
