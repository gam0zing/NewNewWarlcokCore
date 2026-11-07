package com.gam0zing.newnew_warlock_core.mixin;

import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.IdMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.neoforged.neoforge.event.EventHooks;

import com.gam0zing.newnew_warlock_core.enchant.EnchantContext;
import com.gam0zing.newnew_warlock_core.enchant.EnchantScene;
import com.gam0zing.newnew_warlock_core.enchant.WarlockEnchanting;

/**
 * 附魔台的整段替换。
 * <p>原版 {@code EnchantmentMenu#slotsChanged} 负责书架 power 扫描、三行经验成本与线索，
 * {@code #getEnchantmentList} 负责按种子生成候选附魔。这里把两者整体交给
 * {@link WarlockEnchanting#hooks()}，默认实现保持原版结果。</p>
 * <p>关键细节：
 * <ul>
 *     <li>玩家在构造期从 {@link Inventory#player} 捕获，客户端与服务端都会执行；</li>
 *     <li>每次生成候选前必须用 {@code enchantmentSeed + slot} 重置随机种子（原版就是这样做的），
 *     否则线索与实际附魔结果会不一致；</li>
 *     <li>{@link ContainerLevelAccess#NULL} 在客户端会直接跳过回调，因此客户端不会重复计算。</li>
 * </ul>
 */
@Mixin(EnchantmentMenu.class)
public abstract class EnchantmentMenuMixin extends AbstractContainerMenu {

    @Shadow
    @Final
    private Container enchantSlots;

    @Shadow
    @Final
    private ContainerLevelAccess access;

    @Shadow
    @Final
    private RandomSource random;

    @Shadow
    @Final
    private DataSlot enchantmentSeed;

    @Shadow
    @Final
    public int[] costs;

    @Shadow
    @Final
    public int[] enchantClue;

    @Shadow
    @Final
    public int[] levelClue;

    /** 菜单对应的玩家（构造期捕获，可能为 {@code null}）。 */
    @Unique
    @Nullable
    private Player warlock$player;

    /** 最近一次计算时附魔台的位置，供 {@code getEnchantmentList} 组装上下文。 */
    @Unique
    @Nullable
    private BlockPos warlock$tablePos;

    protected EnchantmentMenuMixin(MenuType<?> type, int containerId) {
        super(type, containerId);
    }

    @Inject(
        method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/inventory/ContainerLevelAccess;)V",
        at = @At("TAIL"))
    private void warlock$capturePlayer(
        int containerId, Inventory playerInventory, ContainerLevelAccess access, CallbackInfo ci) {
        this.warlock$player = playerInventory.player;
    }

    @Inject(method = "slotsChanged", at = @At("HEAD"), cancellable = true)
    private void warlock$slotsChanged(Container inventory, CallbackInfo ci) {
        if (inventory != this.enchantSlots) {
            return;
        }
        ci.cancel();

        ItemStack stack = inventory.getItem(0);
        if (stack.isEmpty() || !stack.isEnchantable()) {
            Arrays.fill(this.costs, 0);
            Arrays.fill(this.enchantClue, -1);
            Arrays.fill(this.levelClue, -1);
            return;
        }

        this.access.execute(this::warlock$refreshOffers);
    }

    @Inject(method = "getEnchantmentList", at = @At("HEAD"), cancellable = true)
    private void warlock$getEnchantmentList(
        RegistryAccess registryAccess, ItemStack stack, int slot, int cost,
        CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
        this.random.setSeed((long) (this.enchantmentSeed.get() + slot));
        List<EnchantmentInstance> candidates = WarlockEnchanting.hooks().tableCandidates(
            this.warlock$tableContext(stack), registryAccess, stack, slot, cost, this.random);
        // 原版 clickMenuButton 只判 !isEmpty()，因此 hook 返回 null 必须在这里兜底，否则会 NPE。
        cir.setReturnValue(candidates == null ? List.of() : candidates);
    }

    /**
     * 复刻原版 {@code slotsChanged}：书架扫描 → power → 三行成本（含经验门槛与事件）→ 三行线索。
     */
    @Unique
    private void warlock$refreshOffers(Level level, BlockPos pos) {
        ItemStack stack = this.enchantSlots.getItem(0);
        IdMap<Holder<Enchantment>> idMap =
            level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).asHolderIdMap();

        float shelfPower = 0.0F;
        for (BlockPos offset : EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (EnchantingTableBlock.isValidBookShelf(level, pos, offset)) {
                shelfPower += level.getBlockState(pos.offset(offset)).getEnchantPowerBonus(level, pos.offset(offset));
            }
        }

        this.warlock$tablePos = pos;
        EnchantContext ctx = EnchantContext.table(this.warlock$player, pos, stack, this.random);

        int power = Math.max(0, WarlockEnchanting.hooks().tablePower(ctx, stack, (int) shelfPower));
        power = Math.min(power, Math.max(1, WarlockEnchanting.hooks().tablePowerCap(ctx)));

        this.random.setSeed((long) this.enchantmentSeed.get());
        for (int slot = 0; slot < 3; slot++) {
            this.costs[slot] = WarlockEnchanting.hooks().tableSlotCost(ctx, slot, power, stack, this.random);
            this.enchantClue[slot] = -1;
            this.levelClue[slot] = -1;
            if (this.costs[slot] < slot + 1) {
                this.costs[slot] = 0;
            }
            this.costs[slot] = EventHooks.onEnchantmentLevelSet(level, pos, slot, power, stack, this.costs[slot]);
        }

        for (int slot = 0; slot < 3; slot++) {
            if (this.costs[slot] <= 0) {
                continue;
            }

            // 与原版 getEnchantmentList 一致：用 enchantmentSeed + slot 重置种子，保证线索与点击结果同源。
            this.random.setSeed((long) (this.enchantmentSeed.get() + slot));
            List<EnchantmentInstance> candidates = WarlockEnchanting.hooks().tableCandidates(
                ctx, level.registryAccess(), stack, slot, this.costs[slot], this.random);
            if (candidates != null && !candidates.isEmpty()) {
                EnchantmentInstance instance = candidates.get(this.random.nextInt(candidates.size()));
                this.enchantClue[slot] = idMap.getId(instance.enchantment());
                this.levelClue[slot] = instance.level();
            }
        }

        this.broadcastChanges();
    }

    /** {@code getEnchantmentList} 拿不到坐标，使用最近一次 {@code slotsChanged} 记录的位置。 */
    @Unique
    private EnchantContext warlock$tableContext(ItemStack stack) {
        if (this.warlock$player != null) {
            return EnchantContext.table(this.warlock$player, this.warlock$tablePos, stack, this.random);
        }
        return new EnchantContext(EnchantScene.TABLE, null, null, null, null, this.warlock$tablePos, stack, this.random);
    }
}
