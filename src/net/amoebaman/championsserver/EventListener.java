package net.amoebaman.championsserver;

import java.util.*;

import net.amoebaman.championsserver.tasks.SeekerTask;
import net.amoebaman.championsserver.tasks.SeekerTask.Mode;
import net.amoebaman.championsserver.utils.Utils;
import net.amoebaman.kitmaster.controllers.ItemController;
import net.amoebaman.kitmaster.utilities.ParseItemException;
import net.amoebaman.utils.ParticleEffect;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.*;
import org.bukkit.event.block.BlockIgniteEvent.IgniteCause;
import org.bukkit.event.enchantment.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.hanging.*;
import org.bukkit.event.inventory.*;
import org.bukkit.event.player.*;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.*;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.potion.*;
import org.bukkit.util.Vector;

@SuppressWarnings("deprecation")
public class EventListener implements Listener{
	
	@EventHandler
	public void noBreaking(BlockBreakEvent event){
		if(event.getPlayer().getGameMode() != GameMode.CREATIVE)
			event.setCancelled(true);
	}
	
	@EventHandler
	public void noPlacing(BlockPlaceEvent event){
		if(event.getPlayer().getGameMode() != GameMode.CREATIVE)
			event.setCancelled(true);
	}
	
	@EventHandler
	public void protectHangersFromPlacing(HangingPlaceEvent event){
		if(event.getPlayer().getGameMode() != GameMode.CREATIVE)
			event.setCancelled(true);
	}
	
	@EventHandler
	public void protectHangersFromBreaking(HangingBreakByEntityEvent event){
		Entity culprit = event.getRemover();
		if(culprit instanceof Projectile && ((Projectile) culprit).getShooter() instanceof Entity)
			culprit = (Entity) ((Projectile) culprit).getShooter();
		if(culprit instanceof Player && ((Player) culprit).getGameMode() != GameMode.CREATIVE)
			event.setCancelled(true);
		if(culprit instanceof Player && ((Player) culprit).getGameMode() == GameMode.ADVENTURE){
			Event tester = new BlockBreakEvent(event.getEntity().getLocation().getBlock(), (Player) culprit);
			Bukkit.getPluginManager().callEvent(tester);
			ChampionsServer.logger().info(((Player) culprit).getName() + (((Cancellable) tester).isCancelled() ? " was not " : " was ") + " allowed to break the block in Adventure");
		}
	}
	
	@EventHandler
	public void protectFramesFromExtraction(EntityDamageByEntityEvent event){
		Entity culprit = event.getDamager();
		if(culprit instanceof Projectile && ((Projectile) culprit).getShooter() instanceof Entity)
			culprit = (Entity) ((Projectile) culprit).getShooter();
		if(event.getEntity() instanceof Hanging && culprit instanceof Player && ((Player) culprit).getGameMode() != GameMode.CREATIVE)
			event.setCancelled(true);
	}
	
	@EventHandler
	public void protectHangersFromMeddling(PlayerInteractEntityEvent event){
		if(event.getPlayer().getGameMode() != GameMode.CREATIVE && event.getRightClicked() instanceof Hanging)
			event.setCancelled(true);
	}
	
	@EventHandler
	public void infiniteEnderPearls(final ProjectileLaunchEvent event){
		if(event.getEntity().getType() == EntityType.ENDER_PEARL){
			EnderPearl pearl = (EnderPearl) event.getEntity();
			if(pearl.getShooter() != null && pearl.getShooter() instanceof Entity && ((Entity) pearl.getShooter()).getType() == EntityType.PLAYER)
				Bukkit.getScheduler().scheduleSyncDelayedTask(ChampionsServer.plugin(), new Runnable(){ public void run(){
					((Player) event.getEntity().getShooter()).setItemInHand(new ItemStack(Material.ENDER_PEARL));
				}});
		}
	}
	
	private Map<String, Long> lastTeleport = new HashMap<String, Long>();
	@EventHandler
	public void enderPearlCooldown(PlayerTeleportEvent event){
		if(event.getCause() == TeleportCause.ENDER_PEARL){
			Player player = event.getPlayer();
			if(!lastTeleport.containsKey(player.getName()))
				lastTeleport.put(player.getName(), 0L);
			if(System.currentTimeMillis() - lastTeleport.get(player.getName()) < 10 * 1000){
				event.setCancelled(true);
				player.sendMessage(ChatColor.RED + "You cannot teleport for " + (10 - ((System.currentTimeMillis() - lastTeleport.get(player.getName())) / 1000)) + " more seconds");
			}
			else
				lastTeleport.put(player.getName(), System.currentTimeMillis());
		}
	}
	
	@EventHandler
	public void bowPowerStrengthBuff(ProjectileLaunchEvent event){
		if(event.getEntityType() == EntityType.ARROW){
			Arrow arrow = (Arrow) event.getEntity();
			if(arrow.getShooter() != null && arrow.getShooter() instanceof Player){
				double multiplier = 1.3f;
				for(PotionEffect effect : ((Player) arrow.getShooter()).getActivePotionEffects()){
					if(effect.getType().equals(PotionEffectType.INCREASE_DAMAGE))
						multiplier += 0.3 * (effect.getAmplifier() + 1);
					if(effect.getType().equals(PotionEffectType.WEAKNESS))
						multiplier -= 0.3 * (effect.getAmplifier() + 1);
				}
				arrow.setVelocity(arrow.getVelocity().multiply(multiplier));
				arrow.setMetadata("velocity", new FixedMetadataValue(ChampionsServer.plugin(), multiplier));
			}
		}
	}
	
	@EventHandler
	public void fastArrowNerf(EntityDamageEvent event){
		if(!(event instanceof EntityDamageByEntityEvent))
			return;
		EntityDamageByEntityEvent eEvent = (EntityDamageByEntityEvent) event;
		if(eEvent.getDamager().getType() == EntityType.ARROW){
			Arrow arrow = (Arrow) eEvent.getDamager();
			if(arrow.hasMetadata("velocity"))
				event.setDamage((int) (event.getDamage() / arrow.getMetadata("velocity").get(0).asDouble()));
		}
	}
	
	@EventHandler
	public void infiniteWeaponArmorDurability(EntityDamageEvent event){
		if(event.getEntityType() == EntityType.PLAYER){
			final Player victim = (Player) event.getEntity();
			for(final ItemStack armor : victim.getInventory().getArmorContents())
				if(armor != null)
					Bukkit.getScheduler().scheduleSyncDelayedTask(ChampionsServer.plugin(), new Runnable(){ public void run(){
						armor.setDurability((short) 0);
						victim.updateInventory();
					}});
		}
		if(event instanceof EntityDamageByEntityEvent){
			EntityDamageByEntityEvent eEvent = (EntityDamageByEntityEvent) event;
			final Player damager;
			if(eEvent.getDamager().getType() == EntityType.PLAYER)
				damager = (Player) eEvent.getDamager();
			else if(eEvent.getDamager().getType() == EntityType.ARROW){
				Arrow arrow = (Arrow) eEvent.getDamager();
				if(arrow.getShooter() != null && arrow.getShooter() instanceof Player)
					damager = (Player) arrow.getShooter();
				else
					damager = null;
			}
			else
				damager = null;
			if(damager != null){
				Material weapon = damager.getItemInHand().getType();
				if(weapon.name().contains("SWORD") || weapon.name().contains("AXE"))
					Bukkit.getScheduler().scheduleSyncDelayedTask(ChampionsServer.plugin(), new Runnable(){ public void run(){
						damager.getItemInHand().setDurability((short) 0);
						damager.updateInventory();
					}});
			}
		}
	}
	
	@EventHandler
	public void infiniteBowDurability(ProjectileLaunchEvent event){
		if(event.getEntityType() == EntityType.ARROW){
			final Arrow arrow = (Arrow) event.getEntity();
			if(arrow.getShooter() != null && arrow.getShooter() instanceof Player)
				Bukkit.getScheduler().scheduleSyncDelayedTask(ChampionsServer.plugin(), new Runnable(){ public void run(){
					((Player) arrow.getShooter()).getItemInHand().setDurability((short) 0);
					((Player) arrow.getShooter()).updateInventory();
				}});
		}
	}
	
	@EventHandler
	public void killingCharms(PlayerDeathEvent event){
		Player victim = event.getEntity();
		Player killer = victim.getKiller();
		if(killer == null)
			return;
		int looting = 0;
		int vorpal = 0;
		for(ItemStack stack : killer.getInventory().getContents()){
			if(ChampionsServer.sameItem(stack, CustomItems.get("looting_charm")))
				looting += stack.getAmount();
			if(ChampionsServer.sameItem(stack, CustomItems.get("vorpal_charm")))
				vorpal += stack.getAmount();
		}
		if(Math.random() * 10 < looting){
			int attempts = 100;
			ItemStack drop;
			do{
				drop = victim.getInventory().getItem((int) (Math.random() * victim.getInventory().getSize()));
				attempts--;
			}
			while((drop == null || drop.getType().isEdible() || drop.getType() == Material.ARROW) && attempts > 0);
			victim.getInventory().remove(drop);
			
			ItemMeta meta = drop.getItemMeta();
			List<String> lore = meta.hasLore() ? meta.getLore() : new ArrayList<String>();
			lore.add("Taken from the corpse of " + victim.getName());
			meta.setLore(lore);
			drop.setItemMeta(meta);
			
			victim.getWorld().dropItemNaturally(victim.getLocation(), drop);
		}
		if(Math.random() * 10 < vorpal){
			ItemStack head = new ItemStack(Material.SKULL_ITEM);
			head.setDurability((short) 3);
			SkullMeta meta = (SkullMeta) head.getItemMeta();
			meta.setOwner(victim.getName());
			head.setItemMeta(meta);
			
			victim.getWorld().dropItemNaturally(victim.getLocation(), head);
		}
	}
	
	@EventHandler
	public void pickingUpItems(PlayerPickupItemEvent event){
		Player player = event.getPlayer();
		Item item = event.getItem();
		if(ChampionsServer.getPtValue(item.getItemStack()) > 0 && ChampionsServer.getPtValue(player.getInventory()) + ChampionsServer.getPtValue(item.getItemStack()) > ChampionsServer.getPtCap(player) && ChampionsServer.isPvPZone(player.getLocation())){
			player.sendMessage(ChatColor.RED + "This item would raise your PT value above the cap");
			event.setCancelled(true);
			item.setPickupDelay(100);
			
			if(player.getEnderChest().firstEmpty() >= 0){
				player.getEnderChest().addItem(item.getItemStack());
				ChampionsServer.sql.saveChest(player, player.getEnderChest());
				player.getWorld().playSound(item.getLocation(), Sound.ITEM_PICKUP, 1f, 1f);
				player.sendMessage(ChatColor.GREEN + "The item has been stored in your Ender Chest");
				item.remove();
			}
		}
	}
	
	@EventHandler
	public void droppingItems(PlayerDropItemEvent event){
		Player player = event.getPlayer();
		Item item = event.getItemDrop();
		if(ChampionsServer.getPtValue(item.getItemStack()) > 0 && ChampionsServer.getPtValue(player.getInventory()) - ChampionsServer.getPtValue(item.getItemStack()) > ChampionsServer.getPtCap(player) && ChampionsServer.isPvPZone(player.getLocation())){
			player.sendMessage(ChatColor.RED + "Dropping that item would raise your PT value above the cap");
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void killingExp(PlayerDeathEvent event){
		Player victim = event.getEntity();
		Player killer = victim.getKiller();
		int xp = ChampionsServer.getPtValue(victim.getInventory()) * 2;
		xp += TradeHandler.getAmountInInventory(CustomItems.get("boss_charm"), victim.getInventory()) * 25;
		xp += victim.getLevel() * 10;
		double ratio = 1.0 * ChampionsServer.getPtValue(victim.getInventory()) / (killer != null ? ChampionsServer.getPtValue(killer.getInventory()) : 0);
		xp *= ratio > 3 ? 3 : ratio;
		event.setDroppedExp(xp);
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "xp -" + xp + " " + victim.getName());
		victim.sendMessage(ChatColor.RED + "You lost " + ChatColor.DARK_RED + xp + "XP" + (killer != null ? ChatColor.RED + " to " + ChatColor.DARK_RED + killer.getName() : ""));
		if(killer != null)
			killer.sendMessage(ChatColor.DARK_GREEN + victim.getName() + ChatColor.GREEN + " has dropped " + ChatColor.DARK_GREEN + xp + "XP");
	}
	
	@EventHandler
	public void bossCharmsVanishOnDeath(PlayerDeathEvent event){
		Inventory inv = event.getEntity().getInventory();
		for(int i = 0; i < inv.getSize(); i++)
			if(ChampionsServer.sameItem(inv.getItem(i), CustomItems.get("boss_charm")))
				inv.setItem(i, null);
	}
	
	@EventHandler
	public void savePlayerInventories(PlayerQuitEvent event){
		ChampionsServer.sql.saveInventory(event.getPlayer());
		ChampionsServer.logger().info("Saved " + event.getPlayer().getName() + "'s inventory to the database");
	}
	
	@EventHandler
	public void kicksAreQuits(PlayerKickEvent event){
		savePlayerInventories(new PlayerQuitEvent(event.getPlayer(), "simulated"));
	}
	
	@EventHandler
	public void restrictInventories(InventoryOpenEvent event){
		Inventory inv = event.getInventory();
		switch(inv.getType()){
			case BEACON:
			case BREWING:
			case DISPENSER:
			case DROPPER:
			case FURNACE:
			case HOPPER:
			case MERCHANT:
				event.setCancelled(event.getPlayer().getGameMode() != GameMode.CREATIVE);
				return;
			default:
				break;
		}
	}
	
	@EventHandler
	public void loadPrivateStorage(InventoryOpenEvent event){
		Player player = (Player) event.getPlayer();
		Inventory inv = event.getInventory();
		if(inv.getType() == InventoryType.ENDER_CHEST){
			Inventory stored = ChampionsServer.sql.loadChest(player);
			for(int i = 0; i < stored.getSize() && i < inv.getSize(); i++)
				inv.setItem(i, stored.getItem(i));
		}
	}
	
	@EventHandler
	public void savePrivateStorage(InventoryCloseEvent event){
		Player player = (Player) event.getPlayer();
		Inventory inv = event.getInventory();
		if(inv.getType() == InventoryType.ENDER_CHEST)
			ChampionsServer.sql.saveChest(player, inv);
	}
	
	@EventHandler
	public void footprints(PlayerMoveEvent event){
		Player player = event.getPlayer();
		boolean moved = event.getFrom().length() != event.getTo().length();
		if(player.isOnGround() && moved)
			ParticleEffect.TOWN_AURA.play(event.getFrom().clone(), (float) (Math.random() - 0.5) * 0.5f, 0, (float) (Math.random() - 0.5) * 0.5f, 0, player.isSneaking() ? 1 : 10);
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void enterAndExitSafeRegions(final PlayerMoveEvent event){
		final Player player = event.getPlayer();
		if(ChampionsServer.isPvPZone(event.getTo()) && !ChampionsServer.isPvPZone(event.getFrom()) && player.getGameMode() != GameMode.CREATIVE){
			final int pt = ChampionsServer.getPtValue(player.getInventory());
			final int cap = ChampionsServer.getPtCap(player);
			if(pt > cap){
				Bukkit.getScheduler().scheduleSyncDelayedTask(ChampionsServer.plugin(), new Runnable(){ public void run(){
					if(ChampionsServer.isPvPZone(player.getLocation())){
						player.teleport(event.getFrom());
						player.sendMessage(ChatColor.RED + "You can't enter the PvP area because your inventory's value of " + pt + "PT exceeds the maximum of " + cap + "PT");
					}
				}}, 20L);
			}
		}
	}
	
	@EventHandler(priority=EventPriority.LOWEST)
	public void enterAndExitSafeRegions(PlayerTeleportEvent event){
		Player player = event.getPlayer();
		if(ChampionsServer.isPvPZone(event.getTo()) && player.getGameMode() != GameMode.CREATIVE){
			int pt = ChampionsServer.getPtValue(player.getInventory());
			int cap = ChampionsServer.getPtCap(player);
			if(pt > cap){
				event.setCancelled(true);
				event.setTo(event.getFrom());
				player.sendMessage(ChatColor.RED + "You can't enter the PvP area because your inventory's value of " + pt + "PT exceeds the maximum of " + cap + "PT");
			}
		}
	}
	
	@EventHandler
	public void firstTimePlayers(PlayerJoinEvent event){
		final Player player = event.getPlayer();
		if(!player.hasPlayedBefore()){
			Bukkit.getScheduler().scheduleSyncDelayedTask(ChampionsServer.plugin(), new Runnable(){ public void run(){
				Bukkit.broadcastMessage(ChatColor.GOLD + "Everybody please give " + ChatColor.DARK_RED + player.getName() + ChatColor.GOLD + " a huge welcome to the server");
				Bukkit.broadcastMessage(ChatColor.GOLD + "In total we've had " + ChatColor.DARK_RED + Bukkit.getOfflinePlayers().length + ChatColor.GOLD + " unique players visit this server");
			}});
			Bukkit.getScheduler().scheduleSyncDelayedTask(ChampionsServer.plugin(), new Runnable(){ public void run(){
				player.sendMessage(ChatColor.GREEN + "To help you out as a new player, we've given you some basic gear to start off with");
				player.sendMessage(ChatColor.GREEN + "Go wander around spawn city to learn more about the server");
				player.sendMessage(ChatColor.GREEN + "All the answers you need are here");
				PlayerInventory inv = player.getInventory();
				try {
					inv.setHelmet(ItemController.parseItem("Leather_Helmet:0x00FF00:1"));
					inv.setChestplate(ItemController.parseItem("Leather_Chestplate:0x00FF00:1"));
					inv.setLeggings(ItemController.parseItem("Leather_Leggings:0x00FF00:1"));
					inv.setBoots(ItemController.parseItem("Leather_Boots:0x00FF00:1"));
					inv.addItem(ItemController.parseItem("Iron_Sword:1"));
					inv.addItem(ItemController.parseItem("Bow:1"));
					inv.addItem(ItemController.parseItem("Arrow:64"));
				}
				catch (ParseItemException e) { e.printStackTrace(); }
			}}, 60);
		}
	}
	
	@EventHandler
	public void givePlayersInventoriesOnJoin(PlayerJoinEvent event){
		ChampionsServer.sql.loadInventory(event.getPlayer());
	}
	
	@EventHandler
	public void spawnMobsWithNames(CreatureSpawnEvent event){
		if(event.getSpawnReason() == SpawnReason.SPAWNER_EGG && event.getEntity().getCustomName() != null){
			event.getEntity().setCustomNameVisible(true);
			event.getEntity().setRemoveWhenFarAway(false);
		}
	}
	
	@EventHandler
	public void namedMobsDontAttack(EntityTargetLivingEntityEvent event){
		if(event.getEntity() instanceof LivingEntity && ((LivingEntity) event.getEntity()).getCustomName() != null)
			event.setCancelled(true);
	}
	
	@EventHandler
	public void onlyLegalEnchants(EnchantItemEvent event){
		for(Enchantment enc : event.getEnchantsToAdd().keySet())
			if(!ChampionsServer.enchants.contains(enc))
				event.setCancelled(true);
	}
	
	@EventHandler
	public void tagSpawnerMobs(CreatureSpawnEvent event){
		if(event.getSpawnReason() == SpawnReason.SPAWNER || event.getSpawnReason() == SpawnReason.SLIME_SPLIT)
			event.getEntity().setMetadata("lessXP", new FixedMetadataValue(ChampionsServer.plugin(), true));
	}
	
	@EventHandler
	public void halfXPFromSpawnerMobs(EntityDeathEvent event){
		if(event.getEntity().hasMetadata("lessXP"))
			event.setDroppedExp(event.getDroppedExp() / 3);
	}
	
	@EventHandler
	public void denyFireStarters(BlockIgniteEvent event){
		if(event.getCause() != IgniteCause.FLINT_AND_STEEL && event.getCause() != IgniteCause.EXPLOSION)
			event.setCancelled(true);
	}
	
	@EventHandler
	public void infiniteDispensers(BlockDispenseEvent event){
		BlockState state = event.getBlock().getState();
		if(state instanceof Dispenser || state instanceof Dropper){
			((InventoryHolder) state).getInventory().addItem(event.getItem());
		}
	}
	
	@EventHandler(priority=EventPriority.MONITOR)
	public void noEyesOfEnder(PlayerInteractEvent event){
		if(event.getItem() != null && event.getItem().getType() == Material.EYE_OF_ENDER)
			event.setCancelled(true);
	}
	
	@EventHandler
	public void skokkragAbility(PlayerInteractEvent event){
		final Player player = event.getPlayer();
		if(event.getAction() == Action.RIGHT_CLICK_AIR && ChampionsServer.sameItem(event.getItem(), CustomItems.get("skokkrag")) && player.getLevel() > 0){
			final Location target = player.getTargetBlock(ChampionsServer.transparent, 150).getLocation();
			for(int i = 0; i < 5; i++)
				Bukkit.getScheduler().scheduleSyncDelayedTask(ChampionsServer.plugin(), new Runnable(){ public void run(){
					Location spread = target.clone().add((Math.random() - 0.5) * 10, 0, (Math.random() - 0.5) * 10);
					spread.setY(spread.getWorld().getHighestBlockYAt(spread));
					player.getWorld().strikeLightning(spread);
				}},(long) ((i * 5) + ((Math.random() - 0.5) * 4)));
			for(Entity e : target.getChunk().getEntities())
				if(e instanceof LivingEntity && e.getLocation().distance(target) < 10)
					((LivingEntity) e).damage(0, player);
			player.setLevel(player.getLevel() - 1);
		}
	}
	
	@EventHandler
	public void skietvlamAbility(PlayerInteractEvent event){
		final Player player = event.getPlayer();
		if(event.getAction() == Action.RIGHT_CLICK_AIR && ChampionsServer.sameItem(event.getItem(), CustomItems.get("skietvlam")) && player.getLevel() > 0){
			for(int i = 0; i < 10; i++)
				Bukkit.getScheduler().scheduleSyncDelayedTask(ChampionsServer.plugin(), new Runnable(){ public void run(){
					player.launchProjectile(SmallFireball.class);
					player.getWorld().playEffect(player.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
					player.getWorld().playEffect(player.getLocation(), Effect.BLAZE_SHOOT, 0);
				}},(long) ((i * 5)));
			player.setLevel(player.getLevel() - 1);
		}
	}
	
	@EventHandler
	public void lugvlaagAbility(PlayerInteractEvent event){
		final Player player = event.getPlayer();
		if(event.getAction() == Action.RIGHT_CLICK_AIR && ChampionsServer.sameItem(event.getItem(), CustomItems.get("lugvlaag")) && player.getLevel() > 0){
			final List<Block> los = player.getLineOfSight(ChampionsServer.transparent, 150);
			final List<Entity> shoved = new ArrayList<Entity>();
			for(int i = 0; i < los.size(); i++){
				final Location loc = los.get(i).getLocation();
				Bukkit.getScheduler().scheduleSyncDelayedTask(ChampionsServer.plugin(), new Runnable(){ public void run(){;
				for(Entity e : loc.getChunk().getEntities())
					if(e.getLocation().distance(loc) < 5 && !shoved.contains(e) && !e.equals(player)){
						if(e instanceof LivingEntity)
							((LivingEntity) e).damage(0, player);
						Vector shove = e.getLocation().subtract(loc).toVector();
						shove.multiply(5.0 / shove.length());
						shove.setY(1);
						e.setVelocity(shove);
						shoved.add(e);
					}
				for(int i = 0; i < 5; i++)
					loc.getWorld().playEffect(loc, Effect.SMOKE, i);
				loc.getWorld().playSound(loc, Sound.HORSE_BREATHE, 1, 0.5f);
				}}, i);
			}
			player.setLevel(player.getLevel() - 1);
		}
	}
	
	/*
	private long aardegrafCooldown = 0;
	@EventHandler
	public void aardegrafAbility(PlayerInteractEvent event){
		final Player player = event.getPlayer();
		if(event.getAction() == Action.RIGHT_CLICK_AIR && FFAMaster.sameItem(event.getItem(), CustomItems.get("aardegraf")) && player.getLevel() > 0 && System.currentTimeMillis() - aardegrafCooldown >= 2000){
			Block target = player.getTargetBlock(FFAMaster.transparent, 150);
			for(int x = -2; x <= 2; x++)
				for(int y = -1; y <= 1; y++)
					for(int z = -2; z <= 2; z++){
						final Block other = target.getRelative(x, y, z);
						final BlockState state = other.getState();
						switch(other.getType()){
							case GRASS:
							case DIRT:
							case STONE:
							case COBBLESTONE:
							case GRAVEL:
							case SAND:
							case SANDSTONE:
								Bukkit.getScheduler().scheduleSyncDelayedTask(FFAMaster.plugin(), new Runnable(){ public void run(){
									other.setTypeIdAndData(0, (byte) 0, false);
									other.getWorld().playSound(other.getLocation(), Sound.DIG_GRAVEL, 1, 0.5f);
								}}, Math.abs(x)-y*2+Math.abs(z));
								Bukkit.getScheduler().scheduleSyncDelayedTask(FFAMaster.plugin(), new Runnable(){ public void run(){
									state.update(true, false);
									other.getWorld().playSound(other.getLocation(), Sound.DIG_STONE, 1, 0.5f);
								}}, 30-(Math.abs(x)-y*2+Math.abs(z)));
							default:
						}
					}
			
			for(Entity e : target.getChunk().getEntities())
				if(e instanceof LivingEntity && e.getLocation().distance(target.getLocation()) < 3)
					((LivingEntity) e).damage(0, player);
			player.setLevel(player.getLevel() - 1);
			aardegrafCooldown = System.currentTimeMillis();
		}
	}
	*/
	
	@EventHandler
	public void binocularsAbility(PlayerInteractEvent event){
		if(event.getAction() == Action.RIGHT_CLICK_AIR && ChampionsServer.sameItem(event.getItem(), CustomItems.get("binoculars"))){
			Player player = event.getPlayer();
			
			Map<EntityType, Integer> count = new HashMap<EntityType, Integer>();
			for(EntityType type : EntityType.values())
				count.put(type, 0);
			List<LivingEntity> spotted = new ArrayList<LivingEntity>();
			
			for(Block block : player.getLineOfSight(ChampionsServer.transparent, 50 + (TradeHandler.getAmountInInventory(CustomItems.get("clairvoyance_charm"), player.getInventory()) * 10)))
				for(Entity e : block.getChunk().getEntities())
					if(e instanceof LivingEntity && e.getLocation().distance(block.getLocation()) < 10 && !spotted.contains(e) && !e.equals(player)){
						spotted.add((LivingEntity) e);
						count.put(e.getType(), count.get(e.getType()) + 1);
					}
			
			String countMessage = " Spotted";
			boolean first = true;
			for(EntityType type : count.keySet())
				if(count.get(type) > 0){
					countMessage += (first ? " " : ", ") + count.get(type) + " " + (type == EntityType.PLAYER ? "player" : type.getName().toLowerCase().replace('_', ' ')) + (count.get(type) > 1 ? "s" : "");
					first = false;
				}
			if(!first)
				player.sendMessage(countMessage);
			
			boolean alt = true;
			for(LivingEntity e : spotted){
				String pre = " " + (alt ? ChatColor.GRAY : ChatColor.WHITE);
				if(e instanceof Player){
					
					Player target = (Player) e;
					player.sendMessage(pre + target.getName() + " is carrying " + ChampionsServer.getPtValue(target.getInventory()) + "PT worth of gear");
					
					List<String> lines = new ArrayList<String>();
					int clouding = 0;
					for(ItemStack stack : target.getInventory().getContents())
						if(stack != null && ChampionsServer.isInteresting(stack)){
							if(ChampionsServer.sameItem(stack, CustomItems.get("clouding_charm")))
								clouding += stack.getAmount();
							if(stack != null)
								lines.add(pre + " - " + Utils.friendlyItemString(stack));
						}
					for(ItemStack armor : target.getInventory().getArmorContents())
						if(armor != null && armor.getType() != Material.AIR)
							lines.add(pre + " - " + Utils.friendlyItemString(armor));
					
					Random rand = new Random(9001);
					if(clouding > 0){
						List<String> temp = new ArrayList<String>();
						temp.addAll(lines);
						lines.clear();
						for(String line : temp){
							String clouded = "";
							for(char c : line.toCharArray())
								if(rand.nextInt(10) > clouding)
									clouded += c;
								else
									clouded += " ";
							if(!line.replace(" ", "").isEmpty())
								lines.add(clouded);
						}
					}
					player.sendMessage(lines.toArray(new String[0]));
					alt = !alt;
					
				}
				else{
					
					Inventory inv = Utils.entityGearToInv(e.getEquipment());
					if(inv.firstEmpty() != 0){
						player.sendMessage(pre + "A " + e.getType().name().toLowerCase().replace('_', ' ') + " is carring " + ChampionsServer.getPtValue(inv) + "PT worth of gear");
						for(ItemStack stack : inv.getContents())
							if(stack != null && ChampionsServer.isInteresting(stack))
								player.sendMessage(pre + " - " + Utils.friendlyItemString(stack));
						alt = !alt;
					}
					
				}
			}	
		}
	}
	
	@EventHandler
	public void seekerAbility(PlayerInteractEvent event){
		if(event.getAction() == Action.RIGHT_CLICK_AIR && ChampionsServer.sameItem(event.getItem(), CustomItems.get("seeker"))){
			Player player = event.getPlayer();
			SeekerTask.MODES.put(player, Mode.values()[(SeekerTask.MODES.get(player).ordinal() + 1) % Mode.values().length]);
			switch(SeekerTask.MODES.get(player)){
				case BOSSES:
					player.sendMessage(ChatColor.DARK_PURPLE + " Seeker mode: " + ChatColor.LIGHT_PURPLE + "boss charms");
					break;
				case EQUAL:
					player.sendMessage(ChatColor.DARK_PURPLE + " Seeker mode: " + ChatColor.LIGHT_PURPLE + "equal PT score");
					break;
				case LEGENDS:
					player.sendMessage(ChatColor.DARK_PURPLE + " Seeker mode: " + ChatColor.LIGHT_PURPLE + "legendary weapons");
					break;
				case SHARDS:
					player.sendMessage(ChatColor.DARK_PURPLE + " Seeker mode: " + ChatColor.LIGHT_PURPLE + "Wellspring shards/charm");
					break;
				case STRONGER:
					player.sendMessage(ChatColor.DARK_PURPLE + " Seeker mode: " + ChatColor.LIGHT_PURPLE + "greater PT score");
					break;
			}
		}
	}
}
