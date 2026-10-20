package jaxon.ric.mixin;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.tags.DamageTypeTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class BlastProtectionMixin {
    // Handle explosions at hurtServer so the plate also works for damage
    // sources that bypass the normal armor-absorption method (notably crystal
    // explosions in recent Minecraft versions).
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void ric$blockBlast(ServerLevel level, DamageSource source, float damage,
                                CallbackInfoReturnable<Boolean> cir) {
        if (damage <= 0.0F || !ric$isExplosionDamage(source)) return;
        LivingEntity entity = (LivingEntity) (Object) this;
        var chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        if (ric$isBlastArmor(entity, chest)) {
            ric$breakBlastArmor(entity, chest);
            cir.setReturnValue(true);
        }
    }

    private boolean ric$isBlastArmor(LivingEntity entity, ItemStack chest) {
        CustomData customData = chest.get(DataComponents.CUSTOM_DATA);
        boolean markedRicArmor = customData != null
                && customData.copyTag().getBooleanOr("ric_blast_armor", false);
        if (markedRicArmor) return true;
        if (!(entity.level() instanceof ServerLevel level)) return false;

        return chest.getEnchantments().getLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                        .getOrThrow(Enchantments.BLAST_PROTECTION)
        ) >= 255;
    }

    private void ric$breakBlastArmor(LivingEntity entity, ItemStack chest) {
        entity.onEquippedItemBroken(chest, EquipmentSlot.CHEST);
        chest.shrink(1);
        entity.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY);
    }

    private boolean ric$isExplosionDamage(DamageSource source) {
        // An end crystal hit by a player produces player_explosion damage.
        // Match the concrete explosion types as well as the vanilla tag so the
        // chestplate covers crystals, anchors, and other explosion sources.
        Entity direct = source.getDirectEntity();
        boolean endCrystal = direct != null
                && net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(direct.getType())
                .equals(net.minecraft.resources.Identifier.parse("minecraft:end_crystal"));
        return endCrystal
                || source.is(DamageTypeTags.IS_EXPLOSION)
                || source.is(DamageTypes.EXPLOSION)
                || source.is(DamageTypes.PLAYER_EXPLOSION)
                || source.is(DamageTypes.BAD_RESPAWN_POINT)
                || source.is(DamageTypes.FIREWORKS);
    }
}
