package com.gam0zing.newnew_warlock_core.skill.tree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.resources.Identifier;

/**
 * 一棵技能树的完整定义。
 * <p>由数据包 {@code data/<namespace>/skill_trees/<id>.json} 解析而来；
 * {@link #rawJson()} 用于把定义原样同步给客户端，客户端用同一解析器还原。</p>
 */
public final class SkillTreeDefinition {

    private final Identifier id;
    private final String displayName;
    private final int order;
    private final boolean defaultUnlocked;
    private final double resetXpPerPoint;
    private final List<SkillTreeNode> nodes;
    private final List<SkillTreeEdge> edges;
    private final String rawJson;

    private final Map<String, SkillTreeNode> nodeById;
    private final Map<String, List<SkillTreeNode>> prerequisites;

    public SkillTreeDefinition(
        Identifier id,
        String displayName,
        int order,
        boolean defaultUnlocked,
        double resetXpPerPoint,
        List<SkillTreeNode> nodes,
        List<SkillTreeEdge> edges,
        String rawJson) {
        this.id = id;
        this.displayName = displayName;
        this.order = order;
        this.defaultUnlocked = defaultUnlocked;
        this.resetXpPerPoint = resetXpPerPoint;
        this.nodes = List.copyOf(nodes);
        this.edges = List.copyOf(edges);
        this.rawJson = rawJson;

        Map<String, SkillTreeNode> byId = new LinkedHashMap<>();
        Map<String, List<SkillTreeNode>> incoming = new LinkedHashMap<>();
        for (SkillTreeNode node : this.nodes) {
            byId.put(node.id(), node);
            incoming.put(node.id(), new ArrayList<>());
        }
        for (SkillTreeEdge edge : this.edges) {
            SkillTreeNode from = byId.get(edge.from());
            SkillTreeNode to = byId.get(edge.to());
            if (from != null && to != null) {
                incoming.get(to.id()).add(from);
            }
        }
        this.nodeById = Collections.unmodifiableMap(byId);
        Map<String, List<SkillTreeNode>> frozen = new LinkedHashMap<>();
        incoming.forEach((key, value) -> frozen.put(key, List.copyOf(value)));
        this.prerequisites = Collections.unmodifiableMap(frozen);
    }

    public Identifier id() {
        return this.id;
    }

    public String displayName() {
        return this.displayName;
    }

    public int order() {
        return this.order;
    }

    /** {@return 树是否对玩家默认开放；不开放时由业务层解锁} */
    public boolean defaultUnlocked() {
        return this.defaultUnlocked;
    }

    /** {@return 每重置一点已分配点数需要消耗的经验等级} */
    public double resetXpPerPoint() {
        return this.resetXpPerPoint;
    }

    public List<SkillTreeNode> nodes() {
        return this.nodes;
    }

    public List<SkillTreeEdge> edges() {
        return this.edges;
    }

    public String rawJson() {
        return this.rawJson;
    }

    @Nullable
    public SkillTreeNode node(String nodeId) {
        return this.nodeById.get(nodeId);
    }

    /** {@return 该节点的全部前置节点；没有前置说明它是根节点} */
    public List<SkillTreeNode> prerequisitesOf(String nodeId) {
        return this.prerequisites.getOrDefault(nodeId, List.of());
    }

    /** {@return 节点是否没有前置（可直接花点数解锁）} */
    public boolean isRoot(String nodeId) {
        return prerequisitesOf(nodeId).isEmpty();
    }
}
