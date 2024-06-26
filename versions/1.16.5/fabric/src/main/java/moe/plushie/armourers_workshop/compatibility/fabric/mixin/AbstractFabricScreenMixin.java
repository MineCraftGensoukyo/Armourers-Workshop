package moe.plushie.armourers_workshop.compatibility.fabric.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import moe.plushie.armourers_workshop.init.platform.fabric.event.RenderTooltipCallback;
import moe.plushie.armourers_workshop.utils.MatrixUtils;
import moe.plushie.armourers_workshop.utils.ObjectUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(Screen.class)
public class AbstractFabricScreenMixin {

    private ItemStack aw$tooltipStack = ItemStack.EMPTY;

    @Inject(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;II)V", at = @At(value = "HEAD"))
    public void aw2$renderTooltipPre(PoseStack poseStack, ItemStack itemStack, int i, int j, CallbackInfo ci) {
        aw$tooltipStack = itemStack;
    }

    @Inject(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemStack;II)V", at = @At(value = "RETURN"))
    public void aw2$renderTooltipPost(PoseStack poseStack, ItemStack itemStack, int i, int j, CallbackInfo ci) {
        aw$tooltipStack = ItemStack.EMPTY;
    }

    @Inject(method = "renderTooltip(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/util/List;II)V", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD)
    public void aw2$renderTooltip(PoseStack poseStack, List<? extends FormattedCharSequence> list, int mouseX, int mouseY, CallbackInfo ci, int w, int x, int y, int w2, int h) {
        Screen screen = ObjectUtils.unsafeCast(this);
        int screenWidth = screen.width;
        int screenHeight = screen.height;
        RenderTooltipCallback.EVENT.invoker().onRenderTooltip(MatrixUtils.of(poseStack), aw$tooltipStack, x, y, w, h, mouseX, mouseY, screenWidth, screenHeight);
    }
}
