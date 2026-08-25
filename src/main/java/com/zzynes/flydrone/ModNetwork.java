package com.zzynes.flydrone;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(FlyDroneMod.MOD_ID, "main"),
            () -> "1",
            s -> true,
            s -> true
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, DroneControlPacket.class,
                DroneControlPacket::encode,
                DroneControlPacket::decode,
                DroneControlPacket::handle);
    }
}