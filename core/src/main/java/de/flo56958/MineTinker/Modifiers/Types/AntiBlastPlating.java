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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AntiBlastPlating extends Modifier {

    private boolean compatibleWithProtecting;
    private boolean compatibleWithInsulating;
    private boolean compatibleWithAntiArrow;

    private static AntiBlastPlating instance;

    public static AntiBlastPlating instance() {
        synchronized (AntiBlastPlating.class) {
            if (instance == null) {
                instance = new AntiBlastPlating();
            }
        }

        return instance;
    }

    @Override
    public String getKey() {
        return "Anti-Blast-Plating";
    }

    @Override
    public List<ToolType> getAllowedTools() {
        return Arrays.asList(ToolType.BOOTS, ToolType.LEGGINGS, ToolType.CHESTPLATE, ToolType.HELMET);
    }

    private AntiBlastPlating() {
        super(Main.getPlugin());
    }

    @Override
    public List<Enchantment> getAppliedEnchantments() {
        return Collections.singletonList(Enchantment.PROTECTION_EXPLOSIONS);
    }

    @Override
    public void reload() {
        FileConfiguration config = getConfig();
        config.options().copyDefaults(true);

        String key = getKey();

        config.addDefault(key + ".allowed", true);
        config.addDefault(key + ".name", key);
        config.addDefault(key + ".name_modifier", "Blast Resistant Metal");
        config.addDefault(key + ".modifier_item", "IRON_BLOCK"); //Needs to be a viable Material-Type
        config.addDefault(key + ".description", "Armor mitigates explosion damage!");
        config.addDefault(key + ".description_modifier", "%WHITE%Modifier-Item for the " + key + "-Modifier");
        config.addDefault(key + ".Color", "%WHITE%");
        config.addDefault(key + ".EnchantCost", 10);
        config.addDefault(key + ".MaxLevel", 5);

        config.addDefault(key + ".CompatibleWithProtecting", false);
        config.addDefault(key + ".CompatibleWithInsulating", false);
        config.addDefault(key + ".CompatibleWithAntiArrow", false);

        config.addDefault(key + ".Recipe.Enabled", true);
        config.addDefault(key + ".Recipe.Top", "IMI");
        config.addDefault(key + ".Recipe.Middle", "MDM");
        config.addDefault(key + ".Recipe.Bottom", "IMI");

        Map<String, String> recipeMaterials = new HashMap<>();
        recipeMaterials.put("I", "IRON_BLOCK");
        recipeMaterials.put("M", "TNT");
        recipeMaterials.put("D", "DIAMOND");

        config.addDefault(key + ".Recipe.Materials", recipeMaterials);

        ConfigurationManager.saveConfig(config);
        ConfigurationManager.loadConfig("Modifiers" + File.separator, getFileName());

        init("[" + config.getString(key + ".name_modifier") + "] \u200B" + config.getString(key + ".description"),
                ChatWriter.getColor(config.getString(key + ".Color")), config.getInt(key + ".MaxLevel"),
                modManager.createModifierItem(Material.getMaterial(config.getString(key + ".modifier_item")),
                        ChatWriter.getColor(config.getString(key + ".Color")) + config.getString(key + ".name_modifier"),
                        ChatWriter.addColors(config.getString(key + ".description_modifier")), this));

        this.compatibleWithProtecting = config.getBoolean(key + ".CompatibleWithProtecting");
        this.compatibleWithInsulating = config.getBoolean(key + ".CompatibleWithInsulating");
        this.compatibleWithAntiArrow = config.getBoolean(key + ".CompatibleWithAntiArrow");
    }

    @Override
    public boolean applyMod(Player p, ItemStack tool, boolean isCommand) {
        if (!Modifier.checkAndAdd(p, tool, this, "antiblastplating", isCommand)) {
            return false;
        }

        ItemMeta meta = tool.getItemMeta();

        if (meta != null) {
            if (ToolType.HELMET.contains(tool.getType()) || ToolType.CHESTPLATE.contains(tool.getType())
                    || ToolType.LEGGINGS.contains(tool.getType()) || ToolType.BOOTS.contains(tool.getType())) {

                if (!this.compatibleWithProtecting) {
                    if (modManager.hasMod(tool, Protecting.instance()) || meta.hasEnchant(Enchantment.PROTECTION_ENVIRONMENTAL)) {
                        pluginManager.callEvent(new ModifierFailEvent(p, tool, this, ModifierFailCause.INCOMPATIBLE_MODIFIERS, isCommand));
                        return false;
                    }
                }

                if (!this.compatibleWithInsulating) {
                    if (modManager.hasMod(tool, Insulating.instance()) || meta.hasEnchant(Enchantment.PROTECTION_FIRE)) {
                        pluginManager.callEvent(new ModifierFailEvent(p, tool, this, ModifierFailCause.INCOMPATIBLE_MODIFIERS, isCommand));
                        return false;
                    }
                }

                if (!this.compatibleWithAntiArrow) {
                    if (modManager.hasMod(tool, AntiArrowPlating.instance()) || meta.hasEnchant(Enchantment.PROTECTION_PROJECTILE)) {
                        pluginManager.callEvent(new ModifierFailEvent(p, tool, this, ModifierFailCause.INCOMPATIBLE_MODIFIERS, isCommand));
                        return false;
                    }
                }

                meta.addEnchant(Enchantment.PROTECTION_EXPLOSIONS, modManager.getModLevel(tool, this), true);
            }

            if (Main.getPlugin().getConfig().getBoolean("HideEnchants")) {
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            } else {
                meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            tool.setItemMeta(meta);
        }

        return true;
    }
}
