package com.gam0zing.newnew_warlock_core.skill.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.RecordBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * 点数分配命令类型注册表。
 * <p>注册值是 {@link PointAllocationCommandType}，键是命令类型 id；持久化时只存注册键，
 * 反序列化时按注册键分发。其他模组可以在 {@code RegisterEvent} 阶段往 {@link #COMMANDS} 追加自己的类型。</p>
 */
public final class PointAllocationCommands {

    /** 命令类型注册表键：{@code newnew_warlock_core:point_allocation_command} */
    public static final ResourceKey<Registry<PointAllocationCommandType<?>>> REGISTRY_KEY =
        ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "point_allocation_command"));

    public static final DeferredRegister<PointAllocationCommandType<?>> COMMANDS =
        DeferredRegister.create(REGISTRY_KEY, NewNewWarlockCore.MOD_ID);

    /** 属性命令：消耗点数修饰属性 */
    public static final DeferredHolder<PointAllocationCommandType<?>, PointAllocationCommandType<AttributeCommand>> ATTRIBUTE =
        COMMANDS.register(AttributeCommand.TYPE_ID.getPath(), () -> AttributeCommand.TYPE);

    /**
     * 命令实例的多态 codec：写入 {@code type} 注册键 + 该类型自己的字段。
     * <p>未知类型返回 {@link DataResult#error}，按“解析失败”处理，不做跳过。</p>
     */
    public static final Codec<PointAllocationCommand> CODEC = new Codec<>() {
        @Override
        public <T> DataResult<Pair<PointAllocationCommand, T>> decode(DynamicOps<T> ops, T input) {
            return ops.getMap(input).flatMap(map -> {
                T typeTag = map.get("type");
                if (typeTag == null) {
                    return DataResult.error(() -> "Missing required field 'type' for a point allocation command");
                }
                return Identifier.CODEC.parse(ops, typeTag).flatMap(id -> {
                    MapCodec<? extends PointAllocationCommand> codec = codecFor(id);
                    if (codec == null) {
                        return DataResult.error(() -> "Unknown point allocation command type: " + id);
                    }
                    return codec.codec()
                        .decode(ops, input)
                        .map(pair -> Pair.of((PointAllocationCommand) pair.getFirst(), pair.getSecond()));
                });
            });
        }

        @Override
        public <T> DataResult<T> encode(PointAllocationCommand input, DynamicOps<T> ops, T prefix) {
            MapCodec<? extends PointAllocationCommand> codec = codecFor(input.type());
            if (codec == null) {
                return DataResult.error(() -> "Unregistered point allocation command type: " + input.type());
            }
            RecordBuilder<T> builder = ops.mapBuilder();
            builder.add("type", ops.createString(input.type().toString()));
            return encodeWith(codec, input, ops, builder).build(prefix);
        }
    };

    @Nullable
    private static Registry<PointAllocationCommandType<?>> registry;

    /** 在 {@code NewRegistryEvent} 里创建注册表，必须先于 {@link #COMMANDS} 的注册事件 */
    public static void createRegistry(NewRegistryEvent event) {
        registry = event.create(new RegistryBuilder<>(REGISTRY_KEY));
    }

    /** {@return 注册键对应的命令类型；未注册或注册表尚未创建时为 null} */
    @Nullable
    public static PointAllocationCommandType<?> typeFor(Identifier id) {
        return registry == null ? null : registry.get(id).map(Holder.Reference::value).orElse(null);
    }

    /** {@return 注册键对应的命令实例 codec；未注册时为 null} */
    @Nullable
    public static MapCodec<? extends PointAllocationCommand> codecFor(Identifier id) {
        PointAllocationCommandType<?> type = typeFor(id);
        return type == null ? null : type.codec();
    }

    /** {@return 已注册的全部命令类型，按注册键排序；用于定义表导出} */
    public static List<PointAllocationCommandType<?>> all() {
        List<PointAllocationCommandType<?>> types = new ArrayList<>();
        if (registry != null) {
            for (Map.Entry<ResourceKey<PointAllocationCommandType<?>>, PointAllocationCommandType<?>> entry
                : registry.entrySet()) {
                types.add(entry.getValue());
            }
        }
        return types;
    }

    @SuppressWarnings("unchecked")
    private static <T> RecordBuilder<T> encodeWith(
        MapCodec<? extends PointAllocationCommand> codec,
        PointAllocationCommand input,
        DynamicOps<T> ops,
        RecordBuilder<T> builder) {
        return ((MapCodec<PointAllocationCommand>) codec).encode(input, ops, builder);
    }

    private PointAllocationCommands() {}
}
