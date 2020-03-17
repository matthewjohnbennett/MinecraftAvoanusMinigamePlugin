package _NoVa_CoOKie;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;

/**
 * @author _NoVa_CoOKie
 */
public class Control extends JavaPlugin implements Listener {
	
	

	private File file;
	private FileConfiguration config;
	
	private Game activeGame = null;
	private int spawnTask;
	
	@Override
	public void onEnable() {
		loadConfig();
		getServer().getPluginManager().registerEvents(this, this);
	}
	
	private void loadConfig() {
		if (!getDataFolder().exists())
			getDataFolder().mkdirs();
		
		file = new File(getDataFolder(), "config.yml");
		
		if (!file.exists()) {
			try {
				file.createNewFile();
				FileWriter fr = new FileWriter(file);
				fr.write("Game1:\n");
				/*fr.write("  # point distribution\n");
				fr.write("  pointsPerKill: 1\n");
				fr.write("  pointsPerPlayerKill: 5\n");
				fr.write("  pointsLostPerDeath: 5\n");*/
				fr.write("  # \"difficulty\" value for determining which mobs can spawn\n");
				fr.write("  minDifficulty: 0\n");
				fr.write("  maxDifficulty: 100\n");
				fr.write("  # amount the difficulty should increase after mob kill\n");
				fr.write("  difficultyRate: 5\n");
				fr.write("  maxEnemies: 10\n");
				fr.write("  # max number of mobs to spawn everytime it tries. This is only an upper bound\n");
				fr.write("  maxSpawnPerTry: 3\n");
				fr.write("  # in server ticks, the amount of time between spawning new mobs\n");
				fr.write("  spawnFrequency: 200\n");
				fr.write("  # number of Enemy# properties are there. Please make sure this number is accurate.\n");
				fr.write("  enemyPoolSize: 1\n");
				fr.write("  # name new enemies as Enemy# so that the numbers increase, and no \"holes\" are left.\n");
				fr.write("  Enemy1:\n");
				fr.write("    # exact command used to summon enemy. make sure the spawn coords are \"~ ~ ~\".\n");
				fr.write("    summon: summon zombie ~ ~ ~\n");
				fr.write("    ## the required difficulty for this mob to spawn.\n");
				fr.write("    # make sure at least one enemy has a difficulty less than or equal to the minDifficulty\n");
				fr.write("    difficulty: 0\n");
				fr.write("  # dont have to touch. Instead, use the command addspawn command ingame/addspawn\n");
				fr.write("  # Stand in the position you would like as a spawn, and use this command\n");
				fr.write("  # /addspawn [GameSettingsNumber(1,2,3)]\n");
				fr.write("  # also, if you wish to remove a spawn, please make sure to decrement this value so that it still holds.\n");
				fr.write("  numSpawns: 0\n");
				fr.close();
			} catch (IOException e) {}
		}
		
		config = YamlConfiguration.loadConfiguration(file);
	}

	private void startGame(CommandSender sender, int gameNum, Player[] players) {
		ConfigurationSection gameSettings = config.getConfigurationSection("Game" + gameNum);
		if (gameSettings == null) {
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Couldn't find game settings for Game" + gameNum);
			return;
		}
		activeGame = new Game(this, gameSettings, players);
		spawnTask = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run() {
				activeGame.spawnEnemies(sender);
			}
		}, activeGame.getWaitTime(), activeGame.getWaitTime());
		sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "a game has started with " + players.length + " players using game settings " + gameNum + ".");
	}
	
	private void stopGame(CommandSender sender) {
		//activeGame.sendScores();
		activeGame.sendWinner();
		getServer().getScheduler().cancelTask(spawnTask);
		activeGame = null;
		sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "The game has been stopped.");
	}
	
	private void addSpawn(Player sender, int gameNum) {
		ConfigurationSection gameSettings = config.getConfigurationSection("Game" + gameNum);
		if (gameSettings == null) {
			sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Couldn't find game settings for Game" + gameNum);
			return;
		}
		gameSettings.set("numSpawns", gameSettings.getInt("numSpawns") + 1);
		gameSettings.set("Spawn" + (gameSettings.getInt("numSpawns")), sender.getLocation());
		try {config.save(file);} catch (IOException e) {}
		loadConfig();
		sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Added a spawn to game " + gameNum + ", for a total of " + gameSettings.getInt("numSpawns") + ".");
	}
	
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (label.equalsIgnoreCase("startgame")) {
			if (args.length < 2) return false;
			if (activeGame != null) {
				sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "A game is already in progress!");
				return true;
			}
			Player[] players = new Player[args.length - 1];
			for (int i = 1; i < args.length; ++i) {
				if (getServer().getPlayer(args[i]) == null) {
					sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Player '" + args[i] + "' not found!");
					return true;
				}
				players[i - 1] = getServer().getPlayer(args[i]);
			}
			startGame(sender, Integer.parseInt(args[0]), players);
			return true;
		}
		else if (label.equalsIgnoreCase("stopgame")) {
			if (activeGame == null) {
				sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "There is no game currently in progress!");
				return true;
			}
			stopGame(sender);
			return true;
		}
		else if (label.equalsIgnoreCase("addspawn")) {
			if (args.length < 1) return false;
			if (!(sender instanceof Player)) {
				sender.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Must be a player!");
				return true;
			}
			addSpawn((Player) sender, Integer.parseInt(args[0]));
			return true;
		}
		else if (label.equalsIgnoreCase("minireload")) {
			loadConfig();
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.BOLD + "" + ChatColor.ITALIC + "config file reloaded.");
			return true;
		}
		return false;
	}
	
	@EventHandler
	public void OnDeath(EntityDeathEvent e) {
		if (activeGame == null) return;
		activeGame.notifyOfDeath(e);
	}
}