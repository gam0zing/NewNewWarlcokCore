package com.gam0zing.newnew_warlock_core.combat.standard;

import com.gam0zing.newnew_warlock_core.combat.spine.DamageContext;
import com.gam0zing.newnew_warlock_core.combat.spine.WarlockDamageHost;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.common.extensions.ILivingEntityExtension;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

/**
 * 原版伤害机制。
 * <p>这里放的是原版 {@code LivingEntity#hurtServer} / {@code actuallyHurt} / {@code knockback} 里
 * 与具体伤害类型无关的部分：护甲与附魔减伤、无敌帧、伤害吸收与扣血、击退位移。</p>
 * <p>处理器需要原版行为时显式调用这里的静态方法；需要按伤害类型区分时，在处理器的对应阶段里
 * 调用不同的工具方法或自行实现，而不是在工具类里加分支。</p>
 */
public final class VanillaDamageUtils {

    /** 受击无敌帧长度（刻）：命中后这段时间内只接受更高伤害 */
    public static final int IFRAME_TICKS = 10;

    /** 插槽 A：原版没有与伤害类型无关的预处理。 */
    public static void prepare(DamageContext ctx) {
    }

    /** 插槽 B：原版格挡表现（音效、缴械等），当前尚未接入。 */
    public static void onShieldBlock(DamageContext ctx) {
    }

    /** 插槽 B2：原版盾牌耐久，当前尚未接入。 */
    public static void onShieldDurability(DamageContext ctx) {
    }

    /**
     * 插槽 C：原版减伤链路——护甲 + 韧性 → 保护附魔 / 抗性提升。
     * <p>{@code newnew$getDamageAfterArmorAbsorb} 内部还会按原版规则消耗护甲耐久并发出
     * {@code ArmorHurtEvent}，因此插槽 E 不需要再扣一次。</p>
     */
    public static float calculate(DamageContext ctx, float damage) {
        WarlockDamageHost host = host(ctx);
        float afterArmor = host.newnew$getDamageAfterArmorAbsorb(ctx.getSource(), damage);
        return host.newnew$getDamageAfterMagicAbsorb(ctx.getSource(), afterArmor);
    }

    /**
     * 插槽 D：原版无敌帧。
     * <p>命中后 {@link #IFRAME_TICKS} 刻内只接受更高伤害：更高的伤害会被 {@code INVULNERABILITY} 减免一部分，
     * 更低或相等的伤害直接丢弃。状态存放在受击实体上（骨架宿主接口），因此同一生物的所有伤害共享同一窗口。</p>
     *
     * @return {@code true} 继续落地；{@code false} 丢弃本次伤害
     */
    public static boolean onGate(DamageContext ctx) {
        LivingEntity target = ctx.getTarget();
        DamageSource source = ctx.getSource();
        WarlockDamageHost host = host(ctx);

        float amount = Math.max(0.0F, ctx.getDamageAfterMain());
        long elapsed = target.level().getGameTime() - host.newnew$getLastHitGameTime();
        ctx.setTicksSinceLastHit(elapsed);

        if (elapsed < IFRAME_TICKS && !source.is(DamageTypeTags.BYPASSES_COOLDOWN)) {
            float previous = host.newnew$getLastHurt();
            if (amount <= previous) {
                ctx.setApplied(false);
                return false;
            }
            ctx.getContainer().setReduction(DamageContainer.Reduction.INVULNERABILITY, previous);
            host.newnew$setLastHurt(amount);
            ctx.setApplied(false);
            return true;
        }

        host.newnew$setLastHurt(amount);
        host.newnew$setLastHitGameTime(target.level().getGameTime());
        ctx.setApplied(true);
        return true;
    }

    /**
     * 插槽 E：原版护甲耐久。
     * <p>原版减伤链路（{@link #calculate}）在内部已经扣过护甲耐久并发出 {@code ArmorHurtEvent}，
     * 因此这里默认什么都不做。只有不经过原版减伤链路的处理器才需要自行结算。</p>
     */
    public static void onArmorDurability(DamageContext ctx) {
    }

    /**
     * 插槽 F：原版落地——伤害吸收、扣血、战斗记录与受击回调。
     * <p>击退、受击效果等与伤害类型相关的后续行为由处理器在调用本方法之后自行处理。</p>
     *
     * @return 实际从生命值中扣除的伤害（不含伤害吸收承担的部分）
     */
    public static float apply(DamageContext ctx, float damage) {
        LivingEntity target = ctx.getTarget();
        DamageContainer container = ctx.getContainer();

        float remaining = Math.max(0.0F, damage);

        // 伤害吸收
        float absorbed = 0.0F;
        if (remaining > 0.0F && target.getAbsorptionAmount() > 0.0F) {
            absorbed = Math.min(remaining, target.getAbsorptionAmount());
            container.setReduction(DamageContainer.Reduction.ABSORPTION, absorbed);
            target.setAbsorptionAmount(target.getAbsorptionAmount() - absorbed);
            remaining -= absorbed;
        }
        ctx.setAbsorbedDamage(absorbed);

        if (remaining <= 0.0F) {
            return 0.0F;
        }

        // 扣血与战斗记录
        float healthBefore = target.getHealth();
        target.getCombatTracker().recordDamage(ctx.getSource(), remaining);
        target.setHealth(healthBefore - remaining);
        target.gameEvent(GameEvent.ENTITY_DAMAGE);
        ((ILivingEntityExtension) target).onDamageTaken(container);
        return remaining;
    }

    /**
     * 插槽 F（位移部分）：按 26.3 原版 {@code LivingEntity#knockback} 的位移规则推动目标。
     * <p>调用方已经重发过 {@code LivingKnockBackEvent} 并判断了取消，这里拿到的是最终强度。</p>
     * <p>击退强度按目标的 AABB 体积缩放：体积越大的生物越难被击退。
     * （后续 Body 侧的重量数值成熟后，体积将替换为身体重量。）</p>
     */
    public static void handleKnockback(LivingEntity target, LivingKnockBackEvent event) {
        double strength = event.getStrength() * (1.0D - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        if (strength <= 0.0D) {
            return;
        }
        strength /= boundingBoxVolume(target);
        if (strength <= 0.0D) {
            return;
        }
        double x = event.getRatioX();
        double z = event.getRatioZ();
        while (x * x + z * z < 1.0E-5F) {
            x = (target.getRandom().nextDouble() - target.getRandom().nextDouble()) * 0.01D;
            z = (target.getRandom().nextDouble() - target.getRandom().nextDouble()) * 0.01D;
        }
        target.needsSync = true;
        Vec3 movement = target.getDeltaMovement();
        Vec3 push = new Vec3(x, 0.0D, z).normalize().scale(strength);
        target.setDeltaMovement(
            movement.x / 2.0D - push.x,
            target.onGround() ? Math.min(0.4D, movement.y / 2.0D + strength) : movement.y,
            movement.z / 2.0D - push.z);
    }

    /** {@return 目标 AABB 的体积；作为击退重量的临时近似，防 0 兜底} */
    public static double boundingBoxVolume(LivingEntity entity) {
        var box = entity.getBoundingBox();
        return Math.max(1.0E-6D, box.getXsize() * box.getYsize() * box.getZsize());
    }

    private static WarlockDamageHost host(DamageContext ctx) {
        return (WarlockDamageHost) ctx.getTarget();
    }

    private VanillaDamageUtils() {}
}
