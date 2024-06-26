package extensions.net.minecraft.client.renderer.block.model.ItemOverrides;

import manifold.ext.rt.api.Extension;
import manifold.ext.rt.api.This;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

@Extension
public class ItemOverridesExt {

    @Nullable
    public static BakedModel resolve(@This ItemOverrides itemOverrides, BakedModel bakedModel, ItemStack itemStack, @Nullable ClientLevel clientLevel, @Nullable LivingEntity livingEntity, int index) {
        return itemOverrides.resolve(bakedModel, itemStack, clientLevel, livingEntity);
    }
}
