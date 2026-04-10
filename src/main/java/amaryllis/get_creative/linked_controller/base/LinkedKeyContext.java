package amaryllis.get_creative.linked_controller.base;

import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;

public class LinkedKeyContext implements IKeyConflictContext {

    public static LinkedKeyContext INSTANCE = new LinkedKeyContext();

    public boolean isActive() {
        return !KeyConflictContext.GUI.isActive();
    }

    public boolean conflicts(IKeyConflictContext other) {
        return false;
    }
}