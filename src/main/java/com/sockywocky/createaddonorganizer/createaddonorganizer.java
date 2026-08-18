package com.sockywocky.createaddonorganizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.sockywocky.createaddonorganizer.client.BannerTextures;
import com.sockywocky.createaddonorganizer.client.CaoSection;
import com.sockywocky.createaddonorganizer.client.ClientRegistries;
import com.sockywocky.createaddonorganizer.client.LiveColors;
import com.sockywocky.createaddonorganizer.client.ShippedBanners;
import com.sockywocky.createaddonorganizer.client.simulated.NativeSections;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedHub;
import com.sockywocky.createaddonorganizer.client.simulated.SimulatedSupport;
import com.sockywocky.createaddonorganizer.mixin.CreativeModeInventoryScreenAccessor;
import com.sockywocky.createaddonorganizer.mixin.CreativeModeTabsAccessor;
import com.sockywocky.createaddonorganizer.mixin.ItemPickerMenuAccessor;

import net.mcexpanded.fancytabsections.FTSInternal;
import net.mcexpanded.fancytabsections.FancyTabSections;
import net.mcexpanded.fancytabsections.Section.Section;
import net.mcexpanded.fancytabsections.creativetab.BannerRenderer;
import net.mcexpanded.fancytabsections.creativetab.ConglomerateOfItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackLinkedSet;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(createaddonorganizer.MODID)
public class createaddonorganizer {
    public static final String MODID = "createaddonorganizer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final ResourceLocation CREATE_BASE = ResourceLocation.fromNamespaceAndPath("create", "base");

    public static final Set<ResourceLocation> MANAGED_PARENTS = ConcurrentHashMap.newKeySet();
    static {
        MANAGED_PARENTS.add(CREATE_BASE);
    }

    public static boolean createPresent() {
        return BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(CREATE_BASE);
    }

    public static ResourceLocation defaultHub() {
        if (createPresent()) {
            return CREATE_BASE;
        }
        for (ResourceLocation id : Config.extraMainSections()) {
            if (BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(id)) {
                return id;
            }
        }
        for (TabLayout tab : TabLayoutStore.customTabs()) {
            if (tab.sectionCount() > 0) {
                return tab.id();
            }
        }
        return null;
    }

    private static volatile boolean collecting = false;

    private static final Map<ResourceLocation, List<Section<?>>> PENDING = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Section<?>> OWN_SECTIONS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, List<Section<?>>> CUSTOM_SECTIONS = new LinkedHashMap<>();

    private static final Set<ResourceLocation> LIVE_RELAYOUT = new HashSet<>();
    private static final Map<ResourceLocation, List<Section<?>>> LIVE_SECTIONS = new LinkedHashMap<>();

    private static final Map<ResourceLocation, List<String>> NATIVE_ITEMS = new LinkedHashMap<>();

    public static List<String> nativeItemsOf(ResourceLocation tabId) {
        List<String> captured = NATIVE_ITEMS.get(tabId);
        if (captured != null && !captured.isEmpty()) {
            return captured;
        }
        return NativeItemsStore.get(tabId);
    }

    private static void rememberNativeItems(ResourceLocation tabId, List<String> itemIds) {
        NATIVE_ITEMS.put(tabId, itemIds);
        if (Minecraft.getInstance().level != null) {
            NativeItemsStore.put(tabId, itemIds);
        }
    }

    private static final Map<String, List<ResourceLocation>> SKIP_EXAMPLES = new LinkedHashMap<>();
    private static final int SKIP_EXAMPLES_PER_REASON = 10;
    private static final Set<ResourceLocation> RECOVERED_TABS = new HashSet<>();

    private static final Set<ResourceLocation> SELF_BUILT_HUBS = ConcurrentHashMap.newKeySet();

    public static boolean buildsItsOwnContents(ResourceLocation tabId) {
        return SELF_BUILT_HUBS.contains(tabId);
    }
    private static final Set<ResourceLocation> EMPTY_SECTIONS = new LinkedHashSet<>();

    private static int candidatesSeen = 0;

    private static ResourceLocation lastReconciledTab = null;

    public createaddonorganizer(IEventBus modEventBus, ModContainer modContainer) {
        ConfigMigration.run();
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        CustomTabRegistry.register(modEventBus);
        ModSounds.register(modEventBus);
        if (SimulatedSupport.isLoaded()) {
            MANAGED_PARENTS.add(SimulatedSupport.MAIN_TAB);
        }

        modEventBus.addListener(EventPriority.HIGHEST, createaddonorganizer::onBuildTabContents);
    }

    private static int listenerInvocationsThisPass = 0;

    private static BuildCreativeModeTabContentsEvent lastSeenEvent;

    private static final Set<ResourceLocation> REPORTED_THIS_PASS = new HashSet<>();

    public static void recoverAbortedDispatch(Event aborted) {
        if (!(aborted instanceof BuildCreativeModeTabContentsEvent event) || event == lastSeenEvent) {
            return;
        }
        ResourceLocation tabId = event.getTabKey().location();
        LOGGER.warn("[CAO] another mod's listener aborted the contents event for {} before it reached us; "
                + "capturing that tab directly", tabId);
        try {
            onBuildTabContents(event);
        } catch (Throwable t) {
            LOGGER.error("[CAO] failed to capture {} after the aborted dispatch", tabId, t);
        }
    }

    public static void applyLayoutAfterDispatch(Event dispatched) {
        if (!(dispatched instanceof BuildCreativeModeTabContentsEvent event)) {
            return;
        }
        ResourceLocation tabId = event.getTabKey().location();
        try {
            LayoutApplier.apply(event, tabId);
        } catch (Throwable t) {
            LOGGER.error("[CAO] failed to apply the saved layout for {}; leaving that tab as-is", tabId, t);
        }
    }

    private static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        lastSeenEvent = event;
        ResourceLocation tabId = event.getTabKey().location();
        applyLayoutAfterDispatch(event);
        if (!collecting) {
            if (LIVE_RELAYOUT.contains(tabId)) {
                rememberNativeItems(tabId, LayoutApplier.producedIds(event.getParentEntries()));
                TabLayout layout = TabLayoutStore.byId(tabId);
                if (layout != null && layout.sectionCount() > 0) {
                    LIVE_SECTIONS.put(tabId, layoutSectionsOf(layout, event));
                }
            }
            return;
        }
        listenerInvocationsThisPass++;
        REPORTED_THIS_PASS.add(tabId);
        if (SimulatedSupport.isMainTab(tabId)) {
            return;
        }
        if (MANAGED_PARENTS.contains(tabId)) {
            rememberNativeItems(tabId, LayoutApplier.producedIds(event.getParentEntries()));
            TabLayout custom = TabLayoutStore.byId(tabId);
            if (custom != null && custom.sectionCount() > 0) {
                CUSTOM_SECTIONS.put(tabId, layoutSectionsOf(custom, event));
            } else {
                OWN_SECTIONS.put(tabId, sectionOf(event, tabId));
            }
            return;
        }
        candidatesSeen++;
        String skipReason = AddonDetection.skipReason(tabId);
        if (skipReason != null) {
            List<ResourceLocation> examples = SKIP_EXAMPLES.computeIfAbsent(skipReason, k -> new ArrayList<>());
            if (examples.size() < SKIP_EXAMPLES_PER_REASON) {
                examples.add(tabId);
            }
            return;
        }
        ResourceLocation parent = Config.parentFor(tabId);
        PENDING.computeIfAbsent(parent, k -> new ArrayList<>()).add(sectionOf(event, tabId));
        AbsorbedTabs.IDS.add(tabId);
    }

    public static boolean organize(CreativeModeTab.ItemDisplayParameters params) {
        Set<ResourceLocation> previousManagedParents = new HashSet<>(MANAGED_PARENTS);

        MANAGED_PARENTS.clear();
        if (createPresent()) {
            MANAGED_PARENTS.add(CREATE_BASE);
        }
        MANAGED_PARENTS.addAll(Config.allRouteTargets());
        MANAGED_PARENTS.addAll(Config.extraMainSections());
        MANAGED_PARENTS.addAll(AddonGroups.hubs());
        for (ResourceLocation selfBuilt : SELF_BUILT_HUBS) {
            if (BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(selfBuilt)) {
                MANAGED_PARENTS.add(selfBuilt);
            }
        }
        for (TabLayout layout : TabLayoutStore.all()) {
            if (layout.sectionCount() > 0) {
                MANAGED_PARENTS.add(layout.id());
            }
        }
        if (SimulatedSupport.isLoaded()) {
            MANAGED_PARENTS.add(SimulatedSupport.MAIN_TAB);
        }
        MANAGED_PARENTS.removeIf(Config::isForceExcluded);

        Set<ResourceLocation> touchedParents = new HashSet<>(previousManagedParents);
        touchedParents.addAll(MANAGED_PARENTS);
        Map<ResourceLocation, List<Section<?>>> sectionsBeforePass = new LinkedHashMap<>();
        for (ResourceLocation parent : touchedParents) {
            List<Section<?>> existing = FancyTabSections.REGISTERED_TABS.get(parent);
            if (existing != null && !existing.isEmpty()) {
                sectionsBeforePass.put(parent, new ArrayList<>(existing));
            }
            if (SimulatedSupport.isMainTab(parent)) {
                SimulatedHub.retractAll();
            } else {
                dropParentSections(parent);
            }
        }

        PENDING.clear();
        OWN_SECTIONS.clear();
        CUSTOM_SECTIONS.clear();
        SKIP_EXAMPLES.clear();
        AbsorbedTabs.IDS.clear();
        RECOVERED_TABS.clear();
        EMPTY_SECTIONS.clear();
        candidatesSeen = 0;
        listenerInvocationsThisPass = 0;
        REPORTED_THIS_PASS.clear();
        int totalTabs = CreativeModeTabs.allTabs().size();
        LOGGER.info("[CAO] {} tab(s) registered total, {} managed parent(s): {}", totalTabs,
                MANAGED_PARENTS.size(), MANAGED_PARENTS);
        if (PackDefaults.isActive()) {
            LOGGER.info("[CAO] a pack script supplied {}; the player's own config still wins over all of it",
                    PackDefaults.summary());
        }
        collecting = true;
        forceRebuild(params);
        collecting = false;
        LOGGER.info("[CAO] collection pass invoked our listener {} time(s)", listenerInvocationsThisPass);
        List<ResourceLocation> silentParents = new ArrayList<>();
        for (ResourceLocation parent : MANAGED_PARENTS) {
            if (!SimulatedSupport.isMainTab(parent) && !REPORTED_THIS_PASS.contains(parent)) {
                silentParents.add(parent);
            }
        }
        if (!silentParents.isEmpty()) {
            LOGGER.warn("[CAO] these managed parent(s) never fired a contents event, so they can only be "
                    + "rebuilt from their folded-in sections: {}", silentParents);
        }
        if (listenerInvocationsThisPass == 0) {
            LOGGER.warn("[CAO] collection pass captured nothing; will retry");
            return false;
        }

        keepTabsThatNeverReportedAsHubs(touchedParents);

        int addonCount = 0;
        for (ResourceLocation parent : MANAGED_PARENTS) {
            List<Section<?>> addons = PENDING.getOrDefault(parent, List.of());

            if (SimulatedSupport.isMainTab(parent)) {
                List<ResourceLocation> ids = new ArrayList<>();
                for (Section<?> addon : orderedById(addons)) {
                    CaoSection cao = (CaoSection) addon;
                    SimulatedHub.inject(cao.id(), cao.title());
                    SimulatedHub.foldItems(cao.id(), cao.items().getStacks());
                    ids.add(cao.id());
                }
                SimulatedHub.reorder(ids);
                NativeSections.adoptAll();
                SimulatedHub.reorder(Config.applyOrderStable(ids));
                addonCount += addons.size();
                continue;
            }

            List<Section<?>> customSections = CUSTOM_SECTIONS.get(parent);
            if (addons.isEmpty() && customSections == null && !CREATE_BASE.equals(parent)
                    && !SELF_BUILT_HUBS.contains(parent)
                    && !Config.extraMainSections().contains(parent)) {
                continue;
            }
            Section<?> own = OWN_SECTIONS.get(parent);
            if (customSections == null && own == null) {
                customSections = recoveredSectionsFor(parent);
                if (!customSections.isEmpty()) {
                    LOGGER.warn("[CAO] {} never reported its own contents during the collection pass (its own "
                            + "generator threw); rebuilding its section from {} cached item(s) so the tab keeps "
                            + "its own section", parent, nativeItemsOf(parent).size());
                } else if (sectionsBeforePass.containsKey(parent)) {
                    LOGGER.warn("[CAO] {} never reported its own contents and nothing is cached for it; "
                            + "keeping the sections it already had rather than dropping its own", parent);
                    for (Section<?> section : sectionsBeforePass.get(parent)) {
                        addSection(parent, section);
                    }
                    continue;
                } else {
                    LOGGER.warn("[CAO] {} never reported its own contents during the collection pass; the tab "
                            + "will show its folded-in sections but not its own", parent);
                    customSections = null;
                }
            }

            Set<ResourceLocation> covered = new HashSet<>();
            boolean ownIsOrdered = own != null && Config.sectionOrderContains(parent);
            if (customSections != null) {
                for (Section<?> section : customSections) {
                    addSection(parent, section);
                    covered.add(section.id());
                }
            } else if (own != null && !ownIsOrdered) {
                addSection(parent, own);
            }
            List<Section<?>> queued = new ArrayList<>(addons);
            if (ownIsOrdered && customSections == null) {
                queued.add(own);
            }
            for (Section<?> addon : orderedById(queued)) {
                if (covered.contains(addon.id())) {
                    if (RECOVERED_TABS.contains(addon.id())) {
                        LOGGER.warn("[CAO] {} is already covered by a custom layout on {}, so its own section "
                                + "was not added", addon.id(), parent);
                    }
                    continue;
                }
                if (addSection(parent, addon) && RECOVERED_TABS.contains(addon.id())) {
                    LOGGER.info("[CAO] added recovered section {} to {}", addon.id(), parent);
                }
            }
            addonCount += addons.size();
        }
        LOGGER.info("[CAO] organized {} Create parent tab(s) with {} absorbed addon section(s): {}",
                MANAGED_PARENTS.size(), addonCount, AbsorbedTabs.IDS);
        logSkipDiagnostics();
        logEmptySections();

        rebuildTabs(touchedParents, params);
        reconcileAgainstLiveTab(params);
        CondensedCreativeSupport.suppressOn(MANAGED_PARENTS);
        refreshSearchTrees(params);
        resetCreativeScrollIfOpen();
        NativeItemsStore.flush();
        Config.finishNativeSeed();
        if (Config.bannerDrawDiagnostics()) {
            for (ResourceLocation recoveredId : RECOVERED_TABS) {
                ResourceLocation parent = Config.parentFor(recoveredId);
                List<Section<?>> finalSections = FancyTabSections.REGISTERED_TABS.get(parent);
                boolean stillThere = finalSections != null
                        && finalSections.stream().anyMatch(s -> recoveredId.equals(s.id()));
                LOGGER.info("[CAO] after the rebuild, {} is {} in {}'s section list ({} section(s) total)",
                        recoveredId, stillThere ? "STILL" : "NO LONGER", parent,
                        finalSections == null ? 0 : finalSections.size());
            }
        }
        return true;
    }

    private static void reconcileAgainstLiveTab(CreativeModeTab.ItemDisplayParameters params) {
        Map<ResourceLocation, Drift> changed = new LinkedHashMap<>();
        List<ResourceLocation> emptied = new ArrayList<>();
        for (ResourceLocation parent : MANAGED_PARENTS) {
            if (SimulatedSupport.isMainTab(parent)) {
                continue;
            }
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(parent);
            List<Section<?>> sections = FancyTabSections.REGISTERED_TABS.get(parent);
            if (tab == null || sections == null || sections.isEmpty()) {
                continue;
            }
            Drift drift = reconcileOne(parent, tab, sections, emptied);
            if (drift != null) {
                changed.put(parent, drift);
            }
        }
        if (changed.isEmpty()) {
            return;
        }
        for (Map.Entry<ResourceLocation, Drift> e : changed.entrySet()) {
            Drift drift = e.getValue();
            LOGGER.info("[CAO] {} ended up with {} item(s) our sections did not hold and {} we held that it no "
                    + "longer shows; realigning its section rows{}", e.getKey(), drift.added, drift.removed,
                    drift.examples.isEmpty() ? "" : " -- " + drift.examples);
        }
        if (!emptied.isEmpty()) {
            LOGGER.info("[CAO] {} section(s) lost every item they had ({}) and were left out rather than given a "
                    + "banner with no rows under it: {}", emptied.size(), ItemObliteratorSupport.emptyReason(),
                    emptied);
            EMPTY_SECTIONS.addAll(emptied);
        }
        rebuildTabs(changed.keySet(), params);
    }

    private static Drift reconcileOne(ResourceLocation parent, CreativeModeTab tab, List<Section<?>> sections,
            List<ResourceLocation> emptied) {
        List<Set<ItemStack>> owned = new ArrayList<>(sections.size());
        for (Section<?> section : sections) {
            owned.add(stackSet(section.items().getStacks()));
        }

        Drift delta = new Drift();
        boolean layoutDriven = splitIntoGroups(parent);
        List<List<ItemStack>> rebuilt = new ArrayList<>(sections.size());
        boolean[] fromOwnTab = new boolean[sections.size()];
        for (int i = 0; i < sections.size(); i++) {
            Section<?> section = sections.get(i);
            rebuilt.add(null);
            if (!editable(section)) {
                continue;
            }
            CreativeModeTab source = layoutDriven ? null : sourceTabOf(parent, section.id());
            if (source == null) {
                continue;
            }
            List<ItemStack> updated = reconcileAgainst(source, section.items().getStacks(), delta, section.id());
            if (updated != null) {
                rebuilt.set(i, updated);
                fromOwnTab[i] = true;
            }
        }

        reconcileAgainstHub(tab, sections, owned, rebuilt, fromOwnTab, delta);

        if (delta.added == 0 && delta.removed == 0) {
            return null;
        }

        List<Section<?>> kept = new ArrayList<>(sections.size());
        for (int i = 0; i < sections.size(); i++) {
            Section<?> section = sections.get(i);
            List<ItemStack> stacks = rebuilt.get(i);
            if (stacks == null) {
                kept.add(section);
                continue;
            }
            if (stacks.isEmpty()) {
                emptied.add(section.id());
                continue;
            }
            ConglomerateOfItems conglomerate = ConglomerateOfItems.create();
            for (ItemStack stack : stacks) {
                conglomerate.add(stack);
            }
            conglomerate.resolveStacks(ClientRegistries.access());
            ItemGroupRuntime.clear(section.id());
            CaoSection cao = (CaoSection) section;
            kept.add(new CaoSection(cao.id(), cao.title(), cao.bannerColor(), cao.texture(),
                    cao.textColor(), conglomerate));
        }
        dropParentSections(parent);
        for (Section<?> section : kept) {
            addSection(parent, section);
        }
        return delta;
    }


    private static List<ItemStack> reconcileAgainst(CreativeModeTab source, List<ItemStack> stacks, Drift delta,
            ResourceLocation into) {
        List<ItemStack> live = nonEmpty(source.getDisplayItems());
        Set<ItemStack> present = stackSet(live);
        for (ItemStack stack : source.getSearchTabDisplayItems()) {
            if (!stack.isEmpty()) {
                present.add(stack);
            }
        }
        if (present.isEmpty()) {
            return null;
        }
        Set<ItemStack> held = stackSet(stacks);
        List<ItemStack> out = new ArrayList<>(stacks.size());
        List<ItemStack> dropped = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (present.contains(stack)) {
                out.add(stack);
            } else {
                dropped.add(stack);
            }
        }
        List<ItemStack> gained = new ArrayList<>();
        int slot = 0;
        for (ItemStack stack : live) {
            if (held.contains(stack)) {
                slot = indexIn(out, stack) + 1;
                continue;
            }
            int at = Math.min(slot, out.size());
            out.add(at, stack.copy());
            slot = at + 1;
            gained.add(stack);
        }
        if (gained.isEmpty() && dropped.isEmpty()) {
            return null;
        }
        for (ItemStack stack : gained) {
            delta.gained(stack, into);
        }
        for (ItemStack stack : dropped) {
            delta.lost(stack, into);
        }
        return out;
    }

    private static void reconcileAgainstHub(CreativeModeTab tab, List<Section<?>> sections,
            List<Set<ItemStack>> owned, List<List<ItemStack>> rebuilt, boolean[] fromOwnTab, Drift delta) {
        List<ItemStack> live = nonEmpty(tab.getDisplayItems());
        Set<ItemStack> present = stackSet(live);
        for (ItemStack stack : tab.getSearchTabDisplayItems()) {
            if (!stack.isEmpty()) {
                present.add(stack);
            }
        }
        if (present.isEmpty()) {
            return;
        }

        boolean[] mine = new boolean[sections.size()];
        for (int i = 0; i < sections.size(); i++) {
            if (fromOwnTab[i] || !editable(sections.get(i))) {
                continue;
            }
            mine[i] = true;
            List<ItemStack> stacks = sections.get(i).items().getStacks();
            List<ItemStack> surviving = new ArrayList<>(stacks.size());
            for (ItemStack stack : stacks) {
                if (present.contains(stack)) {
                    surviving.add(stack);
                } else {
                    delta.lost(stack, sections.get(i).id());
                }
            }
            rebuilt.set(i, surviving);
        }

        int anchorSection = -1;
        int anchorSlot = 0;
        for (ItemStack stack : live) {
            int owner = ownerOf(owned, stack);
            if (owner >= 0) {
                if (mine[owner]) {
                    anchorSection = owner;
                    anchorSlot = indexIn(rebuilt.get(owner), stack) + 1;
                }
                continue;
            }
            int target = anchorSection >= 0 ? anchorSection : firstOf(mine);
            if (target < 0) {
                continue;
            }
            List<ItemStack> into = rebuilt.get(target);
            int at = anchorSection >= 0 ? Math.min(anchorSlot, into.size()) : into.size();
            into.add(at, stack.copy());
            anchorSection = target;
            anchorSlot = at + 1;
            delta.gained(stack, sections.get(target).id());
        }
    }

    private static final class Drift {
        private static final int EXAMPLES = 8;

        int added;
        int removed;
        final List<String> examples = new ArrayList<>();

        void gained(ItemStack stack, ResourceLocation into) {
            added++;
            note("+" + idOf(stack) + " in " + into);
        }

        void lost(ItemStack stack, ResourceLocation from) {
            removed++;
            note("-" + idOf(stack) + " from " + from);
        }

        private void note(String example) {
            if (examples.size() < EXAMPLES) {
                examples.add(example);
            } else if (examples.size() == EXAMPLES) {
                examples.add("...");
            }
        }

        private static String idOf(ItemStack stack) {
            ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return key == null ? stack.toString() : key.toString();
        }
    }

    private static CreativeModeTab sourceTabOf(ResourceLocation parent, ResourceLocation sectionId) {
        if (sectionId == null || sectionId.equals(parent) || MANAGED_PARENTS.contains(sectionId)
                || TabLayout.ownerOfSectionId(sectionId) != null
                || FancyTabSections.REGISTERED_TABS.containsKey(sectionId)) {
            return null;
        }
        TabLayout layout = TabLayoutStore.byId(sectionId);
        if (layout != null && layout.sectionCount() > 0) {
            return null;
        }
        return BuiltInRegistries.CREATIVE_MODE_TAB.get(sectionId);
    }

    private static boolean splitIntoGroups(ResourceLocation parent) {
        TabLayout layout = TabLayoutStore.byId(parent);
        return layout != null && layout.sectionCount() > 0;
    }

    private static boolean editable(Section<?> section) {
        return section instanceof CaoSection cao && !FTSInternal.isCollapsed(cao);
    }

    private static List<ItemStack> nonEmpty(Collection<ItemStack> stacks) {
        List<ItemStack> out = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
        return out;
    }

    private static Set<ItemStack> stackSet(Collection<ItemStack> stacks) {
        Set<ItemStack> out = ItemStackLinkedSet.createTypeAndComponentsSet();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                out.add(stack);
            }
        }
        return out;
    }

    private static int ownerOf(List<Set<ItemStack>> owned, ItemStack stack) {
        for (int i = 0; i < owned.size(); i++) {
            if (owned.get(i).contains(stack)) {
                return i;
            }
        }
        return -1;
    }

    private static int indexIn(List<ItemStack> stacks, ItemStack stack) {
        for (int i = 0; i < stacks.size(); i++) {
            if (ItemStack.isSameItemSameComponents(stacks.get(i), stack)) {
                return i;
            }
        }
        return stacks.size() - 1;
    }

    private static int firstOf(boolean[] flags) {
        for (int i = 0; i < flags.length; i++) {
            if (flags[i]) {
                return i;
            }
        }
        return -1;
    }

    public static boolean reconcileOnTabView(ResourceLocation selectedTabId) {
        if (selectedTabId == null || selectedTabId.equals(lastReconciledTab) || !MANAGED_PARENTS.contains(selectedTabId)) {
            return false;
        }
        lastReconciledTab = selectedTabId;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return false;
        }
        reconcileAgainstLiveTab(ClientRegistries.displayParams());
        return true;
    }

    private static void resetCreativeScrollIfOpen() {
        if (!(Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen screen)) {
            return;
        }
        ((CreativeModeInventoryScreenAccessor) screen).setScrollOffs(0f);
        if (screen.getMenu() instanceof ItemPickerMenuAccessor menu) {
            menu.invokeScrollTo(0f);
        }
    }

    private static void rebuildTabs(Collection<ResourceLocation> ids, CreativeModeTab.ItemDisplayParameters params) {
        for (ResourceLocation id : ids) {
            CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
            if (tab == null) {
                continue;
            }
            try {
                ensureContentsEventCanFire(id);
                ModernFixCompat.clearMemoizedParams(tab);
                tab.buildContents(params);
            } catch (Throwable t) {
                LOGGER.error("[CAO] creative tab {} threw while rebuilding its contents; leaving it as-is", id, t);
            }
        }
    }

    private static void keepTabsThatNeverReportedAsHubs(Set<ResourceLocation> touchedParents) {
        List<ResourceLocation> promoted = new ArrayList<>();
        List<ResourceLocation> folded = new ArrayList<>();
        for (ResourceLocation tabId : BuiltInRegistries.CREATIVE_MODE_TAB.keySet()) {
            if (REPORTED_THIS_PASS.contains(tabId) || SimulatedSupport.isMainTab(tabId)) {
                continue;
            }
            boolean known = SELF_BUILT_HUBS.contains(tabId);
            if (MANAGED_PARENTS.contains(tabId) && !known) {
                continue;
            }
            if (!known) {
                candidatesSeen++;
                String skipReason = AddonDetection.skipReason(tabId);
                if (skipReason != null) {
                    SKIP_EXAMPLES.computeIfAbsent(skipReason, k -> new ArrayList<>());
                    List<ResourceLocation> examples = SKIP_EXAMPLES.get(skipReason);
                    if (examples.size() < SKIP_EXAMPLES_PER_REASON) {
                        examples.add(tabId);
                    }
                    continue;
                }
            }
            Section<?> section;
            try {
                section = sectionFromLiveTab(tabId);
            } catch (Throwable t) {
                LOGGER.warn("[CAO] {} builds its own tab contents and reading them back threw; leaving it alone",
                        tabId, t);
                continue;
            }
            if (section == null || isEmptySection(section)) {
                continue;
            }
            RECOVERED_TABS.add(tabId);
            if (Config.isAskedForByName(tabId)) {
                ResourceLocation parent = Config.parentFor(tabId);
                if (parent == null || !MANAGED_PARENTS.contains(parent)) {
                    LOGGER.warn("[CAO] {} would fold into {}, which is not a tab we organize, so it stays where "
                            + "it is", tabId, parent);
                    continue;
                }
                SELF_BUILT_HUBS.remove(tabId);
                MANAGED_PARENTS.remove(tabId);
                PENDING.computeIfAbsent(parent, k -> new ArrayList<>()).add(section);
                AbsorbedTabs.IDS.add(tabId);
                folded.add(tabId);
                LOGGER.info("[CAO] folding {} into {} from its live contents, because it was asked for by name",
                        tabId, parent);
                continue;
            }
            SELF_BUILT_HUBS.add(tabId);
            MANAGED_PARENTS.add(tabId);
            touchedParents.add(tabId);
            OWN_SECTIONS.put(tabId, section);
            if (!known) {
                promoted.add(tabId);
            }
        }
        if (!promoted.isEmpty()) {
            LOGGER.info("[CAO] {} tab(s) build their own contents, so they keep their own tab and take a section "
                    + "of ours there instead of being folded in: {}", promoted.size(), promoted);
        }
        if (!folded.isEmpty()) {
            LOGGER.info("[CAO] {} tab(s) never fired a contents event but were asked for by name; read their "
                    + "sections off the live tab instead: {}", folded.size(), folded);
        }
    }

    private static boolean isEmptySection(Section<?> section) {
        return section.items().getStacks().isEmpty();
    }

    private static boolean addSection(ResourceLocation parent, Section<?> section) {
        if (isEmptySection(section)) {
            EMPTY_SECTIONS.add(section.id());
            return false;
        }
        FancyTabSections.addSection(parent, section);
        return true;
    }

    private static void logEmptySections() {
        if (EMPTY_SECTIONS.isEmpty()) {
            return;
        }
        LOGGER.info("[CAO] {} tab(s) had no items to show{}, so they were left out rather than given a banner with "
                + "no rows under it -- one of those costs every banner below it a row of alignment: {}",
                EMPTY_SECTIONS.size(), ItemObliteratorSupport.emptyNote(), EMPTY_SECTIONS);
    }

    private static void logSkipDiagnostics() {
        if (SKIP_EXAMPLES.isEmpty()) {
            return;
        }
        int skipped = SKIP_EXAMPLES.values().stream().mapToInt(List::size).sum();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<ResourceLocation>> e : SKIP_EXAMPLES.entrySet()) {
            if (!sb.isEmpty()) {
                sb.append("; ");
            }
            sb.append('"').append(e.getKey()).append("\": ").append(e.getValue());
            if (e.getValue().size() >= SKIP_EXAMPLES_PER_REASON) {
                sb.append(" (+more, capped at ").append(SKIP_EXAMPLES_PER_REASON).append(" examples)");
            }
        }
        LOGGER.info("[CAO] {} candidate tab(s) considered, at least {} skipped -- {}", candidatesSeen, skipped, sb);
    }

    private static void ensureContentsEventCanFire(ResourceLocation id) {
        if (id == null || SimulatedSupport.isMainTab(id) || !needsCapture(id)) {
            return;
        }
        if (FancyTabSections.REGISTERED_TABS.containsKey(id)) {
            LOGGER.warn("[CAO] {} was registered with FancyTabSections again mid-pass, which would have "
                    + "suppressed its contents event; dropping it so the tab can report itself", id);
            dropParentSections(id);
        }
    }

    private static boolean needsCapture(ResourceLocation id) {
        return collecting ? MANAGED_PARENTS.contains(id) : LIVE_RELAYOUT.contains(id);
    }

    private static void forceRebuild(CreativeModeTab.ItemDisplayParameters params) {
        List<CreativeModeTab> order = new ArrayList<>();
        List<CreativeModeTab> deferredParents = new ArrayList<>();
        List<CreativeModeTab> nonCategory = new ArrayList<>();
        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() != CreativeModeTab.Type.CATEGORY) {
                nonCategory.add(tab);
            } else if (MANAGED_PARENTS.contains(BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab))) {
                deferredParents.add(tab);
            } else {
                order.add(tab);
            }
        }
        order.addAll(deferredParents);
        order.addAll(nonCategory);

        int completed = 0;
        for (CreativeModeTab tab : order) {
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            try {
                ensureContentsEventCanFire(id);
                ModernFixCompat.clearMemoizedParams(tab);
                tab.buildContents(params);
                completed++;
            } catch (Throwable t) {
                LOGGER.error("[CAO] creative tab {} threw while rebuilding its contents; leaving it as-is",
                        id, t);
            }
        }
        LOGGER.info("[CAO] forceRebuild: {}/{} tab(s) completed without throwing", completed, order.size());
        CreativeModeTabsAccessor.setCachedParameters(params);
        refreshSearchTrees(params);
    }

    private static void refreshSearchTrees(CreativeModeTab.ItemDisplayParameters params) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return;
        }
        List<ItemStack> searchItems = List.copyOf(CreativeModeTabs.searchTab().getDisplayItems());
        connection.searchTrees().updateCreativeTooltips(params.holders(), searchItems);
        connection.searchTrees().updateCreativeTags(searchItems);
    }

    public static void refreshTabLayout(CreativeModeTab.ItemDisplayParameters params) {
        refreshTabLayout(params, null);
    }

    public static void refreshTabLayout(CreativeModeTab.ItemDisplayParameters params, ResourceLocation alsoRebuild) {
        Set<ResourceLocation> ids = MANAGED_PARENTS;
        if (alsoRebuild != null && !MANAGED_PARENTS.contains(alsoRebuild)) {
            ids = new HashSet<>(MANAGED_PARENTS);
            ids.add(alsoRebuild);
        }
        relayoutComposites(ids, params);
        rebuildTabs(ids, params);
        reconcileAgainstLiveTab(params);
        refreshOpenCreativeScreen();
        BannerRenderer.CURRENT_TAB = null;
        CondensedCreativeSupport.requestResync();
        NativeItemsStore.flush();
    }

    public static void rebuildTab(CreativeModeTab.ItemDisplayParameters params, ResourceLocation tabId) {
        if (tabId != null && params != null) {
            rebuildTabs(Set.of(tabId), params);
        }
    }

    public static void refreshTabRows(CreativeModeTab.ItemDisplayParameters params, ResourceLocation tabId) {
        if (tabId == null || params == null) {
            return;
        }
        Set<ResourceLocation> ids = Set.of(tabId);
        relayoutComposites(ids, params);
        rebuildTabs(ids, params);
        lastReconciledTab = null;
        BannerRenderer.CURRENT_TAB = null;
        refreshOpenCreativeScreen();
        CondensedCreativeSupport.requestResync();
    }

    private static void refreshOpenCreativeScreen() {
        if (!(Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen screen)) {
            return;
        }
        CreativeModeTab selected = CreativeModeInventoryScreenAccessor.getSelectedTab();
        if (selected == null || selected.getDisplayItems() == null) {
            return;
        }
        lastReconciledTab = null;
        ((CreativeModeInventoryScreenAccessor) screen)
                .invokeRefreshCurrentTabContents(selected.getDisplayItems());
    }

    private static void relayoutComposites(Collection<ResourceLocation> ids,
            CreativeModeTab.ItemDisplayParameters params) {
        List<ResourceLocation> composites = new ArrayList<>();
        for (ResourceLocation id : ids) {
            TabLayout layout = TabLayoutStore.byId(id);
            if (layout != null && !layout.isCustom() && layout.sectionCount() > 0
                    && !SimulatedSupport.isMainTab(id)) {
                composites.add(id);
            }
        }
        if (composites.isEmpty()) {
            return;
        }

        LIVE_SECTIONS.clear();
        LIVE_RELAYOUT.addAll(composites);
        Map<ResourceLocation, List<Section<?>>> previous = new LinkedHashMap<>();
        for (ResourceLocation parent : composites) {
            List<Section<?>> existing = FancyTabSections.REGISTERED_TABS.get(parent);
            if (existing != null) {
                previous.put(parent, new ArrayList<>(existing));
            }
            dropParentSections(parent);
        }
        try {
            rebuildTabs(composites, params);
        } finally {
            LIVE_RELAYOUT.clear();
        }

        for (ResourceLocation parent : composites) {
            List<Section<?>> sections = LIVE_SECTIONS.get(parent);
            if (sections == null || sections.isEmpty()) {
                List<Section<?>> restore = previous.get(parent);
                if (restore != null) {
                    LOGGER.warn("[CAO] could not re-derive sections for {}; keeping the previous ones", parent);
                    for (Section<?> section : restore) {
                        addSection(parent, section);
                    }
                }
                continue;
            }
            Set<ResourceLocation> covered = new HashSet<>();
            for (Section<?> section : sections) {
                addSection(parent, section);
                covered.add(section.id());
            }
            for (Section<?> addon : orderedById(PENDING.getOrDefault(parent, List.of()))) {
                if (!covered.contains(addon.id())) {
                    addSection(parent, addon);
                }
            }
        }
        LIVE_SECTIONS.clear();
    }

    public static void dropParentSections(ResourceLocation parent) {
        FancyTabSections.REGISTERED_TABS.remove(parent);
    }

    public static void reapplyAbsorption(CreativeModeTab.ItemDisplayParameters params) {
        Set<ResourceLocation> stillWanted = new HashSet<>();
        if (createPresent()) {
            stillWanted.add(CREATE_BASE);
        }
        stillWanted.addAll(Config.allRouteTargets());
        stillWanted.addAll(Config.extraMainSections());
        stillWanted.addAll(AddonGroups.hubs());
        if (SimulatedSupport.isLoaded()) {
            stillWanted.add(SimulatedSupport.MAIN_TAB);
        }
        stillWanted.removeIf(Config::isForceExcluded);

        Set<ResourceLocation> dropped = new HashSet<>();
        for (ResourceLocation parent : new ArrayList<>(MANAGED_PARENTS)) {
            if (!stillWanted.contains(parent)) {
                MANAGED_PARENTS.remove(parent);
                dropped.add(parent);
                if (SimulatedSupport.isMainTab(parent)) {
                    SimulatedHub.retractAll();
                } else {
                    dropParentSections(parent);
                }
            }
        }
        rebuildTabs(dropped, params);
        MANAGED_PARENTS.addAll(stillWanted);
        for (ResourceLocation parent : stillWanted) {
            if (SimulatedSupport.isMainTab(parent)) {
                continue;
            }
            if (!FancyTabSections.REGISTERED_TABS.containsKey(parent)) {
                Section<?> section = sectionFromLiveTab(parent);
                if (section != null) {
                    addSection(parent, section);
                }
            }
        }

        for (CreativeModeTab tab : CreativeModeTabs.allTabs()) {
            if (tab.getType() != CreativeModeTab.Type.CATEGORY) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.CREATIVE_MODE_TAB.getKey(tab);
            if (id == null || MANAGED_PARENTS.contains(id)) {
                continue;
            }
            ResourceLocation currentParent = LiveColors.findParent(id);
            if (!AddonDetection.isAbsorbTarget(id)) {
                if (currentParent != null) {
                    LiveColors.remove(id);
                }
                AbsorbedTabs.IDS.remove(id);
                continue;
            }
            ResourceLocation desiredParent = Config.parentFor(id);
            if (currentParent == null) {
                if (SimulatedSupport.isMainTab(desiredParent)) {
                    CreativeModeTab liveTab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
                    SimulatedHub.inject(id, liveTab.getDisplayName());
                    SimulatedHub.foldItems(id, liveTab.getDisplayItems());
                } else {
                    Section<?> section = sectionFromLiveTab(id);
                    if (section != null) {
                        addSection(desiredParent, section);
                    }
                }
            } else if (!currentParent.equals(desiredParent)) {
                LiveColors.moveToParent(id, desiredParent);
            }
            AbsorbedTabs.IDS.add(id);
        }

        Set<ResourceLocation> toRebuild = new HashSet<>(stillWanted);
        toRebuild.addAll(dropped);
        rebuildTabs(toRebuild, params);
        reconcileAgainstLiveTab(params);
        resetCreativeScrollIfOpen();
    }

    private static List<Section<?>> orderedById(List<Section<?>> addons) {
        if (addons.isEmpty()) {
            return addons;
        }
        Map<ResourceLocation, Section<?>> byId = new HashMap<>();
        for (Section<?> s : addons) {
            byId.put(s.id(), s);
        }
        List<ResourceLocation> ordered = Config.applyOrder(new ArrayList<>(byId.keySet()),
                id -> CaoSection.titleOf(byId.get(id)).getString());
        List<Section<?>> out = new ArrayList<>(ordered.size());
        for (ResourceLocation id : ordered) {
            out.add(byId.get(id));
        }
        return out;
    }

    private static Section<?> sectionOf(BuildCreativeModeTabContentsEvent event, ResourceLocation id) {
        return sectionFromItems(id, event.getParentEntries(), event.getTab().getDisplayName());
    }

    public static Section<?> sectionFromLiveTab(ResourceLocation id) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(id);
        if (tab == null) {
            LOGGER.warn("[CAO] config references unknown creative tab {}; skipping its section", id);
            return null;
        }
        return sectionFromItems(id, tab.getDisplayItems(), tab.getDisplayName());
    }

    private static List<Section<?>> layoutSectionsOf(TabLayout tab, BuildCreativeModeTabContentsEvent event) {
        return layoutSectionsOf(tab, event.getParentEntries(), event.getTab().getDisplayName());
    }

    private static List<Section<?>> layoutSectionsOf(TabLayout tab, Collection<ItemStack> parentEntries,
            Component tabDisplayName) {
        String ownTitle = tab.nameOverride() != null
                ? tab.nameOverride()
                : tabDisplayName.getString();
        List<ItemStack> borrowed = new ArrayList<>();
        List<Section<?>> pending = PENDING.get(tab.id());
        if (pending != null) {
            for (Section<?> section : pending) {
                if (section instanceof CaoSection cao) {
                    List<ItemStack> stacks = cao.items().getStacks();
                    if (stacks != null) {
                        borrowed.addAll(stacks);
                    }
                }
            }
        }
        List<Section<?>> out = new ArrayList<>();
        for (TabLayout.Group group : LayoutApplier.groupsOf(tab, parentEntries, ownTitle, borrowed)) {
            ResourceLocation sectionId = tab.idForGroup(group);
            out.add(sectionFromItems(sectionId, group.items(), Component.literal(group.title())));
            ItemGroupRuntime.register(sectionId, group.safeFolds());
        }
        return out;
    }

    private static List<Section<?>> recoveredSectionsFor(ResourceLocation parent) {
        CreativeModeTab tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(parent);
        if (tab == null) {
            return List.of();
        }
        List<ItemStack> items = LayoutApplier.stacksOf(nativeItemsOf(parent));
        TabLayout layout = TabLayoutStore.byId(parent);
        if (layout != null && layout.sectionCount() > 0) {
            return layoutSectionsOf(layout, items, tab.getDisplayName());
        }
        return List.of(sectionFromItems(parent, items, tab.getDisplayName()));
    }

    private static Section<?> sectionFromItems(ResourceLocation id, Collection<ItemStack> items, Component tabDisplayName) {
        ItemGroupRuntime.clear(id);
        ConglomerateOfItems conglomerate = ConglomerateOfItems.create();
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }
            conglomerate.add(stack.copy());
        }
        conglomerate.resolveStacks(ClientRegistries.access());
        String nameOverride = Config.sectionNameOverride(id);
        Component title = nameOverride != null ? Component.literal(nameOverride) : tabDisplayName;
        ShippedBanners.seedFor(id);
        String bannerRef = Config.bannerRefFor(id);
        ResourceLocation texture = bannerRef == null ? null : BannerTextures.resolve(bannerRef);
        CaoSection section = new CaoSection(id, title, Config.bannerColorFor(id), texture,
                Config.textColorFor(id), conglomerate);
        if (Config.showCollapseToggle() && Config.isSectionCollapsed(id)) {
            FTSInternal.collapse(section, false);
        }
        return section;
    }
}

