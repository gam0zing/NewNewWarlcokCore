package com.gam0zing.newnew_warlock_core.mob.config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;

/**
 * 启动时扫描全部已注册群系，产出写入 {@code difficulty_biome.toml} 的说明信息。
 * <p>维度优先按各 {@link ServerLevel} 的 {@link BiomeSource#possibleBiomes()} 归属，
 * 未出现在任何维度里的群系再按 {@code #minecraft:is_overworld/is_nether/is_end} 标签兜底。
 * 出现频率用当前存档的实际群系源做网格采样：在水平方向按固定步长取样、垂直方向取 5 层，
 * 统计每个群系的采样占比，并按维度内占比排名分成高/中/低三档。</p>
 * <p>采样只读取群系源，不加载区块，因此可以安全地在服务端启动完成时同步执行。</p>
 */
final class BiomeDifficultyScanner {

    /** 水平采样范围（格）；主世界取更大范围，其余维度按实际尺度缩小。 */
    private static final int OVERWORLD_SPAN = 4096;
    private static final int OTHER_SPAN = 2048;
    /** 水平采样步长（格）。 */
    private static final int STEP = 64;
    /** 垂直采样层数。 */
    private static final int LAYERS = 5;

    private static final String UNKNOWN_DIMENSION = "unknown";

    private static final Map<String, String> CHINESE_NAMES = chineseNames();
    private static final Map<String, Double> DEFAULT_DIFFICULTY = defaultDifficulties();

    private BiomeDifficultyScanner() {}

    /**
     * 群系难度的内建默认值。配置里缺失某个群系时使用该值；
     * 未收录的群系（模组群系等）回退为 0。
     */
    static double defaultDifficulty(Identifier biome) {
        return DEFAULT_DIFFICULTY.getOrDefault(biome.toString(), 0.0D);
    }

    /** 扫描全部群系；返回按注册名排序的配置注释，供配置写入使用。 */
    static Map<Identifier, String> scan(MinecraftServer server) {
        Registry<Biome> registry = server.registryAccess().lookupOrThrow(Registries.BIOME);
        List<Holder.Reference<Biome>> biomes = registry.listElements().toList();

        Map<Identifier, String> dimensions = new HashMap<>();
        Map<Identifier, Integer> counts = new HashMap<>();
        Map<String, Integer> totals = new HashMap<>();

        for (ServerLevel level : server.getAllLevels()) {
            String dimension = level.dimension().identifier().toString();
            for (Holder<Biome> holder : level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes()) {
                holder.unwrapKey().ifPresent(key -> dimensions.putIfAbsent(key.identifier(), dimension));
            }
            sample(level, counts, totals);
        }

        Map<String, List<Identifier>> byDimension = new HashMap<>();
        for (Map.Entry<Identifier, Integer> entry : counts.entrySet()) {
            String dimension = dimensions.getOrDefault(entry.getKey(), UNKNOWN_DIMENSION);
            byDimension.computeIfAbsent(dimension, key -> new ArrayList<>()).add(entry.getKey());
        }

        Map<Identifier, String> frequency = new HashMap<>();
        for (Map.Entry<String, List<Identifier>> entry : byDimension.entrySet()) {
            List<Identifier> ids = entry.getValue();
            ids.sort(Comparator.comparingInt((Identifier id) -> counts.getOrDefault(id, 0)).reversed());
            int size = ids.size();
            int highCut = Math.max(1, (size + 2) / 3);
            int mediumCut = Math.max(highCut, (size * 2 + 2) / 3);
            for (int i = 0; i < size; i++) {
                String tier = i < highCut ? "高" : i < mediumCut ? "中" : "低";
                int total = totals.getOrDefault(entry.getKey(), 0);
                int count = counts.getOrDefault(ids.get(i), 0);
                String share = total > 0
                    ? String.format(Locale.ROOT, "采样占比 %.2f%%", count * 100.0D / total)
                    : "采样占比未知";
                frequency.put(ids.get(i), tier + "（" + share + "）");
            }
        }

        Map<Identifier, String> result = new LinkedHashMap<>();
        biomes.stream()
            .map(holder -> holder.unwrapKey().map(key -> key.identifier()).orElse(null))
            .filter(id -> id != null)
            .sorted(Comparator.comparing((Identifier id) -> id.toString()))
            .forEach(id -> {
                String dimension = dimensions.get(id);
                if (dimension == null) {
                    dimension = dimensionFromTags(registry, id);
                }
                String chinese = CHINESE_NAMES.getOrDefault(id.toString(), id.getPath());
                String label = frequency.getOrDefault(id, "低（采样未出现）");
                result.put(id, chinese + "（" + id + "）| 维度: "
                    + dimensionName(dimension) + " | 出现频率: " + label);
            });
        return result;
    }

    private static void sample(ServerLevel level, Map<Identifier, Integer> counts, Map<String, Integer> totals) {
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        Climate.Sampler sampler = level.getChunkSource().randomState()
            .createClimateSampler(SamplerContext.EMPTY_UNCACHED);
        BiomeResolver resolver = source.createResolver(sampler);
        boolean overworld = level.dimension().equals(Level.OVERWORLD);
        int span = overworld ? OVERWORLD_SPAN : OTHER_SPAN;
        int minY = level.getMinY() + 16;
        int maxY = level.getMaxY() - 16;
        String dimension = level.dimension().identifier().toString();
        int total = 0;
        for (int layer = 0; layer < LAYERS; layer++) {
            int y = minY + (int) ((long) (maxY - minY) * layer / (LAYERS - 1));
            for (int x = -span; x <= span; x += STEP) {
                for (int z = -span; z <= span; z += STEP) {
                    Holder<Biome> holder = resolver.getNoiseBiome(x >> 2, y >> 2, z >> 2);
                    Identifier id = holder.unwrapKey().map(key -> key.identifier()).orElse(null);
                    if (id == null) {
                        continue;
                    }
                    counts.merge(id, 1, Integer::sum);
                    total++;
                }
            }
        }
        totals.merge(dimension, total, Integer::sum);
    }

    @Nullable
    private static String dimensionFromTags(Registry<Biome> registry, Identifier id) {
        // 虚空群系只出现在虚空/自定义世界，按主世界归属，避免显示为未知
        if (id.equals(Identifier.withDefaultNamespace("the_void"))) {
            return "minecraft:overworld";
        }
        return registry.get(id)
            .map(holder -> {
                if (holder.is(BiomeTags.IS_NETHER)) {
                    return "minecraft:the_nether";
                }
                if (holder.is(BiomeTags.IS_END)) {
                    return "minecraft:the_end";
                }
                if (holder.is(BiomeTags.IS_OVERWORLD)) {
                    return "minecraft:overworld";
                }
                return UNKNOWN_DIMENSION;
            })
            .orElse(UNKNOWN_DIMENSION);
    }

    private static String dimensionName(@Nullable String dimension) {
        if (dimension == null) {
            return "未知";
        }
        return switch (dimension) {
            case "minecraft:overworld" -> "主世界";
            case "minecraft:the_nether" -> "下界";
            case "minecraft:the_end" -> "末地";
            case UNKNOWN_DIMENSION -> "未知";
            default -> dimension;
        };
    }

    /** 原版群系的中文名；模组群系没有对应项时回退为注册名路径。 */
    private static Map<String, String> chineseNames() {
        Map<String, String> names = new HashMap<>();
        names.put("minecraft:the_void", "虚空");
        names.put("minecraft:plains", "平原");
        names.put("minecraft:sunflower_plains", "向日葵平原");
        names.put("minecraft:snowy_plains", "雪原");
        names.put("minecraft:ice_spikes", "冰刺之地");
        names.put("minecraft:desert", "沙漠");
        names.put("minecraft:swamp", "沼泽");
        names.put("minecraft:mangrove_swamp", "红树林沼泽");
        names.put("minecraft:forest", "森林");
        names.put("minecraft:flower_forest", "繁花森林");
        names.put("minecraft:birch_forest", "白桦森林");
        names.put("minecraft:dappled_forest", "斑驳森林");
        names.put("minecraft:dark_forest", "黑森林");
        names.put("minecraft:pale_garden", "苍白之园");
        names.put("minecraft:old_growth_birch_forest", "原始白桦森林");
        names.put("minecraft:old_growth_pine_taiga", "原始松木针叶林");
        names.put("minecraft:old_growth_spruce_taiga", "原始云杉针叶林");
        names.put("minecraft:taiga", "针叶林");
        names.put("minecraft:snowy_taiga", "积雪针叶林");
        names.put("minecraft:savanna", "热带草原");
        names.put("minecraft:savanna_plateau", "热带高原");
        names.put("minecraft:windswept_hills", "风袭丘陵");
        names.put("minecraft:windswept_gravelly_hills", "风袭沙砾丘陵");
        names.put("minecraft:windswept_forest", "风袭森林");
        names.put("minecraft:windswept_savanna", "风袭热带草原");
        names.put("minecraft:jungle", "丛林");
        names.put("minecraft:sparse_jungle", "稀疏丛林");
        names.put("minecraft:bamboo_jungle", "竹林");
        names.put("minecraft:badlands", "恶地");
        names.put("minecraft:eroded_badlands", "风蚀恶地");
        names.put("minecraft:wooded_badlands", "疏林恶地");
        names.put("minecraft:meadow", "草甸");
        names.put("minecraft:cherry_grove", "樱花树林");
        names.put("minecraft:grove", "雪林");
        names.put("minecraft:snowy_slopes", "积雪山坡");
        names.put("minecraft:frozen_peaks", "冰封山峰");
        names.put("minecraft:jagged_peaks", "尖峭山峰");
        names.put("minecraft:stony_peaks", "裸岩山峰");
        names.put("minecraft:river", "河流");
        names.put("minecraft:frozen_river", "冻河");
        names.put("minecraft:beach", "沙滩");
        names.put("minecraft:snowy_beach", "积雪沙滩");
        names.put("minecraft:stony_shore", "石岸");
        names.put("minecraft:warm_ocean", "暖水海洋");
        names.put("minecraft:lukewarm_ocean", "温水海洋");
        names.put("minecraft:deep_lukewarm_ocean", "温水深海");
        names.put("minecraft:ocean", "海洋");
        names.put("minecraft:deep_ocean", "深海");
        names.put("minecraft:cold_ocean", "冷水海洋");
        names.put("minecraft:deep_cold_ocean", "冷水深海");
        names.put("minecraft:frozen_ocean", "冻洋");
        names.put("minecraft:deep_frozen_ocean", "冰冻深海");
        names.put("minecraft:mushroom_fields", "蘑菇岛");
        names.put("minecraft:dripstone_caves", "溶洞");
        names.put("minecraft:lush_caves", "繁茂洞穴");
        names.put("minecraft:deep_dark", "深暗之域");
        names.put("minecraft:sulfur_caves", "硫磺洞穴");
        names.put("minecraft:nether_wastes", "下界荒地");
        names.put("minecraft:warped_forest", "诡异森林");
        names.put("minecraft:crimson_forest", "绯红森林");
        names.put("minecraft:soul_sand_valley", "灵魂沙峡谷");
        names.put("minecraft:basalt_deltas", "玄武岩三角洲");
        names.put("minecraft:the_end", "末地");
        names.put("minecraft:end_highlands", "末地高地");
        names.put("minecraft:end_midlands", "末地内陆");
        names.put("minecraft:small_end_islands", "末地小岛");
        names.put("minecraft:end_barrens", "末地荒地");
        return names;
    }

    /** 原版群系的内建难度默认值（可为负数）。 */
    private static Map<String, Double> defaultDifficulties() {
        Map<String, Double> values = new HashMap<>();
        values.put("minecraft:plains", -12.0D);
        values.put("minecraft:sunflower_plains", -12.0D);
        values.put("minecraft:snowy_plains", 8.0D);
        values.put("minecraft:ice_spikes", 20.0D);
        values.put("minecraft:desert", 8.0D);
        values.put("minecraft:swamp", 12.0D);
        values.put("minecraft:mangrove_swamp", 8.0D);
        values.put("minecraft:forest", -8.0D);
        values.put("minecraft:flower_forest", -12.0D);
        values.put("minecraft:birch_forest", 0.0D);
        values.put("minecraft:dappled_forest", 8.0D);
        values.put("minecraft:dark_forest", 16.0D);
        values.put("minecraft:pale_garden", 20.0D);
        values.put("minecraft:old_growth_birch_forest", 0.0D);
        values.put("minecraft:old_growth_pine_taiga", 0.0D);
        values.put("minecraft:old_growth_spruce_taiga", 4.0D);
        values.put("minecraft:taiga", 0.0D);
        values.put("minecraft:snowy_taiga", 4.0D);
        values.put("minecraft:savanna", -8.0D);
        values.put("minecraft:savanna_plateau", 8.0D);
        values.put("minecraft:windswept_hills", 8.0D);
        values.put("minecraft:windswept_gravelly_hills", 8.0D);
        values.put("minecraft:windswept_forest", 0.0D);
        values.put("minecraft:windswept_savanna", -8.0D);
        values.put("minecraft:jungle", 12.0D);
        values.put("minecraft:sparse_jungle", 4.0D);
        values.put("minecraft:bamboo_jungle", 0.0D);
        values.put("minecraft:badlands", 8.0D);
        values.put("minecraft:eroded_badlands", 8.0D);
        values.put("minecraft:wooded_badlands", 8.0D);
        values.put("minecraft:meadow", -12.0D);
        values.put("minecraft:cherry_grove", -12.0D);
        values.put("minecraft:grove", 8.0D);
        values.put("minecraft:snowy_slopes", 8.0D);
        values.put("minecraft:frozen_peaks", 12.0D);
        values.put("minecraft:jagged_peaks", 4.0D);
        values.put("minecraft:stony_peaks", 12.0D);
        values.put("minecraft:river", -12.0D);
        values.put("minecraft:frozen_river", 4.0D);
        values.put("minecraft:beach", -12.0D);
        values.put("minecraft:snowy_beach", 0.0D);
        values.put("minecraft:stony_shore", -8.0D);
        values.put("minecraft:warm_ocean", -12.0D);
        values.put("minecraft:lukewarm_ocean", -8.0D);
        values.put("minecraft:deep_lukewarm_ocean", 0.0D);
        values.put("minecraft:ocean", 0.0D);
        values.put("minecraft:deep_ocean", 12.0D);
        values.put("minecraft:cold_ocean", 8.0D);
        values.put("minecraft:deep_cold_ocean", 16.0D);
        values.put("minecraft:frozen_ocean", 12.0D);
        values.put("minecraft:deep_frozen_ocean", 20.0D);
        values.put("minecraft:mushroom_fields", -20.0D);
        values.put("minecraft:dripstone_caves", 12.0D);
        values.put("minecraft:lush_caves", -12.0D);
        values.put("minecraft:deep_dark", 20.0D);
        values.put("minecraft:sulfur_caves", 12.0D);
        values.put("minecraft:nether_wastes", 0.0D);
        values.put("minecraft:warped_forest", 16.0D);
        values.put("minecraft:crimson_forest", 12.0D);
        values.put("minecraft:soul_sand_valley", 12.0D);
        values.put("minecraft:basalt_deltas", 20.0D);
        values.put("minecraft:the_end", 0.0D);
        values.put("minecraft:end_highlands", 16.0D);
        values.put("minecraft:end_midlands", 0.0D);
        values.put("minecraft:small_end_islands", 12.0D);
        values.put("minecraft:end_barrens", 12.0D);
        values.put("minecraft:the_void", 0.0D);
        return values;
    }
}
