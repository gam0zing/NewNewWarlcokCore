package com.gam0zing.newnew_warlock_core.origin.content.power;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import com.cyberday1.neoorigins.api.NeoOriginsAPI;
import com.cyberday1.neoorigins.api.power.EmbeddedPower;
import com.cyberday1.neoorigins.api.power.PowerConfiguration;
import com.cyberday1.neoorigins.api.power.PowerHolder;
import com.cyberday1.neoorigins.api.power.PowerType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

/**
 * 复合能力：一个可见的 power id 下挂载多个子能力。
 * <p>子能力定义写在 {@code powers} 对象里，由 NeoOrigins 的内嵌能力机制解析，
 * 实际 id 为 {@code 父id/_embedded/slot}，不会作为独立技能出现在菜单里。</p>
 *
 * <p>本类型只负责把生命周期转发给子能力；类型查询通过
 * {@code OriginPowerQuery} 递归展开。</p>
 */
public final class CompositePower extends PowerType<CompositePower.Config> {

    /** 子能力所在字段 */
    public static final String POWERS_FIELD = "powers";
    private static final Pattern SLOT_PATTERN = Pattern.compile("[a-z0-9_.-]+");

    @Override
    public Codec<Config> codec() {
        return Config.CODEC;
    }

    /** {@return 子能力定义列表；配置不是本类型时返回空列表} */
    public List<EmbeddedPower> children(PowerConfiguration config) {
        return config instanceof Config composite
            ? List.copyOf(composite.powers().values())
            : List.of();
    }

    @Override
    public void onGranted(ServerPlayer player, Config config) {
        forEachChild(config, holder -> holder.onGranted(player));
    }

    @Override
    public void onRevoked(ServerPlayer player, Config config) {
        forEachChild(config, holder -> holder.onRevoked(player));
    }

    @Override
    public void onLogin(ServerPlayer player, Config config) {
        forEachChild(config, holder -> holder.onLogin(player));
    }

    @Override
    public void onRespawn(ServerPlayer player, Config config) {
        forEachChild(config, holder -> holder.onRespawn(player));
    }

    @Override
    public void onTick(ServerPlayer player, Config config) {
        forEachChild(config, holder -> holder.onTick(player));
    }

    @Override
    public void onActivated(ServerPlayer player, Config config) {
        forEachChild(config, holder -> holder.onActivated(player));
    }

    @Override
    public void onHit(ServerPlayer player, Config config, float amount) {
        forEachChild(config, holder -> holder.onHit(player, amount));
    }

    @Override
    public void onKill(ServerPlayer player, Config config, LivingEntity target) {
        forEachChild(config, holder -> holder.onKill(player, target));
    }

    @Override
    public boolean isActivePower(Config config) {
        final boolean[] active = {false};
        forEachChild(config, holder -> {
            if (childIsActive(holder)) {
                active[0] = true;
            }
        });
        return active[0];
    }

    @Override
    public boolean occupiesHotkeySlot(Config config) {
        final boolean[] occupies = {false};
        forEachChild(config, holder -> {
            if (childOccupiesHotkey(holder)) {
                occupies[0] = true;
            }
        });
        return occupies[0];
    }

    @Override
    public Set<String> capabilities(Config config) {
        Set<String> result = new HashSet<>();
        forEachChild(config, holder -> result.addAll(childStaticCapabilities(holder)));
        return result;
    }

    @Override
    public Set<String> capabilities(ServerPlayer player, Config config) {
        Set<String> result = new HashSet<>();
        forEachChild(config, holder -> result.addAll(holder.capabilities(player)));
        return result;
    }

    @Override
    public boolean appliesToMobs(Config config) {
        final boolean[] applies = {false};
        forEachChild(config, holder -> {
            if (holder.appliesToMobs()) {
                applies[0] = true;
            }
        });
        return applies[0];
    }

    @Override
    public void applyToMob(LivingEntity mob, Config config) {
        forEachChild(config, holder -> holder.applyToMob(mob));
    }

    @Override
    public void removeFromMob(LivingEntity mob, Config config) {
        forEachChild(config, holder -> holder.removeFromMob(mob));
    }

    private static void forEachChild(Config config, Consumer<PowerHolder<?>> action) {
        for (EmbeddedPower child : config.powers().values()) {
            try {
                action.accept(child.holder());
            } catch (RuntimeException ignored) {
                // 数据重载的瞬间子能力可能尚未注册，跳过即可。
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static boolean childIsActive(PowerHolder<?> holder) {
        return ((PowerType) holder.type()).isActivePower(holder.config());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static boolean childOccupiesHotkey(PowerHolder<?> holder) {
        return ((PowerType) holder.type()).occupiesHotkeySlot(holder.config());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Set<String> childStaticCapabilities(PowerHolder<?> holder) {
        return ((PowerType) holder.type()).capabilities(holder.config());
    }

    /** 复合能力的配置：slot -> 子能力定义 */
    public record Config(Map<String, EmbeddedPower> powers) implements PowerConfiguration {

        public Config {
            powers = Collections.unmodifiableMap(new LinkedHashMap<>(powers));
        }

        public static final Codec<Config> CODEC = new Codec<>() {

            @Override
            public <T> DataResult<Pair<Config, T>> decode(DynamicOps<T> ops, T input) {
                JsonElement root = ops.convertTo(JsonOps.INSTANCE, input);
                if (!root.isJsonObject()) {
                    return DataResult.error(() -> "composite power must be a JSON object");
                }
                JsonElement childElement = root.getAsJsonObject().get(POWERS_FIELD);
                if (childElement == null || !childElement.isJsonObject()) {
                    return DataResult.error(() -> "composite power requires an object field 'powers'");
                }
                Map<String, EmbeddedPower> children = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> entry : childElement.getAsJsonObject().entrySet()) {
                    String slot = entry.getKey();
                    if (!SLOT_PATTERN.matcher(slot).matches()) {
                        return DataResult.error(() -> "invalid composite slot: " + slot);
                    }
                    DataResult<EmbeddedPower> parsed =
                        NeoOriginsAPI.powerCodec(slot).parse(JsonOps.INSTANCE, entry.getValue());
                    if (parsed.error().isPresent()) {
                        return DataResult.error(() -> "composite slot '" + slot + "': "
                            + parsed.error().get().message());
                    }
                    EmbeddedPower child = parsed.result().orElse(null);
                    if (child == null) {
                        return DataResult.error(() -> "composite slot '" + slot + "' produced no power");
                    }
                    children.put(slot, child);
                }
                if (children.isEmpty()) {
                    return DataResult.error(() -> "composite power requires at least one child in 'powers'");
                }
                return DataResult.success(Pair.of(new Config(children), ops.empty()));
            }

            @Override
            public <T> DataResult<T> encode(Config input, DynamicOps<T> ops, T prefix) {
                JsonObject children = new JsonObject();
                for (Map.Entry<String, EmbeddedPower> entry : input.powers().entrySet()) {
                    DataResult<JsonElement> encoded = NeoOriginsAPI.powerCodec(entry.getKey())
                        .encodeStart(JsonOps.INSTANCE, entry.getValue());
                    if (encoded.error().isPresent()) {
                        return DataResult.error(() -> "composite slot '" + entry.getKey() + "': "
                            + encoded.error().get().message());
                    }
                    children.add(entry.getKey(), encoded.result().orElseThrow());
                }
                JsonObject root = new JsonObject();
                root.add(POWERS_FIELD, children);
                return DataResult.success(JsonOps.INSTANCE.convertTo(ops, root));
            }
        };
    }
}
