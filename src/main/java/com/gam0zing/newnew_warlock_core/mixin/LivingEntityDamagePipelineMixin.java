package com.gam0zing.newnew_warlock_core.mixin;

import java.util.Stack;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlocksAttacks;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;

import com.gam0zing.newnew_warlock_core.combat.damage.DamageContext;
import com.gam0zing.newnew_warlock_core.combat.damage.DamagePipeline;
import com.gam0zing.newnew_warlock_core.combat.damage.PipelineState;
import com.gam0zing.newnew_warlock_core.combat.damage.WarlockDamageHost;

/**
 * 伤害管线骨架。
 * <p>{@code LivingIncomingDamageEvent} 被 {@code LivingDamageEvt} 取消后，原版 {@code LivingEntity#hurt}
 * 会在事件处直接返回；此处接管事件之后的所有调用。</p>
 * <p><b>链路只保留骨架与外部注入钩子，不含任何原版伤害公式或表现：</b></p>
 * <ul>
 *     <li>骨架：容器数值同步与清理、无敌帧计时、死亡分派、仇恨归因</li>
 *     <li>外部钩子：{@code LivingShieldBlockEvent}、{@code ArmorHurtEvent}、{@code LivingDamageEvent.Pre/Post}、
 *     {@code LivingDeathEvent}（随 {@code die} 发出）、{@code LivingUseTotemEvent}（在业务插槽内触发）</li>
 *     <li>兼容钩子：把业务归属的减免写回容器，保证 {@code addModifier} 与其他监听者仍能读到/改写数值</li>
 *     <li>业务：全部集中在 {@link DamagePipeline} 的 TODO 插槽（含扣血、击退）</li>
 * </ul>
 * <p>另外在 {@code hurt} 的返回处恢复被取消事件吞掉的返回值，并清理未弹出的 DamageContainer。</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDamagePipelineMixin implements WarlockDamageHost {

    @Shadow
    protected Stack<DamageContainer> damageContainers;
    @Shadow
    protected int noActionTime;
    @Shadow
    protected float lastHurt;
    @Shadow
    @Nullable
    private DamageSource lastDamageSource;
    @Shadow
    private long lastDamageStamp;

    // ---- 临时：原版受伤表现用到的入口 ----
    @Shadow
    public int hurtTime;

    @Shadow
    public int hurtDuration;

    @Shadow
    protected abstract void playHurtSound(DamageSource source);

    @Shadow
    protected abstract SoundEvent getDeathSound();

    @Shadow
    public abstract void makeSound(SoundEvent sound);

    @Shadow
    protected abstract float getDamageAfterArmorAbsorb(DamageSource source, float damage);

    @Shadow
    protected abstract float getDamageAfterMagicAbsorb(DamageSource source, float damage);

    /** 原版护甲槽位顺序（与 Player#hurtArmor 一致） */
    @Unique
    private static final EquipmentSlot[] NEWNEW$ARMOR_SLOTS = {
        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};

    /** 原版头盔耐久只作用于头盔槽 */
    @Unique
    private static final EquipmentSlot[] NEWNEW$HELMET_SLOT = {EquipmentSlot.HEAD};

    /** 记录本次 hurt 调用的基线，用于返回时恢复返回值与清理容器 */
    @Inject(method = "hurtServer", at = @At("HEAD"))
    private void newnew$beginInvocation(ServerLevel level, DamageSource source, float amount,
        CallbackInfoReturnable<Boolean> cir) {
        PipelineState.begin(this.damageContainers == null ? 0 : this.damageContainers.size());
    }

    /** 原版 hurt 因事件取消而提前返回时，恢复管线算出的返回值，并清理残留容器 */
    @Inject(method = "hurtServer", at = @At("RETURN"), cancellable = true)
    private void newnew$endInvocation(ServerLevel level, DamageSource source, float amount,
        CallbackInfoReturnable<Boolean> cir) {
        PipelineState.Invocation invocation = PipelineState.end();
        if (invocation == null) {
            return;
        }
        if (this.damageContainers != null) {
            while (this.damageContainers.size() > invocation.containerBaseline) {
                this.damageContainers.pop();
            }
        }
        if (invocation.result != null) {
            cir.setReturnValue(invocation.result);
        }
    }

    @Override
    public boolean newnew$runDamagePipeline(DamageContainer container) {
        boolean result = this.newnew$processDamage(container);
        PipelineState.setCurrentResult(result);
        return result;
    }

    @Override
    public void newnew$hurtArmorItems(DamageSource source, float damage) {
        this.newnew$hurtArmorSlots(source, damage, NEWNEW$ARMOR_SLOTS);
    }

    @Override
    public void newnew$hurtHelmetItem(DamageSource source, float damage) {
        this.newnew$hurtArmorSlots(source, damage, NEWNEW$HELMET_SLOT);
    }

    /**
     * 26.3 的盾牌耐久由 {@code BlocksAttacks} 数据组件负责（原 {@code hurtCurrentlyUsedShield} 已移除），
     * 这里走同一条原版入口。
     */
    @Override
    public void newnew$hurtShield(float blockedDamage) {
        LivingEntity self = (LivingEntity) (Object) this;
        ItemStack blockingItem = self.getItemBlockingWith();
        if (blockingItem == null) {
            return;
        }
        BlocksAttacks blocksAttacks = blockingItem.get(DataComponents.BLOCKS_ATTACKS);
        if (blocksAttacks == null) {
            return;
        }
        blocksAttacks.hurtBlockingItem(self.level(), blockingItem, self, self.getUsedItemHand(), blockedDamage);
    }

    // ---- 临时：原版受伤表现（业务逻辑在 DamagePipeline#playFeedback） ----

    @Override
    public void newnew$markHurt() {
        // 等价于 Entity#markHurt（该方法在父类上，@Shadow 无法跨父类解析）
        this.hurtDuration = 10;
        this.hurtTime = this.hurtDuration;
    }

    @Override
    public void newnew$playHurtSound(DamageSource source) {
        this.playHurtSound(source);
    }

    @Override
    public void newnew$playDeathSound() {
        this.makeSound(this.getDeathSound());
    }

    @Override
    public float newnew$getDamageAfterArmorAbsorb(DamageSource source, float damage) {
        return this.getDamageAfterArmorAbsorb(source, damage);
    }

    @Override
    public float newnew$getDamageAfterMagicAbsorb(DamageSource source, float damage) {
        return this.getDamageAfterMagicAbsorb(source, damage);
    }

    /**
     * 按原版规则扣护甲耐久。
     * <p>1.21.1 的 NeoForge 里 {@code LivingEntity#hurtArmor} 在本体上是空实现（仅 {@code Player} 覆写），
     * 真正的耐久结算在 {@code CommonHooks.onArmorHurt}：它会发出 {@code ArmorHurtEvent}（可取消、可改数值），
     * 再按事件数值对每件装备调用 {@code hurtAndBreak}。因此这里直接走该入口，使任意生物都可用。</p>
     */
    @Unique
    private void newnew$hurtArmorSlots(DamageSource source, float damage, EquipmentSlot[] slots) {
        if (damage <= 0.0F) {
            return;
        }
        int durability = (int) Math.max(1.0F, damage / 4.0F);
        CommonHooks.onArmorHurt(source, slots, (float) durability, (LivingEntity) (Object) this);
    }

    @Unique
    private boolean newnew$processDamage(DamageContainer container) {
        LivingEntity self = (LivingEntity) (Object) this;
        DamageSource source = container.getSource();

        DamageContext ctx = new DamageContext(source, self, container);

        // 插槽 0：内部伤害按标签显式短路，直接扣除生命值
        if (DamagePipeline.routeInnerDamage(ctx)) {
            return ctx.isDealtDamage();
        }

        // 插槽 A：预处理
        DamagePipeline.prepare(ctx);
        container.setNewDamage(ctx.getIncomingDamage());

        // 骨架：受伤时唤醒
        if (self.isSleeping() && !self.level().isClientSide()) {
            self.stopSleeping();
        }
        this.noActionTime = 0;

        // 外部钩子：LivingShieldBlockEvent（格挡结果、格挡量、盾牌耐久消耗同步进上下文）
        if (container.getNewDamage() > 0.0F) {
            LivingShieldBlockEvent shieldEvent =
                CommonHooks.onDamageBlock(self, container, container.getNewDamage(), self.isBlocking());
            if (shieldEvent.getBlocked()) {
                container.setBlockedDamage(shieldEvent);
                ctx.setBlockedDamage(shieldEvent.getBlockedDamage());
                ctx.setShieldDamage(shieldEvent.shieldDamage());
                ctx.setShieldBlocked(true);
                ctx.setShieldFullyBlocked(container.getNewDamage() <= 0.0F);
            }
        }

        // 插槽 B：盾牌行为（音效、反击、表现……）
        DamagePipeline.handleShieldBlock(ctx);

        // 插槽 B2：盾牌耐久（防守方）
        DamagePipeline.applyShieldDurability(ctx);

        // 插槽 C：主伤害计算（护甲/附魔/药水/属性等全部业务）
        ctx.setDamageBeforeMain(container.getNewDamage());
        float amount = DamagePipeline.calculateMainDamage(ctx, container.getNewDamage());
        if (!Float.isFinite(amount) || amount < 0.0F) {
            amount = 0.0F;
        }
        container.setNewDamage(amount);
        ctx.setDamageAfterMain(amount);

        // 骨架：原版无敌帧计时（如需自定义规则，可在此处或拆成插槽）
        boolean applied = true;
        if ((float) self.getInvulnerableTime() > 10.0F && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            if (amount <= this.lastHurt) {
                return false;
            }
            container.setReduction(DamageContainer.Reduction.INVULNERABILITY, this.lastHurt);
            this.lastHurt = amount;
            applied = false;
        } else {
            this.lastHurt = amount;
            self.setInvulnerableTime(container.getPostAttackInvulnerabilityTicks());
        }
        ctx.setApplied(applied);

        // 骨架 + 外部钩子：护甲耐久(ArmorHurtEvent)、LivingDamageEvent.Pre/Post、扣血、onDamageTaken
        this.newnew$applyDamage(ctx);
        ctx.setDealtDamage(!ctx.isShieldFullyBlocked() || container.getNewDamage() > 0.0F);

        // 插槽 F：视听表现与反馈
        DamagePipeline.playFeedback(ctx);

        // 插槽 G：免死；死亡分派（die 内会发出 LivingDeathEvent、掉落与经验）
        if (self.isDeadOrDying() && !DamagePipeline.tryPreventDeath(ctx)) {
            self.die(source);
        }

        // 骨架：仇恨归因（供 AI 反击；如需接管可移入业务插槽）
        Entity attacker = source.getEntity();
        if (attacker != null) {
            if (attacker instanceof LivingEntity livingAttacker
                && !source.is(DamageTypeTags.NO_ANGER)
                && (!source.is(DamageTypes.WIND_CHARGE) || self.getType().getTags()
                    .noneMatch(EntityTypeTags.NO_ANGER_FROM_WIND_CHARGE::equals))) {
                self.setLastHurtByMob(livingAttacker);
            }

            if (attacker instanceof Player player) {
                self.setLastHurtByPlayer(player, 100);
            } else if (attacker instanceof TamableAnimal tamable && tamable.isTame()
                && tamable.getOwner() instanceof Player owner) {
                self.setLastHurtByPlayer(owner, 100);
            }
        }

        if (ctx.isDealtDamage()) {
            this.lastDamageSource = source;
            this.lastDamageStamp = self.level().getGameTime();
        }

        // 插槽 H：伤害后处理（状态效果回调、统计、进度、食物消耗……）
        DamagePipeline.onDamageApplied(ctx);

        return ctx.isDealtDamage();
    }

    /**
     * 落地阶段（对应原版 {@code actuallyHurt}），只保留骨架与外部钩子：
     * 护甲耐久钩子 → 减免写回 → {@code LivingDamageEvent.Pre} → 业务落地插槽 → {@code LivingDamageEvent.Post}。
     */
    @Unique
    private void newnew$applyDamage(DamageContext ctx) {
        LivingEntity self = (LivingEntity) (Object) this;
        DamageSource source = ctx.getSource();
        if (self.level() instanceof ServerLevel serverLevel && self.isInvulnerableTo(serverLevel, source)) {
            return;
        }

        DamageContainer container = ctx.getContainer();

        // 插槽 D：护甲耐久（防守方）
        DamagePipeline.applyArmorDurability(ctx);

        // 兼容钩子：把业务归属的减免写回容器（护甲 → 附魔 → 药水状态），随后才发 Pre
        this.newnew$writeBackReductions(ctx);

        // 外部钩子：LivingDamageEvent.Pre
        float damage = CommonHooks.onLivingDamagePre(self, container);
        ctx.setDamageAfterPre(damage);
        ctx.setFinalDamage(container.getNewDamage());

        // 插槽 E：落地（伤害吸收、扣血、战斗追踪、onDamageTaken……）
        DamagePipeline.applyDamage(ctx, damage);

        // 外部钩子：LivingDamageEvent.Post
        CommonHooks.onLivingDamagePost(self, container);
    }

    /**
     * 把自定义管线的减免写回 {@link DamageContainer}。
     * <p>原版 {@code actuallyHurt} 会把护甲、附魔、药水状态的减免记录到容器，供
     * {@code LivingDamageEvent.Pre/Post} 与其他模组读取。我们用自己的公式替代了这些计算，
     * 因此这里按原版顺序把业务归属的数值补写回去：</p>
     * <ul>
     *     <li>事件监听者读到的 {@code getReduction(ARMOR/ENCHANTMENTS/MOB_EFFECTS)} 保持有效；</li>
     *     <li>通过 {@link DamageContainer#addModifier} 注册的修正函数照常执行，其改动继续影响最终伤害。</li>
     * </ul>
     * <p>业务未归属的剩余减免不会被写回，由自定义规则自身消化。</p>
     */
    @Unique
    private void newnew$writeBackReductions(DamageContext ctx) {
        DamageContainer container = ctx.getContainer();
        DamageSource source = ctx.getSource();

        // 已扣除无敌帧之后的伤害；稍后按“写回减免 + 修正函数的增量”重新设定
        float base = Math.max(0.0F, container.getNewDamage());

        // 物理模型下肉伤可能高于主算前伤害（武器倍率、护甲震动传递），
        // 因此以业务报告的减免为准，只受最终伤害总额约束。
        float armor = Math.min(Math.max(0.0F, ctx.getArmorReduction()), base);
        float available = base - armor;
        float enchantments = Math.min(Math.max(0.0F, ctx.getEnchantmentReduction()), available);
        available -= enchantments;
        float mobEffects = Math.min(Math.max(0.0F, ctx.getMobEffectReduction()), available);

        float requested = 0.0F;
        float recorded = 0.0F;

        // 与原版一致：护甲减免总会被记录一次（即使为 0），好让 addModifier 注册的函数有机会执行
        container.setReduction(DamageContainer.Reduction.ARMOR, armor);
        requested += armor;
        recorded += container.getReduction(DamageContainer.Reduction.ARMOR);

        if (enchantments > 0.0F && !source.is(DamageTypeTags.BYPASSES_ENCHANTMENTS)) {
            container.setReduction(DamageContainer.Reduction.ENCHANTMENTS, enchantments);
            requested += enchantments;
            recorded += container.getReduction(DamageContainer.Reduction.ENCHANTMENTS);
        }

        if (mobEffects > 0.0F && !source.is(DamageTypeTags.BYPASSES_EFFECTS)) {
            container.setReduction(DamageContainer.Reduction.MOB_EFFECTS, mobEffects);
            requested += mobEffects;
            recorded += container.getReduction(DamageContainer.Reduction.MOB_EFFECTS);
        }

        // 修正函数带来的额外增减（正数 = 减免变多，伤害变少）
        float modifierDelta = recorded - requested;
        container.setNewDamage(Math.max(0.0F, base - modifierDelta));
    }
}
