package com.eternalsnow.village;

import com.eternalsnow.block.ConfectionerTableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.*;
import java.util.*;

/** Runs ONLY during template placement; never scans or edits an already generated village. */
public final class ConfectionerHouseProcessor extends StructureProcessor {
    public static final ConfectionerHouseProcessor INSTANCE=new ConfectionerHouseProcessor();
    private ConfectionerHouseProcessor() {}
    @Override protected StructureProcessorType<?> getType() { return ConfectionerRegistry.HOUSE_PROCESSOR.get(); }
    @Override public List<StructureTemplate.StructureBlockInfo> finalizeProcessing(ServerLevelAccessor level,BlockPos origin,BlockPos pivot,
            List<StructureTemplate.StructureBlockInfo> original,List<StructureTemplate.StructureBlockInfo> placed,StructurePlaceSettings settings) {
        // Select by building origin, independent of how many chunks the template spans.
        if(settings.getRandom(origin).nextInt(3)!=0)return placed;
        Map<BlockPos,Integer> byPos=new HashMap<>(); boolean residential=false;
        for(int i=0;i<placed.size();i++) {
            byPos.put(placed.get(i).pos(),i);
            if(placed.get(i).state().is(BlockTags.BEDS))residential=true;
        }
        if(!residential)return placed;
        for(int i=0;i<placed.size();i++) {
            var block=placed.get(i); Integer upper=byPos.get(block.pos().above());
            boolean bench=block.state().is(Blocks.CRAFTING_TABLE);
            boolean smallTable=block.state().is(BlockTags.WOODEN_FENCES) && upper!=null
                    && placed.get(upper).state().is(BlockTags.WOODEN_PRESSURE_PLATES);
            if(!bench && !smallTable)continue;
            // Do not replace containers, beds or another profession's workstation.
            if(bench && (upper==null || !placed.get(upper).state().isAir()))continue;
            List<StructureTemplate.StructureBlockInfo> result=new ArrayList<>(placed);
            result.set(i,new StructureTemplate.StructureBlockInfo(block.pos(),ConfectionerRegistry.TABLE.get().defaultBlockState()
                    .setValue(ConfectionerTableBlock.FACING,settings.getRotation().rotate(Direction.NORTH)),null));
            if(smallTable)result.set(upper,new StructureTemplate.StructureBlockInfo(block.pos().above(),Blocks.AIR.defaultBlockState(),null));
            return result; // at most one workstation per eligible house
        }
        return placed;
    }
}
