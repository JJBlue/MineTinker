package de.flo56958.minetinker.listeners;

import de.flo56958.minetinker.MineTinker;
import de.flo56958.minetinker.data.Lists;
import de.flo56958.minetinker.data.ToolType;
import de.flo56958.minetinker.events.MTBlockBreakEvent;
import de.flo56958.minetinker.events.MTPlayerInteractEvent;
import de.flo56958.minetinker.modifiers.ModManager;
import de.flo56958.minetinker.modifiers.Modifier;
import de.flo56958.minetinker.modifiers.types.Power;
import de.flo56958.minetinker.modifiers.types.SilkTouch;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;

import java.util.List;

public class BlockListener implements Listener {

	private static final ModManager modManager = ModManager.instance();

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public static void onAxeUse(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		if (Lists.WORLDS.contains(player.getWorld().getName())) {
			return;
		}

		if (!(player.getGameMode().equals(GameMode.SURVIVAL) || player.getGameMode().equals(GameMode.ADVENTURE))) {
			return;
		}

		ItemStack tool = player.getInventory().getItemInMainHand();

		if (!ToolType.AXE.contains(tool.getType())) {
			return;
		}

		if (!modManager.isToolViable(tool)) {
			return;
		}

		boolean apply = false;

		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
			if (Lists.getWoodLogs().contains(event.getClickedBlock().getType()))
				apply = true;
			else if (Lists.getWoodWood().contains(event.getClickedBlock().getType()))
				apply = true;
		}

		if (!apply) {
			return;
		}

		if (!modManager.durabilityCheck(event, player, tool)) {
			return;
		}

		modManager.addExp(player, tool, MineTinker.getPlugin().getConfig().getInt("ExpPerBlockBreak"));

		MTPlayerInteractEvent interactEvent = new MTPlayerInteractEvent(tool, event);
		Bukkit.getPluginManager().callEvent(interactEvent);
	}

	//To cancel event if Tool would be broken so other plugins can react faster to MineTinker (e.g. PyroMining)
	//onBlockBreak() has priority highest as it needs to wait on WorldGuard and other plugins to cancel event if necessary
	//TODO: Replace if Issue #111 is implemented
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onBlockBreak_DurabilityCheck(BlockBreakEvent event) {
		Player player = event.getPlayer();
		ItemStack tool = player.getInventory().getItemInMainHand();
		if (modManager.isToolViable(tool)) {
			modManager.durabilityCheck(event, player, tool);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		ItemStack tool = player.getInventory().getItemInMainHand();

		if (Lists.WORLDS.contains(player.getWorld().getName())) {
			return;
		}

		if (event.getBlock().getType().getHardness() == 0 && !(tool.getType() == Material.SHEARS
				|| ToolType.HOE.contains(tool.getType()))) {
			return;
		}

		if (!modManager.isToolViable(tool)) {
			return;
		}

		FileConfiguration config = MineTinker.getPlugin().getConfig();

		//--------------------------------------EXP-CALCULATIONS--------------------------------------------
		if (player.getGameMode() != GameMode.CREATIVE) {
			long cooldown = MineTinker.getPlugin().getConfig().getLong("BlockExpCooldownInSeconds", 60) * 1000;
			boolean eligible = true;
			if (cooldown > 0) {
				List<MetadataValue> blockPlaced = event.getBlock().getMetadata("blockPlaced");
				for(MetadataValue val : blockPlaced) {
					if (val == null) continue;
					if (!MineTinker.getPlugin().equals(val.getOwningPlugin())) continue; //Not MTs value

					Object value = val.value();
					if (value instanceof Long) {
						long time = (long) value;
						eligible = System.currentTimeMillis() - cooldown > time;
						break;
					}
				}
			}

			if (eligible) {
				int expAmount = config.getInt("ExpPerBlockBreak");
				if (!(!config.getBoolean("ExtraExpPerBlock.ApplicableToSilkTouch")
						&& modManager.hasMod(tool, SilkTouch.instance()))) {
					expAmount += config.getInt("ExtraExpPerBlock." + event.getBlock().getType().toString());
					//adds 0 if not in found in config (negative values are also fine)
				}

				modManager.addExp(player, tool, expAmount);
			}
		}

		//-------------------------------------------POWERCHECK---------------------------------------------
		if (Power.HAS_POWER.get(player).get() && !ToolType.PICKAXE.contains(tool.getType())
				&& event.getBlock().getDrops(tool).isEmpty()
				&& event.getBlock().getType() != Material.NETHER_WART) { //Necessary for EasyHarvest NetherWard-Break

			event.setCancelled(true);
			return;
		}

		MTBlockBreakEvent breakEvent = new MTBlockBreakEvent(tool, event);
		Bukkit.getPluginManager().callEvent(breakEvent); //Event-Trigger for Modifiers
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onClick(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		ItemStack norm = null;
		if (event.getHand() == EquipmentSlot.HAND) {
			norm = player.getInventory().getItemInMainHand();
		} else if (event.getHand() == EquipmentSlot.OFF_HAND) {
			norm = player.getInventory().getItemInOffHand();
		}

		if (norm == null) return;

		if (event.getAction() == Action.RIGHT_CLICK_AIR) {
			if (modManager.isModifierItem(norm)) {
				event.setCancelled(true);
			}
		} else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Block block = event.getClickedBlock();

			if (block == null) {
				return;
			}
			if (!player.isSneaking()) {
				Material type = block.getType();

				if (type == Material.ANVIL || type == Material.CRAFTING_TABLE
						|| type == Material.CHEST || type == Material.ENDER_CHEST
						|| type == Material.DROPPER || type == Material.HOPPER
						|| type == Material.DISPENSER || type == Material.TRAPPED_CHEST
						|| type == Material.FURNACE || type == Material.ENCHANTING_TABLE) {

					return;
				}
			}

			if (modManager.isModifierItem(norm)) {
				event.setCancelled(true);
				return;
			}

			if (block.getType() == Material.getMaterial(MineTinker.getPlugin().getConfig().getString("BlockToEnchantModifiers", Material.BOOKSHELF.name()))) {
				ItemStack item = player.getInventory().getItemInMainHand();

				for (Modifier m : modManager.getAllMods()) {
					if (m.getModItem().getType().equals(item.getType())) {
						if (!m.isEnchantable()) continue;
						m.enchantItem(player);
						event.setCancelled(true);
						break;
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onHoeUse(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		if (Lists.WORLDS.contains(player.getWorld().getName())) {
			return;
		}

		if (!(player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {
			return;
		}

		ItemStack tool = player.getInventory().getItemInMainHand();

		if (!ToolType.HOE.contains(tool.getType())) {
			return;
		}

		if (!modManager.isToolViable(tool)) {
			return;
		}

		Block block = event.getClickedBlock();

		boolean apply = false;

		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && block != null) {
			if (block.getType() == Material.GRASS_BLOCK || block.getType() == Material.DIRT)
				apply = true;

			Block b = player.getWorld().getBlockAt(block.getLocation().add(0, 1, 0));
			if (b.getType() != Material.AIR && b.getType() != Material.CAVE_AIR) //Case Block is on top of clicked Block -> No Soil Tilt -> no Exp
				apply = false;
		}

		if (!apply) {
			return;
		}

		if (!modManager.durabilityCheck(event, player, tool)) {
			return;
		}

		modManager.addExp(player, tool, MineTinker.getPlugin().getConfig().getInt("ExpPerBlockBreak"));

		MTPlayerInteractEvent interactEvent = new MTPlayerInteractEvent(tool, event);
		Bukkit.getPluginManager().callEvent(interactEvent);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onShovelUse(PlayerInteractEvent event) {
		Player player = event.getPlayer();

		if (Lists.WORLDS.contains(player.getWorld().getName())) {
			return;
		}

		if (!(player.getGameMode() == GameMode.SURVIVAL || player.getGameMode() == GameMode.ADVENTURE)) {
			return;
		}

		ItemStack tool = player.getInventory().getItemInMainHand();

		if (!ToolType.SHOVEL.contains(tool.getType())) {
			return;
		}

		if (!modManager.isToolViable(tool)) {
			return;
		}

		boolean apply = false;

		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {
			if (event.getClickedBlock().getType() == Material.GRASS_BLOCK)
				apply = true;

			Block b = player.getWorld().getBlockAt(event.getClickedBlock().getLocation().add(0, 1, 0));
			if (b.getType() != Material.AIR && b.getType() != Material.CAVE_AIR)
				//Case Block is on top of clicked Block -> No Path created -> no Exp
				apply = false;
		}

		if (!apply) {
			return;
		}

		if (!modManager.durabilityCheck(event, player, tool)) {
			return;
		}

		modManager.addExp(player, tool, MineTinker.getPlugin().getConfig().getInt("ExpPerBlockBreak"));

		MTPlayerInteractEvent interactEvent = new MTPlayerInteractEvent(tool, event);
		Bukkit.getPluginManager().callEvent(interactEvent);
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		block.setMetadata("blockPlaced", new FixedMetadataValue(MineTinker.getPlugin(), System.currentTimeMillis()));
	}
}