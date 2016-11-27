package pl.jeremi.antirelog;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Entity;
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

import java.util.Collections;
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

        config.addDefault("enable-barapi", true);
        config.addDefault("combat-len", 5);
        config.addDefault("vanish-timeout", 3);
        config.addDefault("busy-message", "&cAntiRelog");
        config.addDefault("free-message", "&aAntiRelog");
        config.addDefault("busy-color", "red");
        config.addDefault("free-color", "green");
        config.addDefault("broadcast-message", "&b[AntiRelog] &6Player &2{displayname} &6has left while in combat!");
        config.addDefault("busy-chat", "&c[AntiRelog] &fYou are now in &6combat&f! It time out in {combatdur} seconds.");
        config.addDefault("free-chat", "&a[AntiRelog] &6Combat&f timed out!");
        config.addDefault("strict", true);
        config.addDefault("subjects/passive", false);
        config.addDefault("subjects/neutral", true);
        config.addDefault("subjects/hostile", true);
        config.addDefault("subjects/player", true);
        config.addDefault("subjects/default", false);
        config.addDefault("subjects/excludes", Collections.emptyList()); // Arrays.asList alternative
        config.options().copyDefaults(true);
        saveConfig();

        handledPlayers = new HashMap<Player, CombatHandle>();
        bypassingPlayers = new HashMap<Player, Boolean>();
        getServer().getPluginManager().registerEvents(this, this);

        CombatHandle.enableBarApi = AntiRelog.config.getBoolean("enable-barapi");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("artoggle")) {
            if (sender instanceof Player && args.length == 0) {
                Player player = (Player) sender;
                if (bypassingPlayers.containsKey(player)) {
                    bypassingPlayers.put(player, !(bypassingPlayers.get(player)));
                    getLogger().log(Level.INFO, "Toggled " + player.getName() + "'s bypass to " + bypassingPlayers.get(player).toString());
                    player.sendMessage("[AntiRelog by Jeremi] " + ChatColor.GREEN + "Successfully set your bypass: " + bypassingPlayers.get(player).toString());
                }
                return true;
            } else if (args.length == 1) {
                Player player = getServer().getPlayer(args[0]);
                if (sender instanceof Player && !player.equals(sender) && !sender.hasPermission("antirelog.toggle.others")) {
                    sender.sendMessage("[AntiRelog by Jeremi] " + ChatColor.RED + "You don't have permission to toggle others bypass.");
                    return true;
                }
                if (player != null && bypassingPlayers.containsKey(player)) {
                    bypassingPlayers.put(player, !(bypassingPlayers.get(player)));
                    getLogger().log(Level.INFO, "Toggled " + player.getName() + "'s bypass to " + bypassingPlayers.get(player).toString());
                    player.sendMessage("[AntiRelog by Jeremi] " + ChatColor.GREEN + "Successfully set your bypass: " + bypassingPlayers.get(player).toString());
                } else {
                    sender.sendMessage("[AntiRelog by Jeremi] " + ChatColor.RED + "Something went wrong. Is this player online?");
                }
                return true;
            }
        }
        return false;
    }

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player && isSubject(event.getEntity())) {
            Player player = (Player) event.getDamager();
            if (!bypassingPlayers.get(player))
                handledPlayers.get(player).startCombat();
        }

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!bypassingPlayers.get(player))
                handledPlayers.get(player).startCombat();
        }

        if(event.getDamager() instanceof Projectile) {
            if (((Projectile)event.getDamager()).getShooter() instanceof Player &&
                    isSubject(event.getEntity())) {
                Player damager = (Player) (((Projectile) event.getDamager()).getShooter());
                if (!bypassingPlayers.get(damager))
                    handledPlayers.get(damager).startCombat();
            }
        }
    }

    private boolean isSubject(Entity entity) {
        if (config.getStringList("subjects/excludes").contains(entity.toString())) return false;
        if (config.getBoolean("subjects/hostile") && isHostile(entity)) return true;
        if (config.getBoolean("subjects/neutral") && isNeutral(entity)) return true;
        if (config.getBoolean("subjects/passive") && isPassive(entity)) return true;
        if (config.getBoolean("subjects/player") && entity.getType().equals(EntityType.PLAYER)) return true;
        if (config.getBoolean("subjects/default")) return true;
        return false;
    }

    private boolean isHostile(Entity entity) {
        if ((Bukkit.getBukkitVersion().contains("1.8") || Bukkit.getBukkitVersion().contains("1.9") || Bukkit.getBukkitVersion().contains("1.10") || Bukkit.getBukkitVersion().contains("1.11")) && (entity.getType() == EntityType.GUARDIAN || entity.getType() == EntityType.ENDERMITE))
            return true;

        if ((Bukkit.getBukkitVersion().contains("1.9") || Bukkit.getBukkitVersion().contains("1.10") || Bukkit.getBukkitVersion().contains("1.11"))
                && entity.getType() == EntityType.SHULKER)
            return true;

        if (Bukkit.getBukkitVersion().contains("1.11")
                && (entity.getType() == EntityType.EVOKER || entity.getType() == EntityType.EVOKER_FANGS || entity.getType() == EntityType.VINDICATOR || entity.getType() == EntityType.VEX || entity.getType() == EntityType.HUSK || entity.getType() == EntityType.ZOMBIE_VILLAGER))
            return true;

        return entity.getType() == EntityType.CREEPER
                || entity.getType() == EntityType.SKELETON
                || entity.getType() == EntityType.SPIDER
                || entity.getType() == EntityType.GIANT
                || entity.getType() == EntityType.ZOMBIE
                || entity.getType() == EntityType.SLIME
                || entity.getType() == EntityType.GHAST
                || entity.getType() == EntityType.ENDERMAN
                || entity.getType() == EntityType.CAVE_SPIDER
                || entity.getType() == EntityType.SILVERFISH
                || entity.getType() == EntityType.BLAZE
                || entity.getType() == EntityType.MAGMA_CUBE
                || entity.getType() == EntityType.ENDER_DRAGON
                || entity.getType() == EntityType.WITHER
                || entity.getType() == EntityType.BAT
                || entity.getType() == EntityType.WITCH;
    }

    private boolean isNeutral(Entity entity) {
        if (Bukkit.getBukkitVersion().contains("1.11") && entity.getType() == EntityType.LLAMA)
            return true;

        if ((Bukkit.getBukkitVersion().contains("1.10") || Bukkit.getBukkitVersion().contains("1.11")) && entity.getType() == EntityType.POLAR_BEAR)
            return true;

        return entity.getType() == EntityType.PIG_ZOMBIE
                || entity.getType() == EntityType.WOLF
                || entity.getType() == EntityType.IRON_GOLEM;
    }

    private boolean isPassive(Entity entity) {
        return entity.getType() == EntityType.CHICKEN
                || entity.getType() == EntityType.SHEEP
                || entity.getType() == EntityType.COW
                || entity.getType() == EntityType.MUSHROOM_COW
                || entity.getType() == EntityType.BAT
                || entity.getType() == EntityType.PIG
                || entity.getType() == EntityType.HORSE
                || entity.getType() == EntityType.OCELOT
                || entity.getType() == EntityType.SNOWMAN
                || entity.getType() == EntityType.RABBIT
                || entity.getType() == EntityType.SQUID
                || entity.getType() == EntityType.VILLAGER;
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
                getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', config.getString("broadcast-message").replaceAll("\\{displayname\\}", player.getDisplayName()).replaceAll("\\{username\\}", player.getName()).replaceAll("\\{combatdur\\}", String.valueOf(config.getInt("combat-len")))));
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
