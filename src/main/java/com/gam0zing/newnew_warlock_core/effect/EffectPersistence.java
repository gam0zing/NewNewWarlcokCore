package com.gam0zing.newnew_warlock_core.effect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;

import com.gam0zing.newnew_warlock_core.share.EffectSources;
import com.gam0zing.newnew_warlock_core.share.EffectStack;
import com.gam0zing.newnew_warlock_core.share.MobEffectStackRegistry;
import com.gam0zing.newnew_warlock_core.share.WarlockEffectHost;

/**
 * 状态效果附加数据的实体存档适配。
 *
 * <p>26.3 起 {@link MobEffectInstance} 不再提供 {@code save/load}，实体存档改由
 * {@code ValueOutput/ValueInput} + Codec 驱动，效果实例 NBT 中无法再塞入模组字段。
 * 因此把“来源 UUID”与“容器子实例列表”独立成一份附带数据，由
 * {@code LivingEntity} 的存档注入器在主效果数据之后读写。</p>
 *
 * <p>只保存有附加信息的效果；普通效果不写入任何数据，保持与原版一致的存档体积。</p>
 */
public final class EffectPersistence {

    /** 实体存档里保存效果附加数据的键 */
    public static final String KEY = "newnew:effect_extras";

    private EffectPersistence() {}

    /** 一个子实例及其来源 UUID */
    public record SavedChild(MobEffectInstance instance, @Nullable UUID source) {

        public static final Codec<SavedChild> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MobEffectInstance.CODEC.fieldOf("instance").forGetter(SavedChild::instance),
            Codec.STRING.optionalFieldOf("source")
                .forGetter(child -> Optional.ofNullable(child.source()).map(UUID::toString))
        ).apply(instance, (effectInstance, source) ->
            new SavedChild(effectInstance, source.map(UUID::fromString).orElse(null))));
    }

    /** 一个效果槽位的附加数据 */
    public record SavedEffect(Holder<MobEffect> effect, @Nullable UUID source, List<SavedChild> children) {

        public static final Codec<SavedEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            MobEffect.CODEC.fieldOf("effect").forGetter(SavedEffect::effect),
            Codec.STRING.optionalFieldOf("source")
                .forGetter(saved -> Optional.ofNullable(saved.source()).map(UUID::toString)),
            SavedChild.CODEC.listOf().optionalFieldOf("children", List.of()).forGetter(SavedEffect::children)
        ).apply(instance, (effect, source, children) ->
            new SavedEffect(effect, source.map(UUID::fromString).orElse(null), children)));
    }

    public static final Codec<List<SavedEffect>> CODEC = SavedEffect.CODEC.listOf();

    /** 收集当前生效效果里需要额外持久化的部分；没有附加信息时返回空列表 */
    public static List<SavedEffect> collect(Map<Holder<MobEffect>, MobEffectInstance> active) {
        List<SavedEffect> saved = new ArrayList<>();
        for (MobEffectInstance instance : active.values()) {
            UUID source = EffectSources.sourceUuidOf(instance);

            List<SavedChild> children = List.of();
            if (instance instanceof EffectStack stack) {
                List<SavedChild> list = new ArrayList<>(stack.children().size());
                for (MobEffectInstance child : stack.children()) {
                    list.add(new SavedChild(child, EffectSources.sourceUuidOf(child)));
                }
                children = list;
            }

            if (source == null && children.isEmpty()) {
                continue;
            }
            saved.add(new SavedEffect(instance.getEffect(), source, children));
        }
        return saved;
    }

    /** 读档后把来源 UUID 与容器子实例写回已加载的效果槽位 */
    public static void apply(Map<Holder<MobEffect>, MobEffectInstance> active, List<SavedEffect> saved) {
        for (SavedEffect entry : saved) {
            MobEffectInstance current = active.get(entry.effect());
            if (current == null) {
                continue;
            }

            if (entry.source() != null && current instanceof WarlockEffectHost host) {
                host.newnew$setEffectSourceUuid(entry.source());
            }

            if (entry.children().isEmpty()) {
                continue;
            }

            List<MobEffectInstance> children = new ArrayList<>(entry.children().size());
            for (SavedChild child : entry.children()) {
                MobEffectInstance instance = child.instance();
                if (instance instanceof WarlockEffectHost host && child.source() != null) {
                    host.newnew$setEffectSourceUuid(child.source());
                }
                children.add(instance);
            }

            MobEffectInstance rebuilt = MobEffectStackRegistry.tryRebuild(current, children);
            if (rebuilt != null) {
                active.put(entry.effect(), rebuilt);
            }
        }
    }
}
