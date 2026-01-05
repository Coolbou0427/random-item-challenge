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

    public ConfigManager() {
        loadConfig();
    }

    private void loadConfig() {
        File configFile = new File(CONFIG_PATH);
        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                JsonObject j = gson.fromJson(reader, JsonObject.class);

                Type itemsT = new TypeToken<List<WeightedItem>>() {}.getType();
                Type cmdsT = new TypeToken<List<String>>() {}.getType();

                items = gson.fromJson(j.get("items"), itemsT);
                regularCommands = gson.fromJson(j.get("regularCommands"), cmdsT);
                resumeCommands = gson.fromJson(j.get("resumeCommands"), cmdsT);

                delay = j.has("delay") ? j.get("delay").getAsDouble() : 15;
                enablemobfriendlyfire = j.has("enablemobfriendlyfire") && j.get("enablemobfriendlyfire").getAsBoolean();
                enabletntautoexplode = j.has("enabletntautoexplode") && j.get("enabletntautoexplode").getAsBoolean();
                return;
            } catch (Exception e) {
                Random_Item_Challenge.LOGGER.error("Failed to load config, resetting to defaults", e);
            }
        }

        setDefaultConfig();
        saveConfig();
    }

    private void setDefaultConfig() {
        items = getDefaultItems();
        regularCommands = StartGameCommands.regularCommands;
        resumeCommands = StartGameCommands.resumeCommands;
        delay = 15;
        enablemobfriendlyfire = false;
        enabletntautoexplode = true;
    }

    public void saveConfig() {
        File configFile = new File(CONFIG_PATH);
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (FileWriter w = new FileWriter(configFile)) {
            String nl = "\n";
            String i1 = "  ";
            String i2 = "    ";

            StringBuilder sb = new StringBuilder();
            sb.append("{").append(nl);

            sb.append(i1).append("\"delay\": ").append(formatNumber(delay)).append(",").append(nl);
            sb.append(i1).append("\"enablemobfriendlyfire\": ").append(enablemobfriendlyfire).append(",").append(nl);
            sb.append(i1).append("\"enabletntautoexplode\": ").append(enabletntautoexplode).append(",").append(nl);

            sb.append(i1).append("\"items\": [").append(nl);
            for (int idx = 0; idx < items.size(); idx++) {
                WeightedItem wi = items.get(idx);
                sb.append(i2).append("{\"items\": [");
                List<String> its = wi.getItems();
                for (int j = 0; j < its.size(); j++) {
                    sb.append("\"").append(jsonEscape(its.get(j))).append("\"");
                    if (j + 1 < its.size()) sb.append(", ");
                }
                sb.append("], \"weight\": ").append(wi.getWeight()).append("}");
                if (idx + 1 < items.size()) sb.append(",");
                sb.append(nl);
            }
            sb.append(i1).append("],").append(nl);

            sb.append(i1).append("\"regularCommands\": [").append(nl);
            for (int idx = 0; idx < regularCommands.size(); idx++) {
                sb.append(i2).append("\"").append(jsonEscape(regularCommands.get(idx))).append("\"");
                if (idx + 1 < regularCommands.size()) sb.append(",");
                sb.append(nl);
            }
            sb.append(i1).append("],").append(nl);

            sb.append(i1).append("\"resumeCommands\": [").append(nl);
            for (int idx = 0; idx < resumeCommands.size(); idx++) {
                sb.append(i2).append("\"").append(jsonEscape(resumeCommands.get(idx))).append("\"");
                if (idx + 1 < resumeCommands.size()) sb.append(",");
                sb.append(nl);
            }
            sb.append(i1).append("]").append(nl);

            sb.append("}").append(nl);

            w.write(sb.toString());
        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Failed to save config to " + CONFIG_PATH, e);
        }
    }

    private static String jsonEscape(String s) {
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': out.append("\\\""); break;
                case '\\': out.append("\\\\"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        String hex = Integer.toHexString(c);
                        out.append("\\u");
                        for (int k = hex.length(); k < 4; k++) out.append('0');
                        out.append(hex);
                    } else {
                        out.append(c);
                    }
            }
        }
        return out.toString();
    }

    private static String formatNumber(double d) {
        if (d == (long) d) return Long.toString((long) d);
        return Double.toString(d);
    }

}
