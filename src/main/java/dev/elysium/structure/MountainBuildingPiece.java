package dev.elysium.structure;

import dev.elysium.registry.ModStructures;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

/** Versioned plans are saved in full; chunk order and later terrain changes cannot move their rooms. */
public final class MountainBuildingPiece extends StructurePiece {
    private static final Direction[] FACES={Direction.SOUTH,Direction.WEST,Direction.NORTH,Direction.EAST};
    private final MountainBuildingPlan plan;
    private final int grammar;
    private int residents;
    public MountainBuildingPiece(MountainBuildingPlan plan) {
        this(plan,2);
    }
    private MountainBuildingPiece(MountainBuildingPlan plan,int grammar) {
        super(ModStructures.MOUNTAIN_BUILDING_PIECE.get(),0,bounds(plan,grammar));this.plan=plan;this.grammar=grammar;setOrientation(null);
    }
    public MountainBuildingPiece(CompoundTag tag) {
        this(read(tag),tag.getInt("Grammar"));residents=tag.getInt("Residents");
    }
    public MountainBuildingPlan plan() { return plan; }
    private static MountainBuildingPlan read(CompoundTag tag) {
        int grammar=tag.getInt("Grammar"),stride=grammar==1?7:10;
        if(grammar<1 || grammar>2)throw new IllegalArgumentException("Unknown mountain building grammar");
        int[] data=tag.getIntArray("Rooms"),profile=tag.getIntArray("Ground"),entry=tag.getIntArray("Entry");
        if(data.length==0 || data.length>stride*4 || data.length%stride!=0 || profile.length==0 || profile.length>4800
                || profile.length%3!=0 || entry.length!=5)throw new IllegalArgumentException("Invalid saved mountain building");
        var rooms=new ArrayList<MountainBuildingPlan.Room>();var ground=new ArrayList<MountainBuildingPlan.Ground>();
        for(int i=0;i<data.length;i+=stride)rooms.add(new MountainBuildingPlan.Room(data[i],data[i+1],data[i+2],data[i+3],data[i+4],data[i+5],data[i+6]!=0,
                grammar==1?0:data[i+7],grammar==1?0:data[i+8],grammar==1?0:data[i+9]));
        for(int i=0;i<profile.length;i+=3)ground.add(new MountainBuildingPlan.Ground(profile[i],profile[i+1],profile[i+2]));
        var plan=new MountainBuildingPlan(tag.getInt("X"),tag.getInt("Z"),tag.getInt("Turn"),tag.getLong("Seed"),
                MountainBuildingPlan.Style.valueOf(tag.getString("Style")),rooms,
                new MountainBuildingPlan.Entry(entry[0],entry[1],entry[2],entry[3],entry[4]),ground,tag.getBoolean("Cellar"));
        return tag.contains("GallerySide")?new MountainBuildingPlan(plan.x(),plan.z(),plan.rotation(),plan.seed(),plan.style(),plan.rooms(),
                plan.entry(),plan.ground(),plan.cellar(),tag.getInt("GallerySide")):plan;
    }
    @Override protected void addAdditionalSaveData(StructurePieceSerializationContext context,CompoundTag tag) {
        tag.putInt("Grammar",grammar);tag.putInt("X",plan.x());tag.putInt("Z",plan.z());tag.putInt("Turn",plan.rotation());
        tag.putLong("Seed",plan.seed());tag.putString("Style",plan.style().name());tag.putBoolean("Cellar",plan.cellar());tag.putInt("Residents",residents);
        tag.putInt("GallerySide",plan.gallerySide());
        int[] rooms=new int[plan.rooms().size()*(grammar==1?7:10)],ground=new int[plan.ground().size()*3];int i=0;
        for(var r:plan.rooms()) {rooms[i++]=r.u();rooms[i++]=r.v();rooms[i++]=r.width();rooms[i++]=r.depth();rooms[i++]=r.floor();rooms[i++]=r.storeys();rooms[i++]=r.crossRoof()?1:0;
            if(grammar==2){rooms[i++]=r.jetty();rooms[i++]=r.roofShift();rooms[i++]=r.facade();}}
        i=0;for(var g:plan.ground()){ground[i++]=g.u();ground[i++]=g.v();ground[i++]=g.original();}
        var e=plan.entry();tag.putIntArray("Entry",new int[]{e.u(),e.v(),e.du(),e.dv(),e.floor()});
        tag.putIntArray("Rooms",rooms);tag.putIntArray("Ground",ground);
    }
    @Override public void postProcess(WorldGenLevel level,StructureManager manager,ChunkGenerator generator,RandomSource random,
            BoundingBox clip,ChunkPos chunk,BlockPos pivot) {
        // Local, ephemeral geometry: no static world cache and no neighboring chunk reads.
        var geometry=geometry();
        // Place the shell before attachments. Door halves, beds and hanging lights
        // must not react to an intermediate air column while their support is being built.
        for(int pass=0;pass<2;pass++)for(var entry:geometry.entrySet()) {
            boolean attachment=switch(entry.getValue().material()) {
                case DOOR_LOW,DOOR_HIGH,BED_FOOT,BED_HEAD,LANTERN,HANGING_LANTERN,LADDER,TRAPDOOR,TABLE_TOP->true;
                default->false;
            };
            if(attachment!=(pass==1))continue;
            var c=entry.getKey();int x=plan.worldX(c.u(),c.v()),z=plan.worldZ(c.u(),c.v());
            if(x<clip.minX() || x>clip.maxX() || z<clip.minZ() || z>clip.maxZ() || c.y()<clip.minY() || c.y()>clip.maxY())continue;
            if(attachment)level.setBlock(new BlockPos(x,c.y(),z),state(entry.getValue()),Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            else placeBlock(level,state(entry.getValue()),x,c.y(),z,clip);
        }
        if(plan.style()==MountainBuildingPlan.Style.WATCH_LODGE)return;
        var spawns=new ArrayList<BlockPos>();
        for(var room:plan.rooms())for(int floor=0;floor<room.storeys();floor++) {
            int y=room.floor()+floor*5+1;
            search:for(int v=room.maxV()-2;v>=room.v()+2;v--)for(int u=room.u()+2;u<=room.maxU()-2;u++) {
                var cell=new MountainArchitecture.Cell(u,y,v);
                if(geometry.getOrDefault(cell,new MountainArchitecture.Voxel(MountainArchitecture.Material.AIR)).material()!=MountainArchitecture.Material.AIR
                        || geometry.getOrDefault(new MountainArchitecture.Cell(u,y+1,v),new MountainArchitecture.Voxel(MountainArchitecture.Material.AIR)).material()!=MountainArchitecture.Material.AIR
                        || geometry.getOrDefault(new MountainArchitecture.Cell(u,y-1,v),new MountainArchitecture.Voxel(MountainArchitecture.Material.AIR)).material()!=MountainArchitecture.Material.PLANK)continue;
                spawns.add(new BlockPos(plan.worldX(u,v),y,plan.worldZ(u,v)));break search;
            }
        }
        for(int i=0;i<2;i++) {
            if(spawns.size()<=i)continue;var at=spawns.get(i);
            if((residents&(1<<i))!=0 || !clip.isInside(at) || !level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir())continue;
            var villager=EntityType.VILLAGER.create(level.getLevel());
            if(villager!=null) {
                villager.setPersistenceRequired();villager.moveTo(at.getX()+.5,at.getY(),at.getZ()+.5,0,0);
                villager.finalizeSpawn(level,level.getCurrentDifficultyAt(at),MobSpawnType.STRUCTURE,null);
                villager.setVillagerData(villager.getVillagerData().setType(VillagerType.TAIGA));
                level.addFreshEntityWithPassengers(villager);residents|=1<<i;
            }
        }
    }
    public Map<MountainArchitecture.Cell,MountainArchitecture.Voxel> geometry() {
        if(grammar==2)return MountainArchitecture.build(plan);
        var blocks=new java.util.LinkedHashMap<MountainArchitecture.Cell,MountainArchitecture.Voxel>();
        LegacyMountainArchitecture.build(plan).forEach((p,b)->blocks.put(new MountainArchitecture.Cell(p.u(),p.y(),p.v()),
                new MountainArchitecture.Voxel(MountainArchitecture.Material.valueOf(b.material().name()),b.facing())));
        return blocks;
    }
    public BlockState state(MountainArchitecture.Voxel voxel) {
        int palette=(int)((plan.seed()>>>16)&3);var facing=FACES[(voxel.facing()+plan.rotation())%4];
        Block roof=palette==2?Blocks.STONE_BRICKS:palette==1?Blocks.DEEPSLATE_BRICKS:Blocks.DEEPSLATE_TILES;
        Block roofStair=palette==2?Blocks.STONE_BRICK_STAIRS:palette==1?Blocks.DEEPSLATE_BRICK_STAIRS:Blocks.DEEPSLATE_TILE_STAIRS;
        Block roofSlab=palette==2?Blocks.STONE_BRICK_SLAB:palette==1?Blocks.DEEPSLATE_BRICK_SLAB:Blocks.DEEPSLATE_TILE_SLAB;
        return switch(voxel.material()) {
            case AIR->Blocks.AIR.defaultBlockState();
            case STONE->Blocks.STONE_BRICKS.defaultBlockState();
            case RUBBLE->Blocks.COBBLESTONE.defaultBlockState();
            case PLANK->Blocks.SPRUCE_PLANKS.defaultBlockState();
            case CLADDING->(palette==1?Blocks.DARK_OAK_PLANKS:palette==2?Blocks.OAK_PLANKS:Blocks.SPRUCE_PLANKS).defaultBlockState();
            case PALE->(palette==2?Blocks.SMOOTH_SANDSTONE:Blocks.CALCITE).defaultBlockState();
            case LOG->Blocks.SPRUCE_LOG.defaultBlockState();
            case STRIPPED->Blocks.STRIPPED_DARK_OAK_LOG.defaultBlockState();
            case LOG_U,BEAM_U->(voxel.material()==MountainArchitecture.Material.BEAM_U?Blocks.DARK_OAK_LOG:Blocks.SPRUCE_LOG)
                    .defaultBlockState().setValue(RotatedPillarBlock.AXIS,plan.rotation()%2==0?Direction.Axis.X:Direction.Axis.Z);
            case LOG_V,BEAM_V->(voxel.material()==MountainArchitecture.Material.BEAM_V?Blocks.DARK_OAK_LOG:Blocks.SPRUCE_LOG)
                    .defaultBlockState().setValue(RotatedPillarBlock.AXIS,plan.rotation()%2==0?Direction.Axis.Z:Direction.Axis.X);
            case ROOF->roof.defaultBlockState();
            case ROOF_STAIR->roofStair.defaultBlockState().setValue(StairBlock.FACING,facing);
            case ROOF_SLAB->roofSlab.defaultBlockState();
            case WOOD_STAIR->Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING,facing);
            case WOOD_SLAB->Blocks.DARK_OAK_SLAB.defaultBlockState();
            case FENCE->Blocks.SPRUCE_FENCE.defaultBlockState();
            case GLASS->Blocks.LIGHT_GRAY_STAINED_GLASS.defaultBlockState();
            case DOOR_LOW,DOOR_HIGH->Blocks.SPRUCE_DOOR.defaultBlockState().setValue(DoorBlock.FACING,facing)
                    .setValue(DoorBlock.HALF,voxel.material()==MountainArchitecture.Material.DOOR_LOW?DoubleBlockHalf.LOWER:DoubleBlockHalf.UPPER);
            case BED_FOOT,BED_HEAD->Blocks.YELLOW_BED.defaultBlockState().setValue(BedBlock.FACING,facing)
                    .setValue(BedBlock.PART,voxel.material()==MountainArchitecture.Material.BED_FOOT?BedPart.FOOT:BedPart.HEAD);
            case LANTERN,HANGING_LANTERN->Blocks.LANTERN.defaultBlockState().setValue(LanternBlock.HANGING,voxel.material()==MountainArchitecture.Material.HANGING_LANTERN);
            case BARREL->Blocks.BARREL.defaultBlockState();
            case BOOKSHELF->Blocks.BOOKSHELF.defaultBlockState();
            case FURNACE->Blocks.FURNACE.defaultBlockState().setValue(FurnaceBlock.FACING,facing);
            case CRAFTING->Blocks.CRAFTING_TABLE.defaultBlockState();
            case TABLE_TOP->Blocks.SPRUCE_PRESSURE_PLATE.defaultBlockState();
            case SIDE_TABLE->Blocks.SPRUCE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.TOP);
            case LADDER->Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,facing);
            case TRAPDOOR->Blocks.SPRUCE_TRAPDOOR.defaultBlockState();
            case CHIMNEY->Blocks.COBBLED_DEEPSLATE.defaultBlockState();
            case CAMPFIRE->Blocks.CAMPFIRE.defaultBlockState();
        };
    }
    private static BoundingBox bounds(MountainBuildingPlan b,int grammar) {
        int margin=grammar==1?2:4;
        int minX=b.x(),maxX=b.x(),minZ=b.z(),maxZ=b.z(),minY=b.minFloor()-2;
        for(var g:b.ground()) {
            int x=b.worldX(g.u(),g.v()),z=b.worldZ(g.u(),g.v());
            minX=Math.min(minX,x-margin);maxX=Math.max(maxX,x+margin);minZ=Math.min(minZ,z-margin);maxZ=Math.max(maxZ,z+margin);minY=Math.min(minY,g.original()-1);
        }
        int maxY=grammar==1?b.rooms().stream().mapToInt(r->r.eaves()+(r.crossRoof()?r.depth():r.width())/2+5).max().orElseThrow():b.maxY();
        return new BoundingBox(minX,minY,minZ,maxX,maxY,maxZ);
    }
}
