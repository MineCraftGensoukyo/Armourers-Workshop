package moe.plushie.armourers_workshop.core.tileentity;

import moe.plushie.armourers_workshop.utils.Rectangle3f;
import net.minecraft.block.BlockState;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public abstract class RotableContainerTileEntity extends AbstractContainerTileEntity {

    public static final AxisAlignedBB ZERO_BOX = new AxisAlignedBB(0, 0, 0, 0, 0, 0);

    private AxisAlignedBB renderBoundingBox;

    public RotableContainerTileEntity(TileEntityType<?> entityType) {
        super(entityType);
    }

    public void setRenderChanged() {
        renderBoundingBox = null;
    }

    @OnlyIn(Dist.CLIENT)
    public Quaternion getRenderRotations() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    public Rectangle3f getRenderBoundingBox(BlockState state) {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (renderBoundingBox != null) {
            return renderBoundingBox;
        }
        Rectangle3f rect = getRenderBoundingBox(getBlockState());
        if (rect == null) {
            return ZERO_BOX;
        }
        Quaternion quaternion = getRenderRotations();
        if (quaternion != null) {
            rect.mul(quaternion);
        }
        renderBoundingBox = rect.asAxisAlignedBB().move(Vector3d.atCenterOf(getBlockPos()));
        return renderBoundingBox;
    }
}