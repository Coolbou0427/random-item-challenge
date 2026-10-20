package jaxon.ric;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static jaxon.ric.Items.getDefaultItems;

public class ConfigManager {
    private static final String CONFIG_TXT_PATH = "config/RIC-config.txt";
    private static final String CONFIG_JSON_PATH = "config/RIC-config.json";

    private static final String SECTION_ITEMS = "items:";
    private static final String SECTION_REGULAR_COMMANDS = "regularCommands:";
    private static final String SECTION_RESUME_COMMANDS = "resumeCommands:";
    private static final String SECTION_MOBS_TO_KEEP_ALIVE = "mobsToKeepAlive:";

    private static final String LIST_PREFIX = "- ";
    private static final String ITEM_JOIN = " + ";
    private static final String WEIGHT_DELIM = " -- weight: ";
    private static final String CATEGORY_DELIM = " -- category: ";

    public List<WeightedItem> items;
    public List<String> regularCommands;
    public List<String> resumeCommands;
    public List<String> mobsToKeepAlive;

    public double delay;
    public boolean enablemobfriendlyfire;
    public boolean enableDirectEnchanting;
    public boolean enabletntautoexplode;
    public boolean enablePlayerPlacedBlockDrops;
    public boolean clearMobsOnStart;

    public ConfigManager() {
        loadConfig();
        migrateUpdatedDrops();
    }

    private void migrateUpdatedDrops() {
        if (items == null) return;
        boolean changed = false;
        List<WeightedItem> updated = new ArrayList<>();
        for (WeightedItem drop : items) {
            List<String> commands = new ArrayList<>();
            for (String command : drop.items()) {
                String replacement = command;
                if (command.trim().matches("(?:minecraft:)?evoker_spawn_egg\\s+10")) {
                    replacement = command.replaceFirst("10\\s*$", "2");
                }
                if (command.contains("ric_blast_armor") && !command.contains("Breaks after blocking one explosion.")) {
                    replacement = replacement.replace("{text:\"When worn:\"",
                            "{text:\"Breaks after blocking one explosion.\",color:\"gold\",italic:false},{text:\"When worn:\"");
                }
                changed |= !command.equals(replacement);
                commands.add(replacement);
            }
            updated.add(new WeightedItem(commands, drop.weight(), drop.categoryOverride()));
        }
        if (changed) {
            items = updated;
            saveConfig();
        }
    }

    private void loadConfig() {
        File txt = new File(CONFIG_TXT_PATH);
        if (txt.exists()) {
            if (tryLoadFromTxt(txt)) return;
        }

        File json = new File(CONFIG_JSON_PATH);
        if (json.exists()) {
            if (tryLoadFromJson(json)) return;
        }

        setDefaultConfig();
        saveConfig();
    }

    private boolean tryLoadFromTxt(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            setDefaultConfig();

            List<WeightedItem> parsedItems = new ArrayList<>();
            List<String> parsedRegular = new ArrayList<>();
            List<String> parsedResume = new ArrayList<>();
            List<String> parsedMobsToKeepAlive = new ArrayList<>();
            boolean foundItems = false;
            boolean foundMobsToKeepAlive = false;
            boolean foundPlayerPlacedBlockDrops = false;

            String section = "";

            String line;
            while ((line = br.readLine()) != null) {
                String t = line.trim();
                if (t.isEmpty()) continue;
                if (t.startsWith("#")) continue;
                if (t.startsWith("//")) continue;

                if (t.equalsIgnoreCase(SECTION_ITEMS)) {
                    section = SECTION_ITEMS;
                    foundItems = true;
                    continue;
                }
                if (t.equalsIgnoreCase(SECTION_REGULAR_COMMANDS)) {
                    section = SECTION_REGULAR_COMMANDS;
                    continue;
                }
                if (t.equalsIgnoreCase(SECTION_RESUME_COMMANDS)) {
                    section = SECTION_RESUME_COMMANDS;
                    continue;
                }
                if (t.equalsIgnoreCase(SECTION_MOBS_TO_KEEP_ALIVE)) {
                    section = SECTION_MOBS_TO_KEEP_ALIVE;
                    foundMobsToKeepAlive = true;
                    continue;
                }

                if (section.isEmpty()) {
                    if (t.regionMatches(true, 0, "enablePlayerPlacedBlockDrops:", 0,
                            "enablePlayerPlacedBlockDrops:".length())) {
                        foundPlayerPlacedBlockDrops = true;
                    }
                    parseTopLevelSetting(t);
                    continue;
                }

                if (!t.startsWith(LIST_PREFIX)) continue;

                String body = t.substring(LIST_PREFIX.length());

                switch (section) {
                    case SECTION_ITEMS -> {
                        WeightedItem wi = parseWeightedItemLine(body);
                        if (wi != null) parsedItems.add(wi);
                    }
                    case SECTION_REGULAR_COMMANDS -> {
                        if (!body.isEmpty()) parsedRegular.add(body);
                    }
                    case SECTION_RESUME_COMMANDS -> {
                        if (!body.isEmpty()) parsedResume.add(body);
                    }
                    case SECTION_MOBS_TO_KEEP_ALIVE -> {
                        if (!body.isEmpty()) parsedMobsToKeepAlive.add(body);
                    }
                }
            }

            if (foundItems) items = parsedItems;
            if (!parsedRegular.isEmpty()) regularCommands = parsedRegular;
            if (!parsedResume.isEmpty()) resumeCommands = parsedResume;
            if (foundMobsToKeepAlive) mobsToKeepAlive = parsedMobsToKeepAlive;
            if (!foundPlayerPlacedBlockDrops) saveConfig();

            return true;
        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Failed to load custom config from {}", file.getPath(), e);
            return false;
        }
    }

    private void parseTopLevelSetting(String t) {
        int colon = t.indexOf(':');
        if (colon < 0) return;

        String key = t.substring(0, colon).trim().toLowerCase();
        String val = t.substring(colon + 1).trim();

        switch (key) {
            case "delay" -> {
                Double d = parseDoubleOrNull(val);
                if (d != null) delay = d;
                return;
            }
            case "enablemobfriendlyfire" -> {
                Boolean b = parseBooleanOrNull(val);
                if (b != null) enablemobfriendlyfire = b;
                return;
            }
            case "enabledirectenchanting" -> {
                Boolean b = parseBooleanOrNull(val);
                if (b != null) enableDirectEnchanting = b;
                return;
            }
            case "enabletntautoexplode" -> {
                Boolean b = parseBooleanOrNull(val);
                if (b != null) enabletntautoexplode = b;
                return;
            }
            case "enableplayerplacedblockdrops" -> {
                Boolean b = parseBooleanOrNull(val);
                if (b != null) enablePlayerPlacedBlockDrops = b;
            }
            case "clearmobsonstart" -> {
                Boolean b = parseBooleanOrNull(val);
                if (b != null) clearMobsOnStart = b;
            }
        }

    }

    private WeightedItem parseWeightedItemLine(String body) {
        String categoryOverride = null;
        int categoryIndex = body.lastIndexOf(CATEGORY_DELIM);
        if (categoryIndex >= 0) {
            categoryOverride = body.substring(categoryIndex + CATEGORY_DELIM.length()).trim();
            body = body.substring(0, categoryIndex).trim();
        }
        int idx = body.lastIndexOf(WEIGHT_DELIM);
        if (idx < 0) return null;

        String itemsPart = body.substring(0, idx).trim();
        String weightPart = body.substring(idx + WEIGHT_DELIM.length()).trim();

        Integer w = parseIntOrNull(weightPart);
        if (w == null) return null;

        if (itemsPart.isEmpty()) return null;

        List<String> itemStrings = splitKeepRaw(itemsPart, ITEM_JOIN);
        if (itemStrings.isEmpty()) return null;

        return new WeightedItem(itemStrings, w, categoryOverride);
    }

    private static List<String> splitKeepRaw(String s, String delim) {
        List<String> out = new ArrayList<>();
        int start = 0;
        int bracketDepth = 0;
        char quote = 0;
        boolean escaped = false;
        for (int index = 0; index < s.length(); index++) {
            char current = s.charAt(index);
            if (quote != 0) {
                if (escaped) escaped = false;
                else if (current == '\\') escaped = true;
                else if (current == quote) quote = 0;
                continue;
            }
            if (current == '"' || current == '\'') {
                quote = current;
                continue;
            }
            if (current == '[' || current == '{' || current == '(') bracketDepth++;
            else if (current == ']' || current == '}' || current == ')') bracketDepth = Math.max(0, bracketDepth - 1);
            if (bracketDepth == 0 && s.startsWith(delim, index)) {
                String piece = s.substring(start, index).trim();
                if (!piece.isEmpty()) out.add(piece);
                index += delim.length() - 1;
                start = index + 1;
            }
        }
        String last = s.substring(start).trim();
        if (!last.isEmpty()) out.add(last);
        return out;
    }

    private boolean tryLoadFromJson(File file) {
        try (FileReader reader = new FileReader(file)) {
            Gson gson = new GsonBuilder().disableHtmlEscaping().create();
            JsonObject j = gson.fromJson(reader, JsonObject.class);

            Type itemsT = new TypeToken<List<WeightedItem>>() {}.getType();
            Type cmdsT = new TypeToken<List<String>>() {}.getType();

            items = gson.fromJson(j.get("items"), itemsT);
            regularCommands = gson.fromJson(j.get("regularCommands"), cmdsT);
            resumeCommands = gson.fromJson(j.get("resumeCommands"), cmdsT);

            delay = j.has("delay") ? j.get("delay").getAsDouble() : 15;
            enablemobfriendlyfire = j.has("enablemobfriendlyfire") && j.get("enablemobfriendlyfire").getAsBoolean();
            enableDirectEnchanting = j.has("enableDirectEnchanting") && j.get("enableDirectEnchanting").getAsBoolean();
            enabletntautoexplode = j.has("enabletntautoexplode") && j.get("enabletntautoexplode").getAsBoolean();
            enablePlayerPlacedBlockDrops = !j.has("enablePlayerPlacedBlockDrops")
                    || j.get("enablePlayerPlacedBlockDrops").getAsBoolean();
            clearMobsOnStart = !j.has("clearMobsOnStart") || j.get("clearMobsOnStart").getAsBoolean();
            mobsToKeepAlive = j.has("mobsToKeepAlive") ? gson.fromJson(j.get("mobsToKeepAlive"), cmdsT) : defaultMobsToKeepAlive();

            saveConfig();
            return true;
        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Failed to load json config, resetting to defaults", e);
            return false;
        }
    }

    private void setDefaultConfig() {
        items = getDefaultItems();
        regularCommands = StartGameCommands.regularCommands;
        resumeCommands = StartGameCommands.resumeCommands;
        delay = 15;
        enablemobfriendlyfire = false;
        enableDirectEnchanting = false;
        enabletntautoexplode = true;
        enablePlayerPlacedBlockDrops = true;
        clearMobsOnStart = true;
        mobsToKeepAlive = defaultMobsToKeepAlive();
    }

    public void saveConfig() {
        File configFile = new File(CONFIG_TXT_PATH);
        File parent = configFile.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();

        try (FileWriter w = new FileWriter(configFile)) {
            String nl = "\n";
            String i1 = "  ";

            StringBuilder sb = new StringBuilder();

            sb.append("delay: ").append(formatNumber(delay)).append(nl);
            sb.append("enablemobfriendlyfire: ").append(enablemobfriendlyfire).append(nl);
            sb.append("enableDirectEnchanting: ").append(enableDirectEnchanting).append(nl);
            sb.append("enabletntautoexplode: ").append(enabletntautoexplode).append(nl);
            sb.append("enablePlayerPlacedBlockDrops: ").append(enablePlayerPlacedBlockDrops).append(nl);
            sb.append("# Clear previous player-placed mobs and hostile mobs, including saved chunks.").append(nl);
            sb.append("clearMobsOnStart: ").append(clearMobsOnStart).append(nl);

            sb.append(nl);
            sb.append(SECTION_REGULAR_COMMANDS).append(nl);
            for (String cmd : regularCommands) {
                sb.append(i1).append(LIST_PREFIX).append(cmd).append(nl);
            }

            sb.append(nl);
            sb.append(SECTION_RESUME_COMMANDS).append(nl);
            for (String cmd : resumeCommands) {
                sb.append(i1).append(LIST_PREFIX).append(cmd).append(nl);
            }

            sb.append(nl);
            sb.append("# Legacy list, ignored by placed/hostile mob cleanup.").append(nl);
            sb.append(SECTION_MOBS_TO_KEEP_ALIVE).append(nl);
            for (String mobId : mobsToKeepAlive) {
                sb.append(i1).append(LIST_PREFIX).append(mobId).append(nl);
            }

            sb.append(nl);
            sb.append(SECTION_ITEMS).append(nl);
            for (WeightedItem wi : items) {
                sb.append(i1).append(LIST_PREFIX);
                List<String> its = wi.getItems();
                for (int i = 0; i < its.size(); i++) {
                    sb.append(its.get(i));
                    if (i + 1 < its.size()) sb.append(ITEM_JOIN);
                }
                sb.append(WEIGHT_DELIM).append(wi.getWeight());
                if (wi.categoryOverride() != null && !wi.categoryOverride().isBlank()) {
                    sb.append(CATEGORY_DELIM).append(wi.categoryOverride());
                }
                sb.append(nl);
            }

            w.write(sb.toString());
        } catch (Exception e) {
            Random_Item_Challenge.LOGGER.error("Failed to save config to " + CONFIG_TXT_PATH, e);
        }
    }


    private static Integer parseIntOrNull(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Double parseDoubleOrNull(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Boolean parseBooleanOrNull(String s) {
        String t = s.trim().toLowerCase();
        if (t.equals("true")) return true;
        if (t.equals("false")) return false;
        return null;
    }

    private static List<String> defaultMobsToKeepAlive() {
        return new ArrayList<>(List.of(
                "minecraft:player", "minecraft:villager", "minecraft:cow", "minecraft:sheep",
                "minecraft:chicken", "minecraft:goat", "minecraft:dolphin", "minecraft:allay",
                "minecraft:ender_dragon", "minecraft:end_crystal", "minecraft:armadillo",
                "minecraft:nautilus", "minecraft:zombie_nautilus"
        ));
    }

    private static String formatNumber(double d) {
        if (d == (long) d) return Long.toString((long) d);
        return Double.toString(d);
    }
}
