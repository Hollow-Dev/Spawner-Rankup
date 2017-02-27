package me.jake.spawners;

import com.google.common.collect.Lists;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class Main
  extends JavaPlugin
  implements Listener
{
  private HashMap<Location, Integer> spawners;
  private ItemStack tier1;
  private ItemStack tier2;
  private ItemStack tier3;
  private HashMap<UUID, Location> clickedSpawner;
  
  public void onEnable()
  {
    this.spawners = new HashMap();
    this.clickedSpawner = new HashMap();
    for (String key : getConfig().getKeys(false))
    {
      Location loc = new Location(Bukkit.getWorld(getConfig().getString(key + ".World")), 
        getConfig().getDouble(key + ".X"), getConfig().getDouble(key + ".Y"), 
        getConfig().getDouble(key + ".Z"));
      
      int tier = getConfig().getInt(key + ".Tier");
      if ((loc.getBlock() != null) && (loc.getBlock().getType() == Material.MOB_SPAWNER))
      {
        this.spawners.put(loc, Integer.valueOf(tier));
        
        CreatureSpawner spawner = (CreatureSpawner)loc.getBlock().getState();
        int delay = 1200;
        switch (tier)
        {
        case 1: 
          delay = 400;
          break;
        case 2: 
          delay = 100;
          break;
        case 3: 
          delay = 25;
          break;
        }
        if (spawner.getDelay() > delay)
        {
          spawner.setDelay(delay);
          spawner.update();
        }
      }
    }
    this.tier1 = new ItemStack(Material.MOB_SPAWNER);
    ItemMeta tier1Meta = this.tier1.getItemMeta();
    tier1Meta.setDisplayName(ChatColor.GRAY + "Tier 1");
    tier1Meta.setLore(Lists.newArrayList(new String[] { ChatColor.GRAY + "Cost: " + ChatColor.YELLOW + "30" }));
    this.tier1.setItemMeta(tier1Meta);
    
    this.tier2 = new ItemStack(Material.MOB_SPAWNER);
    ItemMeta tier2Meta = this.tier2.getItemMeta();
    tier2Meta.setDisplayName(ChatColor.AQUA + "Tier 2");
    tier2Meta.setLore(Lists.newArrayList(new String[] { ChatColor.GRAY + "Cost: " + ChatColor.GREEN + "60" }));
    this.tier2.setItemMeta(tier2Meta);
    
    this.tier3 = new ItemStack(Material.MOB_SPAWNER);
    ItemMeta tier3Meta = this.tier3.getItemMeta();
    tier3Meta.setDisplayName(ChatColor.DARK_AQUA + "Tier 3");
    tier3Meta.setLore(Lists.newArrayList(new String[] { ChatColor.GRAY + "Cost: " + ChatColor.RED + "120" }));
    this.tier3.setItemMeta(tier3Meta);
    
    getServer().getPluginManager().registerEvents(this, this);
  }
  
  public void onDisable()
  {
    int i = 0;
    for (Map.Entry<Location, Integer> spawner : this.spawners.entrySet())
    {
      getConfig().set(i + ".Tier", spawner.getValue());
      getConfig().set(i + ".X", Double.valueOf(((Location)spawner.getKey()).getX()));
      getConfig().set(i + ".Y", Double.valueOf(((Location)spawner.getKey()).getY()));
      getConfig().set(i + ".Z", Double.valueOf(((Location)spawner.getKey()).getZ()));
      getConfig().set(i + ".World", ((Location)spawner.getKey()).getWorld().getName());
      
      i++;
    }
    saveConfig();
  }
  
  @EventHandler
  public void onSpawnerClick(PlayerInteractEvent e)
  {
    if ((e.getAction() == Action.RIGHT_CLICK_BLOCK) && (e.getClickedBlock().getType() == Material.MOB_SPAWNER)) {
      openMenu(e.getPlayer(), e.getClickedBlock().getLocation());
    }
  }
  
  private void openMenu(Player player, Location loc)
  {
    if ((loc.getBlock() != null) && (loc.getBlock().getType() == Material.MOB_SPAWNER))
    {
      Inventory inv = Bukkit.createInventory(null, 9, ChatColor.GOLD + "Spawner tier");
      if (!this.spawners.containsKey(loc)) {
        this.spawners.put(loc, Integer.valueOf(0));
      }
      int tier = ((Integer)this.spawners.get(loc)).intValue();
      
      inv.setItem(2, this.tier1.clone());
      inv.setItem(4, this.tier2.clone());
      inv.setItem(6, this.tier3.clone());
      switch (tier)
      {
      case 3: 
        inv.getItem(6).addUnsafeEnchantment(Enchantment.DURABILITY, 3);
      case 2: 
        inv.getItem(4).addUnsafeEnchantment(Enchantment.DURABILITY, 2);
      case 1: 
        inv.getItem(2).addUnsafeEnchantment(Enchantment.DURABILITY, 1);
      }
      this.clickedSpawner.put(player.getUniqueId(), loc);
      
      player.openInventory(inv);
    }
  }
  
  @EventHandler
  public void inventoryClickEvent(InventoryClickEvent e)
  {
    if (e.getView().getTopInventory().getName().equals(ChatColor.GOLD + "Spawner tier"))
    {
      Player player = (Player)e.getWhoClicked();
      e.setCancelled(true);
      if ((e.getClickedInventory() != null) && (e.getClickedInventory().equals(e.getView().getTopInventory())) && 
        (e.getCurrentItem() != null) && (e.getCurrentItem().getType() != Material.AIR)) {
        if (!e.getCurrentItem().containsEnchantment(Enchantment.DURABILITY))
        {
          int tier = 0;
          if (this.spawners.containsKey(this.clickedSpawner.get(e.getWhoClicked().getUniqueId()))) {
            tier = ((Integer)this.spawners.get(this.clickedSpawner.get(e.getWhoClicked().getUniqueId()))).intValue();
          }
          if (((!e.getCurrentItem().equals(this.tier1)) || (tier != 0)) && 
            ((!e.getCurrentItem().equals(this.tier2)) || (tier != 1)) && (
            (!e.getCurrentItem().equals(this.tier3)) || (tier != 2)))
          {
            e.getWhoClicked().sendMessage(ChatColor.RED + "You must buy the other tiers first");
            return;
          }
          int cost = Integer.parseInt(ChatColor.stripColor((String)e.getCurrentItem().getItemMeta().getLore().get(0))
            .replace("Cost: ", ""));
          if (player.getLevel() >= cost)
          {
            int delayMax = 1200;
            switch (tier)
            {
            case 1: 
              delayMax = 400;
              break;
            case 2: 
              delayMax = 100;
              break;
            case 3: 
              delayMax = 25;
              break;
            }
            CreatureSpawner spawner = (CreatureSpawner)((Location)this.clickedSpawner.get(player.getUniqueId())).getBlock()
              .getState();
            
            double percentage = spawner.getDelay() / delayMax;
            
            tier++;
            int delay = 1200;
            switch (tier)
            {
            case 1: 
              delay = 400;
              break;
            case 2: 
              delay = 100;
              break;
            case 3: 
              delay = 25;
              break;
            }
            spawner.setDelay((int)Math.ceil(delay * percentage));
            spawner.update();
            
            this.spawners.put((Location)this.clickedSpawner.get(player.getUniqueId()), Integer.valueOf(tier));
            player.setLevel(player.getLevel() - cost);
            switch (tier)
            {
            case 3: 
              e.getView().getTopInventory().getItem(6).addUnsafeEnchantment(Enchantment.DURABILITY, 3);
            case 2: 
              e.getView().getTopInventory().getItem(4).addUnsafeEnchantment(Enchantment.DURABILITY, 2);
            case 1: 
              e.getView().getTopInventory().getItem(2).addUnsafeEnchantment(Enchantment.DURABILITY, 1);
            }
          }
          else
          {
            player.sendMessage(ChatColor.RED + "You do not have enough levels to buy that tier");
          }
        }
        else
        {
          e.getWhoClicked().sendMessage(ChatColor.RED + "You already own that tier");
        }
      }
    }
  }
  
  @EventHandler
  public void inventoryCloseEvent(InventoryCloseEvent e)
  {
    if (this.clickedSpawner.containsKey(e.getPlayer().getUniqueId())) {
      this.clickedSpawner.remove(e.getPlayer().getUniqueId());
    }
  }
  
  @EventHandler
  public void onSpawnerPlace(BlockPlaceEvent e)
  {
    if (e.getBlock().getType() == Material.MOB_SPAWNER)
    {
      this.spawners.put(e.getBlock().getLocation(), Integer.valueOf(0));
      
      final Block block = e.getBlock();
      new BukkitRunnable()
      {
        public void run()
        {
          CreatureSpawner spawner = (CreatureSpawner)block.getState();
          spawner.setDelay(1199);
          spawner.update();
        }
      }.runTaskLater(this, 1L);
    }
  }
  
  @EventHandler
  public void onSpawnerBreak(BlockBreakEvent e)
  {
    if (this.spawners.containsKey(e.getBlock().getLocation())) {
      this.spawners.remove(e.getBlock().getLocation());
    }
  }
  
  @EventHandler
  public void mobSpawnEvent(SpawnerSpawnEvent e)
  {
    e.setCancelled(true);
    
    final CreatureSpawner spawner = e.getSpawner();
    
    final EntityType type = e.getEntityType();
    
    final Location loc = e.getLocation();
    
    new BukkitRunnable()
    {
      public void run()
      {
        loc.getWorld().spawnEntity(loc, type);
        
        int tier = 0;
        if (Main.this.spawners.containsKey(spawner.getLocation())) {
          tier = ((Integer)Main.this.spawners.get(spawner.getLocation())).intValue();
        }
        switch (tier)
        {
        case 0: 
          spawner.setDelay(1199);
          break;
        case 1: 
          spawner.setDelay(399);
          break;
        case 2: 
          spawner.setDelay(99);
          break;
        case 3: 
          spawner.setDelay(24);
          break;
        }
        spawner.update();
      }
    }.runTaskLater(this, 1L);
  }
}
