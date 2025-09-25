package com.xinian.tickaccelerate.util;

import com.xinian.tickaccelerate.TickAccelerate;
import com.xinian.tickaccelerate.config.TOMLConfiguration;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Mask {

    private final TOMLConfiguration file;
    private final MaskType maskType;
    private final IForgeRegistry<?> registry;
    private final RegistryIndex index;
    private final Set<ResourceLocation> entries;

    public Mask(IForgeRegistry<?> registry, TOMLConfiguration file, String maskKey) {
        this.file = file;

        String type = file.get("type");
        if (type == null) {
            TickAccelerate.LOGGER.warn("(TickAccelerate) Mask type is missing in '{}', defaulting to 'whitelist'.", file.fileName);
            this.maskType = MaskType.WHITELIST;
        } else {
            this.maskType = MaskType.fromString(type);
        }

        this.registry = registry;
        this.index = RegistryIndex.getIndex(this.registry);
        this.entries = new HashSet<>();

        List<String> maskList = file.get(maskKey);
        if (maskList == null) {
            TickAccelerate.LOGGER.error("(TickAccelerate) Mask entry '{}' is missing or not an array in '{}'.", maskKey, file.fileName);
            return;
        }

        for (String element : maskList) {
            entries.addAll(manageEntry(element));
        }
    }

    // @SuppressWarnings("ConstantConditions") // 移除此注解，因为它不再需要
    public List<ResourceLocation> manageEntry(String entry) {
        String[] split = entry.split(":");

        if (split.length != 2) {
            TickAccelerate.LOGGER.error("(TickAccelerate) '" + entry + "' is not a valid identifier. Correct format is <namespace>:<path>");
            return new ArrayList<>();
        }

        // if *:*

        if (split[0].equals("*") && split[1].equals("*")) {
            return index.getIdentifiers();
        }

        // if <namespace>:<path>

        if (!split[0].equals("*") && !split[1].equals("*")) {
            return List.of(ResourceLocation.tryBuild(split[0], split[1]));
        }

        // if *:<path>

        if (split[0].equals("*") && !split[1].equals("*")) {
            return index.getPathIndex().getOrDefault(split[1], new ArrayList<>());
        }


        // if <namespace>:*

        if (!split[0].equals("*") && split[1].equals("*")) {
            return index.getNamespaceIndex().getOrDefault(split[0], new ArrayList<>());
        }
        // 移除不可达的 return null;
        throw new IllegalStateException("Should not reach here: manageEntry did not return a list for entry: " + entry);
    }

    public IForgeRegistry<?> getRegistry() {
        return registry;
    }

    public TOMLConfiguration getFile() {
        return file;
    }

    public boolean matches(ResourceLocation identifier) {
        return entries.contains(identifier);
    }

    public boolean isOkay(ResourceLocation identifier) {
        boolean contains = entries.contains(identifier);
        return maskType == MaskType.WHITELIST ? contains : !contains;
    }
}