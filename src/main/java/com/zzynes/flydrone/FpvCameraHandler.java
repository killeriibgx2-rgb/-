package com.zzynes.flydrone;

import com.zzynes.flydrone.entity.FpvDroneEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ComputeFovModifierEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Quaternionf;

@Mod.EventBusSubscriber(modid = FlyDroneMod.MOD_ID, value = Dist.CLIENT)
public class FpvCameraHandler {

    @SubscribeEvent
    public static void onCameraSetup(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (!(player.getVehicle() instanceof FpvDroneEntity drone)) return;

        // Применяем ориентацию дрона к камере
        Quaternionf orient = drone.getOrientation();
        float pitch = (float) Math.toDegrees(Math.asin(-2.0 * (orient.x * orient.z - orient.w * orient.y)));
        float yaw = (float) Math.toDegrees(Math.atan2(2.0 * (orient.x * orient.y + orient.w * orient.z), orient.w * orient.w + orient.x * orient.x - orient.y * orient.y - orient.z * orient.z));
        float roll = (float) Math.toDegrees(Math.atan2(2.0 * (orient.y * orient.z + orient.w * orient.x), orient.w * orient.w - orient.x * orient.x - orient.y * orient.y + orient.z * orient.z));

        event.setPitch(pitch);
        event.setYaw(yaw);
        event.setRoll(roll);
    }

    @SubscribeEvent
    public static void onFov(ComputeFovModifierEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (player.getVehicle() instanceof FpvDroneEntity) {
            event.setNewFovModifier(1.2f); // Широкий FOV как у FPV камеры
        }
    }
}