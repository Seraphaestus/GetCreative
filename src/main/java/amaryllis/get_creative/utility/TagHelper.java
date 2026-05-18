package amaryllis.get_creative.utility;

import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import org.jetbrains.annotations.Nullable;

public class TagHelper {

    public static @Nullable HolderSet.Named<Block> getBlocks(RegistryAccess registryAccess, TagKey<Block> tag) {
        return registryAccess.lookupOrThrow(BuiltInRegistries.BLOCK.key()).get(tag)
                .orElse(null);
    }
    public static @Nullable HolderSet.Named<Fluid> getFluids(RegistryAccess registryAccess, TagKey<Fluid> tag) {
        return registryAccess.lookupOrThrow(BuiltInRegistries.FLUID.key()).get(tag)
                .orElse(null);
    }

    public static boolean hasAnyBlocks(RegistryAccess registryAccess, TagKey<Block> tag) {
        var blocks = getBlocks(registryAccess, tag);
        return blocks != null && blocks.size() > 0;
    }
    public static boolean hasAnyFluids(RegistryAccess registryAccess, TagKey<Fluid> tag) {
        var fluids = getFluids(registryAccess, tag);
        return fluids != null && fluids.size() > 0;
    }

}
