package com.gam0zing.newnew_warlock_core.skill.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.gam0zing.newnew_warlock_core.NewNewWarlockCore;
import com.gam0zing.newnew_warlock_core.skill.net.SkillProgressSyncPayload;
import com.gam0zing.newnew_warlock_core.skill.net.SkillTreeSyncPayload;
import com.gam0zing.newnew_warlock_core.skill.tree.SkillTreeDefinition;
import com.gam0zing.newnew_warlock_core.skill.tree.SkillTreeParser;
import com.google.gson.JsonParser;

import net.minecraft.resources.Identifier;

/**
 * 客户端缓存的技能树定义与进度，供界面读取。
 * <p>定义由服务端下发原始 JSON，客户端用与服务端相同的解析器还原，保证语义一致。</p>
 * <p>这里只维护客户端自己的副本：{@code SkillTrees} 是服务端权威快照，单机下与服务端共享同一份静态状态，
 * 客户端清缓存或解析失败都不能污染它。</p>
 */
public final class SkillTreeClientData {

    private static volatile Map<Identifier, SkillTreeDefinition> trees = Map.of();
    private static volatile Map<Identifier, SkillProgressSyncPayload.TreeView> views = Map.of();
    private static volatile double totalSpent;

    private SkillTreeClientData() {}

    public static void acceptTrees(SkillTreeSyncPayload payload) {
        Map<Identifier, SkillTreeDefinition> parsed = new LinkedHashMap<>();
        for (SkillTreeSyncPayload.Entry entry : payload.trees()) {
            try {
                parsed.put(entry.id(),
                    SkillTreeParser.parse(entry.id(), JsonParser.parseString(entry.json()).getAsJsonObject()));
            } catch (RuntimeException exception) {
                NewNewWarlockCore.LOGGER.error("[skill] 客户端解析技能树 {} 失败：{}", entry.id(), exception.getMessage());
            }
        }
        trees = Map.copyOf(parsed);
    }

    public static void acceptProgress(SkillProgressSyncPayload payload) {
        Map<Identifier, SkillProgressSyncPayload.TreeView> received = new LinkedHashMap<>();
        for (SkillProgressSyncPayload.TreeView view : payload.trees()) {
            received.put(view.treeId(), view);
        }
        views = Map.copyOf(received);
        totalSpent = payload.totalSpent();
    }

    public static Map<Identifier, SkillTreeDefinition> trees() {
        return trees;
    }

    /** {@return 按 order、再按 id 排序的树列表，用于页签} */
    public static List<SkillTreeDefinition> sortedTrees() {
        List<SkillTreeDefinition> list = new ArrayList<>(trees.values());
        list.sort(Comparator.comparingInt(SkillTreeDefinition::order)
            .thenComparing(definition -> definition.id().toString()));
        return list;
    }

    @Nullable
    public static SkillProgressSyncPayload.TreeView view(Identifier treeId) {
        return views.get(treeId);
    }

    public static boolean isNodeUnlocked(Identifier treeId, String nodeId) {
        SkillProgressSyncPayload.TreeView view = views.get(treeId);
        return view != null && view.nodes().contains(nodeId);
    }

    public static double totalSpent() {
        return totalSpent;
    }

    public static void clear() {
        trees = Map.of();
        views = Map.of();
        totalSpent = 0.0D;
    }
}
