package com.xinian.tickaccelerate.config;

import com.xinian.tickaccelerate.util.Mask;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

public class BlockEntityMaskConfig extends TOMLConfiguration {

    private Mask mask;

    public BlockEntityMaskConfig() {
        super("block_entity_mask.toml");
        putIfEmpty("type", "whitelist");
        setComment("type", "The type of the mask. Can be 'whitelist' or 'blacklist'.");
        putIfEmpty("blocks", List.of("*:*"));
        setComment("blocks", "A list of block identifiers to be included in the mask.\nExamples: [\"minecraft:furnace\", \"minecraft:chest\"]");
        save();
        reload();
    }

    public Mask getMask() {
        return this.mask;
    }

    @Override
    public void reload() {
        super.reload();
        this.mask = new Mask(ForgeRegistries.BLOCKS, this, "blocks");
    }
}