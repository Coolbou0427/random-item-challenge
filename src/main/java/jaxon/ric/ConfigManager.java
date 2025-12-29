package jaxon.ric;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.List;
import static jaxon.ric.Items.getDefaultItems;

public class ConfigManager {
    private static final String CONFIG_PATH = "config/RIC-config.json";
    public List<WeightedItem> items;
    public List<String> regularCommands;
    public List<String> resumeCommands;
    public double delay;
    public boolean enablemobfriendlyfire;
    public boolean enabletntautoexplode;

    public ConfigManager() { loadConfig(); }

    private void loadConfig() {
        File configFile = new File(CONFIG_PATH);
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                Gson gson = new Gson();
                JsonObject j = gson.fromJson(reader, JsonObject.class);
                Type itemsT = new TypeToken<List<WeightedItem>>(){}.getType();
                Type cmdsT  = new TypeToken<List<String>>(){}.getType();
                items           = gson.fromJson(j.get("items"), itemsT);
                regularCommands = gson.fromJson(j.get("regularCommands"), cmdsT);
                resumeCommands  = gson.fromJson(j.get("resumeCommands"), cmdsT);
                delay = j.has("delay") ? j.get("delay").getAsDouble() : 15;
                if (delay < 0) delay = 15;
                enablemobfriendlyfire = j.has("enablemobfriendlyfire") ? j.get("enablemobfriendlyfire").getAsBoolean() : false;
                enabletntautoexplode  = j.has("enabletntautoexplode")  ? j.get("enabletntautoexplode").getAsBoolean()  : false;
            } catch (Exception e) {
                setDefaultConfig();
                saveConfig();
            }
        } else {
            setDefaultConfig();
            saveConfig();
        }
    }

    private void setDefaultConfig() {
        items = getDefaultItems();
        regularCommands = StartGameCommands.regularCommands;
        resumeCommands  = StartGameCommands.resumeCommands;
        delay = 15;
        enablemobfriendlyfire = false;
        enabletntautoexplode  = true;
    }

    public void saveConfig() {
        Gson g = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        try (FileWriter w = new FileWriter(CONFIG_PATH)) {
            w.write("{\n");
            w.write(String.format("  \"delay\": %s,\n", g.toJson(delay)));
            w.write(String.format("  \"enablemobfriendlyfire\": %s,\n", g.toJson(enablemobfriendlyfire)));
            w.write(String.format("  \"enabletntautoexplode\": %s,\n\n", g.toJson(enabletntautoexplode)));
            writeItems(w, g);
            g = new GsonBuilder().disableHtmlEscaping().create();
            writeCommands("regularCommands", regularCommands, w, g);
            writeCommands("resumeCommands",  resumeCommands,  w, g);
            w.write("\n}");
        } catch (Exception ignored) {}
    }

    private void writeItems(FileWriter w, Gson g) throws Exception {
        w.write("  \"items\": [\n");
        for (int i = 0; i < items.size(); i++) {
            String line = g.toJson(items.get(i)).replaceAll("\n", "");
            w.write("    " + line + (i < items.size() - 1 ? ",\n" : "\n"));
        }
        w.write("  ]");
    }

    private void writeCommands(String key, List<String> list, FileWriter w, Gson g) throws Exception {
        w.write(",\n  \"" + key + "\": [\n");
        for (int i = 0; i < list.size(); i++) {
            w.write("    " + g.toJson(list.get(i)) + (i < list.size() - 1 ? ",\n" : "\n  ]"));
        }
    }
}
