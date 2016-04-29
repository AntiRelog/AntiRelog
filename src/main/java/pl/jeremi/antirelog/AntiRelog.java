package pl.jeremi.antirelog;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.logging.Level;

/**
 * Created by Jeremiasz N. on 2016-04-26.
 */
public class AntiRelog extends JavaPlugin implements Listener {
    public static YamlConfiguration config;
    HashMap<Player, CombatHandle> handledPlayers;
    HashMap<Player, Boolean> bypassingPlayers;
    File configFile;

    @Override
    public void onEnable() {
        configFile = new File(getDataFolder(), "config.yml");
        try {
            if (!configFile.exists()) {
                configFile.getParentFile().mkdirs();
                copy(getResource("config.yml"), configFile);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

        handledPlayers = new HashMap<Player, CombatHandle>();
        bypassingPlayers = new HashMap<Player, Boolean>();
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        try {
            config.save(configFile);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void copy(InputStream in, File file) {
        try {
            OutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
            out.close();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("artoggle")) {
            if (sender instanceof Player && args.length == 0) {
                Player player = (Player) sender;
                if (bypassingPlayers.containsKey(player)) {
                    bypassingPlayers.put(player, !(bypassingPlayers.get(player)));
                    getLogger().log(Level.INFO, "Toggled " + player.getName() + "'s bypass to " + bypassingPlayers.get(player).toString());
                    player.sendMessage("[AntiRelog] " + ChatColor.GREEN + "Successfully set your bypass: " + bypassingPlayers.get(player).toString());
                }
                return true;
            } else if (args.length == 1) {
                Player player = getServer().getPlayer(args[1]);
                if (sender instanceof Player && !player.equals(sender) && !sender.hasPermission("antirelog.toggle.others")) {
                    sender.sendMessage("[AntiRelog] " + ChatColor.RED + "You don't have permission to toggle others bypass.");
                    return true;
                }
                if (player != null && bypassingPlayers.containsKey(player)) {
                    bypassingPlayers.put(player, !(bypassingPlayers.get(player)));
                    getLogger().log(Level.INFO, "Toggled " + player.getName() + "'s bypass to " + bypassingPlayers.get(player).toString());
                    player.sendMessage("[AntiRelog] " + ChatColor.GREEN + "Successfully set your bypass: " + bypassingPlayers.get(player).toString());
                } else {
                    sender.sendMessage("[AntiRelog] " + ChatColor.RED + "Something went wrong. Is this player online?");
                }
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && isHostile(event.getEntity())) {
            Player player = (Player) event.getDamager();
            if (!bypassingPlayers.get(player))
                handledPlayers.get(player).startCombat();
        }
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!bypassingPlayers.get(player))
                handledPlayers.get(player).startCombat();
        }
    }

    private boolean isHostile(Entity entity) {
        return entity.getType() == EntityType.WITHER_SKULL
                || entity.getType() == EntityType.CREEPER
                || entity.getType() == EntityType.SKELETON
                || entity.getType() == EntityType.SPIDER
                || entity.getType() == EntityType.GIANT
                || entity.getType() == EntityType.ZOMBIE
                || entity.getType() == EntityType.SLIME
                || entity.getType() == EntityType.GHAST
                || entity.getType() == EntityType.PIG_ZOMBIE
                || entity.getType() == EntityType.ENDERMAN
                || entity.getType() == EntityType.CAVE_SPIDER
                || entity.getType() == EntityType.SILVERFISH
                || entity.getType() == EntityType.BLAZE
                || entity.getType() == EntityType.MAGMA_CUBE
                || entity.getType() == EntityType.ENDER_DRAGON
                || entity.getType() == EntityType.WITHER
                || entity.getType() == EntityType.BAT
                || entity.getType() == EntityType.WITCH
                || entity.getType() == EntityType.ENDERMITE
                || entity.getType() == EntityType.GUARDIAN
                || entity.getType() == EntityType.SHULKER
                || entity.getType() == EntityType.WOLF
                || entity.getType() == EntityType.IRON_GOLEM
                || entity.getType() == EntityType.PLAYER;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        handledPlayers.put(event.getPlayer(), new CombatHandle(event.getPlayer(), this));
        bypassingPlayers.put(event.getPlayer(), event.getPlayer().hasPermission("antirelog.bypass"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!bypassingPlayers.get(player) && handledPlayers.get(player).isInCombat()) {
            player.setHealth(0);
            if (!config.getString("broadcast-message").isEmpty())
                getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', config.getString("broadcast-message").replaceAll("\\{displayname\\}", player.getDisplayName()).replaceAll("\\{username\\}", player.getName())));
        }
        if (handledPlayers.containsKey(player)) {
            handledPlayers.get(player).cleanUp();
            handledPlayers.remove(player);
        }
        if (bypassingPlayers.containsKey(player)) {
            bypassingPlayers.remove(player);
        }
    }
}
