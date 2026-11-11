package com.gam0zing.newnew_warlock_core.core;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;

/**
 * 模组模块入口。
 * <p>每个顶层模块提供一个实现，模块内的 DeferredRegister、事件监听、数据加载器等全部在
 * {@link #init(IEventBus, ModContainer)} 里自行注册；模组入口只通过
 * {@link ModuleRegistry#init(IEventBus, ModContainer)} 统一遍历，不持有任何模块细节。</p>
 * <p>实现类必须提供公共无参构造器，并在
 * {@code META-INF/services/com.gam0zing.newnew_warlock_core.core.ModModule} 中列出一行，
 * 该文件就是模块清单本身，没有隐藏的扫描规则。</p>
 */
public interface ModModule {

    /** {@return 模块标识，用于日志与同优先级排序（例如 {@code "core"}）} */
    String id();

    /**
     * {@return 初始化优先级，数值小的先执行}
     * <p>默认 100；核心注册（属性、数据组件、效果）应排在依赖它们的模块之前。</p>
     */
    default int priority() {
        return 100;
    }

    /**
     * 注册本模块的全部内容。
     * <p>只在模组构造期调用一次，此时注册表事件尚未触发，可以安全地创建 DeferredRegister。</p>
     */
    void init(IEventBus modEventBus, ModContainer modContainer);
}
