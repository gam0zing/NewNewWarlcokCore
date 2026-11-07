package com.gam0zing.newnew_warlock_core.enchant;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * 一次附魔计算所需的上下文。
 * <p>玩家相关规则（等级、属性、进度等）依赖 {@link #player()}；战利品路径依赖 {@link #lootContext()}；
 * 村民交易路径只有 {@link #trader()}（交易生成时原版拿不到玩家）。</p>
 * <p>所有字段都可能为 {@code null}，具体由场景决定，规则实现必须先判空再使用。</p>
 *
 * @param scene       调用场景
 * @param player      玩家，可能为 {@code null}
 * @param trader      村民/流浪商人等交易实体，可能为 {@code null}
 * @param lootContext 战利品上下文，可能为 {@code null}
 * @param level       所在世界，可能为 {@code null}
 * @param pos         附魔台等方块坐标，可能为 {@code null}
 * @param stack       正在处理的物品，可能为 {@code null}
 * @param random      随机源，可能为 {@code null}
 */
public record EnchantContext(
    EnchantScene scene,
    @Nullable Player player,
    @Nullable Entity trader,
    @Nullable LootContext lootContext,
    @Nullable Level level,
    @Nullable BlockPos pos,
    @Nullable ItemStack stack,
    @Nullable RandomSource random) {

    /** 没有上下文的兜底场景（例如全局 {@code getMaxLevel} 注入）。 */
    public static EnchantContext of(EnchantScene scene) {
        return new EnchantContext(scene, null, null, null, null, null, null, null);
    }

    /** 附魔台上下文。 */
    public static EnchantContext table(
        @Nullable Player player, @Nullable BlockPos pos, ItemStack stack, RandomSource random) {
        return new EnchantContext(
            EnchantScene.TABLE, player, null, null, player == null ? null : player.level(), pos, stack, random);
    }

    /** 铁砧上下文。 */
    public static EnchantContext anvil(Player player, ItemStack stack) {
        return new EnchantContext(EnchantScene.ANVIL, player, null, null, player.level(), null, stack, null);
    }

    /** 战利品上下文。 */
    public static EnchantContext loot(EnchantScene scene, LootContext lootContext, ItemStack stack, RandomSource random) {
        return new EnchantContext(scene, null, null, lootContext, lootContext.getLevel(), null, stack, random);
    }

    /** 村民交易上下文（原版交易生成时没有玩家）。 */
    public static EnchantContext trade(EnchantScene scene, Entity trader, RandomSource random) {
        return new EnchantContext(scene, null, trader, null, trader.level(), null, null, random);
    }

    /**
     * 村民交易中走战利品函数的上下文。
     * <p>26.3 的交易物品由 {@code given_item_modifier}（{@code EnchantRandomlyFunction} /
     * {@code EnchantWithLevelsFunction}）生成，此时既有交易实体，也有 {@link LootContext}。</p>
     */
    public static EnchantContext tradeLoot(
        EnchantScene scene, Entity trader, LootContext lootContext, ItemStack stack, RandomSource random) {
        return new EnchantContext(scene, null, trader, lootContext, lootContext.getLevel(), null, stack, random);
    }

    /**
     * 取得当前上下文可用的注册表访问器。
     * <p>优先使用世界；战利品使用 {@link LootContext}；最后尝试玩家所在世界。</p>
     */
    @Nullable
    public RegistryAccess registryAccess() {
        if (this.level != null) {
            return this.level.registryAccess();
        }
        if (this.lootContext != null) {
            return this.lootContext.getLevel().registryAccess();
        }
        return this.player != null ? this.player.level().registryAccess() : null;
    }

    public boolean hasPlayer() {
        return this.player != null;
    }
}
