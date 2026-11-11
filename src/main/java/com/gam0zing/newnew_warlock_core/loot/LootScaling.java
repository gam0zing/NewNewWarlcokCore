package com.gam0zing.newnew_warlock_core.loot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.mob.MobDifficulty;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

/**
 * 掉落相关的属性规则。
 * <ul>
 *     <li>{@code fortune} / {@code looting}：任何读取对应附魔等级的地方（战利品表的奖励数量公式）追加等级，
 *     向下取整，最终等级不小于 0；</li>
 *     <li>{@code looting_scale}：击杀生物后战利品的掉落次数倍率。</li>
 * </ul>
 */
public final class LootScaling {

    /**
     * 按属性追加时运/抢夺等级。
     *
     * @param enchantment 公式引用的附魔
     * @param level       该附魔在工具上的原始等级
     * @param bonus       属性追加值（已向下取整）
     * @return 最终等级；不是时运/抢夺时原样返回
     */
    public static int adjustEnchantmentLevel(Holder<Enchantment> enchantment, int level, double bonus) {
        if (bonus == 0.0D || !isAffected(enchantment)) {
            return level;
        }
        return Math.max(0, level + (int) Math.floor(bonus));
    }

    /** {@return 该附魔是否受属性追加影响（时运 / 抢夺）} */
    public static boolean isAffected(Holder<Enchantment> enchantment) {
        Optional<ResourceKey<Enchantment>> key = enchantment.unwrapKey();
        return key.isPresent()
            && (key.get().equals(Enchantments.FORTUNE) || key.get().equals(Enchantments.LOOTING));
    }

    /** 按战利品上下文的归属生物追加时运/抢夺等级。 */
    public static int adjustEnchantmentLevel(Holder<Enchantment> enchantment, int level, LootContext ctx) {
        LivingEntity owner = ownerOf(ctx);
        if (owner == null) {
            return level;
        }

        Optional<ResourceKey<Enchantment>> key = enchantment.unwrapKey();
        if (key.isEmpty()) {
            return level;
        }

        double bonus;
        if (key.get().equals(Enchantments.FORTUNE)) {
            bonus = ModAttributes.getValue(owner, ModAttributes.FORTUNE);
        } else if (key.get().equals(Enchantments.LOOTING)) {
            bonus = ModAttributes.getValue(owner, ModAttributes.LOOTING);
        } else {
            return level;
        }

        return adjustEnchantmentLevel(enchantment, level, bonus);
    }

    /** {@return 战利品上下文中的归属生物：优先击杀者，其次战利品表持有者} */
    @Nullable
    public static LivingEntity ownerOf(LootContext ctx) {
        Entity entity = ctx.getOptional(LootContextParams.ATTACKING_ENTITY);
        if (entity == null) {
            entity = ctx.getOptional(LootContextParams.THIS_ENTITY);
        }
        return entity instanceof LivingEntity living ? living : null;
    }

    /**
     * 击杀掉落次数倍率。
     * <p>整数部分为额外的完整掉落轮数，小数部分为再追加一轮的概率；
     * 小于 1 时按概率清空掉落，使期望数量等于倍率。</p>
     */
    public static void scaleDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();
        if (level.isClientSide()) {
            return;
        }
        if (!(event.getSource().getEntity() instanceof LivingEntity looter)) {
            return;
        }

        double scale = ModAttributes.getValue(looter, ModAttributes.LOOTING_SCALE);
        if (scale == 1.0D || scale < 0.0D || event.getDrops().isEmpty()) {
            return;
        }

        if (scale < 1.0D) {
            if (looter.getRandom().nextDouble() >= scale) {
                event.getDrops().clear();
            }
            return;
        }

        int rounds = (int) Math.floor(scale) - 1;
        double chance = scale - Math.floor(scale);
        if (chance > 0.0D && looter.getRandom().nextDouble() < chance) {
            rounds++;
        }
        if (rounds <= 0) {
            return;
        }

        List<ItemEntity> snapshot = new ArrayList<>(event.getDrops());
        for (int round = 0; round < rounds; round++) {
            for (ItemEntity drop : snapshot) {
                event.getDrops().add(copyOf(level, drop));
            }
        }
    }

    /**
     * 险地矿工：挖掉矿物（{@code c:ores}）时按挖掘者的 {@code perilous_miner} 概率使每份掉落物数量翻倍，
     * 可连续翻倍；概率档位由 {@link MobDifficulty#rollDoublingTimes} 按位置难度折算。
     */
    public static void doubleOreDrops(BlockDropsEvent event) {
        if (!event.getState().is(Tags.Blocks.ORES)) {
            return;
        }
        if (!(event.getBreaker() instanceof LivingEntity miner)) {
            return;
        }

        double value = ModAttributes.getValue(miner, ModAttributes.PERILOUS_MINER);
        if (value <= 0.0D || event.getDrops().isEmpty()) {
            return;
        }

        BlockPos pos = event.getPos();
        int times = MobDifficulty.rollDoublingTimes(
            value, event.getLevel(), pos.getX(), pos.getY(), pos.getZ(), pos, miner.getRandom());
        if (times <= 0) {
            return;
        }

        long multiplier = 1L << Math.min(times, 20);
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            long count = stack.getCount() * multiplier;
            stack.setCount((int) Math.min(count, Integer.MAX_VALUE));
        }
    }

    private static ItemEntity copyOf(Level level, ItemEntity original) {
        ItemStack stack = original.getItem().copy();
        ItemEntity copy = new ItemEntity(level, original.getX(), original.getY(), original.getZ(), stack);
        copy.setDeltaMovement(original.getDeltaMovement());
        return copy;
    }

    private LootScaling() {}
}
