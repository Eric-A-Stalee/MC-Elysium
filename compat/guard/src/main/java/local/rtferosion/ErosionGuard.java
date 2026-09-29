package local.rtferosion;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.LongAdder;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

@Mod("rtferosionguard")
public final class ErosionGuard {
    private static final ErosionPolicy DEFAULT = new ErosionPolicy(List.of());
    private static volatile ErosionPolicy policy = DEFAULT;
    private static final ModConfigSpec SPEC;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ADDITIONAL;
    private static final boolean AUDIT = Boolean.getBoolean("rtferosionguard.audit");
    private static final ConcurrentHashMap<ResourceLocation, LongAdder[]> COUNTS = new ConcurrentHashMap<>();

    static {
        var builder = new ModConfigSpec.Builder();
        ADDITIONAL = builder.comment("Additional dimension IDs allowed to run RTF erosion. Overworld is always allowed. Restart required.")
                .worldRestart().defineListAllowEmpty("additional_dimensions", List.of(), ErosionPolicy::validId);
        SPEC = builder.build();
    }

    public ErosionGuard(IEventBus bus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, SPEC, "rtf-erosion-dimensions.toml");
        bus.addListener(ErosionGuard::onLoad);
        bus.addListener(ErosionGuard::onUnload);
        if (AUDIT) NeoForge.EVENT_BUS.addListener(ErosionGuard::onStopping);
    }

    private static void onLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            policy = new ErosionPolicy(ADDITIONAL.get());
            COUNTS.clear();
            LogUtils.getLogger().info("RTF erosion dimensions: minecraft:overworld plus {}", ADDITIONAL.get());
        }
    }

    private static void onUnload(ModConfigEvent.Unloading event) {
        if (event.getConfig().getSpec() == SPEC) policy = DEFAULT;
    }

    public static boolean allows(ResourceLocation dimension) {
        boolean allowed = policy.allows(dimension);
        if (AUDIT) COUNTS.computeIfAbsent(dimension, key -> new LongAdder[]{new LongAdder(), new LongAdder()})[allowed ? 0 : 1].increment();
        return allowed;
    }

    private static void onStopping(ServerStoppingEvent event) {
        COUNTS.forEach((dimension, counts) -> LogUtils.getLogger().info("[erosion-guard] dimension={} allowed={} blocked={}", dimension, counts[0].sum(), counts[1].sum()));
    }
}
