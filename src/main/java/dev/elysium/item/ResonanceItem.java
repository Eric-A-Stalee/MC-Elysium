package dev.elysium.item;

import dev.elysium.forge.ForgeDiscoveries;
import dev.elysium.forge.ForgeLore;
import dev.elysium.forge.StructureSurvey;
import dev.elysium.portal.ElysiumTravel;
import dev.elysium.registry.ModBlocks;
import dev.elysium.structure.TartarusChainPiece;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Server-owned survey and bearings. No ticking scan, client packet or forced-load compass is needed. */
public final class ResonanceItem extends Item {
    private final boolean forge;
    public ResonanceItem(Properties properties,boolean forge){super(properties);this.forge=forge;}
    public static CompoundTag data(ItemStack stack){return stack.getOrDefault(DataComponents.CUSTOM_DATA,CustomData.EMPTY).copyTag();}
    public static UUID identity(ItemStack stack) {
        var data=data(stack);
        if(data.hasUUID("elysium_survey_id"))return data.getUUID("elysium_survey_id");
        var id=UUID.randomUUID();CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putUUID("elysium_survey_id",id));return id;
    }
    public static boolean identifies(ItemStack stack,UUID id) {
        var data=data(stack);return (stack.getItem() instanceof ResonanceItem||stack.getItem() instanceof DawnWayfinderItem) && data.hasUUID("elysium_survey_id")&&id.equals(data.getUUID("elysium_survey_id"));
    }
    public static void rememberForge(ItemStack stack,BlockPos pos) {
        CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putLong("elysium_forge",pos.asLong()));
    }
    public static void bind(ItemStack stack,BlockPos target) {
        CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.putLong("elysium_target",target.asLong()));
    }
    public static Optional<BlockPos> target(ItemStack stack) {
        var data=data(stack);return data.contains("elysium_target",Tag.TAG_LONG)?Optional.of(BlockPos.of(data.getLong("elysium_target"))):Optional.empty();
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        var stack=player.getItemInHand(hand);
        if(player instanceof ServerPlayer server) {
            if(!level.dimension().equals(ElysiumTravel.DIMENSION)) {ForgeLore.tell(server,"outside");return InteractionResultHolder.fail(stack);}
            if(player.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(stack);
            player.getCooldowns().addCooldown(this,30);
            var destination=forge?StructureSurvey.Destination.FORGE:player.getY()<dev.elysium.worldgen.TartarusTerrain.TOP?StructureSurvey.Destination.PROMETHEUS:StructureSurvey.Destination.CHAIN;
            if(!forge && player.isShiftKeyDown()) {
                var data=data(stack);
                if(data.contains("elysium_forge",Tag.TAG_LONG)) bearing(server,BlockPos.of(data.getLong("elysium_forge")),true);
                else ForgeLore.tell(server,"no_memory");
            } else if(forge&&player.isShiftKeyDown() || target(stack).isEmpty() || !data(stack).getString("elysium_target_kind").equals(destination.name())) {
                if(forge&&player.isShiftKeyDown())CustomData.update(DataComponents.CUSTOM_DATA,stack,t->t.remove("elysium_target"));
                StructureSurvey.begin(server,stack,destination);
            } else if(destination==StructureSurvey.Destination.PROMETHEUS)deepBearing(server,target(stack).orElseThrow());
            else bearing(server,target(stack).orElseThrow(),forge);
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public InteractionResult useOn(UseOnContext context) {
        if(!forge && context.getPlayer() instanceof ServerPlayer player && context.getLevel().getBlockState(context.getClickedPos()).is(ModBlocks.TARTARUS_CHAIN_BLOCK.get())) {
            if(!player.level().dimension().equals(ElysiumTravel.DIMENSION))return InteractionResult.PASS;
            var structure=player.serverLevel().registryAccess().registryOrThrow(Registries.STRUCTURE).get(StructureSurvey.Destination.CHAIN.id());
            if(structure==null)return InteractionResult.PASS;
            var start=player.serverLevel().structureManager().getStructureWithPieceAt(context.getClickedPos(),structure);
            if(start.isValid())for(var piece:start.getPieces()) if(piece instanceof TartarusChainPiece chain) {
                var p=chain.plan();
                if(context.getClickedPos().getY()<=p.floor()+8 && context.getClickedPos().distToCenterSqr(p.x()+.5,p.floor()+6.5,p.z()+.5)<=16) {
                    readAnchor(player,context.getItemInHand());return InteractionResult.SUCCESS;
                }
            }
            ForgeLore.tell(player,"follow_chain");return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    public static void readAnchor(ServerPlayer player,ItemStack relic) {
        if(!player.level().dimension().equals(ElysiumTravel.DIMENSION))return;
        if(ForgeDiscoveries.get(player.serverLevel()).discover(player.getUUID(),ForgeDiscoveries.ANCHOR)) {
            ForgeLore.give(player,ForgeLore.book(ForgeLore.Record.ANCHOR));
            player.serverLevel().sendParticles(ParticleTypes.SOUL,player.getX(),player.getY()+1,player.getZ(),16,.7,.4,.7,.01);
        }
        CustomData.update(DataComponents.CUSTOM_DATA,relic,t->t.putBoolean("elysium_anchor_heard",true));
        ForgeLore.award(player,"voice_in_iron");ForgeLore.tell(player,"anchor");
        player.level().playSound(null,player.blockPosition(),SoundEvents.AMETHYST_BLOCK_RESONATE,SoundSource.PLAYERS,1,.5f);
    }
    public static String direction(int dx,int dz) {
        String[] directions={"north","northeast","east","southeast","south","southwest","west","northwest"};
        return directions[Math.floorMod((int)Math.round(Math.atan2(dx,-dz)/(Math.PI/4)),8)];
    }
    public static void bearing(ServerPlayer player,BlockPos target,boolean forge) {
        if(forge && arrived(player,target)) {
            player.displayClientMessage(Component.translatable("message.elysium.forge.arrived"),true);
            return;
        }
        int dx=target.getX()-player.getBlockX(),dz=target.getZ()-player.getBlockZ();
        double distance=Math.hypot(dx,dz);
        var direction=Component.translatable("direction.elysium."+direction(dx,dz));
        String strength=distance<32?"near":distance<192?"strong":distance<1024?"clear":"distant";
        player.displayClientMessage(Component.translatable("message.elysium.forge.bearing."+(forge?"forge":"chain"),
                direction,Component.translatable("message.elysium.forge.distance."+strength)),true);
        player.level().playSound(null,player.blockPosition(),SoundEvents.AMETHYST_BLOCK_CHIME,SoundSource.PLAYERS,.7f,distance<192?1.4f:.7f);
        if(distance>3)for(int i=1;i<=6;i++)player.serverLevel().sendParticles(ParticleTypes.END_ROD,
                player.getX()+dx/distance*i*.55,player.getY()+1.1,player.getZ()+dz/distance*i*.55,1,0,0,0,0);
    }
    public static boolean arrived(ServerPlayer player,BlockPos target) {
        return target.distToCenterSqr(player.getX(),player.getY(),player.getZ()) <= 64;
    }
    public static void deepBearing(ServerPlayer player,BlockPos target){
        int dx=target.getX()-player.getBlockX(),dz=target.getZ()-player.getBlockZ();
        player.displayClientMessage(Component.translatable("message.elysium.prometheus.bearing",Component.translatable("direction.elysium."+direction(dx,dz)),Math.round(Math.hypot(dx,dz)),target.getY()),true);
        player.level().playSound(null,player.blockPosition(),SoundEvents.CHAIN_HIT,SoundSource.PLAYERS,.6f,.5f);
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> lines,TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.elysium."+(forge?"forge_survey":"chain_resonator")).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip.elysium."+(forge?"survey_again":"forge_memory")).withStyle(ChatFormatting.GRAY));
        if(data(stack).getBoolean("elysium_anchor_heard"))lines.add(Component.translatable("tooltip.elysium.anchor_heard").withStyle(ChatFormatting.DARK_AQUA));
    }
}
