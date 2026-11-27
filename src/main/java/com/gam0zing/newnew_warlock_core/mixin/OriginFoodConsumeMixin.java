package com.gam0zing.newnew_warlock_core.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.gam0zing.newnew_warlock_core.origin.OriginTraits;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.level.Level;

/**
 * 种族食物规则：猫与天堂鸟按食物类型改写营养与饱和度。
 * <p>客户端也要改写：本地玩家会先本地结算一次，若规则不一致，
 * 而服务端数值没有变化，就不会下发纠正数据包。</p>
 */
@Mixin(FoodProperties.class)
public abstract class OriginFoodConsumeMixin {

    @Redirect(
        method = "onConsume",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/food/FoodProperties;)V"))
    private void newnew$eat(
        FoodData foodData,
        FoodProperties food,
        Level level,
        LivingEntity user,
        ItemStack stack,
        Consumable consumable) {
        if (user instanceof Player player) {
            foodData.eat(OriginTraits.modifyFood(player, stack, food));
        } else {
            foodData.eat(food);
        }
    }
}
