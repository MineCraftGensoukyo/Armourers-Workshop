package moe.plushie.armourers_workshop.core.armature.thirdparty;

import moe.plushie.armourers_workshop.api.client.model.IModelHolder;
import moe.plushie.armourers_workshop.api.data.IDataPackObject;
import moe.plushie.armourers_workshop.api.math.ITransformf;
import moe.plushie.armourers_workshop.core.armature.ArmatureBuilder;
import moe.plushie.armourers_workshop.core.armature.ArmatureModifier;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;

public class EpicFightArmatureBuilder extends ArmatureBuilder {

    public EpicFightArmatureBuilder(ResourceLocation name) {
        super(name);
    }

    @Override
    public ITransformf buildTransform(Collection<ArmatureModifier> modifiers, IModelHolder<?> model) {
        ITransformf transform = super.buildTransform(modifiers, model);
        return poseStack -> {
            transform.apply(poseStack);
            poseStack.scale(-1, -1, 1);
        };
    }

    @Override
    public Collection<ArmatureModifier> getTargets(IDataPackObject object) {
        switch (object.type()) {
            case DICTIONARY: {
                return getTargets(object.get("target"));
            }
            case STRING: {
                String value = object.stringValue();
                if (!value.isEmpty()) {
                    return Collections.singleton(new EpicFightJointBinder(value));
                }
                return Collections.emptyList();
            }
            default: {
                return Collections.emptyList();
            }
        }
    }
}
