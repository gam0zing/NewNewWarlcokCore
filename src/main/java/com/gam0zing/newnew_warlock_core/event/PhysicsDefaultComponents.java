package com.gam0zing.newnew_warlock_core.event;

import java.util.List;
import java.util.Map;

import com.gam0zing.newnew_warlock_core.combat.physics.PhysicsArmor;
import com.gam0zing.newnew_warlock_core.combat.physics.PhysicsDefenceFunctions;
import com.gam0zing.newnew_warlock_core.combat.physics.PhysicsStat;
import com.gam0zing.newnew_warlock_core.combat.physics.PhysicsWeapon;
import com.gam0zing.newnew_warlock_core.registry.ModDataComponents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.ModifyDefaultComponentsEvent;

/**
 * 为原版物品挂上默认物理档案（注册期执行）。
 * <p>只处理 {@code minecraft} 命名空间，避免误伤其他模组同后缀物品。
 * 这里的数值是<b>初始占位值</b>，用于让物理系统开箱可用，后续按实际手感调整。</p>
 * <p>其他模组物品可通过数据包 {@code set_components} 写入同名组件，或在自己的模组里监听
 * {@link ModifyDefaultComponentsEvent} 追加。</p>
 */
public final class PhysicsDefaultComponents {

    // ============================== 武器占位档案 ==============================

    private static final PhysicsWeapon SWORD = weapon(1.0D, 1.0D, 0.9D, 0.06D);
    private static final PhysicsWeapon AXE = weapon(0.9D, 1.1D, 1.2D, 0.08D);
    private static final PhysicsWeapon MACE = weapon(0.8D, 1.2D, 1.6D, 0.12D);
    private static final PhysicsWeapon TRIDENT = weapon(1.1D, 1.1D, 1.0D, 0.15D);
    private static final PhysicsWeapon BOW = weapon(1.0D, 1.0D, 0.8D, 0.05D);
    private static final PhysicsWeapon ARROW = weapon(1.0D, 1.0D, 0.8D, 0.05D);

    // ============================== 护甲材质与部位 ==============================

    /** 材质基准值：厚度 / 密度 / 韧性 / 刚性 / 缓冲性 / 隔热 / 灵活性 */
    private record MaterialProfile(
        double thickness, double density, double toughness, double rigidity,
        double cushioning, double insulation, double flexibility) {}

    private static final Map<String, MaterialProfile> MATERIALS = Map.of(
        "leather", new MaterialProfile(1.0D, 1.0D, 0.05D, 0.5D, 0.6D, 0.4D, 1.0D),
        "chainmail", new MaterialProfile(1.5D, 2.0D, 0.10D, 1.0D, 0.10D, 0.10D, 0.9D),
        "iron", new MaterialProfile(2.0D, 3.0D, 0.20D, 1.5D, 0.05D, 0.05D, 0.7D),
        "golden", new MaterialProfile(1.5D, 2.5D, 0.05D, 1.0D, 0.10D, 0.20D, 0.9D),
        "diamond", new MaterialProfile(2.5D, 2.5D, 0.40D, 2.0D, 0.10D, 0.10D, 0.8D),
        "netherite", new MaterialProfile(3.0D, 4.0D, 0.60D, 3.0D, 0.15D, 0.10D, 0.75D),
        "turtle", new MaterialProfile(1.5D, 1.5D, 0.15D, 1.0D, 0.3D, 0.2D, 0.85D));

    /** 部位相对厚度（由材质基准厚度缩放） */
    private static final Map<String, Double> SLOT_THICKNESS = Map.of(
        "helmet", 0.7D,
        "chestplate", 1.0D,
        "leggings", 0.85D,
        "boots", 0.6D);

    private PhysicsDefaultComponents() {}

    public static void onModifyDefaultComponents(ModifyDefaultComponentsEvent event) {
        for (Item item : BuiltInRegistries.ITEM) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!"minecraft".equals(id.getNamespace())) {
                continue;
            }
            String path = id.getPath();

            PhysicsWeapon weapon = weaponFor(path);
            if (weapon != null) {
                event.modify(item, (builder, registries, target) ->
                    builder.set(ModDataComponents.PHYSICS_WEAPON.get(), weapon));
                continue;
            }

            PhysicsArmor armor = armorFor(path);
            if (armor != null) {
                event.modify(item, (builder, registries, target) ->
                    builder.set(ModDataComponents.PHYSICS_ARMOR.get(), armor));
            }
        }
    }

    /** 按物品注册名匹配武器占位档案 */
    private static PhysicsWeapon weaponFor(String path) {
        if (path.endsWith("_sword")) {
            return SWORD;
        }
        if (path.endsWith("_axe")) {
            return AXE;
        }
        return switch (path) {
            case "trident" -> TRIDENT;
            case "mace" -> MACE;
            case "bow", "crossbow" -> BOW;
            case "arrow", "spectral_arrow", "tipped_arrow" -> ARROW;
            default -> null;
        };
    }

    /** 按物品注册名匹配护甲占位档案（材质前缀 + 部位后缀） */
    private static PhysicsArmor armorFor(String path) {
        for (Map.Entry<String, Double> slot : SLOT_THICKNESS.entrySet()) {
            String suffix = "_" + slot.getKey();
            if (!path.endsWith(suffix)) {
                continue;
            }
            // 头盔类物品统一取前缀（turtle_helmet → turtle，netherite_helmet → netherite）
            String material = path.substring(0, path.length() - suffix.length());
            MaterialProfile profile = MATERIALS.get(material);
            if (profile == null) {
                return null;
            }
            double thickness = profile.thickness() * slot.getValue();
            return new PhysicsArmor(
                PhysicsDefenceFunctions.GENERAL,
                PhysicsStat.of(thickness),
                PhysicsStat.of(profile.density()),
                PhysicsStat.of(profile.toughness()),
                PhysicsStat.of(profile.rigidity()),
                PhysicsStat.of(profile.cushioning()),
                PhysicsStat.of(profile.insulation()),
                PhysicsStat.of(profile.flexibility()),
                List.of());
        }
        return null;
    }

    private static PhysicsWeapon weapon(double low, double mid, double high, double penetration) {
        return new PhysicsWeapon(
            PhysicsStat.of(low),
            PhysicsStat.of(mid),
            PhysicsStat.of(high),
            PhysicsStat.of(penetration),
            List.of());
    }
}
