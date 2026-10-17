package dev.elysium.story;

import dev.elysium.block.PandoraVesselBlock;
import dev.elysium.forge.ForgeLore;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModBlocks;
import dev.elysium.structure.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.*;

/** Called only after a real complete batch. Failed, paused or obstructed work cannot open the vessel. */
public final class FirstFlameConsequences {
    public static void completed(ServerLevel l,ForgePlan plan){
        if(!l.dimension().equals(ElysiumTravel.DIMENSION))return;
        boolean first=RealmCycle.get(l).recordWork(ForgeFixtures.hearth(plan));
        var vessel=PandoraVesselBlock.position(plan);var chunk=l.getChunkSource().getChunkNow(vessel.getX()>>4,vessel.getZ()>>4);
        if(chunk!=null&&chunk.getBlockState(vessel).is(ModBlocks.PANDORA_VESSEL.get())){
            l.setBlock(vessel,chunk.getBlockState(vessel).setValue(PandoraVesselBlock.OPEN,true),3);
            l.sendParticles(ParticleTypes.LARGE_SMOKE,vessel.getX()+.5,vessel.getY()+1,vessel.getZ()+.5,70,.8,.6,.8,.035);
            l.sendParticles(ParticleTypes.ASH,vessel.getX()+.5,vessel.getY()+1,vessel.getZ()+.5,50,1,1,1,.08);
        }
        if(first){l.playSound(null,ForgeFixtures.hearth(plan),SoundEvents.IRON_GOLEM_DEATH,SoundSource.BLOCKS,2,.4f);RealmClock.announce(l,"sunset");for(var p:l.players())ForgeLore.award(p,"pandora_opened");}
    }
    private FirstFlameConsequences(){}
}
