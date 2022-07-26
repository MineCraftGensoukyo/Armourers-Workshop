package moe.plushie.armourers_workshop.init.platform.forge.builder;

import moe.plushie.armourers_workshop.ArmourersWorkshop;
import moe.plushie.armourers_workshop.api.other.builder.IItemGroupBuilder;
import moe.plushie.armourers_workshop.utils.ObjectUtils;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ItemGroupBuilderImpl<T extends CreativeModeTab> implements IItemGroupBuilder<T> {

    protected Supplier<Supplier<ItemStack>> icon = () -> () -> ItemStack.EMPTY;
    protected BiConsumer<List<ItemStack>, CreativeModeTab> appendItems;

    public ItemGroupBuilderImpl() {
    }

    @Override
    public IItemGroupBuilder<T> icon(Supplier<Supplier<ItemStack>> icon) {
        this.icon = icon;
        return this;
    }

    @Override
    public IItemGroupBuilder<T> appendItems(BiConsumer<List<ItemStack>, CreativeModeTab> appendItems) {
        this.appendItems = appendItems;
        return this;
    }

    @Override
    public T build(String name) {
        return ObjectUtils.unsafeCast(creativeModeTab(name));
    }

    private CreativeModeTab creativeModeTab(String name) {
        return new CreativeModeTab(ArmourersWorkshop.MOD_ID + "." + name) {

            @Override
            public ItemStack makeIcon() {
                return icon.get().get();
            }

            @Override
            public void fillItemList(NonNullList<ItemStack> arg) {
                if (appendItems != null) {
                    appendItems.accept(arg, this);
                }
            }
        };
    }
}
