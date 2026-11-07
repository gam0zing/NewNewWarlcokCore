package com.gam0zing.newnew_warlock_core.event;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.combat.damage.DamageCategory;
import com.gam0zing.newnew_warlock_core.combat.damage.DamageScaling;
import com.gam0zing.newnew_warlock_core.combat.damage.WarlockDamageHost;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

@EventBusSubscriber(modid = NewNewWarlockCore.MOD_ID)
public class LivingDamageEvt {

    /**
     * 伤害管线入口。
     * <p>在这里取消 {@link LivingIncomingDamageEvent}，原版 {@code LivingEntity#hurt} 会在该事件处
     * 直接返回，之后的一切原版计算（盾牌、无敌帧、护甲、附魔、抗性、吸收、击退、死亡等）都不会执行。
     * 随后由 {@link LivingEntityDamagePipelineMixin} 提供的管线重写这些表现，并重新发出被跳过的
     * NeoForge 事件（LivingShieldBlockEvent、ArmorHurtEvent、LivingDamageEvent.Pre/Post、
     * LivingKnockBackEvent、LivingUseTotemEvent、LivingDeathEvent 等），其他模组的监听不会失效。</p>
     * <p>优先级 LOWEST：让其他模组先决定是否取消；我们只在没人取消时接管。</p>
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingIncomingDamage(LivingIncomingDamageEvent event) {
        // 已有模组取消：尊重其决定，原版同样不会造成伤害
        if (event.isCanceled()) {
            return;
        }

        // TODO: 业务区域 —— 可在此处做全局拦截（例如无敌/剧情保护/伤害来源过滤）

        // 断流：阻止原版 hurt 中该事件之后的所有调用
        event.setCanceled(true);

        LivingEntity target = event.getEntity();
        if (!(target instanceof WarlockDamageHost host)) {
            NewNewWarlockCore.LOGGER.error("伤害管线接管失败：{} 未应用 LivingEntityDamagePipelineMixin", target);
            return;
        }

        try {
            host.newnew$runDamagePipeline(event.getContainer());
        } catch (Throwable throwable) {
            NewNewWarlockCore.LOGGER.error("伤害管线执行异常，本次伤害已丢弃", throwable);
        }
    }

    /**
     * 伤害强度修正：按攻击者持有的 {@code damage_scale}（通用）与分类伤害强度（{@code <tag>_damage_scale}）修正伤害。
     * <p>优先级 LOW：排在其它模组对 {@code LivingDamageEvent.Pre} 的常规修正之后、本模组限伤（LOWEST）之前。</p>
     * <p>说明：这些属性是“造成伤害”的属性，因此只读取伤害来源生物（{@code DamageSource#getEntity()}）；
     * 环境伤害等没有攻击者的伤害不受影响。</p>
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        DamageSource source = event.getSource();
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;
        float original = event.getNewDamage();
        float scaled = DamageScaling.apply(attacker, DamageCategory.resolve(source), original);
        if (scaled != original) {
            event.setNewDamage(scaled);
        }
    }

    /**
     * 限伤（业务区域）。
     * <p>优先级 LOWEST：排在所有模组的 Pre 修正与上面的伤害强度之后，保证最终数值可控。</p>
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLivingDamagePreLimit(LivingDamageEvent.Pre event) {
        // TODO: 业务区域 —— 限伤
        //   例如：单次伤害上限、最大生命值百分比上限、阶段血条保护、Boss 免伤等
    }
}
