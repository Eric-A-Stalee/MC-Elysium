package dev.elysium.registry;

import dev.elysium.Elysium;
import dev.elysium.structure.ElysianBridgePiece;
import dev.elysium.structure.ElysianBridgeStructure;
import dev.elysium.structure.LandscapeStructure;
import dev.elysium.structure.LandscapePiece;
import dev.elysium.structure.BankPathPiece;
import dev.elysium.structure.MountainTownStructure;
import dev.elysium.structure.TerracePathPiece;
import dev.elysium.structure.TartarusChainStructure;
import dev.elysium.structure.TartarusChainPiece;
import dev.elysium.structure.ValleyTownStructure;
import dev.elysium.structure.TownTerrainPiece;
import dev.elysium.structure.ContourTownStructure;
import dev.elysium.structure.MountainBuildingPiece;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    private static final DeferredRegister<StructureType<?>> STRUCTURES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Elysium.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Elysium.MOD_ID);
    public static final DeferredHolder<StructureType<?>, StructureType<ContourTownStructure>> CONTOUR_TOWN =
            STRUCTURES.register("contour_town", () -> () -> ContourTownStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> MOUNTAIN_BUILDING_PIECE =
            PIECES.register("mountain_building", () -> (StructurePieceType.ContextlessType) MountainBuildingPiece::new);
    public static final DeferredHolder<StructureType<?>, StructureType<ValleyTownStructure>> VALLEY_TOWN =
            STRUCTURES.register("valley_town", () -> () -> ValleyTownStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> TOWN_TERRAIN_PIECE =
            PIECES.register("town_terrain", () -> (StructurePieceType.ContextlessType) TownTerrainPiece::new);
    public static final DeferredHolder<StructureType<?>, StructureType<TartarusChainStructure>> TARTARUS_CHAIN =
            STRUCTURES.register("tartarus_chain", () -> () -> TartarusChainStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> TARTARUS_CHAIN_PIECE =
            PIECES.register("tartarus_chain", () -> (StructurePieceType.ContextlessType) TartarusChainPiece::new);
    public static final DeferredHolder<StructureType<?>, StructureType<ElysianBridgeStructure>> ELYSIAN_BRIDGE =
            STRUCTURES.register("elysian_bridge", () -> () -> ElysianBridgeStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ELYSIAN_BRIDGE_PIECE =
            PIECES.register("elysian_bridge", () -> (StructurePieceType.ContextlessType) ElysianBridgePiece::new);
    public static final DeferredHolder<StructureType<?>, StructureType<LandscapeStructure>> LANDSCAPE_SITE =
            STRUCTURES.register("landscape_site", () -> () -> LandscapeStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> LANDSCAPE_PIECE =
            PIECES.register("landscape_site", () -> (StructurePieceType.StructureTemplateType) LandscapePiece::new);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> BANK_PATH_PIECE =
            PIECES.register("bank_path", () -> (StructurePieceType.ContextlessType) BankPathPiece::new);
    public static final DeferredHolder<StructureType<?>, StructureType<MountainTownStructure>> MOUNTAIN_TOWN =
            STRUCTURES.register("mountain_town", () -> () -> MountainTownStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> TERRACE_PATH_PIECE =
            PIECES.register("terrace_path", () -> (StructurePieceType.ContextlessType) TerracePathPiece::new);

    private ModStructures() {}

    public static void register(IEventBus modBus) {
        STRUCTURES.register(modBus);
        PIECES.register(modBus);
    }
}
