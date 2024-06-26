package moe.plushie.armourers_workshop.utils;


import moe.plushie.armourers_workshop.api.skin.ISkinPaintType;
import moe.plushie.armourers_workshop.api.skin.ISkinPartType;
import moe.plushie.armourers_workshop.api.skin.ISkinType;
import moe.plushie.armourers_workshop.core.data.slot.ItemOverrideType;
import moe.plushie.armourers_workshop.core.skin.SkinTypes;
import moe.plushie.armourers_workshop.init.ModLog;
import moe.plushie.armourers_workshop.init.platform.CommonNativeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.ArrayList;

public final class TranslateUtils {

    public static MutableComponent formatted(String content) {
        return Component.literal(getFormattedString(content));
    }

    public static MutableComponent title(String key, Object... args) {
        return CommonNativeManager.getFactory().createTranslatableComponent(key, args);
    }

    public static MutableComponent subtitle(String key, Object... args) {
        return title(key, args).setStyle(Style.EMPTY.withColor(ChatFormatting.GRAY));
    }

    public static ArrayList<Component> subtitles(String key) {
        ArrayList<Component> results = new ArrayList<>();
        MutableComponent value1 = subtitle(key);
        String value = value1.getString();
        if (key.equals(value)) {
            return results;
        }
        Style style = Style.EMPTY.withColor(ChatFormatting.GRAY);
        for (String line : getFormattedString(value).split("(\\r?\\n)|(%n)")) {
            results.add(Component.literal(line).setStyle(style));
        }
        return results;
    }

    public static String getEmbeddedStyle(String value) {
        int i = value.length();
        StringBuilder results = new StringBuilder();
        for (int j = 0; j < i; ++j) {
            char c0 = value.charAt(j);
            if (c0 == 167) {
                if (j + 1 >= i) {
                    break;
                }
                char c1 = value.charAt(j + 1);
                results.append(c0);
                results.append(c1);
                ++j;
            }
        }
        return results.toString();
    }

    public static String getFormattedString(String value) {
        // The following color codes can be added to the start of text to colur it.
        // &0 Black
        // &1 Dark Blue
        // &2 Dark Green
        // &3 Dark Aqua
        // &4 Dark Red
        // &5 Dark Purple
        // &6 Gold
        // &7 Gray
        // &8 Dark Gray
        // &9 Blue
        // &a Green
        // &b Aqua
        // &c Red
        // &d Light Purple
        // &e Yellow
        // &f White
        //
        // A new line can be inserted with %n. Please add/remove new lines to fit the localisations you are writing.
        //
        // The text %s will be replace with text. Example: "Author: %s" could become "Author: RiskyKen".
        // The text %d will be replace with a number. Example: "Radius: %d*%d*%d" could become "Radius: 3*3*3"
        value = value.replace("\n", System.lineSeparator());
        value = value.replace("%n", System.lineSeparator());
        return value;
    }

    public static class Name {

        public static MutableComponent of(ItemOverrideType overrideType) {
            return title("itemOverrideType.armourers_workshop." + overrideType.getName());
        }

        public static MutableComponent of(ISkinType skinType) {
            if (skinType == SkinTypes.UNKNOWN) {
                return title("skinType.armourers_workshop.all");
            }
            String path = skinType.getRegistryName().getPath();
            return title("skinType.armourers_workshop." + path);
        }

        public static MutableComponent of(ISkinPartType skinPartType) {
            String path = skinPartType.getRegistryName().getPath();
            String key = "skinPartType.armourers_workshop." + path;
            MutableComponent text = title(key);
            if (!text.getString().equals(key)) {
                return text;
            }
            ModLog.debug("missing translation text for key {}", key);
            return title("skinPartType.armourers_workshop.all.base");
        }

        public static MutableComponent of(ISkinPaintType paintType) {
            String path = paintType.getRegistryName().getPath();
            return title("paintType.armourers_workshop." + path);
        }
    }
}
