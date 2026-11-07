package com.gam0zing.newnew_warlock_core.skill.progress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 玩家在一棵技能树上的进度。
 */
public final class TreeProgress {

    public static final Codec<TreeProgress> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.optionalFieldOf("unlocked", false).forGetter(TreeProgress::isUnlocked),
        Codec.STRING.listOf().optionalFieldOf("nodes", List.of())
            .forGetter(progress -> List.copyOf(progress.nodes)),
        AppliedNode.CODEC.listOf().optionalFieldOf("applied", List.of()).forGetter(TreeProgress::applied)
    ).apply(instance, TreeProgress::new));

    private boolean unlocked;
    private final Set<String> nodes = new LinkedHashSet<>();
    private final List<AppliedNode> applied = new ArrayList<>();

    private TreeProgress(boolean unlocked, List<String> nodes, List<AppliedNode> applied) {
        this.unlocked = unlocked;
        this.nodes.addAll(nodes);
        this.applied.addAll(applied);
    }

    static TreeProgress empty() {
        return new TreeProgress(false, List.of(), List.of());
    }

    public boolean isUnlocked() {
        return this.unlocked;
    }

    public void unlockTree() {
        this.unlocked = true;
    }

    public boolean isNodeUnlocked(String nodeId) {
        return this.nodes.contains(nodeId);
    }

    public Set<String> nodeIds() {
        return Collections.unmodifiableSet(this.nodes);
    }

    public List<AppliedNode> applied() {
        return Collections.unmodifiableList(this.applied);
    }

    /** {@return 该树已分配的点数} */
    public double spent() {
        double total = 0.0D;
        for (AppliedNode node : this.applied) {
            total += node.cost();
        }
        return total;
    }

    public void addNode(AppliedNode node) {
        this.nodes.add(node.nodeId());
        this.applied.add(node);
    }

    /** 逆序撤销时取最后一条 */
    AppliedNode peekLast() {
        return this.applied.isEmpty() ? null : this.applied.get(this.applied.size() - 1);
    }

    void removeLast() {
        if (!this.applied.isEmpty()) {
            AppliedNode removed = this.applied.remove(this.applied.size() - 1);
            this.nodes.remove(removed.nodeId());
        }
    }

    public void clearNodes() {
        this.nodes.clear();
        this.applied.clear();
    }
}
