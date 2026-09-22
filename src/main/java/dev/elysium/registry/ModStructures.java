package dev.elysium.registry;

import dev.elysium.Elysium;
import dev.elysium.structure.ElysianBridgePiece;
import dev.elysium.structure.ElysianBridgeStructure;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModStructures {
    private static final DeferredRegister<StructureType<?>> STRUCTURES = DeferredRegister.create(Registries.STRUCTURE_TYPE, Elysium.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECES = DeferredRegister.create(Registries.STRUCTURE_PIECE, Elysium.MOD_ID);
    public static final DeferredHolder<StructureType<?>, StructureType<ElysianBridgeStructure>> ELYSIAN_BRIDGE =
            STRUCTURES.register("elysian_bridge", () -> () -> ElysianBridgeStructure.CODEC);
    public static final DeferredHolder<StructurePieceType, StructurePieceType> ELYSIAN_BRIDGE_PIECE =
            PIECES.register("elysian_bridge", () -> (StructurePieceType.ContextlessType) ElysianBridgePiece::new);

    private ModStructures() {}

    public static void register(IEventBus modBus) {
        STRUCTURES.register(modBus);
        PIECES.register(modBus);
    }
}
