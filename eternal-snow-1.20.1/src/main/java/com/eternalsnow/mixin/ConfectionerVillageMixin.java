package com.eternalsnow.mixin;

import com.eternalsnow.village.ConfectionerHouseProcessor;
import com.mojang.datafixers.util.Either;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SinglePoolElement.class)
public abstract class ConfectionerVillageMixin {
    @Shadow @Final protected Either<ResourceLocation,StructureTemplate> template;
    @Inject(method="getSettings",at=@At("RETURN"))
    private void eternalsnow$decorateNewHouse(Rotation rotation,BoundingBox bounds,boolean keepJigsaws,CallbackInfoReturnable<StructurePlaceSettings> cir) {
        template.left().ifPresent(id->{
            if(id.getNamespace().equals("minecraft") && id.getPath().startsWith("village/")
                    && id.getPath().contains("/houses/") && !id.getPath().contains("zombie"))
                cir.getReturnValue().addProcessor(ConfectionerHouseProcessor.INSTANCE);
        });
    }
}
