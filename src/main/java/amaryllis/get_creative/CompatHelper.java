package amaryllis.get_creative;

import net.neoforged.fml.loading.LoadingModList;

import java.util.function.Supplier;

public class CompatHelper {

    public static boolean isModLoaded(String modID) {
        return LoadingModList.get().getModFileById(modID) != null;
    }

    public static boolean isJEILoaded() {
        return isModLoaded("jei") || isModLoaded("toomanyrecipeviewers") || isModLoaded("roughlyenoughitems");
    }

    public static void safeRunIf(boolean condition, Supplier<Runnable> callback) {
        if (condition) callback.get().run();
    }
}
