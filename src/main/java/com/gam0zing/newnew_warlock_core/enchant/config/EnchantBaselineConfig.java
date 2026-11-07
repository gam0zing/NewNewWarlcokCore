package com.gam0zing.newnew_warlock_core.enchant.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.file.FileNotFoundAction;
import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.enchant.EnchantBaseline;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.fml.loading.FMLPaths;

/**
 * 附魔基准表的两层配置管理。
 * <ul>
 *     <li><b>common</b>：{@code config/newnew_warlock_core/enchantment_baseline.toml}，全局默认值，所有存档共用；</li>
 *     <li><b>server</b>：{@code <存档>/serverconfig/newnew_warlock_core/enchantment_baseline.toml}，世界实际使用的值；
 *     新世界首次启动时直接从 common 复制生成。</li>
 * </ul>
 * <p><b>增量合并语义</b>：每次启动或数据包重载只补齐「配置文件里不存在」的附魔条目，已存在的条目（包括玩家改过的值、
 * 未知的额外键、被删除但未注销的条目）一律不覆盖、不删除。删除文件或手动清空条目后，下次加载会按注册表重新生成。</p>
 * <p>之所以不用 {@code ModConfigSpec}：附魔是数据包注册表，COMMON 配置加载时还不存在；且 Spec 的键必须静态声明，
 * 无法按注册表动态追加，它的 correction 也会删除未知键。</p>
 */
public final class EnchantBaselineConfig {

    private static final String FILE_NAME = "enchantment_baseline.toml";
    private static final String MOD_DIR = NewNewWarlockCore.MOD_ID;
    private static final int VERSION = 1;

    /** 最大等级硬上限（与 {@code EnchantmentDefinition} / {@code ItemEnchantments} 一致）。 */
    private static final int LEVEL_CAP = EnchantBaseline.LEVEL_HARD_CAP;
    /** 原版 weight 编解码器的合法区间。 */
    private static final int WEIGHT_CAP = 1024;
    /** 附魔台 power 上限：防止极端配置让费用公式里的 {@code power * 2} / {@code nextInt(power + 1)} 溢出。 */
    private static final int POWER_CAP = EnchantBaseline.LEVEL_HARD_CAP;

    @Nullable
    private static CommentedFileConfig common;
    @Nullable
    private static CommentedFileConfig server;
    @Nullable
    private static Path commonPath;
    @Nullable
    private static Path serverPath;

    private static volatile GlobalSettings settings = GlobalSettings.DEFAULT;

    /**
     * 客户端是否已经应用了服务端下发的基准快照。
     * <p>多人游戏里客户端本地只有 common 表，数值可能与服务端世界不一致；收到快照后必须以服务端为准，
     * 之后客户端的 {@link #refresh(RegistryAccess, boolean)}（切维度、重载资源）不能再被 common 覆盖。
     * 断开连接时由客户端事件清除，回到用 common 兜底的状态。</p>
     */
    private static volatile boolean clientSnapshot;

    private EnchantBaselineConfig() {}

    /** 与附魔无关的全局标量（附魔台 power、铁砧费用上限等）。 */
    public record GlobalSettings(int maxPower, double xpCostScale, int extraPower, int anvilMaxCost) {
        public static final GlobalSettings DEFAULT = new GlobalSettings(15, 1.0D, 0, 40);
    }

    /**
     * 模组初始化时调用：加载 common 表并补齐全局默认键。
     * <p>此时附魔注册表尚未加载，不能写附魔条目；附魔条目在 {@link #refresh(RegistryAccess, boolean)} 中补齐。</p>
     */
    public static synchronized void init() {
        if (common != null) {
            return;
        }

        commonPath = FMLPaths.CONFIGDIR.get().resolve(MOD_DIR).resolve(FILE_NAME);
        common = open(commonPath);
        if (common != null && writeGlobalDefaults(common)) {
            save(common);
        }
        settings = common == null ? GlobalSettings.DEFAULT : readSettings(common);
    }

    /**
     * 依据当前注册表补齐缺失条目并重建 {@link EnchantBaseline} 索引。
     * <p>服务端资源重载（{@link net.neoforged.neoforge.event.AddReloadListenerEvent}）、标签重绑
     * （{@link net.neoforged.neoforge.event.TagsUpdatedEvent}）、世界启动、客户端世界载入时调用。</p>
     *
     * @param tagsAvailable 当前注册表里的附魔标签是否已经绑定。{@code false} 时只读取已有配置、
     *                      不生成新条目：数据包加载（{@code AddReloadListenerEvent}）阶段标签还没绑定，
     *                      此时生成会把 {@code in_table}/{@code tradeable} 等开关全部写成 false，
     *                      后续的 {@code TagsUpdatedEvent} 会补上正确的生成与索引重建。
     */
    public static synchronized void refresh(RegistryAccess registries, boolean tagsAvailable) {
        if (clientSnapshot && server == null) {
            // 多人游戏客户端：服务端快照才是权威数据，不能被本地 common 覆盖。
            return;
        }

        CommentedFileConfig config = active();
        if (config == null) {
            NewNewWarlockCore.LOGGER.warn("[enchant] 附魔基准配置未初始化，本次使用原版默认值");
            return;
        }

        int added = 0;
        boolean dirty = false;
        if (tagsAvailable) {
            added = mergeEnchantments(config, registries);
            dirty |= added > 0;
            if (!isTagsSynced(config)) {
                // 旧文件（生成时标签尚未绑定，开关全为 false）一次性按标签修正开关，数值保持不变。
                dirty |= syncTagFlags(config, registries);
                setTagsSynced(config, true);
                dirty = true;
            }
        }
        if (dirty) {
            save(config);
        }

        settings = readSettings(config);
        EnchantBaseline.rebuild(registries, readEntries(config, registries));

        Registry<Enchantment> registry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        NewNewWarlockCore.LOGGER.info(
            "[enchant] 基准表已刷新：新增 {} 条，标签已绑定={}，注册表共 {} 条，来源={}",
            added, tagsAvailable, registry.keySet().size(), server != null ? "server" : "common");
    }

    /**
     * 世界启动时调用：定位该存档的 server 表；不存在时从 common 复制生成（新世界种子）。
     */
    public static synchronized void loadServer(MinecraftServer minecraftServer) {
        if (server != null) {
            return;
        }

        Path worldDir = minecraftServer.getWorldPath(LevelResource.ROOT);
        Path path = worldDir.resolve("serverconfig").resolve(MOD_DIR).resolve(FILE_NAME);
        try {
            Files.createDirectories(path.getParent());
            if (!Files.exists(path) && commonPath != null && Files.exists(commonPath)) {
                if (common != null) {
                    save(common);
                }
                Files.copy(commonPath, path);
                NewNewWarlockCore.LOGGER.info("[enchant] 新存档：已用 common 表生成 {}", path);
            }

            server = open(path);
            serverPath = path;
        } catch (Exception exception) {
            NewNewWarlockCore.LOGGER.error("[enchant] 无法加载世界基准表 {}", path, exception);
            server = null;
            serverPath = null;
        }
    }

    /** 世界停止时调用：关闭 server 表并清空索引，客户端后续载入世界时改用 common。 */
    public static synchronized void unloadServer() {
        if (server != null) {
            close(server);
            server = null;
        }
        serverPath = null;
        clientSnapshot = false;
        EnchantBaseline.clear();
        settings = common == null ? GlobalSettings.DEFAULT : readSettings(common);
    }

    /**
     * 从磁盘重新读取当前生效的表（{@code /newnew enchant baseline reload}）。
     * <p>手动编辑 TOML 后调用；读取完会再次补齐缺失条目并重建索引。</p>
     */
    public static synchronized void reloadFromDisk(RegistryAccess registries) {
        if (commonPath != null) {
            if (common != null) {
                close(common);
            }
            common = open(commonPath);
        }
        if (serverPath != null) {
            if (server != null) {
                close(server);
            }
            server = open(serverPath);
        }

        CommentedFileConfig config = active();
        if (config != null && writeGlobalDefaults(config)) {
            save(config);
        }
        clientSnapshot = false;
        refresh(registries, true);
    }

    /**
     * 强制按当前注册表重写当前生效表的所有附魔条目（{@code /newnew enchant baseline regenerate}）。
     * <p>会覆盖玩家对该表做过的所有条目修改；只在玩家显式执行命令时发生。</p>
     *
     * @return 重写后的条目数量
     */
    public static synchronized int regenerate(RegistryAccess registries) {
        CommentedFileConfig config = active();
        if (config == null) {
            return 0;
        }

        if (config.contains(List.of("enchantments"))) {
            config.remove(List.of("enchantments"));
        }
        mergeEnchantments(config, registries);
        setTagsSynced(config, true);
        save(config);

        settings = readSettings(config);
        clientSnapshot = false;
        EnchantBaseline.rebuild(registries, readEntries(config, registries));
        return EnchantBaseline.entries().size();
    }

    /**
     * 应用服务端下发的基准快照（方案 B，客户端专用）。
     * <p>之后客户端 {@link #refresh(RegistryAccess, boolean)} 不会再用 common 覆盖，直到断开连接。
     * 服务端缺失、客户端多出的附魔会被忽略。</p>
     * <p>空快照视为「服务端没有可用基准表」，直接回退到本地 common 表：若此时锁定 {@code clientSnapshot}，
     * 客户端会一直用空表直到重登。收到的数值一律按本地规则清洗，避免畸形数据包把费率/上限写成非法值。</p>
     */
    public static synchronized void applySnapshot(
        RegistryAccess registries,
        List<EnchantBaseline.Entry> entries,
        GlobalSettings snapshotSettings) {
        if (entries.isEmpty()) {
            // 服务端没有可用基准表：解锁快照标志并用本地 common 表重建，避免客户端停在空表上。
            NewNewWarlockCore.LOGGER.warn("[enchant] 收到空的基准快照，已忽略并回退到本地表");
            clientSnapshot = false;
            refresh(registries, true);
            return;
        }

        Map<Identifier, EnchantBaseline.Entry> table = new HashMap<>(entries.size());
        for (EnchantBaseline.Entry entry : entries) {
            EnchantBaseline.Entry sanitized = sanitize(entry);
            table.put(sanitized.id(), sanitized);
        }

        settings = sanitize(snapshotSettings);
        clientSnapshot = true;
        EnchantBaseline.rebuild(registries, table);
        NewNewWarlockCore.LOGGER.info("[enchant] 已应用服务端基准快照：{} 条", table.size());
    }

    /** 断开连接时调用：丢弃服务端快照，回到 common 兜底。 */
    public static synchronized void clearClientSnapshot() {
        if (!clientSnapshot) {
            return;
        }
        revertToLocalBaseline();
    }

    /** 丢弃服务端快照并回到本地 common 表（断开连接、收到空快照时使用）。 */
    private static void revertToLocalBaseline() {
        clientSnapshot = false;
        EnchantBaseline.clear();
        settings = common == null ? GlobalSettings.DEFAULT : readSettings(common);
    }

    /** 命令用：当前生效路径与来源说明。 */
    public static String describeSource() {
        if (serverPath != null) {
            return "server（" + serverPath + "）";
        }
        if (commonPath != null) {
            return "common（" + commonPath + "）";
        }
        return "未加载";
    }

    public static GlobalSettings settings() {
        return settings;
    }

    @Nullable
    public static CommentedFileConfig active() {
        return server != null ? server : common;
    }

    @Nullable
    public static Path commonPath() {
        return commonPath;
    }

    @Nullable
    public static Path serverPath() {
        return serverPath;
    }

    // ============================================================
    // 生成 / 读取
    // ============================================================

    /**
     * 只补齐缺失的附魔条目，返回新增数量。
     */
    private static int mergeEnchantments(CommentedFileConfig config, RegistryAccess registries) {
        Registry<Enchantment> registry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        int added = 0;
        for (Holder.Reference<Enchantment> holder : registry.listElements().toList()) {
            Identifier id = holder.key().identifier();
            List<String> base = entryPath(id);
            if (config.contains(base)) {
                continue;
            }

            writeEntry(config, EnchantBaseline.fromDefinition(holder));
            added++;
        }
        return added;
    }

    private static Map<Identifier, EnchantBaseline.Entry> readEntries(
        CommentedFileConfig config, RegistryAccess registries) {
        Registry<Enchantment> registry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        Map<Identifier, EnchantBaseline.Entry> entries = new HashMap<>(registry.keySet().size());
        for (Holder.Reference<Enchantment> holder : registry.listElements().toList()) {
            EnchantBaseline.Entry fallback = EnchantBaseline.fromDefinition(holder);
            entries.put(fallback.id(), readEntry(config, fallback));
        }
        return entries;
    }

    /**
     * 把已存在条目的五个标签开关按当前注册表标签修正，返回是否有改动。
     * <p>只改开关，不动玩家配置的数值；只会在 {@code meta.tags_synced = false} 时执行一次，
     * 用于修复「数据包加载阶段标签未绑定导致开关被写成 false」的旧文件。</p>
     */
    private static boolean syncTagFlags(CommentedFileConfig config, RegistryAccess registries) {
        Registry<Enchantment> registry = registries.lookupOrThrow(Registries.ENCHANTMENT);
        boolean changed = false;
        for (Holder.Reference<Enchantment> holder : registry.listElements().toList()) {
            List<String> base = entryPath(holder.key().identifier());
            if (!config.contains(base)) {
                continue;
            }

            EnchantBaseline.Entry defaults = EnchantBaseline.fromDefinition(holder);
            changed |= setBooleanIfDifferent(config, append(base, "in_table"), defaults.inTable());
            changed |= setBooleanIfDifferent(config, append(base, "treasure"), defaults.treasure());
            changed |= setBooleanIfDifferent(config, append(base, "tradeable"), defaults.tradeable());
            changed |= setBooleanIfDifferent(config, append(base, "on_random_loot"), defaults.onRandomLoot());
            changed |= setBooleanIfDifferent(config, append(base, "on_traded_equipment"), defaults.onTradedEquipment());
        }
        return changed;
    }

    private static boolean setBooleanIfDifferent(CommentedFileConfig config, List<String> path, boolean value) {
        if (readBoolean(config, path, value) == value) {
            return false;
        }
        config.set(path, value);
        return true;
    }

    /** 开关是否已按标签校正过（旧文件没有这个键，视为未校正）。 */
    private static boolean isTagsSynced(CommentedFileConfig config) {
        return readBoolean(config, List.of("meta", "tags_synced"), false);
    }

    private static void setTagsSynced(CommentedFileConfig config, boolean synced) {
        config.set(List.of("meta", "tags_synced"), synced);
        config.setComment(List.of("meta", "tags_synced"),
            "附魔标签开关是否已按注册表标签校正（由模组维护，请勿手动修改）");
    }

    private static EnchantBaseline.Entry readEntry(CommentedFileConfig config, EnchantBaseline.Entry fallback) {
        List<String> base = entryPath(fallback.id());
        return sanitize(new EnchantBaseline.Entry(
            fallback.id(),
            readInt(config, append(base, "max_level"), fallback.maxLevel()),
            readInt(config, append(base, "weight"), fallback.weight()),
            readInt(config, append(base, List.of("min_cost", "base")), fallback.minCostBase()),
            readInt(config, append(base, List.of("min_cost", "per_level_above_first")),
                fallback.minCostPerLevelAboveFirst()),
            readInt(config, append(base, List.of("max_cost", "base")), fallback.maxCostBase()),
            readInt(config, append(base, List.of("max_cost", "per_level_above_first")),
                fallback.maxCostPerLevelAboveFirst()),
            readInt(config, append(base, "anvil_cost"), fallback.anvilCost()),
            readBoolean(config, append(base, "in_table"), fallback.inTable()),
            readBoolean(config, append(base, "treasure"), fallback.treasure()),
            readBoolean(config, append(base, "tradeable"), fallback.tradeable()),
            readBoolean(config, append(base, "on_random_loot"), fallback.onRandomLoot()),
            readBoolean(config, append(base, "on_traded_equipment"), fallback.onTradedEquipment())));
    }

    /**
     * 把条目清洗到合法区间。
     * <p>配置文件与网络快照走同一套规则：最大等级 1~255、权重 1~1024、其余数值不得为负。</p>
     */
    private static EnchantBaseline.Entry sanitize(EnchantBaseline.Entry entry) {
        return new EnchantBaseline.Entry(
            entry.id(),
            Mth.clamp(entry.maxLevel(), 1, LEVEL_CAP),
            Mth.clamp(entry.weight(), 1, WEIGHT_CAP),
            Math.max(0, entry.minCostBase()),
            Math.max(0, entry.minCostPerLevelAboveFirst()),
            Math.max(0, entry.maxCostBase()),
            Math.max(0, entry.maxCostPerLevelAboveFirst()),
            Math.max(0, entry.anvilCost()),
            entry.inTable(),
            entry.treasure(),
            entry.tradeable(),
            entry.onRandomLoot(),
            entry.onTradedEquipment());
    }

    /** 把全局标量清洗到合法区间（配置读取与网络快照共用）。 */
    private static GlobalSettings sanitize(GlobalSettings settings) {
        return new GlobalSettings(
            Mth.clamp(settings.maxPower(), 1, POWER_CAP),
            Math.max(0.0D, settings.xpCostScale()),
            Mth.clamp(settings.extraPower(), 0, POWER_CAP),
            Math.max(1, settings.anvilMaxCost()));
    }

    private static void writeEntry(CommentedFileConfig config, EnchantBaseline.Entry entry) {
        List<String> base = entryPath(entry.id());
        config.setComment(base, "附魔 " + entry.id() + " 的全局基准值；玩家相关规则会在此基础上继续计算");

        setInt(config, append(base, "max_level"), entry.maxLevel(),
            "基准最大等级（1~255）；附魔台与铁砧会先取这个值");
        setInt(config, append(base, "weight"), entry.weight(),
            "抽取权重（1~1024），只影响附魔台的加权随机池");
        setInt(config, append(base, List.of("min_cost", "base")), entry.minCostBase(),
            "附魔台等级成本曲线：1 级的最小 power");
        setInt(config, append(base, List.of("min_cost", "per_level_above_first")), entry.minCostPerLevelAboveFirst(),
            "每提高 1 级需要额外的最小 power");
        setInt(config, append(base, List.of("max_cost", "base")), entry.maxCostBase(),
            "附魔台等级成本曲线：1 级的最大 power");
        setInt(config, append(base, List.of("max_cost", "per_level_above_first")), entry.maxCostPerLevelAboveFirst(),
            "每提高 1 级需要额外的最大 power");
        setInt(config, append(base, "anvil_cost"), entry.anvilCost(),
            "铁砧每级附魔的消耗倍率");
        setBoolean(config, append(base, "in_table"), entry.inTable(), "是否进入附魔台随机池");
        setBoolean(config, append(base, "treasure"), entry.treasure(), "是否属于宝藏附魔");
        setBoolean(config, append(base, "tradeable"), entry.tradeable(), "是否可被村民交易");
        setBoolean(config, append(base, "on_random_loot"), entry.onRandomLoot(), "是否出现在战利品中");
        setBoolean(config, append(base, "on_traded_equipment"), entry.onTradedEquipment(),
            "是否出现在村民出售的附魔装备中");
    }

    private static boolean writeGlobalDefaults(CommentedFileConfig config) {
        boolean dirty = false;
        if (!config.contains("version")) {
            config.set("version", VERSION);
            config.setComment("version", "附魔基准表版本（由模组维护，请勿手动修改）");
            dirty = true;
        }
        // 逐键补缺，便于后续版本往 common 表里追加新的全局开关而不影响已有文件。
        if (config.contains(List.of("table", "extra_power_per_shelf"))) {
            // 旧版本的键名迁移（该键从未生效，直接移除即可）。
            config.remove(List.of("table", "extra_power_per_shelf"));
            dirty = true;
        }
        dirty |= setDefaultInt(config, List.of("table", "max_power"), 15,
            "附魔台书架 power 上限（原版 15）；实际硬上限为 max_power + extra_power");
        dirty |= setDefaultDouble(config, List.of("table", "xp_cost_scale"), 1.0D, "附魔台经验消耗倍率（1.0 = 原版）");
        dirty |= setDefaultInt(config, List.of("table", "extra_power"), 0,
            "在已按 max_power 截断的书架 power 之上追加的 power（默认 0）；同时放宽硬上限");
        dirty |= setDefaultInt(config, List.of("anvil", "max_cost"), 40, "铁砧允许的最高经验消耗（原版 40，达到即拒绝）");
        dirty |= setDefaultBoolean(config, List.of("meta", "tags_synced"), false,
            "附魔标签开关是否已按注册表标签校正（由模组维护，请勿手动修改）");
        return dirty;
    }

    private static GlobalSettings readSettings(CommentedFileConfig config) {
        return sanitize(new GlobalSettings(
            readInt(config, List.of("table", "max_power"), GlobalSettings.DEFAULT.maxPower()),
            readDouble(config, List.of("table", "xp_cost_scale"), GlobalSettings.DEFAULT.xpCostScale()),
            readInt(config, List.of("table", "extra_power"), GlobalSettings.DEFAULT.extraPower()),
            readInt(config, List.of("anvil", "max_cost"), GlobalSettings.DEFAULT.anvilMaxCost())));
    }

    // ============================================================
    // 工具
    // ============================================================

    private static List<String> entryPath(Identifier id) {
        return List.of("enchantments", id.toString());
    }

    private static List<String> append(List<String> base, String key) {
        return append(base, List.of(key));
    }

    private static List<String> append(List<String> base, List<String> suffix) {
        java.util.ArrayList<String> path = new java.util.ArrayList<>(base.size() + suffix.size());
        path.addAll(base);
        path.addAll(suffix);
        return List.copyOf(path);
    }

    private static int readInt(CommentedFileConfig config, List<String> path, int fallback) {
        try {
            return config.getIntOrElse(path, fallback);
        } catch (RuntimeException exception) {
            NewNewWarlockCore.LOGGER.warn("[enchant] 基准表键 {} 不是整数，已回退默认值 {}", String.join(".", path), fallback);
            return fallback;
        }
    }

    private static double readDouble(CommentedFileConfig config, List<String> path, double fallback) {
        try {
            Object value = config.getOrElse(path, fallback);
            return value instanceof Number number ? number.doubleValue() : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static boolean readBoolean(CommentedFileConfig config, List<String> path, boolean fallback) {
        try {
            Object value = config.getOrElse(path, fallback);
            return value instanceof Boolean bool ? bool : fallback;
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static void setInt(CommentedFileConfig config, List<String> path, int value, String comment) {
        config.set(path, value);
        config.setComment(path, comment);
    }

    private static boolean setDefaultInt(
        CommentedFileConfig config, List<String> path, int value, String comment) {
        if (config.contains(path)) {
            return false;
        }
        setInt(config, path, value, comment);
        return true;
    }

    private static boolean setDefaultDouble(
        CommentedFileConfig config, List<String> path, double value, String comment) {
        if (config.contains(path)) {
            return false;
        }
        config.set(path, value);
        config.setComment(path, comment);
        return true;
    }

    private static void setBoolean(CommentedFileConfig config, List<String> path, boolean value, String comment) {
        config.set(path, value);
        config.setComment(path, comment);
    }

    private static boolean setDefaultBoolean(
        CommentedFileConfig config, List<String> path, boolean value, String comment) {
        if (config.contains(path)) {
            return false;
        }
        setBoolean(config, path, value, comment);
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
            NewNewWarlockCore.LOGGER.error("[enchant] 无法加载附魔基准表 {}", path, exception);
            return null;
        }
    }

    private static void save(CommentedFileConfig config) {
        try {
            config.save();
        } catch (Exception exception) {
            NewNewWarlockCore.LOGGER.error("[enchant] 无法保存附魔基准表", exception);
        }
    }

    private static void close(CommentedFileConfig config) {
        try {
            config.close();
        } catch (Exception exception) {
            NewNewWarlockCore.LOGGER.error("[enchant] 无法关闭附魔基准表", exception);
        }
    }
}
