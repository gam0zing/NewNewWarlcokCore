package com.gam0zing.newnew_warlock_core.combat.damage;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.combat.physics.PhysicsContext;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.damagesource.DamageContainer;

/**
 * 一次伤害流程的上下文，贯穿自定义管线的所有阶段。
 * <p>该对象只在单次伤害流程内有效，由管线创建并传递，不对外暴露引用。</p>
 */
public final class DamageContext {
    private final DamageSource source;
    private final LivingEntity target;
    private final DamageContainer container;
    private final DamageCategory category;

    /** 进入管线时的伤害值（对玩家而言已经过原版难度缩放，见 DamagePipeline#prepare 的 TODO） */
    private float incomingDamage;
    /** 盾牌实际格挡掉的伤害 */
    private float blockedDamage;
    /** 本次格挡需要消耗的盾牌耐久值（LivingShieldBlockEvent#shieldDamage） */
    private float shieldDamage;
    private boolean shieldBlocked;
    /** 盾牌是否完全挡下（格挡后伤害为 0） */
    private boolean shieldFullyBlocked;
    /** 本次是否真正进入落地阶段（无敌帧不足时为 false） */
    private boolean applied;
    /** LivingDamageEvent.Pre 之后的伤害（供 Post 阶段或业务逻辑参考） */
    private float damageAfterPre;
    /** 吸收承担掉的伤害 */
    private float absorbedDamage;
    /** 最终写入血量的伤害 */
    private float finalDamage;
    /** 本次是否真正造成伤害（hurt 返回值与后续表现分派都用它） */
    private boolean dealtDamage;
    /** 主伤害计算之前的伤害（已扣除盾牌格挡，供减免归属与写回使用） */
    private float damageBeforeMain;
    /** 主伤害计算之后的伤害（尚未扣除无敌帧与吸收） */
    private float damageAfterMain;

    /** 归属为“护甲减免”的数值，会写回 DamageContainer.Reduction.ARMOR */
    private float armorReduction;
    /** 归属为“附魔减免”的数值，会写回 DamageContainer.Reduction.ENCHANTMENTS */
    private float enchantmentReduction;
    /** 归属为“药水/状态减免”的数值，会写回 DamageContainer.Reduction.MOB_EFFECTS */
    private float mobEffectReduction;

    /** 物理伤害业务上下文；不进入物理模型时为 null */
    @Nullable
    private PhysicsContext physics;

    public DamageContext(DamageSource source, LivingEntity target, DamageContainer container) {
        this.source = source;
        this.target = target;
        this.container = container;
        this.category = DamageCategory.resolve(source);
        this.incomingDamage = container.getNewDamage();
    }

    public DamageSource getSource() {
        return this.source;
    }

    public LivingEntity getTarget() {
        return this.target;
    }

    public DamageContainer getContainer() {
        return this.container;
    }

    public DamageCategory getCategory() {
        return this.category;
    }

    /** {@return 攻击者（伤害来源的生物实体），环境伤害等无来源时为 null} */
    public LivingEntity getAttacker() {
        return this.source.getEntity() instanceof LivingEntity living ? living : null;
    }

    public float getIncomingDamage() {
        return this.incomingDamage;
    }

    public void setIncomingDamage(float incomingDamage) {
        this.incomingDamage = incomingDamage;
    }

    public float getBlockedDamage() {
        return this.blockedDamage;
    }

    public void setBlockedDamage(float blockedDamage) {
        this.blockedDamage = blockedDamage;
    }

    public float getShieldDamage() {
        return this.shieldDamage;
    }

    public void setShieldDamage(float shieldDamage) {
        this.shieldDamage = shieldDamage;
    }

    public boolean isShieldBlocked() {
        return this.shieldBlocked;
    }

    public void setShieldBlocked(boolean shieldBlocked) {
        this.shieldBlocked = shieldBlocked;
    }

    public boolean isShieldFullyBlocked() {
        return this.shieldFullyBlocked;
    }

    public void setShieldFullyBlocked(boolean shieldFullyBlocked) {
        this.shieldFullyBlocked = shieldFullyBlocked;
    }

    public boolean isApplied() {
        return this.applied;
    }

    public void setApplied(boolean applied) {
        this.applied = applied;
    }

    public float getAbsorbedDamage() {
        return this.absorbedDamage;
    }

    public void setAbsorbedDamage(float absorbedDamage) {
        this.absorbedDamage = absorbedDamage;
    }

    public float getFinalDamage() {
        return this.finalDamage;
    }

    public void setFinalDamage(float finalDamage) {
        this.finalDamage = finalDamage;
    }

    public boolean isDealtDamage() {
        return this.dealtDamage;
    }

    public void setDealtDamage(boolean dealtDamage) {
        this.dealtDamage = dealtDamage;
    }

    public float getDamageAfterPre() {
        return this.damageAfterPre;
    }

    public void setDamageAfterPre(float damageAfterPre) {
        this.damageAfterPre = damageAfterPre;
    }

    public float getDamageBeforeMain() {
        return this.damageBeforeMain;
    }

    public void setDamageBeforeMain(float damageBeforeMain) {
        this.damageBeforeMain = damageBeforeMain;
    }

    public float getDamageAfterMain() {
        return this.damageAfterMain;
    }

    public void setDamageAfterMain(float damageAfterMain) {
        this.damageAfterMain = damageAfterMain;
    }

    public float getArmorReduction() {
        return this.armorReduction;
    }

    /** 业务区域：本次伤害中由护甲吸收掉的部分（会写回容器，供事件读取） */
    public void setArmorReduction(float armorReduction) {
        this.armorReduction = armorReduction;
    }

    public float getEnchantmentReduction() {
        return this.enchantmentReduction;
    }

    /** 业务区域：本次伤害中由附魔吸收掉的部分（会写回容器，供事件读取） */
    public void setEnchantmentReduction(float enchantmentReduction) {
        this.enchantmentReduction = enchantmentReduction;
    }

    public float getMobEffectReduction() {
        return this.mobEffectReduction;
    }

    /** 业务区域：本次伤害中由药水/状态效果吸收掉的部分（会写回容器，供事件读取） */
    public void setMobEffectReduction(float mobEffectReduction) {
        this.mobEffectReduction = mobEffectReduction;
    }

    /** {@return 物理伤害业务上下文；不进入物理模型时为 null} */
    @Nullable
    public PhysicsContext getPhysics() {
        return this.physics;
    }

    public void setPhysics(@Nullable PhysicsContext physics) {
        this.physics = physics;
    }
}
