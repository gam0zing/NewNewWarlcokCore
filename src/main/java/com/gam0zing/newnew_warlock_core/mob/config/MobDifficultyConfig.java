package com.gam0zing.newnew_warlock_core.mob.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * 生物重建的难度来源配置。
 * <ul>
 *     <li>{@code difficulty_global.toml}：服务器难度总倍率、通用难度阈值、游戏天数、垂直/水平距离、传送门进度；</li>
 *     <li>{@code difficulty_level.toml}：维度附加难度；</li>
 *     <li>{@code difficulty_biome.toml}：群系附加难度。</li>
 * </ul>
 * <p>三份文件都遵循 common + server 两层结构，路径为
 * {@code config/newnew_warlock_core/mob_rebuild/} 与
 * {@code <存档>/serverconfig/newnew_warlock_core/mob_rebuild/}。
 * 新世界首次启动时直接用 common 复制生成 server；server 表缺失的键按默认值补齐。</p>
 * <p>所有数值都是“难度点”，由 {@code MobDifficulty} 汇总。服务器游戏难度（简单/普通/困难）
 * 是一个固定难度偏移，直接加在难度值上，只作用于怪物；玩家侧加成使用不含该偏移的原始难度。
 * 和平固定为 0，不读取配置。</p>
 * <p>通用难度阈值（中/高）供险地矿工、探险家与 HUD 颜色等“难度档位”判定共用。</p>
 */
public final class MobDifficultyConfig {

    private static final String MOD_DIR = NewNewWarlockCore.MOD_ID;
    private static final String DIR = "mob_rebuild";
    private static final int VERSION = 1;

    private static final FileSet GLOBAL =
        new FileSet("difficulty_global.toml", MobDifficultyConfig::ensureGlobalDefaults);
    private static final FileSet LEVEL =
        new FileSet("difficulty_level.toml", MobDifficultyConfig::ensureLevelDefaults);
    private static final FileSet BIOME =
        new FileSet("difficulty_biome.toml", MobDifficultyConfig::ensureBiomeDefaults);

    private MobDifficultyConfig() {}

    /** 由模组构造器调用：挂上服务端启动/停止的生命周期监听。 */
    public static void register() {
        NeoForge.EVENT_BUS.addListener(MobDifficultyConfig::onServerAboutToStart);
        NeoForge.EVENT_BUS.addListener(MobDifficultyConfig::onServerStarted);
        NeoForge.EVENT_BUS.addListener(MobDifficultyConfig::onServerStopped);
    }

    private static void onServerAboutToStart(ServerAboutToStartEvent event) {
        MinecraftServer server = event.getServer();
        GLOBAL.load(server);
        LEVEL.load(server);
        BIOME.load(server);
    }

    /**
     * 服务端启动完成后再扫描群系：此时各个维度已经创建、群系标签已经加载，
     * 才能按实际群系源采样出现频率并判断维度归属。
     */
    private static void onServerStarted(ServerStartedEvent event) {
        Map<Identifier, String> comments = BiomeDifficultyScanner.scan(event.getServer());
        for (CommentedFileConfig config : BIOME.configs()) {
            if (mergeBiomeComments(config, comments)) {
                save(config);
            }
        }
    }

    /** 只补齐缺失的群系条目，已有条目的数值保持不动；注释始终刷新为当前扫描结果。 */
    private static boolean mergeBiomeComments(CommentedFileConfig config, Map<Identifier, String> comments) {
        boolean changed = false;
        for (Map.Entry<Identifier, String> entry : comments.entrySet()) {
            List<String> path = List.of("biomes", entry.getKey().toString());
            if (!config.contains(path)) {
                config.set(path, BiomeDifficultyScanner.defaultDifficulty(entry.getKey()));
                config.setComment(path, entry.getValue());
                changed = true;
            } else if (!entry.getValue().equals(config.getComment(path))) {
                config.setComment(path, entry.getValue());
                changed = true;
            }
        }
        return changed;
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        GLOBAL.unload();
        LEVEL.unload();
        BIOME.unload();
    }

    // ============================================================
    // 难度读取
    // ============================================================

    /** 服务器游戏难度带来的固定难度偏移；和平固定为 0。 */
    public static double difficultyOffset(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL -> 0.0D;
            case EASY -> finite(GLOBAL.read(List.of("server_difficulty", "easy"), -12.0D), -12.0D);
            case NORMAL -> finite(GLOBAL.read(List.of("server_difficulty", "normal"), 0.0D), 0.0D);
            case HARD -> finite(GLOBAL.read(List.of("server_difficulty", "hard"), 12.0D), 12.0D);
        };
    }

    /** 中等难度阈值：难度达到该值时，档位类判定取中档（如险地矿工概率减半、HUD 黄字）。 */
    public static double mediumThreshold() {
        return nonNegative(GLOBAL.read(List.of("threshold", "medium"), 20.0D), 20.0D);
    }

    /** 高难度阈值：难度达到该值时，档位类判定取高档（如险地矿工概率全值、HUD 红字）。 */
    public static double highThreshold() {
        double high = nonNegative(GLOBAL.read(List.of("threshold", "high"), 40.0D), 40.0D);
        return Math.max(mediumThreshold(), high);
    }

    /** 游戏天数带来的难度。 */
    public static double dayBonus(double days) {
        double gain = nonNegative(GLOBAL.read(List.of("day", "gain-per-day"), 0.025D), 0.025D);
        double max = nonNegative(GLOBAL.read(List.of("day", "max"), 5.0D), 5.0D);
        return clamp(days * gain, max);
    }

    /** 相对海平面的垂直距离带来的难度（每格 y 轴）。 */
    public static double verticalBonus(double y, int seaLevel) {
        double gain = nonNegative(GLOBAL.read(List.of("vertical", "gain-per-block"), 0.1D), 0.1D);
        double max = nonNegative(GLOBAL.read(List.of("vertical", "max"), 10.0D), 10.0D);
        return clamp(Math.abs(y - seaLevel) * gain, max);
    }

    /** 相对世界出生点的水平距离带来的难度（每 100 格）。 */
    public static double horizontalBonus(double dx, double dz) {
        double blocks = Math.sqrt(dx * dx + dz * dz) / 100.0D;
        double gain = nonNegative(GLOBAL.read(List.of("horizontal", "gain-per-100-blocks"), 0.25D), 0.25D);
        double max = nonNegative(GLOBAL.read(List.of("horizontal", "max"), 5.0D), 5.0D);
        return clamp(blocks * gain, max);
    }

    /** 传送门进度带来的难度（主世界专用）。 */
    public static double portalBonus(boolean netherOpened, boolean endOpened) {
        double value = 0.0D;
        if (netherOpened) {
            value += nonNegative(GLOBAL.read(List.of("portal", "nether-opened"), 8.0D), 8.0D);
        }
        if (endOpened) {
            value += nonNegative(GLOBAL.read(List.of("portal", "end-opened"), 12.0D), 12.0D);
        }
        return value;
    }

    /** 维度附加难度；未列出的维度使用 default.difficulty。 */
    public static double dimensionBonus(Identifier dimension) {
        double fallback = nonNegative(LEVEL.read(List.of("default", "difficulty"), 0.0D), 0.0D);
        return nonNegative(LEVEL.read(List.of("dimensions", dimension.toString()), fallback), fallback);
    }

    /** 群系附加难度；可为负数。未列出的群系使用 default.difficulty。 */
    public static double biomeBonus(@Nullable Identifier biome) {
        double fallback = finite(BIOME.read(List.of("default", "difficulty"), 0.0D), 0.0D);
        if (biome == null) {
            return fallback;
        }
        return finite(BIOME.read(List.of("biomes", biome.toString()), fallback), fallback);
    }

    // ============================================================
    // 默认值
    // ============================================================

    private static boolean ensureGlobalDefaults(CommentedFileConfig config) {
        boolean changed = ensureVersion(config, "生物重建全局难度表版本（由模组维护，请勿手动修改）");
        changed |= setDefault(config, List.of("server_difficulty", "easy"), -12.0D,
            "简单难度固定偏移（加在难度值上，仅作用于怪物）");
        changed |= setDefault(config, List.of("server_difficulty", "normal"), 0.0D,
            "普通难度固定偏移（加在难度值上，仅作用于怪物）");
        changed |= setDefault(config, List.of("server_difficulty", "hard"), 12.0D,
            "困难难度固定偏移（加在难度值上，仅作用于怪物）");
        changed |= setDefault(config, List.of("threshold", "medium"), 20.0D,
            "中等难度阈值（险地矿工/探险家概率减半、HUD 黄字）");
        changed |= setDefault(config, List.of("threshold", "high"), 40.0D,
            "高难度阈值（险地矿工/探险家概率全值、HUD 红字）");
        changed |= setDefault(config, List.of("day", "gain-per-day"), 0.025D, "每经过 1 个游戏日增加的难度");
        changed |= setDefault(config, List.of("day", "max"), 5.0D, "游戏天数难度上限");
        changed |= setDefault(config, List.of("vertical", "gain-per-block"), 0.1D,
            "相对海平面每格 y 轴增加的难度");
        changed |= setDefault(config, List.of("vertical", "max"), 10.0D, "垂直距离难度上限");
        changed |= setDefault(config, List.of("horizontal", "gain-per-100-blocks"), 0.25D,
            "相对世界出生点每 100 格直线距离增加的难度");
        changed |= setDefault(config, List.of("horizontal", "max"), 5.0D, "水平距离难度上限");
        changed |= setDefault(config, List.of("portal", "nether-opened"), 8.0D,
            "主世界专用：本存档进入过下界后增加的难度");
        changed |= setDefault(config, List.of("portal", "end-opened"), 12.0D,
            "主世界专用：本存档进入过末地后增加的难度");
        return changed;
    }

    private static boolean ensureLevelDefaults(CommentedFileConfig config) {
        boolean changed = ensureVersion(config, "生物重建维度难度表版本（由模组维护，请勿手动修改）");
        changed |= setDefault(config, List.of("default", "difficulty"), 0.0D,
            "未在下表列出的维度使用的难度");
        changed |= setDefault(config, List.of("dimensions", "minecraft:the_nether"), 15.0D, "下界附加难度");
        changed |= setDefault(config, List.of("dimensions", "minecraft:the_end"), 25.0D, "末地附加难度");
        return changed;
    }

    private static boolean ensureBiomeDefaults(CommentedFileConfig config) {
        boolean changed = ensureVersion(config, "生物重建群系难度表版本（由模组维护，请勿手动修改）");
        changed |= setDefault(config, List.of("default", "difficulty"), 0.0D,
            "未在 [biomes] 里列出的群系使用的难度");
        return changed;
    }

    // ============================================================
    // 工具
    // ============================================================

    private static double clamp(double value, double max) {
        return Math.max(0.0D, Math.min(value, max));
    }

    private static double nonNegative(double value, double fallback) {
        return Double.isFinite(value) && value >= 0.0D ? value : fallback;
    }

    private static double finite(double value, double fallback) {
        return Double.isFinite(value) ? value : fallback;
    }

    private interface DefaultsWriter {
        boolean ensure(CommentedFileConfig config);
    }

    /** 一组 common + server 两层配置文件。 */
    private static final class FileSet {

        private final String fileName;
        private final DefaultsWriter defaults;
        @Nullable
        private CommentedFileConfig common;
        @Nullable
        private CommentedFileConfig server;

        FileSet(String fileName, DefaultsWriter defaults) {
            this.fileName = fileName;
            this.defaults = defaults;
        }

        void load(MinecraftServer minecraftServer) {
            Path commonPath = FMLPaths.CONFIGDIR.get().resolve(MOD_DIR).resolve(DIR).resolve(fileName);
            if (common == null) {
                common = open(commonPath);
            }
            if (common != null && defaults.ensure(common)) {
                save(common);
            }

            Path serverPath = minecraftServer.getWorldPath(LevelResource.ROOT)
                .resolve("serverconfig").resolve(MOD_DIR).resolve(DIR).resolve(fileName);
            try {
                Files.createDirectories(serverPath.getParent());
                if (!Files.exists(serverPath) && Files.exists(commonPath)) {
                    if (common != null) {
                        save(common);
                    }
                    Files.copy(commonPath, serverPath);
                }
            } catch (Exception exception) {
                NewNewWarlockCore.LOGGER.error("[mob_difficulty] 无法生成世界配置 {}", serverPath, exception);
            }

            close(server);
            server = open(serverPath);
            if (server != null && defaults.ensure(server)) {
                save(server);
            }
        }

        void unload() {
            close(server);
            server = null;
            close(common);
            common = null;
        }

        /** 当前已打开的 common/server 表，用于启动完成后的群系扫描写入。 */
        List<CommentedFileConfig> configs() {
            List<CommentedFileConfig> configs = new ArrayList<>(2);
            if (common != null) {
                configs.add(common);
            }
            if (server != null) {
                configs.add(server);
            }
            return configs;
        }

        double read(List<String> path, double fallback) {
            CommentedFileConfig config = server != null ? server : common;
            if (config == null) {
                return fallback;
            }
            try {
                Object value = config.getOrElse(path, fallback);
                return value instanceof Number number ? number.doubleValue() : fallback;
            } catch (RuntimeException exception) {
                NewNewWarlockCore.LOGGER.warn("[mob_difficulty] 配置键 {} 不是数字，已回退默认值 {}",
                    String.join(".", path), fallback);
                return fallback;
            }
        }
    }

    private static boolean ensureVersion(CommentedFileConfig config, String comment) {
        if (config.contains("version")) {
            return false;
        }
        config.set("version", VERSION);
        config.setComment("version", comment);
        return true;
    }

    private static boolean setDefault(CommentedFileConfig config, List<String> path, double value, String comment) {
        if (config.contains(path)) {
            return false;
        }
        config.set(path, value);
        config.setComment(path, comment);
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
            NewNewWarlockCore.LOGGER.error("[mob_difficulty] 无法加载难度配置 {}", path, exception);
            return null;
        }
    }

    private static void save(CommentedFileConfig config) {
        try {
            config.save();
        } catch (Exception exception) {
            NewNewWarlockCore.LOGGER.error("[mob_difficulty] 无法保存难度配置", exception);
        }
    }

    private static void close(@Nullable CommentedFileConfig config) {
        if (config == null) {
            return;
        }
        try {
            config.close();
        } catch (Exception exception) {
            NewNewWarlockCore.LOGGER.error("[mob_difficulty] 无法关闭难度配置", exception);
        }
    }
}
