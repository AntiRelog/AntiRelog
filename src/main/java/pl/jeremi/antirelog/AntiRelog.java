package pl.jeremi.antirelog;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.logging.Level;

/**
 * Created by Jeremiasz N. on 2016-04-26.
 */
public class AntiRelog extends JavaPlugin implements Listener {
    static FileConfiguration config;
    private HashMap<Player, CombatHandle> handledPlayers;
    private HashMap<Player, Boolean> bypassingPlayers;

    @Override
    public void onEnable() {
        config = getConfig();

        config.addDefault("enable-bar", true);
        config.addDefault("combat-len", 15);
        config.addDefault("vanish-timeout", 5);
        config.addDefault("busy-message", "&cDo not log out before&7: &r{timeleft} secs.");
        config.addDefault("free-message", "&aYou can now log out");
        config.addDefault("busy-color", "red");
        config.addDefault("free-color", "green");
        config.addDefault("bar-style", "segmented_6");
        config.addDefault("broadcast-message", "&b[AntiRelog] &6Player &2{displayname} &6has left while in combat!");
        config.addDefault("busy-chat", "&c[AntiRelog] &fYou are now in &6combat&f! It time out in {timeout} seconds.");
        config.addDefault("free-chat", "&a[AntiRelog] &6Combat&f timed out!");
        config.addDefault("subjects", new String[]{"Player", "Zombie", "Husk", "Zombie_Villager"});
        config.options().copyDefaults(true);
        saveConfig();

        handledPlayers = new HashMap<>();
        bypassingPlayers = new HashMap<>();
        getServer().getPluginManager().registerEvents(this, this);

        CombatHandle.enableBar = AntiRelog.config.getBoolean("enable-bar");
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
                Player player = getServer().getPlayer(args[0]);
                if (player != null && sender instanceof Player && !player.equals(sender) && !sender.hasPermission("antirelog.toggle.others")) {
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

    @SuppressWarnings("unused")
    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && isSubject(event.getEntity().getType())) {
            Player player = (Player) event.getDamager();
            if (!bypassingPlayers.get(player))
                handledPlayers.get(player).startCombat();
        }

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!bypassingPlayers.get(player))
                handledPlayers.get(player).startCombat();
        }

        if (event.getDamager() instanceof Projectile) {
            if (((Projectile) event.getDamager()).getShooter() instanceof Player &&
                    isSubject(event.getEntity().getType())) {
                Player damager = (Player) (((Projectile) event.getDamager()).getShooter());
                if (!bypassingPlayers.get(damager))
                    handledPlayers.get(damager).startCombat();
            }
        }
    }

    protected static String getConfigString(String key) {
        String value = config.getString(key);
        return value != null
                ? value
                : "";
    }

    private boolean isSubject(EntityType entity) {
        for (String s : config.getStringList("subjects")) {
            if (s.toUpperCase().equals(entity.name())) return true;
        }
        return false;
    }

    @SuppressWarnings("unused")
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        handledPlayers.put(event.getPlayer(), new CombatHandle(event.getPlayer(), this));
        bypassingPlayers.put(event.getPlayer(), event.getPlayer().hasPermission("antirelog.bypass"));
    }

    @SuppressWarnings("unused")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!bypassingPlayers.get(player) && handledPlayers.get(player).shouldBePunished()) {
            player.setHealth(0);
            String broadcastMessage = config.getString("broadcast-message");
            if (broadcastMessage != null && !broadcastMessage.isEmpty()) {
                event.setQuitMessage(ChatColor.translateAlternateColorCodes(
                        '&',
                        broadcastMessage
                                .replaceAll("\\{displayname}", player.getDisplayName())
                                .replaceAll("\\{username}", player.getName())
                ));
            }
        }
        if (handledPlayers.containsKey(player)) {
            handledPlayers.get(player).cleanUp();
            handledPlayers.remove(player);
        }
        bypassingPlayers.remove(player);
    }
}
