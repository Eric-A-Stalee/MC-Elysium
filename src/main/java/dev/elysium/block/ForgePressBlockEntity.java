package dev.elysium.block;

import dev.elysium.forge.*;
import dev.elysium.registry.*;
import dev.elysium.structure.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** A one-job physical press, with saved ingredient escrow and collect-once output. */
public final class ForgePressBlockEntity extends BlockEntity {
    public static final BlockPos CONTROL=new BlockPos(-9,1,76), FEED=new BlockPos(-9,0,76);
    private static final int[] STROKE={0,0,0,0,1,2,4,3,2,1,0};
    private ForgePlan plan;
    private String work="";
    private final List<ItemStack> escrow=new ArrayList<>();
    private ItemStack output=ItemStack.EMPTY;
    private int phase,drop,strikes;
    private boolean running;
    private UUID smith;
    public ForgePressBlockEntity(BlockPos pos,BlockState state){super(ModBlockEntities.FORGE_PRESS.get(),pos,state);}
    public void bind(ForgePlan p){if(!ForgeHammer.world(p,CONTROL.getX(),CONTROL.getY(),CONTROL.getZ()).equals(worldPosition))throw new IllegalArgumentException("Foreign press");plan=p;setChanged();}
    public ForgePlan plan(){return plan;}
    public int drop(){return drop;}
    public boolean running(){return running;}
    public ItemStack output(){return output.copy();}
    public List<ItemStack> escrow(){return escrow.stream().map(ItemStack::copy).toList();}
    public boolean powered() {
        if(!(level instanceof ServerLevel server)||plan==null)return false;
        var source=ForgeLighting.loadedService(server,ForgeFixtures.source(plan));
        var feed=ForgeLighting.loadedService(server,ForgeHammer.world(plan,FEED.getX(),FEED.getY(),FEED.getZ()));
        return source!=null&&source.source()&&source.burning()&&source.hearthRestored(server)&&feed!=null&&feed.belongsTo(plan)
            &&source.restored().contains(feed.getBlockPos())&&feed.getBlockState().getValue(ForgeServiceBlock.DECAY)==0;
    }
    public boolean queue(Player player,ForgeRecipes.Work recipe) {
        if(plan==null||recipe==null||!work.isEmpty()||!output.isEmpty()||!powered())return false;
        var inventory=player.getInventory();
        // Check the complete bill first. Failed jobs cannot eat a subset of the materials.
        for(var cost:recipe.costs()) {
            int total=0;for(int i=0;i<inventory.getContainerSize();i++)if(inventory.getItem(i).is(cost.item()))total+=inventory.getItem(i).getCount();
            if(total<cost.count()&&!player.getAbilities().instabuild)return false;
        }
        if(!player.getAbilities().instabuild)for(var cost:recipe.costs()) {
            int remaining=cost.count();
            for(int i=0;i<inventory.getContainerSize()&&remaining>0;i++) {
                var stack=inventory.getItem(i);if(!stack.is(cost.item()))continue;
                int taken=Math.min(remaining,stack.getCount());escrow.add(stack.split(taken));remaining-=taken;
            }
        }
        work=recipe.id();smith=player.getUUID();phase=0;strikes=0;running=false;setChanged();inventory.setChanged();return true;
    }
    public boolean start(){if(work.isEmpty()||running||!powered())return false;running=true;setChanged();return true;}
    public void use(Player player,ItemStack held) {
        if(!(player instanceof ServerPlayer server))return;
        if(!output.isEmpty()) {
            var reward=output;output=ItemStack.EMPTY;setChanged();ForgeLore.give(server,reward);ForgeLore.award(server,"smiths_work");return;
        }
        if(!work.isEmpty()) {
            if(!running&&player.isShiftKeyDown()&&held.isEmpty()){start();tell(player,running?"started":"cold");}
            else if(!running&&held.isEmpty())tell(player,"loaded");
            else tell(player,powered()?"busy":"paused");
            return;
        }
        if(held.isEmpty()) {ForgeManual.give(server);tell(player,powered()?"ready":"cold");return;}
        if(held.is(net.minecraft.world.item.Items.BOOK)){ForgeLore.give(server,ForgeManual.book());if(!player.getAbilities().instabuild)held.shrink(1);return;}
        var recipe=ForgeRecipes.select(held);
        tell(player,queue(player,recipe)?"loaded":!powered()?"cold":"materials");
    }
    private static void tell(Player p,String key){p.displayClientMessage(Component.translatable("message.elysium.press."+key),false);}
    public void advance() {
        if(!(level instanceof ServerLevel server)||!running||!powered())return;
        int next=STROKE[phase];
        if(!ForgeHammer.move(server,plan,drop,next))return;
        if(next!=drop)server.playSound(null,worldPosition,SoundEvents.CHAIN_PLACE,SoundSource.BLOCKS,.7f,.6f);
        if(next==4&&drop!=4) {
            var impact=ForgeHammer.world(plan,0,6,76);
            server.playSound(null,impact,SoundEvents.ANVIL_LAND,SoundSource.BLOCKS,2,.5f);
            server.sendParticles(ParticleTypes.LAVA,impact.getX()+.5,impact.getY()+.1,impact.getZ()+.5,24,3,.2,2,0);
            for(var living:server.getEntitiesOfClass(LivingEntity.class,new AABB(impact).inflate(3.6,.2,2.6)))
                if(!(living instanceof Player p&&(p.isCreative()||p.isSpectator())))living.hurt(server.damageSources().anvil(null),16);
            var source=ForgeLighting.loadedService(server,ForgeFixtures.source(plan));if(source!=null)source.consume(200);
        }
        drop=next;phase++;
        if(phase>=STROKE.length) {
            phase=0;strikes++;
            if(strikes>=3) {
                var recipe=ForgeRecipes.byId(work);
                if(recipe!=null)output=recipe.output();
                escrow.clear();work="";running=false;
                if(smith!=null){var player=server.getServer().getPlayerList().getPlayer(smith);if(player!=null)tell(player,"finished");}
            }
        }
        setChanged();
    }
    public static void tick(Level level,BlockPos pos,BlockState state,ForgePressBlockEntity press){if(level.getGameTime()%5==0)press.advance();}
    public void releaseContents(){if(level==null||level.isClientSide)return;for(var stack:escrow)Block.popResource(level,worldPosition,stack);escrow.clear();if(!output.isEmpty())Block.popResource(level,worldPosition,output);output=ItemStack.EMPTY;setChanged();}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider) {
        super.saveAdditional(tag,provider);
        if(plan!=null){tag.putLong("Entrance",plan.key());tag.putInt("Turn",plan.turn());}
        tag.putString("Work",work);tag.putInt("Phase",phase);tag.putInt("Drop",drop);tag.putInt("Strikes",strikes);tag.putBoolean("Running",running);
        if(smith!=null)tag.putUUID("Smith",smith);
        var items=new ListTag();for(var stack:escrow)items.add(stack.save(provider));tag.put("Escrow",items);
        if(!output.isEmpty())tag.put("Output",output.save(provider));
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider) {
        super.loadAdditional(tag,provider);plan=null;
        if(tag.contains("Entrance",Tag.TAG_LONG)&&tag.getInt("Turn")>=0&&tag.getInt("Turn")<4){var p=BlockPos.of(tag.getLong("Entrance"));var candidate=new ForgePlan(p.getX(),p.getZ(),p.getY(),tag.getInt("Turn"));if(ForgeHammer.world(candidate,-9,1,76).equals(worldPosition))plan=candidate;}
        work=tag.getString("Work");phase=Math.floorMod(tag.getInt("Phase"),STROKE.length);drop=Math.max(0,Math.min(4,tag.getInt("Drop")));strikes=Math.max(0,Math.min(2,tag.getInt("Strikes")));running=tag.getBoolean("Running")&&!work.isEmpty();
        smith=tag.hasUUID("Smith")?tag.getUUID("Smith"):null;escrow.clear();for(var t:tag.getList("Escrow",Tag.TAG_COMPOUND)){var item=ItemStack.parseOptional(provider,(CompoundTag)t);if(!item.isEmpty())escrow.add(item);}
        output=tag.contains("Output",Tag.TAG_COMPOUND)?ItemStack.parseOptional(provider,tag.getCompound("Output")):ItemStack.EMPTY;
    }
}
