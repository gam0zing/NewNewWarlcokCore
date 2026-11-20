package com.gam0zing.newnew_warlock_core.origin;

/**
 * 设置起源的结果，屏蔽 NeoOrigins 自己的枚举类型。
 */
public enum OriginSetResult {

    /** 设置成功。 */
    SUCCESS,

    /** 目标起源与当前起源相同。 */
    UNCHANGED,

    /** 层不存在。 */
    UNKNOWN_LAYER,

    /** 起源未注册。 */
    UNKNOWN_ORIGIN,

    /** 该层已被其他系统占用，无法设置。 */
    CLAIMED,

    /** 事件监听器取消了本次设置。 */
    CANCELLED,

    /** 未安装 NeoOrigins，功能不可用。 */
    UNAVAILABLE
}
