package com.gam0zing.newnew_warlock_core.combat.standard;

import java.util.List;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.combat.spine.DamageCategory;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 光明荆棘：被伤害时，以自身 AABB 几何中心为起点，对周围 3×3×3 范围内的生物
 * 造成 {@code 5 × 属性值} 的火焰伤害。
 * <p>伤害来源记为荆棘持有者自身，使用 {@code newnew_warlock_core:light_thorns} 伤害类型
 * （归入 fire 标签，走常规火焰伤害链路）；该类型也是递归保护：由光明荆棘造成的伤害
 * 不会再次触发任何生物的光明荆棘。</p>
 */
public final class LightThorns {

    private static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(
        Registries.DAMAGE_TYPE,
        Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "light_thorns"));

    /** 触发范围：以自身 AABB 几何中心为中心的 3×3×3 立方体 */
    private static final double RANGE = 3.0D;
    /** 每点属性值对应的火焰伤害 */
    private static final float FIRE_DAMAGE_PER_POINT = 5.0F;

    /**
     * 受击结算完成后触发一次光明荆棘。
     *
     * @param self   本次受到伤害的生物
     * @param source 本次伤害来源
     */
    public static void retaliate(LivingEntity self, DamageSource source) {
        if (source.is(DAMAGE_TYPE)) {
            return;
        }
        // 仅物理、魔法分类的伤害能触发
        DamageCategory category = DamageCategory.resolve(source);
        if (category != DamageCategory.PHYSICS && category != DamageCategory.MAGIC) {
            return;
        }

        double value = ModAttributes.getValue(self, ModAttributes.LIGHT_THORNS);
        if (value <= 0.0D) {
            return;
        }
        if (!(self.level() instanceof ServerLevel level)) {
            return;
        }

        float fireDamage = (float) (FIRE_DAMAGE_PER_POINT * value);

        Vec3 center = self.getBoundingBox().getCenter();
        AABB area = AABB.ofSize(center, RANGE, RANGE, RANGE);
        List<LivingEntity> targets = level.getEntitiesOfClass(
            LivingEntity.class, area, entity -> entity != self && entity.isAlive());
        if (targets.isEmpty()) {
            return;
        }

        Holder<DamageType> type = level.damageSources().damageTypes.get(DAMAGE_TYPE).orElse(null);
        if (type == null) {
            NewNewWarlockCore.LOGGER.warn("[light_thorns] 伤害类型 {} 未加载，本次火焰伤害未结算", DAMAGE_TYPE.identifier());
        }

        DamageSource fire = type == null ? null : new DamageSource(type, null, self, center);
        for (LivingEntity target : targets) {
            if (fire != null) {
                target.hurtServer(level, fire, fireDamage);
            }
        }
    }

    private LightThorns() {}
}
