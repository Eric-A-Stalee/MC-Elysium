package dev.elysium.structure;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/** Authored, supported fixtures are also the circuit's complete address book. H is above the foundry floor. */
public final class ForgeFixtures {
    public record Fixture(int u, int h, int v, int circuit, boolean controller, Direction facing) {
        public BlockPos worldPosition(ForgePlan plan) { return new BlockPos(plan.worldX(u,v), plan.floor()+h, plan.worldZ(u,v)); }
    }
    private static Fixture lamp(int u,int h,int v,int circuit) { return new Fixture(u,h,v,circuit,false,Direction.NORTH); }
    private static Fixture control(int u,int h,int v,int circuit,Direction facing) { return new Fixture(u,h,v,circuit,true,facing); }
    public static final List<Fixture> ALL = List.of(
            control(-3,1,40,0,Direction.EAST),
            lamp(-5,4,40,0), lamp(5,4,45,0), lamp(-24,4,64,0), lamp(24,5,64,0),
            lamp(-17,6,74,0), lamp(17,9,74,0), lamp(-10,23,75,0), lamp(10,23,75,0),
            lamp(-9,0,57,0), lamp(11,0,85,0),
            control(-39,1,54,1,Direction.SOUTH),
            lamp(-38,2,49,1), lamp(-44,4,57,1), lamp(-44,5,72,1), lamp(-30,4,77,1),
            lamp(43,5,69,1), lamp(43,8,80,1), lamp(35,1,83,1),
            control(34,1,66,2,Direction.NORTH),
            lamp(-24,9,66,2), lamp(-24,9,74,2), lamp(24,9,66,2), lamp(24,9,74,2),
            lamp(-7,5,107,2), lamp(7,5,107,2), lamp(0,10,118,2));
    private ForgeFixtures() {}
}
