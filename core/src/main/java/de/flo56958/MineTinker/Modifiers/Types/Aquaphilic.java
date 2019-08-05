package de.flo56958.MineTinker.Modifiers.Types;

import de.flo56958.MineTinker.Data.ModifierFailCause;
import de.flo56958.MineTinker.Data.ToolType;
import de.flo56958.MineTinker.Events.ModifierFailEvent;
import de.flo56958.MineTinker.Main;
import de.flo56958.MineTinker.Modifiers.Modifier;
import de.flo56958.MineTinker.Utilities.ChatWriter;
import de.flo56958.MineTinker.Utilities.ConfigurationManager;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Aquaphilic extends Modifier {

    private static Aquaphilic instance;

    public static Aquaphilic instance() {
        synchronized (Aquaphilic.class) {
            if (instance == null) {
                instance = new Aquaphilic();
            }
        }

        return instance;
    }

    @Override
    public String getKey() {
        return "Aquaphilic";
    }

    @Override
    public List<ToolType> getAllowedTools() {
        return new ArrayList<>(Arrays.asList(ToolType.BOOTS, ToolType.HELMET));
    }

    private Aquaphilic() {
        super(Main.getPlugin());
    }

    @Override
    public List<Enchantment> getAppliedEnchantments() {
        return Arrays.asList(Enchantment.DEPTH_STRIDER, Enchantment.OXYGEN, Enchantment.WATER_WORKER);
    }

    @Override
    public void reload() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);

        String key = getKey();

        config.addDefault(key + ".allowed", true);
        config.addDefault(key + ".name", key);
        config.addDefault(key + ".name_modifier", "Pearl of the ocean");
        config.addDefault(key + ".modifier_item", "HEART_OF_THE_SEA"); //Needs to be a viable Material-Type
        config.addDefault(key + ".description", "Make the water your friend");
        config.addDefault(key + ".description_modifier", "%WHITE%Modifier-Item for the " + key + "-Modifier");
        config.addDefault(key + ".Color", "%AQUA%");
        config.addDefault(key + ".EnchantCost", 10);
        config.addDefault(key + ".MaxLevel", 3); //higher will have no effect on depth strider

        config.addDefault(key + ".Recipe.Enabled", true);
        config.addDefault(key + ".Recipe.Top", "PNP");
        config.addDefault(key + ".Recipe.Middle", "NHN");
        config.addDefault(key + ".Recipe.Bottom", "PNP");

        Map<String, String> recipeMaterials = new HashMap<>();
        recipeMaterials.put("H", "HEART_OF_THE_SEA");
        recipeMaterials.put("N", "NAUTILUS_SHELL");
        recipeMaterials.put("P", "PRISMARINE_SHARD");

        config.addDefault(key + ".Recipe.Materials", recipeMaterials);

        ConfigurationManager.saveConfig(config);
        ConfigurationManager.loadConfig("Modifiers" + File.separator, getFileName());

        init("[" + config.getString(key + ".name_modifier") + "] \u200B" + config.getString(key + ".description"),
                ChatWriter.getColor(config.getString(key + ".Color")),
                config.getInt(key + ".MaxLevel"),
                modManager.createModifierItem(Material.getMaterial(config.getString(key + ".modifier_item")), ChatWriter.getColor(config.getString(key + ".Color")) + config.getString(key + ".name_modifier"), ChatWriter.addColors(config.getString(key + ".description_modifier")), this));

    }

    @Override
    public boolean applyMod(Player p, ItemStack tool, boolean isCommand) {
        if (modManager.hasMod(tool, Freezing.instance())) {
            pluginManager.callEvent(new ModifierFailEvent(p, tool, this, ModifierFailCause.INCOMPATIBLE_MODIFIERS, isCommand));
            return false;
        }

        if (!Modifier.checkAndAdd(p, tool, this, "aquaphilic", isCommand)) {
            return false;
        }

        ItemMeta meta = tool.getItemMeta();

        if (meta != null) {
            if (ToolType.BOOTS.contains(tool.getType())) {
                meta.addEnchant(Enchantment.DEPTH_STRIDER, modManager.getModLevel(tool, this), true);
            } else if (ToolType.HELMET.contains(tool.getType())) {
                meta.addEnchant(Enchantment.OXYGEN, modManager.getModLevel(tool, this), true);
                meta.addEnchant(Enchantment.WATER_WORKER, modManager.getModLevel(tool, this), true);
            }

            if (Main.getPlugin().getConfig().getBoolean("HideEnchants")) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

        }

        tool.setItemMeta(meta);

        return true;
    }
}
