package com.sockywocky.createaddonorganizer;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

public final class AddonGroups {
    private static final Gson GSON = new Gson();
    private static final String SCAN_PATH = "createaddonorganizer/groups";

    private record RawGroup(String hub, List<String> members) {}

    private record Groups(Set<ResourceLocation> hubs, Map<ResourceLocation, ResourceLocation> memberToHub) {}

    private static volatile Groups cached;

    private AddonGroups() {}

    public static Set<ResourceLocation> hubs() {
        return groups().hubs();
    }

    public static ResourceLocation hubFor(ResourceLocation member) {
        return groups().memberToHub().get(member);
    }

    public static boolean isMember(ResourceLocation id) {
        return groups().memberToHub().containsKey(id);
    }

    public static void invalidate() {
        cached = null;
    }

    private static Groups groups() {
        Groups snapshot = cached;
        if (snapshot == null) {
            snapshot = load();
            cached = snapshot;
        }
        return snapshot;
    }

    private static Groups load() {
        Set<ResourceLocation> hubs = new HashSet<>();
        Map<ResourceLocation, ResourceLocation> memberToHub = new HashMap<>();
        Map<ResourceLocation, ResourceLocation> hubSource = new HashMap<>();
        Map<ResourceLocation, ResourceLocation> memberSource = new HashMap<>();

        Set<ResourceLocation> files = new TreeSet<>(Minecraft.getInstance().getResourceManager()
                .listResources(SCAN_PATH, path -> path.getPath().endsWith(".json")).keySet());

        for (ResourceLocation fileId : files) {
            RawGroup raw = parse(fileId);
            if (raw == null || raw.hub() == null) {
                continue;
            }
            ResourceLocation hub = ResourceLocation.tryParse(raw.hub());
            if (hub == null) {
                createaddonorganizer.LOGGER.warn("[CAO] addon group {} has an invalid \"hub\" id: {}", fileId, raw.hub());
                continue;
            }
            if (memberToHub.containsKey(hub)) {
                createaddonorganizer.LOGGER.warn(
                        "[CAO] addon group {} declares hub {}, but it's already a member of another group (from {}); ignoring",
                        fileId, hub, memberSource.get(hub));
                continue;
            }

            List<ResourceLocation> members = new ArrayList<>();
            if (raw.members() != null) {
                for (String memberRaw : raw.members()) {
                    ResourceLocation member = ResourceLocation.tryParse(memberRaw);
                    if (member == null) {
                        createaddonorganizer.LOGGER.warn("[CAO] addon group {} has an invalid member id: {}", fileId, memberRaw);
                        continue;
                    }
                    if (member.equals(hub)) {
                        continue;
                    }
                    if (hubs.contains(member)) {
                        createaddonorganizer.LOGGER.warn(
                                "[CAO] addon group {} lists {} as a member, but it's already a hub declared by {}; ignoring",
                                fileId, member, hubSource.get(member));
                        continue;
                    }
                    ResourceLocation existingHub = memberToHub.get(member);
                    if (existingHub != null && !existingHub.equals(hub)) {
                        createaddonorganizer.LOGGER.warn(
                                "[CAO] addon group {} claims {} for hub {}, but it's already grouped under {} (from {}); keeping the first",
                                fileId, member, hub, existingHub, memberSource.get(member));
                        continue;
                    }
                    members.add(member);
                }
            }
            if (members.isEmpty()) {
                createaddonorganizer.LOGGER.warn("[CAO] addon group {} declares hub {} with no valid members; ignoring", fileId, hub);
                continue;
            }

            hubs.add(hub);
            hubSource.put(hub, fileId);
            for (ResourceLocation member : members) {
                memberToHub.put(member, hub);
                memberSource.put(member, fileId);
            }
        }

        return new Groups(hubs, memberToHub);
    }

    private static RawGroup parse(ResourceLocation fileId) {
        try (InputStream in = Minecraft.getInstance().getResourceManager().open(fileId);
                Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
            return GSON.fromJson(reader, RawGroup.class);
        } catch (IOException | JsonSyntaxException e) {
            createaddonorganizer.LOGGER.warn("[CAO] failed to parse addon group {}", fileId, e);
            return null;
        }
    }
}
