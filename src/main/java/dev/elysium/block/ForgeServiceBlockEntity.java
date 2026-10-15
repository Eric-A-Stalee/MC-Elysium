package dev.elysium.block;

import dev.elysium.forge.ForgeLighting;
import dev.elysium.registry.ModBlockEntities;
import dev.elysium.structure.ForgePlan;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A fixture remembers its own circuit; it has no per-tick block-entity ticker. */
public final class ForgeServiceBlockEntity extends BlockEntity {
    private ForgePlan plan;
    private int circuit = -1;

    public ForgeServiceBlockEntity(BlockPos pos, BlockState state) { super(ModBlockEntities.FORGE_SERVICE.get(), pos, state); }
    public ForgePlan plan() { return plan; }
    public int circuit() { return circuit; }
    public boolean belongsTo(ForgePlan other, int group) { return other.equals(plan) && group == circuit; }

    public void bind(ForgePlan plan, int circuit) {
        if (!ForgeLighting.valid(circuit)) throw new IllegalArgumentException("Unknown Forge circuit");
        this.plan = plan;
        this.circuit = circuit;
        setChanged();
        if (level instanceof ServerLevel server) server.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    @Override public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel server && plan != null)
            server.scheduleTick(worldPosition, getBlockState().getBlock(), 1);
    }

    public void reconcile(ServerLevel server) {
        if (plan == null) return;
        var state = server.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof ForgeServiceBlock)) return;
        boolean active = ForgeLighting.get(server).active(plan, circuit);
        if (state.getValue(ForgeServiceBlock.LIT) != active)
            server.setBlock(worldPosition, state.setValue(ForgeServiceBlock.LIT, active), 3);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        if (plan != null) {
            tag.putLong("Entrance", plan.key());
            tag.putInt("Turn", plan.turn());
            tag.putInt("Circuit", circuit);
        }
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        plan = null;
        circuit = -1;
        int group = tag.getInt("Circuit"), turn = tag.getInt("Turn");
        if (tag.contains("Entrance", Tag.TAG_LONG) && ForgeLighting.valid(group) && turn >= 0 && turn < 4) {
            var entrance = BlockPos.of(tag.getLong("Entrance"));
            plan = new ForgePlan(entrance.getX(), entrance.getZ(), entrance.getY(), turn);
            circuit = group;
        }
    }
}
