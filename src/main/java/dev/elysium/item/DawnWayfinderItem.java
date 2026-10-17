package dev.elysium.item;

import dev.elysium.forge.*;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.story.RealmCycle;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

/** Follows the next unanswered tradition. Survey work uses the same bounded queue as the folio. */
public final class DawnWayfinderItem extends Item {
    public DawnWayfinderItem(Properties p){super(p);}
    public static int next(int mask){for(int i=0;i<3;i++)if((mask&(1<<i))==0)return i;return -1;}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(!(player instanceof ServerPlayer sp))return InteractionResultHolder.success(stack);
        if(!level.dimension().equals(ElysiumTravel.DIMENSION)){ForgeLore.tell(sp,"outside");return InteractionResultHolder.fail(stack);}
        if(player.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(stack);player.getCooldowns().addCooldown(this,30);
        var cycle=RealmCycle.get(sp.serverLevel());int kind=next(cycle.watches());
        if(player.isShiftKeyDown()||kind<0) {
            if(cycle.forge()!=null)ResonanceItem.bearing(sp,cycle.forge(),true);else ForgeLore.tell(sp,"no_memory");
            return InteractionResultHolder.consume(stack);
        }
        var data=ResonanceItem.data(stack);
        if(data.getInt("watch_kind")!=kind||ResonanceItem.target(stack).isEmpty()) {
            CustomData.update(DataComponents.CUSTOM_DATA,stack,t->{t.putInt("watch_kind",kind);t.remove("elysium_target");});
            StructureSurvey.begin(sp,stack,switch(kind){case 0->StructureSurvey.Destination.HARVEST_WATCH;case 1->StructureSurvey.Destination.WATER_WATCH;default->StructureSurvey.Destination.HIGH_WATCH;});
        } else bearing(sp,ResonanceItem.target(stack).orElseThrow(),kind);
        return InteractionResultHolder.consume(stack);
    }
    public static void bearing(ServerPlayer player,BlockPos target,int kind) {
        int dx=target.getX()-player.getBlockX(),dz=target.getZ()-player.getBlockZ();double distance=Math.hypot(dx,dz);
        player.displayClientMessage(Component.translatable("message.elysium.watch.bearing",Component.translatable("watch.elysium."+kind),Component.translatable("direction.elysium."+ResonanceItem.direction(dx,dz)),Math.round(distance)),true);
        if(distance>3)for(int i=1;i<=6;i++)player.serverLevel().sendParticles(ParticleTypes.END_ROD,player.getX()+dx/distance*i*.6,player.getY()+1.1,player.getZ()+dz/distance*i*.6,1,0,0,0,0);
    }
    @Override public void appendHoverText(ItemStack s,TooltipContext context,List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("tooltip.elysium.dawn_wayfinder"));}
}
