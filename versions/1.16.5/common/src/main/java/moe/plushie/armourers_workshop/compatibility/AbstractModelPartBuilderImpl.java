package moe.plushie.armourers_workshop.compatibility;

import moe.plushie.armourers_workshop.utils.ModelPartBuilder;
import moe.plushie.armourers_workshop.utils.math.Rectangle3f;
import net.minecraft.client.model.geom.ModelPart;

public class AbstractModelPartBuilderImpl extends ModelPartBuilder {

    public AbstractModelPartBuilderImpl(int width, int texHeight) {
        super(width, texHeight);
    }

    @Override
    public ModelPart build() {
        ModelPart part = new ModelPart(texWidth, texHeight, texU, texV);
        part.setPos(offset.getX(), offset.getY(), offset.getZ());
        for (Cube cube : cubes) {
            Rectangle3f rect = cube.rect;
            float x = rect.getX();
            float y = rect.getY();
            float z = rect.getZ();
            float w = rect.getWidth();
            float h = rect.getHeight();
            float d = rect.getDepth();
            part.addBox(x, y, z, w, h, d, cube.scale);
        }
        return part;
    }
}
