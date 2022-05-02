package moe.plushie.armourers_workshop.core.item;

import moe.plushie.armourers_workshop.utils.TranslateUtils;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

@SuppressWarnings("NullableProblems")
public class FlavouredItem extends Item {

    public FlavouredItem(Properties properties) {
        super(properties);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack itemStack, @Nullable World world, List<ITextComponent> tooltips, ITooltipFlag flags) {
        super.appendHoverText(itemStack, world, tooltips, flags);
        tooltips.addAll(TranslateUtils.subtitles(getDescriptionId(itemStack) + ".flavour"));
    }
}