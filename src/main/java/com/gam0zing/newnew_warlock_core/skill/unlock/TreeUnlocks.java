package com.gam0zing.newnew_warlock_core.skill.unlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.skill.command.ParamSpec;
import com.gam0zing.newnew_warlock_core.skill.command.ParamValues;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;

/**
 * 技能树解锁方式注册表。
 * <p>注册值是 {@link TreeUnlockMode}，键是类型 id；数据包只存类型 id 与参数，
 * 幂等解析时按注册表分发。其他模组可以在 {@code RegisterEvent} 阶段追加类型。</p>
 */
public final class TreeUnlocks {

    public static final ResourceKey<Registry<TreeUnlockMode>> REGISTRY_KEY =
        ResourceKey.createRegistryKey(
            Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "tree_unlock_mode"));

    public static final DeferredRegister<TreeUnlockMode> MODES =
        DeferredRegister.create(REGISTRY_KEY, NewNewWarlockCore.MOD_ID);

    public static final Identifier DEFAULT_ID =
        Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "default");
    public static final Identifier LOCKED_ID =
        Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "locked");
    public static final Identifier ORIGIN_ID =
        Identifier.fromNamespaceAndPath(NewNewWarlockCore.MOD_ID, "origin");

    /** 旧格式 {@code default_unlocked: true} 的回退实例 */
    public static final TreeUnlock DEFAULT_UNLOCK = DefaultTreeUnlock.INSTANCE;
    /** 旧格式 {@code default_unlocked: false} 的回退实例 */
    public static final TreeUnlock LOCKED_UNLOCK = LockedTreeUnlock.INSTANCE;

    public static final DeferredHolder<TreeUnlockMode, TreeUnlockMode> DEFAULT =
        MODES.register("default", () -> new SimpleTreeUnlockMode(
            DEFAULT_ID,
            "默认解锁",
            "技能树始终对玩家开放。",
            List.of(),
            values -> DEFAULT_UNLOCK));

    public static final DeferredHolder<TreeUnlockMode, TreeUnlockMode> LOCKED =
        MODES.register("locked", () -> new SimpleTreeUnlockMode(
            LOCKED_ID,
            "不解锁",
            "技能树始终锁定，只能由其他系统主动开放。",
            List.of(),
            values -> LOCKED_UNLOCK));

    public static final DeferredHolder<TreeUnlockMode, TreeUnlockMode> ORIGIN =
        MODES.register("origin", () -> new SimpleTreeUnlockMode(
            ORIGIN_ID,
            "起源解锁",
            "玩家任意一层起源等于参数中的注册名时开放；未安装 NeoOrigins 时保持锁定。",
            List.of(ParamSpec.string("origin", "")),
            values -> {
                String raw = values.getString("origin");
                Identifier origin = Identifier.tryParse(raw);
                if (origin == null) {
                    throw new IllegalArgumentException("origin 不是合法的注册名：" + raw);
                }
                return new OriginTreeUnlock(origin);
            }));

    @Nullable
    private static Registry<TreeUnlockMode> registry;

    /** 在 {@code NewRegistryEvent} 里创建注册表，必须先于 {@link #MODES} 的注册事件 */
    public static void createRegistry(NewRegistryEvent event) {
        registry = event.create(new RegistryBuilder<>(REGISTRY_KEY));
    }

    /** {@return 类型 id 对应的解锁方式；未注册或注册表尚未创建时为 null} */
    @Nullable
    public static TreeUnlockMode modeFor(Identifier id) {
        return registry == null ? null : registry.get(id).map(Holder.Reference::value).orElse(null);
    }

    /** {@return 已注册的全部解锁方式，用于定义表导出} */
    public static List<TreeUnlockMode> all() {
        List<TreeUnlockMode> modes = new ArrayList<>();
        if (registry != null) {
            for (Map.Entry<ResourceKey<TreeUnlockMode>, TreeUnlockMode> entry : registry.entrySet()) {
                modes.add(entry.getValue());
            }
        }
        return modes;
    }

    private TreeUnlocks() {}
}
