package moe.plushie.armourers_workshop.common.blocks;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

public class BlockSkinCubeGlass extends BlockSkinCube {

    public BlockSkinCubeGlass(String name, boolean glowing) {
        super(name, glowing);
        setSortPriority(121);
        if (glowing) {
            setSortPriority(120);
        }
    }
    
    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }
    
    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }
    
    @Override
    public boolean isNormalCube(IBlockState state, IBlockAccess world, BlockPos pos) {
        return false;
    }
    
    @Override
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        if (blockAccess.getBlockState(pos.offset(side)).getBlock() == this) {
            return false;
        }
        return super.shouldSideBeRendered(blockState, blockAccess, pos, side);
    }
}
