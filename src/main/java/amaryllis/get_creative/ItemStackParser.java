package amaryllis.get_creative;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

public class ItemStackParser {
    protected static HolderLookup.Provider registries;
    protected static ItemParser parser;

    public static ItemStack parse(String string) {
        return parse(string, e -> {});
    }
    public static ItemStack parse(String string, Consumer<Exception> errorHandler) {
        if (registries == null) registries = VanillaRegistries.createLookup();
        if (parser == null) parser = new ItemParser(registries);
        try {
            ItemParser.ItemResult result = parser.parse(new StringReader(string.trim()));
            return new ItemStack(result.item(), 1, result.components());
        } catch (Exception e) {
            errorHandler.accept(e);
            return ItemStack.EMPTY;
        }
    }

    public static void clean() {
        registries = null;
        parser = null;
    }
}
