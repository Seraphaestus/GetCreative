package amaryllis.get_creative;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class GetCreativeMixinPlugin implements IMixinConfigPlugin {

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        // Only port Steam n Rails station assembly if the mod is not loaded
        if (mixinClassName.equals("amaryllis.get_creative.mixin.StationBlockMixin"))
            return !CompatHelper.isModLoaded("railways");

        // Only mixin recipe viewer categories if JEI or a JEI compat mod is loaded
        if (mixinClassName.startsWith("amaryllis.get_creative.mixin.jei"))
            return CompatHelper.isJEILoaded();

        return true;
    }



    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() { return null; }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
