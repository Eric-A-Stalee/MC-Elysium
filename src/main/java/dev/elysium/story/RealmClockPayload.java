package dev.elysium.story;

import dev.elysium.Elysium;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record RealmClockPayload(long time,boolean moving) implements CustomPacketPayload {
    public static final Type<RealmClockPayload> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(Elysium.MOD_ID,"realm_clock"));
    public static final StreamCodec<RegistryFriendlyByteBuf,RealmClockPayload> CODEC=new StreamCodec<>() {
        public RealmClockPayload decode(RegistryFriendlyByteBuf b){return new RealmClockPayload(b.readVarLong(),b.readBoolean());}
        public void encode(RegistryFriendlyByteBuf b,RealmClockPayload p){b.writeVarLong(p.time);b.writeBoolean(p.moving);}
    };
    @Override public Type<RealmClockPayload> type(){return TYPE;}
    @EventBusSubscriber(modid=Elysium.MOD_ID,bus=EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event){event.registrar("1").playToClient(TYPE,CODEC,(payload,context)->RealmClock.accept(context.player().level(),payload.time,payload.moving));}
        private Registration(){}
    }
}
