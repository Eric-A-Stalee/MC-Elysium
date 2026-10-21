package dev.elysium.gametest;

import dev.elysium.forge.ForgeDiscoveries;
import dev.elysium.story.RealmCycle;
import dev.elysium.story.StoryJournal;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("elysium") @PrefixGameTestTemplate(false)
public final class StoryDiscoveryGameTests {
    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void journalUsesPersonalDiscoveriesInsteadOfTheWorldsFuture(GameTestHelper h) {
        var cycle=new RealmCycle();var notes=new ForgeDiscoveries();
        var reader=UUID.randomUUID();var newcomer=UUID.randomUUID();
        h.assertTrue(StoryJournal.entries(cycle,notes,reader).size()==2&&hint(cycle,notes,reader).endsWith(".explore"),
                "A new reader gets no undiscovered watches, restraints or complete expedition route");
        notes.discover(reader,ForgeDiscoveries.SURVEY);
        h.assertTrue(hint(cycle,notes,reader).endsWith(".forge"),"A survey gives a foundry bearing, not a teacher or prison walkthrough");
        notes.discover(reader,ForgeDiscoveries.HEARTH);
        h.assertTrue(hint(cycle,notes,reader).endsWith(".chain"),"The hearth introduces the known resonator");
        notes.discover(reader,ForgeDiscoveries.ANCHOR);
        h.assertTrue(hint(cycle,notes,reader).endsWith(".depths"),"The inspected anchor introduces the passage below it");
        notes.discover(reader,ForgeDiscoveries.PROMETHEUS);
        h.assertTrue(hint(cycle,notes,reader).endsWith(".smithing"),"Teaching explains operating the press, not its future consequences");
        cycle.awaken(BlockPos.ZERO);
        for(int i=0;i<2500;i++)cycle.advance(true);
        h.assertTrue(hint(cycle,notes,reader).endsWith(".watches"),"A taught smith can follow newly lit bowls after night arrives");
        h.assertTrue(StoryJournal.entries(cycle,notes,newcomer).size()==2&&hint(cycle,notes,newcomer).endsWith(".explore"),
                "Someone else's forging must not give a newcomer a walkthrough or undiscovered objective counts");
        notes.discover(reader,128);
        h.assertTrue(StoryJournal.entries(cycle,notes,reader).size()==3,"A watch record reveals only the watch count");
        for(int kind=0;kind<3;kind++)cycle.answerWatch(kind,new BlockPos(kind,70,0));
        h.assertTrue(hint(cycle,notes,reader).endsWith(".workshop"),"Without the vessel discovery, return to inspect the workshop rather than naming its contents");
        notes.discover(reader,ForgeDiscoveries.HOPE);
        h.assertTrue(hint(cycle,notes,reader).endsWith(".bindings"),"The opened vessel makes returning below a useful lead");
        h.assertTrue(StoryJournal.entries(cycle,notes,reader).size()==3,"Holding the ember does not reveal an unseen restraint count");
        notes.discover(reader,4096);
        h.assertTrue(StoryJournal.entries(cycle,notes,reader).size()==4,"Inspecting a restraint adds the corresponding repair notes");
        for(int kind=0;kind<3;kind++)cycle.bindDeep(kind);
        h.assertTrue(hint(cycle,notes,reader).endsWith(".governor"),"The repaired fittings point back to the regulator");
        cycle.resolve(true);
        h.assertTrue(hint(cycle,notes,reader).endsWith(".living")&&hint(cycle,notes,newcomer).endsWith(".explore"),
                "Resolution instructions stay with a reader who knows the workshop");
        h.succeed();
    }

    @GameTest(template="portal_test_empty",timeoutTicks=100)
    public static void undiscoveredAdvancementsDoNotAdvertiseThePlot(GameTestHelper h) {
        var manager=h.getLevel().getServer().getAdvancements();
        for(String id:new String[]{"forge_survey","first_flame","promethean_craft","smiths_work","first_sunset",
                "first_night","pandora_opened","hope_remains","binding_0","binding_1","binding_2","living_realm","chosen_hour"}) {
            var advancement=manager.get(ResourceLocation.fromNamespaceAndPath("elysium",id));
            h.assertTrue(advancement!=null,"Discovery advancement must still exist: "+id);
            var display=advancement.value().display().orElseThrow();
            h.assertTrue(display.isHidden(),"Unfound chapters cannot appear as future objectives: "+id);
            h.assertTrue(advancement.value().parent().orElseThrow().equals(ResourceLocation.fromNamespaceAndPath("elysium","elysium")),
                    "A later discovery cannot expose an unvisited encounter through its ancestors: "+id);
            if(id.equals("first_sunset")||id.equals("first_night")||id.equals("pandora_opened"))
                h.assertTrue(!display.shouldShowToast(),"Automatic changes may not announce unseen events: "+id);
        }
        h.succeed();
    }

    private static String hint(RealmCycle cycle,ForgeDiscoveries notes,UUID reader) {
        Component entry=StoryJournal.entries(cycle,notes,reader).getLast();
        return ((TranslatableContents)entry.getContents()).getKey();
    }
    private StoryDiscoveryGameTests() {}
}
