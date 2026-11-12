package com.gam0zing.newnew_warlock_core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import net.minecraft.util.Mth;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.enchantment.Enchantment;

import com.gam0zing.newnew_warlock_core.enchant.EnchantBaseline;
import com.gam0zing.newnew_warlock_core.enchant.EnchantContext;
import com.gam0zing.newnew_warlock_core.enchant.EnchantUnlockData;
import com.gam0zing.newnew_warlock_core.enchant.WarlockEnchanting;
import com.gam0zing.newnew_warlock_core.enchant.config.EnchantBaselineConfig;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 铁砧的最大等级接管。
 * <p>原版 {@code AnvilMenu#createResult} 在合并附魔时用 {@code Enchantment#getMaxLevel} 截断等级
 * （同一表达式里调用两次）。这里把返回值换成「全局基准 → 玩家相关修正」的结果。</p>
 * <p>注意必须取 {@code max(现有等级, 上限)} 才不会让高等级物品在合并时降级——原版已经按
 * {@code j2 = i2 == j2 ? j2 + 1 : Math.max(j2, i2)} 保证现有等级不会被压低。</p>
 * <p>另外把铁砧的费用上限（原版写死的 40 / 39）接到 {@code anvil.max_cost}：
 * 原版顺序为 {@code i = 40}（叠加物品惩罚封顶，不改）、{@code cost >= 40 → cost = 39}、
 * {@code cost >= 40 && !instabuild → 结果清空}，因此 40 的前两个有效序号分别是「截断阈值」与「结果清空阈值」。</p>
 * <p><b>版本约束</b>：下面三处 {@code @Constant} 的 ordinal 是硬编码的字节码顺序，已针对 26.3
 * （{@code AnvilMenu.createResultInternal} 第 243 / 275 / 282 行）核对过。升级 Minecraft 版本后必须用
 * {@code mixin.debug.export} + {@code javap} 复核；若不匹配，Mixin 不会报错，只会静默地不再替换。</p>
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    /**
     * 当前玩家的铁砧费用上限：配置值 + 玩家属性 {@link ModAttributes#EXTRA_ANVIL_MAX_COST}，结果不小于 0。
     * <p>属性缺失（非玩家或未挂载）时只看配置值。</p>
     */
    private int warlock$anvilMaxCost() {
        ItemCombinerMenu menu = (ItemCombinerMenu) (Object) this;
        int configured = EnchantBaselineConfig.settings().anvilMaxCost();
        int extra = Mth.floor(ModAttributes.getValue(menu.player, ModAttributes.EXTRA_ANVIL_MAX_COST));
        return Math.max(0, configured + extra);
    }

    /** {@code this.cost.set(39)}：超过上限时显示的费用。 */
    @ModifyConstant(method = "createResultInternal", constant = @Constant(intValue = 39))
    private int warlock$clampAnvilDisplayCost(int original) {
        return Math.max(0, this.warlock$anvilMaxCost() - 1);
    }

    /** {@code this.cost.get() >= 40}：把费用截断到上限。 */
    @ModifyConstant(method = "createResultInternal", constant = @Constant(intValue = 40, ordinal = 1))
    private int warlock$anvilCostThreshold(int original) {
        return this.warlock$anvilMaxCost();
    }

    /** {@code this.cost.get() >= 40 && !instabuild}：超过上限视为「过于昂贵」。 */
    @ModifyConstant(method = "createResultInternal", constant = @Constant(intValue = 40, ordinal = 2))
    private int warlock$anvilTooExpensiveThreshold(int original) {
        return this.warlock$anvilMaxCost();
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

    /**
     * 首次打赢袭击后，本次结算的累计工作惩罚减半。
     * <p>{@code createResultInternal} 里 {@code long tax} 只有两次写入：方法开头的 {@code 0L} 初始化，
     * 以及累加完两个输入的 {@code REPAIR_COST} 之后的赋值，这里要改的是后者。
     * 物品自身存储的 {@code REPAIR_COST} 仍按原版 {@code calculateIncreasedRepairCost} 增长，只影响本次费用。</p>
     * <p><b>注意</b>：{@code @ModifyVariable} 自己的 {@code ordinal} 是「同类型局部变量的序号」，不是 STORE 指令序号。
     * 该方法里只有 {@code tax} 一个 long 局部变量，它的 ordinal 恒为 0，所以写 {@code ordinal = 1} 会直接注入失败
     * （启动报 {@code failed injection check, (0/1) succeeded}）。正确的做法是把 ordinal 写在 {@code @At} 上：
     * {@code @At("STORE")} 在这个注入器里只匹配与处理器返回值同类型的 store（这里是 {@code LSTORE}），
     * 26.3 中 {@code lstore_3} 位于偏移 27（初始化为 {@code 0L}）与 117（累加之后），故 {@code ordinal = 1} 即后者。</p>
     * <p><b>版本约束</b>：ordinal 依赖字节码顺序，已针对 26.3 核对。升级 Minecraft 后必须用 {@code javap} 复核，
     * 顺序变了会注入失败（{@code defaultRequire = 1}），不会静默失效。</p>
     */
    @ModifyVariable(method = "createResultInternal", at = @At(value = "STORE", ordinal = 1))
    private long warlock$halveWorkPenalty(long tax) {
        return EnchantUnlockData.raidDefeated() ? tax / 2L : tax;
    }
}
