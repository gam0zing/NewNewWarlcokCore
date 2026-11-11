package com.gam0zing.newnew_warlock_core.mob;

/**
 * 由 mixin 织入 {@link net.minecraft.world.entity.LivingEntity} 的“生物重建”接口。
 * <p>调用方式：{@code ((NewNewRebuildHost) livingEntity).newnewRebuild()}。</p>
 * <p>没有 {@code newnew_mob_rebuild} 属性的实体（玩家、傀儡生物等）调用时为空操作。</p>
 */
public interface NewNewRebuildHost {
    /**
     * 检查自身的重建标识：{@code newnew_mob_rebuild} 的 base 值为 0 时，立即置为 1，并进入 rebuild 逻辑。
     * <p>标识本身会随属性 NBT 持久化，因此同一只生物只会重建一次；跨维度、区块重载再次加入世界时自动跳过。</p>
     * <p>仅在服务端生效，客户端调用为空操作。</p>
     */
    void newnewRebuild();
}
