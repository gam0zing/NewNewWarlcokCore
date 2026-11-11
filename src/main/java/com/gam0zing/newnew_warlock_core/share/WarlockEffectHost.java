package com.gam0zing.newnew_warlock_core.share;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 由 mixin 织入 {@link MobEffectInstance} 的“带来源效果实例”接口。
 * <p>来源生物指“施加这个效果的一方”（投掷药水的生物、喝下/吃下自己效果的使用者等），
 * 可以为 null（原版无来源途径施加的效果保持 null）。</p>
 * <p>来源身份同时以 UUID 形式保存在效果实例上并写入实体存档（服务端 NBT），
 * 读档后由 {@link EffectSources} 懒解析回活体；不进入物品组件、不参与网络同步。</p>
 * <p>已知边界：隐藏效果（{@code hiddenEffect}）只随外层实例持久化一次；来源解析失败时按无来源处理；
 * 完整边界清单见 {@link EffectSources} 的“已知边界”一节。</p>
 * <p>调用方式：{@code ((WarlockEffectHost) instance).newnew$effectSource()}。</p>
 */
public interface WarlockEffectHost {

    /** {@return 该效果实例的来源生物；没有来源或尚未解析时为 null} */
    @Nullable
    LivingEntity newnew$effectSource();

    /**
     * 设置来源生物（同时同步来源 UUID）。
     *
     * @param source 来源生物，可为 null
     */
    void newnew$setEffectSource(@Nullable LivingEntity source);

    /** {@return 该效果实例的来源 UUID；没有来源或未记录时为 null} */
    @Nullable
    UUID newnew$effectSourceUuid();

    /**
     * 只设置来源 UUID（读档时使用，不立即解析）。
     *
     * @param uuid 来源 UUID，可为 null
     */
    void newnew$setEffectSourceUuid(@Nullable UUID uuid);
}
