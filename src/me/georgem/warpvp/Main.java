package me.georgem.warpvp;

import java.util.HashSet;
import java.util.Iterator;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

@SuppressWarnings("deprecation")
public class Main extends JavaPlugin implements Listener {
	Logger logger;
	
/////////////
//Variables//
/////////////
	//Shortcuts
	public String prefix = ChatColor.GOLD + "[" + ChatColor.RED + "WarPvP" + ChatColor.GOLD + "] "+ ChatColor.RESET;
	public String msgNoPerm = prefix + ChatColor.RED + "You don't have access to this command!";
	public String setSpawnUsage = prefix + "Usage: /warpvp setspawn <blue/red/lobby>";
	
	//Scoreboard Setup
	public Scoreboard sb;
	public HashSet<Player> playerlist;
	public Iterator<Player> it;
	public Objective ob;
	public Team blueteam;
	public Team redteam;
	
	//Game Data Setup
	public boolean teambalance = true;
	public boolean gamestate = false;
	public Location bluespawn; //team blue
	public Location redspawn; //team red
	public Location lobbyspawn;
	
	//Creating the equipment
	public ItemStack pistol = new ItemStack(Material.STICK);
	public ItemStack smg = new ItemStack(Material.BLAZE_ROD);
	public ItemStack sniper = new ItemStack(Material.FISHING_ROD);
	
	//Editing equipment's values
	public ItemMeta metapistol = pistol.getItemMeta();
	public ItemMeta metasmg = smg.getItemMeta();
	public ItemMeta metasniper = sniper.getItemMeta();
	
	//Editing equipment's ammunition
	public Arrow pistolArrow;
	public Arrow smgArrow;
	
	@Override
	public void onEnable(){
		logger = getLogger();
		
		//Setting up playerlist
		playerlist = new HashSet<Player>();
		
		//Setting equipment's name
		metapistol.setDisplayName("Pistol");
		metasmg.setDisplayName("SMG");
		metasniper.setDisplayName("Sniper "+ChatColor.RED+"(Coming Soon)");
		pistol.setItemMeta(metapistol);
		smg.setItemMeta(metasmg);
		sniper.setItemMeta(metasniper);
		
		//Plugin management
		PluginManager manager = getServer().getPluginManager();
		manager.registerEvents(this, this);
		
		//Setting up scoreboard
		sb = getServer().getScoreboardManager().getNewScoreboard();
		ob = sb.registerNewObjective("warpvp", "dummy");
		ob.setDisplayName(ChatColor.GREEN+"WarPvP Score");
		ob.setDisplaySlot(DisplaySlot.SIDEBAR);
		//Setting up teams
		blueteam = sb.registerNewTeam("Blue");
		redteam = sb.registerNewTeam("Red");
		blueteam.setPrefix(ChatColor.BLUE + "");
		redteam.setPrefix(ChatColor.RED + "");
		blueteam.setAllowFriendlyFire(false);
		redteam.setAllowFriendlyFire(false);
		
		logger.info(this.getDescription().getVersion() + " has been enabled!");
	}
	
	@Override
	public void onDisable(){
		logger.info(this.getDescription().getVersion() + " has been disabled!");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String commandlabel, String[] args) {
		if (commandlabel.equalsIgnoreCase("warpvp"))
		{
			//TODO: Test: if (args.length == 0 || args.length == 1 && args[0].equalsIgnoreCase("help"))
			if (args.length == 0)
			{
				sender.sendMessage("=====================" + ChatColor.RED + "[WarPvP]" + ChatColor.RESET + "====================");
				sender.sendMessage("= /warpvp start     -  Starts the game          =");
				sender.sendMessage("= /warpvp stop      -  Stops the game           =");
				sender.sendMessage("= /warpvp join      -  Joins the game           =");
				sender.sendMessage("= /warpvp leave     -  Leaves the game          =");
			    sender.sendMessage("= /warpvp setspawn  -  Set blue/red/lobby spawn =");
				sender.sendMessage("= /warpvp lobby     -  Go to WarPvp lobby       =");
				sender.sendMessage("=================================================");
				//TODO: Ability to kick sb from game
				//TODO: Ability to switch teams
				return true;
			}
			if (!(sender instanceof Player))
			{
				sender.sendMessage(prefix + "This command is only usable ingame!");
				return true;
			}
			Player player = (Player) sender;
/////////
//START//
/////////
			if (args.length == 1 && args[0].equalsIgnoreCase("start"))
			{
				if (!(player.hasPermission("warpvp.manage")))
				{
					player.sendMessage(msgNoPerm);
					return true;
				}
				//Everything ok. Game starting!
				StartTheGame(player);
				return true;
			}
////////
//STOP//
////////
			else if (args.length == 1 && args[0].equalsIgnoreCase("stop"))
			{
				if (!(player.hasPermission("warpvp.manage")))
				{
					player.sendMessage(msgNoPerm);
					return true;
				}
				if (gamestate == false)
				{
					player.sendMessage(prefix+"There is no active game to stop.");
					return true;
				}
				StopTheGame(player);
				return true;
			}
////////
//JOIN//
////////
			else if (args.length == 1 && args[0].equalsIgnoreCase("join"))
			{
				if (!(player.hasPermission("warpvp.play")))
				{
					player.sendMessage(msgNoPerm);
					return true;
				}
				if (gamestate == true)
				{
					player.sendMessage(prefix+"Game has already started.");
					return true;
				}
				if (bluespawn == null && redspawn == null && lobbyspawn == null)
				{
					player.sendMessage(prefix+ChatColor.RED+"Spawn points not set.");
					return true;
				}
				/*if (player.getInventory().getContents().equals(null))
				{
					player.sendMessage(prefix+"Clear your inventory before joining!");
					return true;
				}*/
				//No problems. Adding the player
				playerlist.add(player.getPlayer());
				TeamBalancer(player);
				return true;
			}
/////////
//LEAVE//
/////////
			else if (args.length == 1 && args[0].equalsIgnoreCase("leave"))
			{
				if (!(player.hasPermission("warpvp.play")))
				{
					player.sendMessage(msgNoPerm);
					return true;
				}
				if (!(playerlist.contains(player)))
				{
					player.sendMessage(prefix+"You're not in an active game to leave.");
					return true;
				}
				//Removing the player
				playerlist.remove(player.getPlayer());
				//Stop the game if there is only 1 player in the game
				if (playerlist.size() == 0)//TODO: <2
				{
					StopTheGame(player);
					return true;
				}
				//Checking in which team was the player and removing him
				player.sendMessage(prefix+"You left WarPvP!");
				it = playerlist.iterator();
				while(it.hasNext())
				{
					Player pl = it.next();
					//TODO: Add team color to the player when it leaves
					pl.sendMessage(prefix+player.getDisplayName()+ChatColor.RESET+" left WarPvP.");
				}
				if (sb.getTeam("blue").getPlayers().contains(player)) sb.getTeam("blue").getPlayers().remove(player);
				if (sb.getTeam("red").getPlayers().contains(player)) sb.getTeam("red").getPlayers().remove(player);
				return true;
			}
////////////
//SETSPAWN//
////////////
			else if (args.length > 1 && args[0].equalsIgnoreCase("setspawn"))
			{
				if (args.length == 1)
				{
					player.sendMessage(setSpawnUsage);
					return true;
				}
				if (args[1].equalsIgnoreCase("blue"))
				{
					bluespawn = player.getLocation();
					player.sendMessage(prefix+"Spawn point for "+ChatColor.BLUE+"Blue Team "+ChatColor.RESET+"successfully set.");
					return true;
				}
				else if (args[1].equalsIgnoreCase("red"))
				{
					redspawn = player.getLocation();
					player.sendMessage(prefix+"Spawn point for "+ChatColor.RED+"Red Team "+ChatColor.RESET+"successfully set.");
					return true;
				}
				else if (args[1].equalsIgnoreCase("lobby"))
				{
					lobbyspawn = player.getLocation();
					player.sendMessage(prefix+"Spawn point for "+ChatColor.GRAY+"Lobby "+ChatColor.RESET+"successfully set.");
					return true;
				}
				else
				{
					player.sendMessage(prefix+ChatColor.RED+"Invalid Arguments.");
					player.sendMessage(setSpawnUsage);
					return true;
				}
			}
/////////
//LOBBY//
/////////
			else if (args.length == 1 && args[0].equalsIgnoreCase("lobby"))
			{
				if (!(player.hasPermission("warpvp.play")))
				{
					player.sendMessage(msgNoPerm);
					return true;
				}
				if (lobbyspawn == null)
				{
					player.sendMessage(prefix+"Cannot get you to the Lobby. Spawn not set!");
					return true;
				}
				player.teleport(lobbyspawn);
				player.sendMessage(prefix+"Teleported to Lobby");
				return true;
			}
			else if (args.length == 1 && args[0].equalsIgnoreCase("test"))
			{
				openGUI(player);
				return true;
			}
			else if (args.length == 1 && args[0].equalsIgnoreCase("list"))
			{
				if (playerlist.isEmpty())
				{
					player.sendMessage(prefix+"There are no players at the moment");
					return true;
				}
				player.sendMessage(prefix+"Players on WarPvP right now:");
				it = playerlist.iterator();
				while (it.hasNext())
				{
					player.sendMessage(prefix+"- " + it.next());
				}
				return true;
			}
			else
			{
				sender.sendMessage(ChatColor.RED+"Invalid Arguments. Use "+ChatColor.GOLD+"'/warpvp'"+ChatColor.RED+" for help.");
				return true;
			}
		}
		return false;
	}
	
//////////
//EVENTS//
//////////
	
/////////////////////////////////////////////////////////////////
/////////// P L A Y E R   I N T E R A C T   E V E N T ///////////
/////////////////////////////////////////////////////////////////
	@EventHandler
	public void onPlayerInteract(PlayerInteractEvent event){
		Player p = event.getPlayer();
		ItemStack handItem = p.getInventory().getItemInHand();
		if (playerlist.contains(p))
		{
			if (gamestate == true)
			{
				if (handItem.getType().equals(pistol.getType()))
				{
					pistolArrow = p.launchProjectile(Arrow.class);
					pistolArrow.setShooter(p);
					pistolArrow.setCritical(true);
					pistolArrow.setVelocity(p.getLocation().getDirection().multiply(2.5));
					p.getWorld().playSound(p.getLocation(), Sound.EXPLODE, 5, 2);
					return;
				}
				if (handItem.getType().equals(smg.getType()))
				{
					smgArrow = p.launchProjectile(Arrow.class);
					smgArrow.setShooter(p);
					smgArrow.setVelocity(p.getLocation().getDirection().multiply(5.0));
					p.getWorld().playSound(p.getLocation(), Sound.EXPLODE, 5, 2);
					return;
				}
			}
		}
	}
	
/////////////////////////////////////////////////////////
/////////// P L A Y E R   M O V E   E V E N T ///////////
/////////////////////////////////////////////////////////
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event){
		Player p = event.getPlayer();
		if (playerlist.contains(p))
		{
			if (gamestate == false)
			{
				p.teleport(p.getLocation());
			}
		}
	}
	
//////////////////////////////////////////////////////////
/////////// P L A Y E R   Q U I T   E V E N T ////////////
//////////////////////////////////////////////////////////
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event){
		Player p = event.getPlayer();
		if (playerlist.contains(p))
		{
			playerlist.remove(p);
			if (sb.getTeam("blue").getPlayers().contains(p)) sb.getTeam("blue").getPlayers().remove(p);
			if (sb.getTeam("red").getPlayers().contains(p)) sb.getTeam("red").getPlayers().remove(p);
			if (playerlist.size() < 1)//TODO: <2
			{
				StopTheGame(p);
			}
			it = playerlist.iterator();
			while(it.hasNext())
			{
				Player pl = it.next();
				pl.sendMessage(prefix + ChatColor.GREEN + p.getName() + ChatColor.RESET + " left WarPvP.");
			}
		}
	}
	
///////////////////////////////////////////////////////////
/////////// P L A Y E R   D E A T H   E V E N T ///////////
///////////////////////////////////////////////////////////
	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event){
		if (gamestate == true)
		{
			if (event.getEntity() instanceof Player)
			{
				Player p = event.getEntity();
				if (p.getKiller() instanceof Player)
				{
					Player damager = p.getKiller();
					if (playerlist.contains(p))
					{
						if (sb.getTeam("blue").getPlayers().contains(p)) p.teleport(bluespawn);
						if (sb.getTeam("red").getPlayers().contains(p)) p.teleport(redspawn);
						it = playerlist.iterator();
						while(it.hasNext())
						{
							Player pl = it.next();
							pl.sendMessage(prefix + damager + " killed " + p);
						}
						ob.getScore(damager).setScore(+1);
					}
				}
			}
		}
	}
	
/////////////////////////////////////////////////////////////
/////////// P L A Y E R   D A M A G E   E V E N T ///////////
/////////////////////////////////////////////////////////////
	@EventHandler
	public void onPlayerDamage(EntityDamageByEntityEvent event){
		if (gamestate == true)
		{
			if (event.getEntity() instanceof Player)
			{
				Player p = ((Player) event).getPlayer();
				if (playerlist.contains(p))
				{
					if (event.getDamager() == pistolArrow)
					{
						event.setCancelled(true);
						p.damage(10);
						return;
					}
					if (event.getDamager() == smgArrow)
					{
						event.setCancelled(true);
						p.damage(3);
						return;
					}
				}
			}
		}
	}
	
/////////////////////////////////////////////////////////////////////
/////////// F O O D   L E V E L   C H A N G E   E V E N T ///////////
/////////////////////////////////////////////////////////////////////
	@EventHandler
	public void onFoodLevelChange(FoodLevelChangeEvent event){
		if (gamestate == true)
		{
			if (event instanceof Player)
			{
				Player p = ((Player) event).getPlayer();
				if (playerlist.contains(p))
				{
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
/////////////////////////////////////////////////////////
/////////// P L A Y E R   C H A T   E V E N T ///////////
/////////////////////////////////////////////////////////
	@EventHandler
	public void onPlayerChat(PlayerChatEvent event){
		if (gamestate == true)
		{
			Player p = event.getPlayer();
			if (!(playerlist.contains(p)))
			{
				//WarPvP players won't see msgs sent from non-WarPvp players.
				event.getRecipients().remove(playerlist);
			}
			if (playerlist.contains(p))
			{
				//Non-WarPvP players won't see msgs sent from WarPvP players.
				event.getRecipients().remove(Bukkit.getOnlinePlayers());
				event.getRecipients().addAll(playerlist);
			}
		}
	}
	
///////////////////////////////////////////////////
/////////// S T A R T   T H E   G A M E ///////////
///////////////////////////////////////////////////
	public void StartTheGame(Player player){
		if (gamestate == true)
		{
			player.sendMessage(prefix+"Game has already started.");
			return;
		}
		if (bluespawn == null && redspawn == null && lobbyspawn == null)
		{
			player.sendMessage(prefix+ChatColor.RED+"Spawn points not set.");
			return;
		}//TODO: <2
		if (playerlist.size() == 0)
		{
			player.sendMessage(prefix+"There must be 2 or more players to start.");
			return;
		}
		//sb.getPlayers().add(player);
		gamestate = true;
		player.sendMessage(prefix + "Game started.");
		it = playerlist.iterator();
		while(it.hasNext())
		{
			Player pl = it.next();
			pl.sendMessage(prefix+"Game started! Go kill 'em!");
		}
	}
	
/////////////////////////////////////////////////
/////////// S T O P   T H E   G A M E ///////////
/////////////////////////////////////////////////
	public void StopTheGame(Player player){
		it = playerlist.iterator();
		while(it.hasNext())
		{
			Player pl = it.next();
			pl.teleport(lobbyspawn);
			if (playerlist.size() == 0)//TODO: <2
			{
				pl.sendMessage(prefix + "Players were less than 2. Game stopped.");
			}
			else
			{
				pl.sendMessage(prefix + ChatColor.GREEN + player.getName() + ChatColor.RESET + " ended the game.");
			}
		}
		//Everything ok. Game stopping!
		playerlist.clear();
		sb.getTeam("blue").getPlayers().clear();
		sb.getTeam("red").getPlayers().clear();
		gamestate = false;
	}
	
/////////////////////////////////////////////////
/////////// T E A M   B A L A N C E R ///////////
/////////////////////////////////////////////////
	public void TeamBalancer(Player player){
		if (teambalance == true)
		{
			sb.getTeam("Blue").addPlayer(player);
			player.teleport(bluespawn);
		}
		if (teambalance == false)
		{
			sb.getTeam("Red").addPlayer(player);
			player.teleport(redspawn);
		}
		player.sendMessage(prefix + "You joined the " + sb.getPlayerTeam(player).getDisplayName() + " team! Now wait 'till the administrator starts the game.");
		if (teambalance == true)
		{
			teambalance = false;
			return;
		}
		if (teambalance == false) 
		{
			teambalance = true;
		}
	}
	
///////////////////////////////////////////////////////
/////////// G U I   M E N U   M A N A G E R ///////////
///////////////////////////////////////////////////////
	public void openGUI(Player player){
		//Creating a fake inventory-menu
		Inventory inv = Bukkit.createInventory(null, 9, ChatColor.DARK_RED+"=====["+ChatColor.BOLD+"WarPvP"+ChatColor.DARK_RED+"]=====");
		
		//Setting equipment's inventory slot
		inv.setItem(3, pistol);
		inv.setItem(4, smg);
		inv.setItem(5, sniper);
		
		player.openInventory(inv);
	}
}