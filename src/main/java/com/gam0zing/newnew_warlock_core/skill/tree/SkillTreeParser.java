package com.gam0zing.newnew_warlock_core.skill.tree;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.skill.command.CommandContext;
import com.gam0zing.newnew_warlock_core.skill.command.ParamValues;
import com.gam0zing.newnew_warlock_core.skill.command.PointAllocationCommand;
import com.gam0zing.newnew_warlock_core.skill.command.PointAllocationCommandType;
import com.gam0zing.newnew_warlock_core.skill.command.PointAllocationCommands;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/**
 * 技能树 JSON 解析器。
 * <p>服务端加载数据包与客户端接收同步都走这里，保证两侧对同一份 JSON 的理解完全一致。
 * 解析失败一律抛 {@link IllegalArgumentException}，由调用方决定跳过整棵树还是整体失败。</p>
 */
public final class SkillTreeParser {

    /** 当前支持的格式版本 */
    public static final int SCHEMA_VERSION = 1;
    public static final String DIRECTORY = "skill_trees";

    private static final Pattern NODE_ID = Pattern.compile("[a-z0-9_./-]+");

    private SkillTreeParser() {}

    /**
     * @param id   树 id（由文件路径决定）
     * @param json 树的 JSON
     * @throws IllegalArgumentException 结构非法
     */
    public static SkillTreeDefinition parse(Identifier id, JsonObject json) {
        int schema = optInt(json, "schema", SCHEMA_VERSION);
        if (schema > SCHEMA_VERSION) {
            throw new IllegalArgumentException("unsupported schema " + schema + ", this build supports " + SCHEMA_VERSION);
        }

        String displayName = requireString(json, "display_name");
        int order = optInt(json, "order", 0);
        boolean defaultUnlocked = optBool(json, "default_unlocked", false);
        double resetXpPerPoint = optDouble(json, "reset_xp_per_point", 0.0D);
        if (resetXpPerPoint < 0.0D) {
            throw new IllegalArgumentException("reset_xp_per_point must be >= 0");
        }

        List<SkillTreeNode> nodes = parseNodes(id, requireArray(json, "nodes"));
        List<SkillTreeEdge> edges = parseEdges(json, nodes);

        return new SkillTreeDefinition(
            id, displayName, order, defaultUnlocked, resetXpPerPoint, nodes, edges, json.toString());
    }

    private static List<SkillTreeNode> parseNodes(Identifier treeId, JsonArray array) {
        List<SkillTreeNode> nodes = new ArrayList<>(array.size());
        Set<String> seen = new HashSet<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("node entries must be objects");
            }
            JsonObject node = element.getAsJsonObject();
            String nodeId = requireString(node, "id");
            if (!NODE_ID.matcher(nodeId).matches()) {
                throw new IllegalArgumentException("node id '" + nodeId + "' must match " + NODE_ID.pattern());
            }
            if (!seen.add(nodeId)) {
                throw new IllegalArgumentException("duplicate node id '" + nodeId + "'");
            }

            float x = requireFloat(node, "x");
            float y = requireFloat(node, "y");
            String name = requireString(node, "name");
            String description = optString(node, "description", "");
            Identifier icon = parseIcon(optString(node, "icon", null), nodeId);
            List<NodeCommand> commands = parseCommands(treeId, nodeId, node);

            nodes.add(new SkillTreeNode(nodeId, x, y, name, description, icon, commands));
        }
        return nodes;
    }

    private static List<NodeCommand> parseCommands(Identifier treeId, String nodeId, JsonObject node) {
        JsonArray array = optArray(node, "commands");
        List<NodeCommand> commands = new ArrayList<>(array.size());
        for (int index = 0; index < array.size(); index++) {
            JsonElement element = array.get(index);
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("node '" + nodeId + "': command entries must be objects");
            }
            JsonObject entry = element.getAsJsonObject();
            String typeIdText = requireString(entry, "command");
            Identifier typeId = Identifier.tryParse(typeIdText);
            if (typeId == null) {
                throw new IllegalArgumentException("node '" + nodeId + "': invalid command id '" + typeIdText + "'");
            }
            PointAllocationCommandType<?> type = PointAllocationCommands.typeFor(typeId);
            if (type == null) {
                throw new IllegalArgumentException("node '" + nodeId + "': unknown command type '" + typeId + "'");
            }
            try {
                ParamValues values = ParamValues.parse(type.params(), optObject(entry, "params"));
                PointAllocationCommand command = type.create(values, new CommandContext(treeId, nodeId, index));
                commands.add(new NodeCommand(type, values, command));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException(
                    "node '" + nodeId + "', command #" + index + " (" + typeId + "): " + exception.getMessage(),
                    exception);
            }
        }
        return commands;
    }

    private static List<SkillTreeEdge> parseEdges(JsonObject json, List<SkillTreeNode> nodes) {
        Set<String> ids = new HashSet<>();
        for (SkillTreeNode node : nodes) {
            ids.add(node.id());
        }
        JsonArray array = optArray(json, "edges");
        List<SkillTreeEdge> edges = new ArrayList<>(array.size());
        Set<String> seen = new HashSet<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                throw new IllegalArgumentException("edge entries must be objects");
            }
            JsonObject edge = element.getAsJsonObject();
            String from = requireString(edge, "from");
            String to = requireString(edge, "to");
            if (!ids.contains(from) || !ids.contains(to)) {
                throw new IllegalArgumentException("edge '" + from + "' -> '" + to + "' references a missing node");
            }
            if (from.equals(to)) {
                throw new IllegalArgumentException("edge '" + from + "' -> '" + to + "' is a self loop");
            }
            if (!seen.add(from + "\u0000" + to)) {
                throw new IllegalArgumentException("duplicate edge '" + from + "' -> '" + to + "'");
            }
            edges.add(new SkillTreeEdge(from, to));
        }
        return edges;
    }

    @Nullable
    private static Identifier parseIcon(@Nullable String text, String nodeId) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Identifier itemId = Identifier.tryParse(text);
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            NewNewWarlockCore.LOGGER.warn("[skill] 节点 '{}' 的图标 '{}' 不是已注册物品，将不显示图标", nodeId, text);
            return null;
        }
        return itemId;
    }

    // ==========================================================================================
    // JSON 读取
    // ==========================================================================================

    private static String requireString(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("missing string field '" + key + "'");
        }
        return element.getAsString();
    }

    @Nullable
    private static String optString(JsonObject json, String key, @Nullable String fallback) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("field '" + key + "' must be a string");
        }
        return element.getAsString();
    }

    private static int optInt(JsonObject json, String key, int fallback) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsInt();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("field '" + key + "' must be an int", exception);
        }
    }

    private static double optDouble(JsonObject json, String key, double fallback) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsDouble();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("field '" + key + "' must be a float", exception);
        }
    }

    private static boolean optBool(JsonObject json, String key, boolean fallback) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        try {
            return element.getAsBoolean();
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("field '" + key + "' must be a bool", exception);
        }
    }

    private static float requireFloat(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("missing number field '" + key + "'");
        }
        return element.getAsFloat();
    }

    private static JsonArray requireArray(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException("missing array field '" + key + "'");
        }
        return element.getAsJsonArray();
    }

    private static JsonArray optArray(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return new JsonArray();
        }
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("field '" + key + "' must be an array");
        }
        return element.getAsJsonArray();
    }

    @Nullable
    private static JsonObject optObject(JsonObject json, String key) {
        JsonElement element = json.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("field '" + key + "' must be an object");
        }
        return element.getAsJsonObject();
    }
}
