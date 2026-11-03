package dk.anchormacro;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;

/** Indstillinger i config/anchormacro.json. Rediger filen, og genstart spillet. */
public class AnchorConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Antal glowstone der fyldes på (1-4). */
    public static int charges = 1;
    /** Kræv totem i offhand, før macroen kører. */
    public static boolean requireTotem = true;
    /** Flyt selv en totem fra inventory til offhand. */
    public static boolean autoEquipTotem = true;
    /** Sæt din hotbar-slot tilbage bagefter. */
    public static boolean restoreSlot = true;

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("anchormacro.json");
    }

    public static void load() {
        Path f = file();
        if (!Files.exists(f)) {
            save();
            return;
        }
        try (Reader r = Files.newBufferedReader(f)) {
            JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
            if (o.has("charges")) charges = Math.max(1, Math.min(4, o.get("charges").getAsInt()));
            if (o.has("requireTotem")) requireTotem = o.get("requireTotem").getAsBoolean();
            if (o.has("autoEquipTotem")) autoEquipTotem = o.get("autoEquipTotem").getAsBoolean();
            if (o.has("restoreSlot")) restoreSlot = o.get("restoreSlot").getAsBoolean();
        } catch (Exception e) {
            AnchorMacro.LOGGER.warn("Kunne ikke læse config, bruger standardværdier", e);
        }
    }

    public static void save() {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("charges", charges);
            o.addProperty("requireTotem", requireTotem);
            o.addProperty("autoEquipTotem", autoEquipTotem);
            o.addProperty("restoreSlot", restoreSlot);
            Files.writeString(file(), GSON.toJson(o));
        } catch (Exception e) {
            AnchorMacro.LOGGER.warn("Kunne ikke gemme config", e);
        }
    }
}
