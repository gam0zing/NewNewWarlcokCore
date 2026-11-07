package com.gam0zing.newnew_warlock_core.skill.tree;

/**
 * 技能树的一条有向边：{@code from} 是前置，{@code to} 是后继。
 */
public record SkillTreeEdge(String from, String to) {
}
