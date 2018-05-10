package mods.belgabor.bitdrawers.core.recipes.factories.conditions;

import com.google.gson.JsonObject;
import mods.belgabor.bitdrawers.BitDrawers;
import net.minecraft.util.JsonUtils;
import net.minecraftforge.common.crafting.IConditionFactory;
import net.minecraftforge.common.crafting.JsonContext;

import java.util.function.BooleanSupplier;

/**
 * @author GuntherDW
 */
public class FeatureEnabled implements IConditionFactory {

    private static final String JSON_FEATURE_KEY = "feature";

    @Override
    public BooleanSupplier parse (JsonContext context, JsonObject json) {
        final boolean result;
        if (JsonUtils.isString( json, JSON_FEATURE_KEY )) {
            final String feature = JsonUtils.getString( json, JSON_FEATURE_KEY );
            // Right now only controllers are toggleable
            result = feature.equalsIgnoreCase("BitController") && BitDrawers.config.enableBitController;
        } else {
            result = false;
        }

        return () -> result;
    }
}
