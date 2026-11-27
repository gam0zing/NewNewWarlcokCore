package com.gam0zing.newnew_warlock_core.origin.content.power;

/**
 * 无需逐 tick 维护的被动能力标记。
 * <p>这类能力的实际效果由对应子系统按能力 id 查询后执行（例如食物规则、同类无视），
 * 因此是否生效只取决于玩家是否被授予该能力，而不是玩家拥有哪个起源。</p>
 */
final class PassivePower extends TraitPower {
}
