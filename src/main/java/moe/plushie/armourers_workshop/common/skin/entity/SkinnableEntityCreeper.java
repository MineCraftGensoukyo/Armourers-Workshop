package moe.plushie.armourers_workshop.common.skin.entity;

import java.util.ArrayList;

import moe.plushie.armourers_workshop.api.client.render.entity.ISkinnableEntityRenderer;
import moe.plushie.armourers_workshop.api.common.skin.entity.ISkinnableEntity;
import moe.plushie.armourers_workshop.api.common.skin.type.ISkinType;
import moe.plushie.armourers_workshop.client.render.entity.SkinnableEntityCreeperRenderer;
import moe.plushie.armourers_workshop.common.skin.type.SkinTypeRegistry;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class SkinnableEntityCreeper implements ISkinnableEntity {

    @Override
    public ArrayList<Class<? extends EntityLivingBase>> getEntityClass() {
        ArrayList<Class<? extends EntityLivingBase>> classes = new ArrayList<Class<? extends EntityLivingBase>>();
        classes.add(EntityCreeper.class);
        return classes;
    }

    @SideOnly(Side.CLIENT)
    @Override
    public Class<? extends ISkinnableEntityRenderer> getRendererClass() {
        return SkinnableEntityCreeperRenderer.class;
    }

    @Override
    public boolean canUseWandOfStyle() {
        return true;
    }

    @Override
    public boolean canUseSkinsOnEntity() {
        return false;
    }

    @Override
    public void getValidSkinTypes(ArrayList<ISkinType> skinTypes) {
        skinTypes.add(SkinTypeRegistry.skinHead);
    }

}
