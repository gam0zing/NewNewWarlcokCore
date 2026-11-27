package com.gam0zing.newnew_warlock_core.origin.content.power;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.Vec3;

/**
 * 【天敌】：苦力怕不会把猫锁定为目标，并主动远离猫。
 * <p>被猫攻击过的苦力怕不受限制，可以正常反击与自爆；
 * 「不主动索敌」由 {@code OriginTraits#onChangeTarget} 在设置目标阶段拦截。</p>
 */
final class CreeperFearPower extends TraitPower {

    @Override
    public void onTick(ServerPlayer player, NoConfig config) {
        if (player.tickCount % 10 != 0) {
            return;
        }
        double range = OriginTuning.CAT_CREEPER_FEAR_RANGE;
        for (Creeper creeper : player.level().getEntitiesOfClass(Creeper.class,
            player.getBoundingBox().inflate(range))) {
            if (creeper.getLastHurtByMob() == player) {
                continue;
            }
            if (creeper.getTarget() == player) {
                creeper.setTarget(null);
                creeper.getNavigation().stop();
            }
            if (creeper.position().distanceToSqr(player.position()) > range * range) {
                creeper.getNavigation().stop();
                continue;
            }
            Vec3 away = creeper.position().subtract(player.position());
            if (away.lengthSqr() < 1.0E-4D) {
                away = new Vec3(0.0D, 0.0D, 1.0D);
            }
            Vec3 target = creeper.position().add(away.normalize().scale(range));
            creeper.getNavigation().moveTo(target.x, target.y, target.z, 1.2D);
        }
    }
}
