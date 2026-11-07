package com.gam0zing.newnew_warlock_core.enchant;

/**
 * 附魔改造的调用场景。
 * <p>每个场景对应一个原版入口，后续 Mixin 会把入口参数整理成 {@link EnchantContext} 再交给 {@link WarlockEnchantHooks}。</p>
 */
public enum EnchantScene {
    /** 附魔台（{@code EnchantmentMenu}） */
    TABLE("附魔台"),
    /** 铁砧合并与命名（{@code AnvilMenu}） */
    ANVIL("铁砧"),
    /** 战利品中的附魔书（{@code EnchantRandomlyFunction}） */
    LOOT_BOOK("战利品附魔书"),
    /** 战利品中的工具/装备附魔（{@code EnchantWithLevelsFunction}） */
    LOOT_TOOL("战利品工具附魔"),
    /** 村民交易的附魔书（{@code EnchantBookForEmeralds}） */
    TRADE_BOOK("村民交易附魔书"),
    /** 村民交易的附魔物品（{@code EnchantedItemForEmeralds}） */
    TRADE_ITEM("村民交易附魔物品"),
    /** {@code /enchant} 指令 */
    COMMAND("指令"),
    /** 创造模式物品栏生成的附魔书 */
    CREATIVE("创造模式"),
    /** 其他读取最大等级的代码路径（含其他模组调用） */
    OTHER("其他");

    private final String description;

    EnchantScene(String description) {
        this.description = description;
    }

    public String description() {
        return this.description;
    }
}
