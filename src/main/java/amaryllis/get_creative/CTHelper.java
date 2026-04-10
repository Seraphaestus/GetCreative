package amaryllis.get_creative;

import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;

public class CTHelper {

    public static CTSpriteShiftEntry get(CTType type, String blockTextureName) {
        return get(type, blockTextureName, blockTextureName);
    }
    public static CTSpriteShiftEntry get(CTType type, String blockTextureName, String connectedTextureName) {
        return CTSpriteShifter.getCT(type, GetCreative.ID("block/" + blockTextureName),
                GetCreative.ID("block/" + connectedTextureName + "_connected"));
    }
}
