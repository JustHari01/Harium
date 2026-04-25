package me.jellysquid.mods.lithium.mixin.ai.useless_sensors.goat_item_sensor;

import me.jellysquid.mods.lithium.common.ai.brain.SensorHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.sensor.SensorType;
import net.minecraft.entity.passive.GoatEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GoatEntity.class)
public abstract class GoatEntityMixin extends LivingEntity {

    protected GoatEntityMixin(EntityType<? extends GoatEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void disableItemSensor(CallbackInfo ci) {
        if (this.getWorld().isClient) {
            return;
        }
        SensorHelper.disableSensor(this, SensorType.NEAREST_ITEMS);
    }
}
