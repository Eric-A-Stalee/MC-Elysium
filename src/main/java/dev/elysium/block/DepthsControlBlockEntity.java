package dev.elysium.block;

import dev.elysium.entity.SlagChimeraEntity;
import dev.elysium.forge.ForgeLore;
import dev.elysium.registry.ModBlockEntities;
import dev.elysium.registry.ModBlocks;
import dev.elysium.registry.ModEntities;
import dev.elysium.registry.ModItems;
import dev.elysium.structure.TartarusDepthsPlan;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Saved bridge repair and encounter ownership. Never force-loads the arena. */
public final class DepthsControlBlockEntity extends BlockEntity {
    private TartarusDepthsPlan plan;
    private UUID creature;
    private boolean repaired,defeated;
    private int missingTicks;
    private final Set<UUID> claimed=new HashSet<>();
    public DepthsControlBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntities.DEPTHS_CONTROL.get(),pos,state);}
    public void bind(TartarusDepthsPlan value){plan=value;setChanged();}
    public TartarusDepthsPlan plan(){return plan;}
    public UUID creature(){return creature;}
    public boolean defeated(){return defeated;}
    public boolean repaired(){return repaired;}
    private boolean heart(){return getBlockState().is(ModBlocks.TARTARUS_HEART.get());}
    private void tell(Player player,String key){player.displayClientMessage(Component.translatable("message.elysium.depths."+key),false);}
    public void use(Player player,ItemStack held) {
        if(!(level instanceof ServerLevel server)||plan==null){tell(player,"unbound");return;}
        if(!heart()){repair(player,held);return;}
        if(defeated){claim(player,held);return;}
        if(creature!=null){tell(player,"awake");return;}
        if(!held.is(ModItems.CHAIN_RESONATOR.get())){tell(player,"resonator");return;}
        if(!arenaLoaded(server)){tell(player,"unloaded");return;}
        var mob=ModEntities.SLAG_CHIMERA.get().create(server);
        if(mob==null)return;
        mob.bind(worldPosition,Vec3.atBottomCenterOf(plan.monster()));mob.setYRot(25);mob.setYHeadRot(25);mob.yBodyRot=25;
        if(server.addFreshEntity(mob)){creature=mob.getUUID();missingTicks=0;setChanged();tell(player,"awakening");}
    }
    public boolean repair(Player player,ItemStack held) {
        if(!(level instanceof ServerLevel server)||plan==null||heart())return false;
        if(repaired){tell(player,"repaired");return false;}
        if(!player.getAbilities().instabuild&&(!held.is(Items.CHAIN)||held.getCount()<4)){tell(player,"chains");return false;}
        for(int u=6;u<=22;u++)for(int v=189;v<=191;v++) {
            var pos=plan.at(u,-184,v);
            if(server.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null){tell(player,"unloaded");return false;}
            if(!server.getBlockState(pos).isAir()&&!((u==7||u==21)&&v!=190&&server.getBlockState(pos).is(Blocks.CHAIN))){tell(player,"obstructed");return false;}
            if(!server.getBlockState(plan.at(u,-200,v)).equals(dev.elysium.structure.TartarusDepthsPiece.fallenDeck(v))){tell(player,"span_missing");return false;}
        }
        for(int u=6;u<=22;u++)for(int v=189;v<=191;v++) {
            server.setBlock(plan.at(u,-200,v),Blocks.AIR.defaultBlockState(),3);
            server.setBlock(plan.at(u,-184,v),dev.elysium.structure.TartarusDepthsPiece.fallenDeck(v),3);
        }
        if(!player.getAbilities().instabuild)held.shrink(4);
        repaired=true;setChanged();server.playSound(null,worldPosition,SoundEvents.CHAIN_PLACE,SoundSource.BLOCKS,1,.6f);tell(player,"repaired");
        if(player instanceof ServerPlayer p)ForgeLore.award(p,"tartarus_crossing");return true;
    }
    private boolean arenaLoaded(ServerLevel server) {
        var p=plan.monster();
        for(int x=(p.getX()-16)>>4;x<=(p.getX()+16)>>4;x++)for(int z=(p.getZ()-16)>>4;z<=(p.getZ()+16)>>4;z++)
            if(server.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }
    public void defeat(UUID id) {
        if(!id.equals(creature)||defeated||!(level instanceof ServerLevel server))return;
        defeated=true;setChanged();
        for(var player:server.players())if(player.distanceToSqr(Vec3.atCenterOf(plan.monster()))<64*64)claim(player,ItemStack.EMPTY);
    }
    public boolean claim(Player player,ItemStack held) {
        if(!defeated||!(player instanceof ServerPlayer server))return false;
        boolean previous=claimed.contains(player.getUUID());
        boolean has=false;
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(ModItems.FIRST_FLAME.get()))has=true;
        if(previous&&(has||!held.is(ModItems.ELYSIUM_FRAGMENT.get()))){tell(player,"claimed");return false;}
        if(previous&&!player.getAbilities().instabuild)held.shrink(1);
        claimed.add(player.getUUID());setChanged();ForgeLore.give(server,new ItemStack(ModItems.FIRST_FLAME.get()));
        ForgeLore.award(server,"first_flame");tell(player,"flame");return true;
    }
    public static void tick(Level level,BlockPos pos,BlockState state,DepthsControlBlockEntity control) {
        if(!(level instanceof ServerLevel server)||!control.heart()||control.plan==null||control.creature==null||control.defeated||level.getGameTime()%20!=0)return;
        // The creature never leaves home. Only reconcile a missing UUID after its entire home is loaded.
        if(!control.arenaLoaded(server)){control.missingTicks=0;return;}
        if(server.getEntity(control.creature)!=null){control.missingTicks=0;return;}
        if((control.missingTicks+=20)<200)return;
        var found=server.getEntitiesOfClass(SlagChimeraEntity.class,new AABB(control.plan.monster()).inflate(20),e->pos.equals(e.control()));
        control.creature=found.isEmpty()?null:found.getFirst().getUUID();control.missingTicks=0;control.setChanged();
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider) {
        super.saveAdditional(tag,provider);
        if(plan!=null){tag.putBoolean("Bound",true);tag.putInt("X",plan.x);tag.putInt("Z",plan.z);tag.putLong("Seed",plan.seed);}
        if(creature!=null)tag.putUUID("Creature",creature);
        tag.putBoolean("Repaired",repaired);tag.putBoolean("Defeated",defeated);
        ListTag list=new ListTag();for(UUID id:claimed)list.add(StringTag.valueOf(id.toString()));tag.put("Claimed",list);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider) {
        super.loadAdditional(tag,provider);plan=tag.getBoolean("Bound")?new TartarusDepthsPlan(tag.getInt("X"),tag.getInt("Z"),tag.getLong("Seed")):null;
        if(plan!=null&&!worldPosition.equals(plan.heart())&&!worldPosition.equals(plan.bridgeControl()))plan=null;
        creature=tag.hasUUID("Creature")?tag.getUUID("Creature"):null;repaired=tag.getBoolean("Repaired");defeated=tag.getBoolean("Defeated");
        missingTicks=0;claimed.clear();for(Tag value:tag.getList("Claimed",Tag.TAG_STRING))try{claimed.add(UUID.fromString(value.getAsString()));}catch(IllegalArgumentException ignored){}
    }
}
