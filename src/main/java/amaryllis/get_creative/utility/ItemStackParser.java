package amaryllis.get_creative.utility;

import com.mojang.brigadier.StringReader;
import net.minecraft.commands.arguments.item.ItemParser;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Consumer;
import java.util.regex.Pattern;

public class ItemStackParser {

    protected static final Pattern countRegex = Pattern.compile("\\d+ *x");

    protected static HolderLookup.Provider registries;
    protected static ItemParser parser;

    public static void init() {
        registries = VanillaRegistries.createLookup();
    }

    public static ItemStack parse(String string) {
        return parse(string, e -> {});
    }
    public static ItemStack parse(String string, Consumer<Exception> errorHandler) {
        if (parser == null) parser = new ItemParser(registries);
        try {
            if (string.startsWith("#")) throw new Exception("Item tags not allowed in this context.");

            var countData = parseCount(string);
            string = countData.getA();
            int count = countData.getB();

            if (string.startsWith("#")) throw new Exception("Item tag ingredients cannot have a count.");

            ItemParser.ItemResult result = parser.parse(new StringReader(string.trim()));
            int maxCount = result.item().value().getDefaultMaxStackSize();
            return new ItemStack(result.item(), Math.min(count, maxCount), result.components());
        } catch (Exception e) {
            errorHandler.accept(e);
            return ItemStack.EMPTY;
        }
    }

    protected static Tuple<String, Integer> parseCount(String string) {
        int count = 1;
        var countMatcher = countRegex.matcher(string);
        if (countMatcher.find() && countMatcher.start() == 0) {
            String[] split = string.split("x", 2);
            count = Integer.parseInt(split[0].trim());
            string = split[1];
        }
        return new Tuple<>(string, count);
    }

    public static Ingredient parseIngredient(String string, Consumer<Exception> errorHandler) {
        if (!string.startsWith("#")) return Ingredient.of(parse(string, errorHandler));

        string = string.substring(1);
        ResourceLocation tagID = ResourceLocation.tryParse(string);
        if (tagID == null) {
            errorHandler.accept(new Exception("Malformed tag: #" + string));
            return Ingredient.EMPTY;
        }
        return Ingredient.of(TagKey.create(Registries.ITEM, tagID));
    }

    public static void clean() {
        parser = null;
    }
}
