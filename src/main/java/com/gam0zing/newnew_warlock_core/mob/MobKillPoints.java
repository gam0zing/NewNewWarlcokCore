package com.gam0zing.newnew_warlock_core.mob;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;
import com.mojang.serialization.Codec;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * 玩家击杀使用 points 的生物所累计的属性点加成，随玩家 NBT 持久化。
 * <p>累计值存在带 {@code copyOnDeath()} 的附件里，因此死亡重生不会丢失；
 * 属性上的加成仍然只由一个 {@code newnew_warlock_core:mob_kill_points} 修饰符承担，
 * 登录与重生时按附件里的累计值重建该修饰符（与技能树重放命令的做法一致）。</p>
 */
public final class MobKillPoints {

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
        DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, NewNewWarlockCore.MOD_ID);

    /** 玩家击杀累计获得的属性点；默认 0，死亡保留。 */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Double>> KILL_POINTS =
        ATTACHMENTS.register("mob_kill_points", () -> AttachmentType.builder(() -> 0.0D)
            .serialize(Codec.DOUBLE.fieldOf("points"))
            .copyOnDeath()
            .build());

    /** 玩家身上管理击杀加成的唯一修饰符 */
    public static final Identifier MODIFIER_ID =
        Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "mob_kill_points");

    private MobKillPoints() {}

    public static void register(IEventBus modEventBus) {
        ATTACHMENTS.register(modEventBus);
    }

    /** {@return 玩家累计获得的击杀点数} */
    public static double get(Player player) {
        Double value = player.getExistingDataOrNull(KILL_POINTS.get());
        return value != null ? value : 0.0D;
    }

    /** 累加击杀点数并返回累加后的总值。 */
    public static double add(Player player, double amount) {
        double total = Math.max(get(player) + amount, 0.0D);
        player.setData(KILL_POINTS.get(), total);
        return total;
    }

    /** 用累计值重建属性修饰符；累计为 0 时移除修饰符。 */
    public static void applyModifier(ServerPlayer player) {
        AttributeInstance attribute = player.getAttribute(ModAttributes.NEWNEW_POINTS);
        if (attribute == null) {
            return;
        }
        double total = get(player);
        if (total <= 0.0D) {
            attribute.removeModifier(MODIFIER_ID);
            return;
        }
        attribute.addOrReplacePermanentModifier(
            new AttributeModifier(MODIFIER_ID, total, AttributeModifier.Operation.ADD_VALUE));
    }

    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            applyModifier(player);
        }
    }

    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // 重生会换成新的玩家实体，永久属性修饰符不会随之复制，需要按持久化值重新挂上
            applyModifier(player);
        }
    }
}
