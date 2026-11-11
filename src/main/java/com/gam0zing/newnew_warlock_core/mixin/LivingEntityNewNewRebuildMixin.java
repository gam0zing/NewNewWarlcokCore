package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;

import com.gam0zing.newnew_warlock_core.mob.config.MobPointsConfig;
import com.gam0zing.newnew_warlock_core.mob.MobDerivedAttributes;
import com.gam0zing.newnew_warlock_core.mob.MobDifficulty;
import com.gam0zing.newnew_warlock_core.mob.NewNewRebuildHost;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

/**
 * 为 {@link LivingEntity} 织入 {@link NewNewRebuildHost#newnewRebuild()}。
 * <p>
 * 只新增方法，不改动原版逻辑。
 * </p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityNewNewRebuildMixin implements NewNewRebuildHost {

    @Override
    public void newnewRebuild() {
        LivingEntity self = (LivingEntity) (Object) this;

        // 重建标记只在服务端写入，客户端不执行
        if (self.level().isClientSide()) {
            return;
        }

        AttributeInstance rebuildAttr = self.getAttribute(ModAttributes.NEWNEW_MOB_REBUILD);
        // 没有重建标识（玩家、傀儡生物等）或已经重建过：直接跳过
        if (rebuildAttr == null || rebuildAttr.getBaseValue() != 0.0D) {
            return;
        }

        rebuildAttr.setBaseValue(1.0D);

        AttributeInstance pointsAttr = self.getAttribute(ModAttributes.NEWNEW_POINTS);
        AttributeInstance strengthAttr = self.getAttribute(ModAttributes.NEWNEW_STRENGTH);
        AttributeInstance toughnessAttr = self.getAttribute(ModAttributes.NEWNEW_TOUGHNESS);
        AttributeInstance dexterityAttr = self.getAttribute(ModAttributes.NEWNEW_DEXTERITY);
        AttributeInstance spiritualityAttr = self.getAttribute(ModAttributes.NEWNEW_SPIRITUALITY);
        if (pointsAttr != null && strengthAttr != null && toughnessAttr != null
                && dexterityAttr != null && spiritualityAttr != null) {
            // 每种生物的分配权重与最小值来自 mob_rebuild/mobs 配置
            MobPointsConfig.Entry mobPoints = MobPointsConfig.get(self.getType());

            double strengthMin = mobPoints.strengthMin();
            double toughnessMin = mobPoints.toughnessMin();
            double dexterityMin = mobPoints.dexterityMin();
            double spiritualityMin = mobPoints.spiritualityMin();

            double strengthWeight = mobPoints.strengthWeight();
            double toughnessWeight = mobPoints.toughnessWeight();
            double dexterityWeight = mobPoints.dexterityWeight();
            double spiritualityWeight = mobPoints.spiritualityWeight();

            // points数据来源为config
            // rebuild过程整体数据流向为："生物points基准-->难度增幅-->正态分布随机化-->四维-->属性和技能（暂不实现）"
            double pointsWithDifficulty = MobDifficulty.scaledPoints(self, mobPoints);

            double pointsByRandom = MobDifficulty.randomize(self, pointsWithDifficulty);

            double strength = strengthAttr.getBaseValue();
            double toughness = toughnessAttr.getBaseValue();
            double dexterity = dexterityAttr.getBaseValue();
            double spirituality = spiritualityAttr.getBaseValue();

            double pointsCanUse = Math.max(
                    pointsByRandom +
                            strength - strengthMin +
                            toughness - toughnessMin +
                            dexterity - dexterityMin +
                            spirituality - spiritualityMin,
                            0.0D);
                            
            double[] finals = { strengthMin, toughnessMin, dexterityMin, spiritualityMin };

            if (pointsCanUse > 0.0D) {
                double totalWeight = strengthWeight +
                        toughnessWeight +
                        dexterityWeight +
                        spiritualityWeight;

                double[] weights = { strengthWeight, toughnessWeight, dexterityWeight, spiritualityWeight };

                int forTimes = 40;

                for (int i = 0; i < forTimes; i++) {
                    double randomWeight = Math.random() * totalWeight;
                    double weightMin = 0;
                    double weightMax = 0;
                    for (int j = 0; j < weights.length; j++) {
                        weightMin = weightMax;
                        weightMax += weights[j];
                        if (randomWeight >= weightMin && randomWeight <= weightMax) {
                            finals[j] += pointsCanUse / forTimes;
                            break;
                        }
                        // 给浮点误差兜底，如果weightMax最终小于totalWeight，那么可能会走到这里，此时视为落在最后一位
                        if (j == weights.length - 1) {
                            finals[j] += pointsCanUse / forTimes;
                        }
                    }
                }
            }

            pointsAttr.setBaseValue(pointsByRandom);
            strengthAttr.setBaseValue(finals[0]);
            toughnessAttr.setBaseValue(finals[1]);
            dexterityAttr.setBaseValue(finals[2]);
            spiritualityAttr.setBaseValue(finals[3]);

            // 四维已写入，按其当前值投影到原版属性；spirituality 暂无目标属性
            // 生命上限在生成后才提高，原本满血的生物补到新上限，已受伤的生物不顺带治疗
            boolean wasFull = self.getHealth() >= self.getMaxHealth() - 1.0E-4D;
            MobDerivedAttributes.sync(self);
            if (self.isAlive()) {
                float maxHealth = self.getMaxHealth();
                if (wasFull || self.getHealth() > maxHealth) {
                    self.setHealth(maxHealth);
                }
            }
        }
    }
}
