package com.gam0zing.newnew_warlock_core.mob.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.share.ModAttributes;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * 生物属性点分配参数的两层配置。
 * <ul>
 *     <li><b>common</b>：{@code config/newnew_warlock_core/mob_rebuild/mobs.toml}，全局默认值，所有存档共用；</li>
 *     <li><b>server</b>：{@code <存档>/serverconfig/newnew_warlock_core/mob_rebuild/mobs.toml}，世界实际使用的值；
 *     新世界首次启动时直接从 common 复制生成。</li>
 * </ul>
 * <p>每种拥有 {@code newnew_points} 的生物（玩家除外）都有一份数据：四项属性的分配权重与最小值。
 * 增量合并语义：只补齐配置文件里不存在的生物类型，已存在的条目一律不覆盖、不删除；
 * 注册表里被移除的类型也保留在文件里。文件缺失时按默认值重新生成。</p>
 * <p>之所以不用 {@code ModConfigSpec}：条目键由实体注册表动态决定，无法静态声明；且 Spec 的 correction
 * 会删除未知键，与“只增不删”的要求冲突。</p>
 */
public final class MobPointsConfig {

    private static final String DIR = "mob_rebuild";
    private static final String FILE_NAME = "mobs.toml";
    private static final String MOD_DIR = NewNewWarlockCore.MOD_ID;
    private static final int VERSION = 1;

    /** 属性点分配参数：points 基准值、每点难度增幅，以及四项属性的权重与最小值。 */
    public record Entry(
        double pointsBase,
        double pointsGainPerDifficulty,
        double strengthWeight,
        double toughnessWeight,
        double dexterityWeight,
        double spiritualityWeight,
        double strengthMin,
        double toughnessMin,
        double dexterityMin,
        double spiritualityMin) {

        public static final Entry DEFAULT = new Entry(
            0.0D, 1.0D,
            1.0D, 1.0D, 1.0D, 1.0D,
            5.0D, 5.0D, 5.0D, 5.0D);
    }

    @Nullable
    private static CommentedFileConfig common;
    @Nullable
    private static CommentedFileConfig server;
    @Nullable
    private static Path commonPath;

    private static volatile Map<Identifier, Entry> entries = Map.of();

    private MobPointsConfig() {}

    /** 由模组构造器调用：挂上服务端启动/停止的生命周期监听。 */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(MobPointsConfig::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(MobPointsConfig::onServerStopped);
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        load(event.getServer());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        unload();
    }

    /**
     * 游戏启动时初始化：扫描注册表里所有拥有 {@code newnew_points} 的生物（玩家除外），
     * 为 common 与当前存档的 server 表补齐缺失条目，并构建运行时索引。
     */
    public static synchronized void load(MinecraftServer minecraftServer) {
        openCommon();
        List<Identifier> types = collectTypes();

        if (common != null) {
            boolean dirty = ensureVersion(common);
            dirty |= mergeMissing(common, types);
            if (dirty) {
                save(common);
            }
        }

        Path path = minecraftServer.getWorldPath(LevelResource.ROOT)
            .resolve("serverconfig").resolve(MOD_DIR).resolve(DIR).resolve(FILE_NAME);
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path) && commonPath != null && Files.exists(commonPath)) {
                if (common != null) {
                    save(common);
                }
                Files.copy(commonPath, path);
                NewNewWarlockCore.LOGGER.info("[mob_points] 新存档：已用 common 表生成 {}", path);
            }
        } catch (Exception exception) {
            NewNewWarlockCore.LOGGER.error("[mob_points] 无法生成世界配置 {}", path, exception);
        }

        if (server != null) {
            close(server);
        }
        server = open(path);

        if (server != null) {
            boolean dirty = ensureVersion(server);
            dirty |= mergeMissing(server, types);
            if (dirty) {
                save(server);
            }
            rebuildEntries(server, types);
        } else {
            // server 表打不开时退回 common，避免直接退回硬编码默认值。
            rebuildEntries(common, types);
        }
    }

    /** 世界停止时调用：关闭两张表并清空运行时索引，下次启动会重新从磁盘读取。 */
    public static synchronized void unload() {
        if (server != null) {
            close(server);
            server = null;
        }
        if (common != null) {
            close(common);
            common = null;
        }
        entries = Map.of();
    }

    /** 读取某种生物的分配参数；未初始化或没有该类型时返回默认值。 */
    public static Entry get(EntityType<?> type) {
        Entry entry = entries.get(EntityType.getKey(type));
        return entry != null ? entry : Entry.DEFAULT;
    }

    // ============================================================
    // 生成 / 读取
    // ============================================================

    private static void openCommon() {
        if (common != null) {
            return;
        }
        commonPath = FMLPaths.CONFIGDIR.get().resolve(MOD_DIR).resolve(DIR).resolve(FILE_NAME);
        common = open(commonPath);
    }

    /**
     * 收集注册表中所有拥有 {@code newnew_points} 的生物类型（玩家除外）。
     * <p>属性由 {@code AttributeEvt} 在 {@code EntityAttributeModificationEvent} 中挂载，
     * 因此这里直接查询 NeoForge 合并后的默认属性表，能同时覆盖原版与其它模组的生物。</p>
     */
    @SuppressWarnings("unchecked")
    private static List<Identifier> collectTypes() {
        List<Identifier> types = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type == EntityTypes.PLAYER || !DefaultAttributes.hasSupplier(type)) {
                continue;
            }
            AttributeSupplier supplier = DefaultAttributes.getSupplier((EntityType<? extends LivingEntity>) type);
            if (supplier == null || !supplier.hasAttribute(ModAttributes.NEWNEW_POINTS)) {
                continue;
            }
            types.add(EntityType.getKey(type));
        }
        return types;
    }

    /** 只补齐缺失的生物类型与缺失的字段，返回是否有改动。 */
    private static boolean mergeMissing(CommentedFileConfig config, List<Identifier> types) {
        int added = 0;
        for (Identifier id : types) {
            if (!config.contains(entryPath(id))) {
                writeEntry(config, id, Entry.DEFAULT);
                added++;
                continue;
            }
            if (ensureEntryFields(config, id)) {
                added++;
            }
        }
        if (added > 0) {
            NewNewWarlockCore.LOGGER.info("[mob_points] 已为 {} 种生物补齐默认分配参数（来源：{}）",
                added, config == server ? "server" : "common");
        }
        return added > 0;
    }

    /** 给已存在的生物条目补上新增字段，不覆盖任何已有值。 */
    private static boolean ensureEntryFields(CommentedFileConfig config, Identifier id) {
        Entry current = readEntry(config, id);
        List<String> base = entryPath(id);
        boolean changed = false;
        changed |= setDoubleIfMissing(config, append(base, List.of("points", "base")),
            current.pointsBase(), "points 基准值（难度增幅前）");
        changed |= setDoubleIfMissing(config, append(base, List.of("points", "gain-per-difficulty")),
            current.pointsGainPerDifficulty(), "每 1 点难度增加的 points");
        changed |= setDoubleIfMissing(config, append(base, List.of("weight", "strength")),
            current.strengthWeight(), "力量分配权重");
        changed |= setDoubleIfMissing(config, append(base, List.of("weight", "toughness")),
            current.toughnessWeight(), "坚韧分配权重");
        changed |= setDoubleIfMissing(config, append(base, List.of("weight", "dexterity")),
            current.dexterityWeight(), "灵巧分配权重");
        changed |= setDoubleIfMissing(config, append(base, List.of("weight", "spirituality")),
            current.spiritualityWeight(), "灵性分配权重");
        changed |= setDoubleIfMissing(config, append(base, List.of("min", "strength")),
            current.strengthMin(), "力量最小值");
        changed |= setDoubleIfMissing(config, append(base, List.of("min", "toughness")),
            current.toughnessMin(), "坚韧最小值");
        changed |= setDoubleIfMissing(config, append(base, List.of("min", "dexterity")),
            current.dexterityMin(), "灵巧最小值");
        changed |= setDoubleIfMissing(config, append(base, List.of("min", "spirituality")),
            current.spiritualityMin(), "灵性最小值");
        return changed;
    }

    private static void rebuildEntries(@Nullable CommentedFileConfig config, List<Identifier> types) {
        if (config == null) {
            entries = Map.of();
            return;
        }
        Map<Identifier, Entry> table = new HashMap<>(types.size());
        for (Identifier id : types) {
            table.put(id, readEntry(config, id));
        }
        entries = Map.copyOf(table);
    }

    private static Entry readEntry(CommentedFileConfig config, Identifier id) {
        List<String> base = entryPath(id);
        return sanitize(new Entry(
            readDouble(config, append(base, List.of("points", "base")), Entry.DEFAULT.pointsBase()),
            readDouble(config, append(base, List.of("points", "gain-per-difficulty")),
                Entry.DEFAULT.pointsGainPerDifficulty()),
            readDouble(config, append(base, List.of("weight", "strength")), Entry.DEFAULT.strengthWeight()),
            readDouble(config, append(base, List.of("weight", "toughness")), Entry.DEFAULT.toughnessWeight()),
            readDouble(config, append(base, List.of("weight", "dexterity")), Entry.DEFAULT.dexterityWeight()),
            readDouble(config, append(base, List.of("weight", "spirituality")), Entry.DEFAULT.spiritualityWeight()),
            readDouble(config, append(base, List.of("min", "strength")), Entry.DEFAULT.strengthMin()),
            readDouble(config, append(base, List.of("min", "toughness")), Entry.DEFAULT.toughnessMin()),
            readDouble(config, append(base, List.of("min", "dexterity")), Entry.DEFAULT.dexterityMin()),
            readDouble(config, append(base, List.of("min", "spirituality")), Entry.DEFAULT.spiritualityMin())));
    }

    /** 把配置值清洗到合法区间：points 与权重非负，最小值落在属性允许的 [1, 32767] 内。 */
    private static Entry sanitize(Entry entry) {
        return new Entry(
            sanitizeWeight(entry.pointsBase(), Entry.DEFAULT.pointsBase()),
            sanitizeWeight(entry.pointsGainPerDifficulty(), Entry.DEFAULT.pointsGainPerDifficulty()),
            sanitizeWeight(entry.strengthWeight(), Entry.DEFAULT.strengthWeight()),
            sanitizeWeight(entry.toughnessWeight(), Entry.DEFAULT.toughnessWeight()),
            sanitizeWeight(entry.dexterityWeight(), Entry.DEFAULT.dexterityWeight()),
            sanitizeWeight(entry.spiritualityWeight(), Entry.DEFAULT.spiritualityWeight()),
            sanitizeMin(entry.strengthMin(), Entry.DEFAULT.strengthMin()),
            sanitizeMin(entry.toughnessMin(), Entry.DEFAULT.toughnessMin()),
            sanitizeMin(entry.dexterityMin(), Entry.DEFAULT.dexterityMin()),
            sanitizeMin(entry.spiritualityMin(), Entry.DEFAULT.spiritualityMin()));
    }

    private static double sanitizeWeight(double value, double fallback) {
        return Double.isFinite(value) && value >= 0.0D ? Math.min(value, 32767.0D) : fallback;
    }

    private static double sanitizeMin(double value, double fallback) {
        return Double.isFinite(value) ? Math.max(1.0D, Math.min(value, 32767.0D)) : fallback;
    }

    private static void writeEntry(CommentedFileConfig config, Identifier id, Entry entry) {
        List<String> base = entryPath(id);
        config.setComment(base, "生物 " + id + " 的属性点分配参数；留空/删除条目会在下次启动时按默认值重建");

        setDouble(config, append(base, List.of("points", "base")), entry.pointsBase(), "points 基准值（难度增幅前）");
        setDouble(config, append(base, List.of("points", "gain-per-difficulty")), entry.pointsGainPerDifficulty(),
            "每 1 点难度增加的 points");

        setDouble(config, append(base, List.of("weight", "strength")), entry.strengthWeight(), "力量分配权重");
        setDouble(config, append(base, List.of("weight", "toughness")), entry.toughnessWeight(), "坚韧分配权重");
        setDouble(config, append(base, List.of("weight", "dexterity")), entry.dexterityWeight(), "灵巧分配权重");
        setDouble(config, append(base, List.of("weight", "spirituality")), entry.spiritualityWeight(), "灵性分配权重");

        setDouble(config, append(base, List.of("min", "strength")), entry.strengthMin(), "力量最小值");
        setDouble(config, append(base, List.of("min", "toughness")), entry.toughnessMin(), "坚韧最小值");
        setDouble(config, append(base, List.of("min", "dexterity")), entry.dexterityMin(), "灵巧最小值");
        setDouble(config, append(base, List.of("min", "spirituality")), entry.spiritualityMin(), "灵性最小值");
    }

    private static boolean ensureVersion(CommentedFileConfig config) {
        if (config.contains("version")) {
            return false;
        }
        config.set("version", VERSION);
        config.setComment("version", "生物属性点分配表版本（由模组维护，请勿手动修改）");
        return true;
    }

    // ============================================================
    // 工具
    // ============================================================

    private static List<String> entryPath(Identifier id) {
        return List.of("mobs", id.toString());
    }

    private static List<String> append(List<String> base, List<String> suffix) {
        List<String> path = new ArrayList<>(base.size() + suffix.size());
        path.addAll(base);
        path.addAll(suffix);
        return path;
    }

    private static double readDouble(CommentedFileConfig config, List<String> path, double fallback) {
        try {
            Object value = config.getOrElse(path, fallback);
            return value instanceof Number number ? number.doubleValue() : fallback;
        } catch (RuntimeException exception) {
            NewNewWarlockCore.LOGGER.warn("[mob_points] 配置键 {} 不是数字，已回退默认值 {}",
                String.join(".", path), fallback);
            return fallback;
        }
    }

    private static void setDouble(CommentedFileConfig config, List<String> path, double value, String comment) {
        config.set(path, value);
        config.setComment(path, comment);
    }

    private static boolean setDoubleIfMissing(CommentedFileConfig config, List<String> path, double value,
        String comment) {
        if (config.contains(path)) {
            return false;
        }
        setDouble(config, path, value, comment);
        return true;
    }

    @Nullable
    private static CommentedFileConfig open(Path path) {
        try {
            Files.createDirectories(path.getParent());
            CommentedFileConfig config = CommentedFileConfig.builder(path)
                .onFileNotFound(FileNotFoundAction.CREATE_EMPTY)
                .preserveInsertionOrder()
                .build();
            config.load();
            return config;
        } catch (Exception exception) {
            NewNewWarlockCore.LOGGER.error("[mob_points] 无法加载生物属性点分配表 {}", path, exception);
            return null;
        }
    }

    private static void save(CommentedFileConfig config) {
        try {
            config.save();
        } catch (Exception exception) {
            NewNewWarlockCore.LOGGER.error("[mob_points] 无法保存生物属性点分配表", exception);
        }
    }

    private static void close(CommentedFileConfig config) {
        try {
            config.close();
        } catch (Exception exception) {
            NewNewWarlockCore.LOGGER.error("[mob_points] 无法关闭生物属性点分配表", exception);
        }
    }
}
