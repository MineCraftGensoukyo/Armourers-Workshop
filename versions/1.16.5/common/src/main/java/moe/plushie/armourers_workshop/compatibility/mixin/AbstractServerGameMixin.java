package moe.plushie.armourers_workshop.compatibility.mixin;

import moe.plushie.armourers_workshop.core.entity.MannequinEntity;
import moe.plushie.armourers_workshop.utils.DataFixerUtils;
import moe.plushie.armourers_workshop.utils.ObjectUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public class AbstractServerGameMixin {

    @Inject(method = "noBlocksAround", at = @At("RETURN"), cancellable = true)
    private void aw2$noBlocksAround(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue()) {
            return;
        }
        ServerLevel level = ObjectUtils.safeCast(entity.level, ServerLevel.class);
        if (level != null) {
            AABB alignedBB = entity.getBoundingBox().inflate(0.0625D).expandTowards(0.0D, -0.55D, 0.0D);
            boolean flag = level.getEntityCollisions(entity, alignedBB, e -> e instanceof MannequinEntity).allMatch(VoxelShape::isEmpty);
            callback.setReturnValue(flag);
        }
    }
}
