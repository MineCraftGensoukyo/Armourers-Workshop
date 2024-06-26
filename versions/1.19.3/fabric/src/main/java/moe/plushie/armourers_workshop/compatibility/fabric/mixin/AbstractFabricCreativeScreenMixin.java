package moe.plushie.armourers_workshop.compatibility.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import moe.plushie.armourers_workshop.compatibility.fabric.AbstractFabricClientNativeImpl;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public class AbstractFabricCreativeScreenMixin {

    @Inject(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;II)V", at = @At(value = "HEAD"))
    public void aw2$renderTooltipPre(PoseStack poseStack, ItemStack itemStack, int i, int j, CallbackInfo ci) {
        AbstractFabricClientNativeImpl.TOOLTIP_ITEM_STACK = itemStack;
    }

    @Inject(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;II)V", at = @At(value = "RETURN"))
    public void aw2$renderTooltipPost(PoseStack poseStack, ItemStack itemStack, int i, int j, CallbackInfo ci) {
        AbstractFabricClientNativeImpl.TOOLTIP_ITEM_STACK = ItemStack.EMPTY;
    }
}
