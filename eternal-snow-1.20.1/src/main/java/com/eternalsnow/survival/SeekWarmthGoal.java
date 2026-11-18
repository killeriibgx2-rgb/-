package com.eternalsnow.survival;

import com.eternalsnow.state.EternalSnowState;
import com.eternalsnow.thermal.ThermalEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.EnumSet;
import java.util.WeakHashMap;

/** Bounded, low-priority navigation for all AI mobs, using their own movement medium. */
@Mod.EventBusSubscriber(modid="eternalsnow")
public final class SeekWarmthGoal extends Goal {
    private static final WeakHashMap<ServerLevel,long[]> BUDGET = new WeakHashMap<>();
    private final Mob mob;
    private long nextSearch, deadline;
    private Path path;
    private BlockPos target;
    public SeekWarmthGoal(Mob mob) { this.mob=mob;setFlags(EnumSet.of(Flag.MOVE)); }
    @SubscribeEvent public static void join(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof Mob mob) {
            // Unloading/reloading can rejoin the same instance.
            if(mob.goalSelector.getAvailableGoals().stream().noneMatch(g -> g.getGoal() instanceof SeekWarmthGoal))
                mob.goalSelector.addGoal(7,new SeekWarmthGoal(mob));
        }
    }
    private boolean allowed() {
        var s=EternalSnowState.getServerSettings();
        return s.enabled && s.enablePlayerTemperature && EternalSnowState.isOverworldLike(mob.level())
                && !mob.isNoAi() && !mob.isPassenger() && !mob.isLeashed()
                && mob.getTarget()==null && mob.getLastHurtByMob()==null && mob.isAlive();
    }
    private static boolean hazard(ServerLevel level,BlockPos pos) {
        if(!level.hasChunkAt(pos))return true;
        var s=level.getBlockState(pos);
        return s.is(Blocks.LAVA)||s.is(Blocks.FIRE)||s.is(Blocks.SOUL_FIRE)||s.is(Blocks.MAGMA_BLOCK)
                ||s.is(Blocks.CACTUS)||s.is(Blocks.SWEET_BERRY_BUSH)||s.is(Blocks.POWDER_SNOW)
                ||s.is(Blocks.WITHER_ROSE)||(s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT));
    }
    private boolean safe(ServerLevel level,BlockPos pos) {
        // Include footprint/headroom, and reject paths touching unloaded chunk boundaries.
        int radius=Math.max(1,(int)Math.ceil(mob.getBbWidth()/2));
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++)
            for(int y=-1;y<=Math.ceil(mob.getBbHeight());y++)
                if(hazard(level,pos.offset(x,y,z)))return false;
        return true;
    }
    @Override public boolean canUse() {
        if(!(mob.level() instanceof ServerLevel level)||!allowed()||!mob.getNavigation().isDone())return false;
        long now=level.getGameTime();
        if(now<nextSearch)return false;
        nextSearch=now+100+mob.getRandom().nextInt(100);
        long[] budget=BUDGET.computeIfAbsent(level,l->new long[]{-1,0});
        if(budget[0]!=now){budget[0]=now;budget[1]=0;}
        if(budget[1]>=4)return false;
        budget[1]++;
        float current=ThermalEngine.getLocalTemperatureC(level,mob.blockPosition());
        if(current>=4)return false;
        target=null;float best=current+2;
        for(int i=0;i<8;i++) {
            var pos=mob.blockPosition().offset(mob.getRandom().nextInt(17)-8,mob.getRandom().nextInt(7)-3,mob.getRandom().nextInt(17)-8);
            if(!safe(level,pos))continue;
            // Aquatic creatures stay submerged; terrestrial creatures don't seek a freezing bath.
            if(mob.isInWaterOrBubble()!=level.getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER))continue;
            if(!mob.getNavigation().isStableDestination(pos))continue;
            float temp=ThermalEngine.getLocalTemperatureC(level,pos);
            if(temp>best && temp<=35){best=temp;target=pos;}
        }
        if(target==null)return false;
        path=mob.getNavigation().createPath(target,0);
        if(path==null||!path.canReach())return false;
        for(int i=0;i<path.getNodeCount();i++)if(!safe(level,path.getNode(i).asBlockPos()))return false;
        return true;
    }
    @Override public void start(){deadline=mob.level().getGameTime()+160;mob.getNavigation().moveTo(path,1.0);}
    @Override public boolean canContinueToUse(){return allowed() && mob.level().getGameTime()<deadline
            && !mob.getNavigation().isDone() && target!=null && mob.level() instanceof ServerLevel level
            && safe(level,target);}
    @Override public void tick() {
        // Revalidate the next step: a player may light a fire after path creation.
        if (mob.level() instanceof ServerLevel level) {
            Path active=mob.getNavigation().getPath();
            if(active!=null && !active.isDone() && !safe(level,active.getNextNodePos()))
                mob.getNavigation().stop();
        }
    }
    @Override public void stop(){mob.getNavigation().stop();path=null;target=null;}
}
