package dev.elysium.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Contained flame above an exposed runner. It burns entities but never spreads. */
public final class ForgeFlameBlock extends Block {
    public static final BooleanProperty RESTORED=ForgeServiceBlock.RESTORED;
    public static final BooleanProperty BRAZIER=BooleanProperty.create("brazier");
    public ForgeFlameBlock(Properties p){super(p);registerDefaultState(stateDefinition.any().setValue(RESTORED,false).setValue(BRAZIER,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(RESTORED,BRAZIER);}
    @Override protected VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return Shapes.empty();}
    @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos) {
        var below=level.getBlockState(pos.below());
        return below.getBlock() instanceof ForgeServiceBlock&&below.getValue(ForgeServiceBlock.LIT)&&below.getValue(ForgeServiceBlock.DECAY)==0;
    }
    @Override protected BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos other) {
        return d==Direction.DOWN&&!canSurvive(s,l,p)?Blocks.AIR.defaultBlockState():s;
    }
    @Override protected void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moving){l.scheduleTick(p,this,10);}
    @Override protected void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource r) {
        if(!canSurvive(s,l,p))l.removeBlock(p,false);
        else {var below=l.getBlockState(p.below());boolean restored=below.getValue(RESTORED);
            if(s.getValue(RESTORED)!=restored)l.setBlock(p,s.setValue(RESTORED,restored),3);
            l.scheduleTick(p,this,10);}
    }
    @Override protected void entityInside(BlockState s,Level l,BlockPos p,Entity entity){burn(l,entity,s.getValue(RESTORED));}
    public static void burn(Level level,Entity entity,boolean restored) {
        if(level.isClientSide||!(entity instanceof LivingEntity living)||living.fireImmune()
                ||living.hasEffect(MobEffects.FIRE_RESISTANCE)||living.isSpectator()
                ||living instanceof Player player&&player.isCreative())return;
        living.hurt(level.damageSources().inFire(),restored?3:1);
        living.igniteForSeconds(restored?6:2);
    }
    @Override public void animateTick(BlockState s,Level l,BlockPos p,RandomSource r) {
        boolean high=s.getValue(RESTORED),bowl=s.getValue(BRAZIER);
        if(r.nextInt(high?2:4)==0)l.addParticle(ParticleTypes.FLAME,p.getX()+.25+r.nextDouble()*.5,
                p.getY()+.15+r.nextDouble()*(bowl?.9:.5),p.getZ()+.25+r.nextDouble()*.5,0,high?.035:.012,0);
        if(r.nextInt(high?7:12)==0)l.addParticle(ParticleTypes.SMOKE,p.getX()+.5,p.getY()+.6,p.getZ()+.5,0,.025,0);
    }
}
