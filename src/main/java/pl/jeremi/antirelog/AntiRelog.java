package pl.jeremi.antirelog;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Jeremiasz N. on 2016-04-26.
 */
public final class AntiRelog extends JavaPlugin implements Listener {
    static FileConfiguration config;
    private final HashMap<Player, CombatHandle> handledPlayers = new HashMap<>();

    @Override
    public void onEnable() {
        config = getConfig();

        // lines 37-45 was stolen from this https://github.com/NEZNAMY/TAB/blob/master/shared/src/main/java/me/neznamy/tab/shared/config/ConfigurationFile.java#L43
        final File configFile = new File(getDataFolder(), "config.yml");
        if (configFile.getParentFile() != null)
            configFile.getParentFile().mkdirs();
        if(!configFile.exists()) {
            try {
                Files.copy(AntiRelog.class.getClassLoader().getResourceAsStream("config.yml"), configFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            config.addDefault("combat-length", 15);
            config.addDefault("vanish-timeout", 5);

            final ConfigurationSection barSection = config.createSection("bossbar");
            barSection.addDefault("enable-bar", true);
            barSection.addDefault("busy-color", "red");
            barSection.addDefault("free-color", "green");
            barSection.addDefault("busy-style", "segmented_6");
            barSection.addDefault("free-style", "solid");
            barSection.addDefault("busy-message", "&cDo not log out before&7: &r{timeleft} secs.");
            barSection.addDefault("free-message", "&aYou can now log out");

            final ConfigurationSection chatSection = config.createSection("chat");
            chatSection.addDefault("broadcast-message", "&b[AntiRelog] &6Player &2{displayname} &6has left while in combat!");
            chatSection.addDefault("busy-chat", "&c[AntiRelog] &fYou are now in &6combat&f! It time out in {timeout} seconds.");
            chatSection.addDefault("free-chat", "&a[AntiRelog] &6Combat&f timed out!");

            config.addDefault("subjects", new String[]{"Player", "Zombie", "Husk", "Zombie_Villager"});
            config.options().copyDefaults(true);
            saveDefaultConfig();
        }

        getServer().getPluginManager().registerEvents(this, this);

        CombatHandle.enableBar = config.getConfigurationSection("bar").getBoolean("enable-bar");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (label.equalsIgnoreCase("arreload")) {
            sender.sendMessage("[AntiRelog] " + ChatColor.GREEN + "Config was successfully reloaded.");
            reloadConfig();
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCombat(final EntityDamageByEntityEvent event) {
        if(event.isCancelled())
            return;

        if (event.getDamager() instanceof Player && isSubject(event.getEntity().getType())) {
            final Player player = (Player) event.getDamager();
            if (!player.hasPermission("antirelog.bypass"))
                handledPlayers.get(player).startCombat();
        } else if (event.getDamager() instanceof Projectile) {
            if (((Projectile) event.getDamager()).getShooter() instanceof Player && isSubject(event.getEntity().getType())) {
                final Player damager = (Player) (((Projectile) event.getDamager()).getShooter());

                if (damager != null && !damager.hasPermission("antirelog.bypass"))
                    handledPlayers.get(damager).startCombat();
            }
        }

        if (event.getEntity() instanceof Player) {
            final Player player = (Player) event.getEntity();
            if (!player.hasPermission("antirelog.bypass"))
                handledPlayers.get(player).startCombat();
        }
    }

    private boolean isSubject(EntityType entity) {
        final List<String> subjects = config.getStringList("subjects");
        return (subjects.contains("ALL") || subjects.contains(entity.name()));
    }

    @EventHandler
    public void onJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        handledPlayers.put(player, new CombatHandle(player, this));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        if (!player.hasPermission("antirelog.bypass") && handledPlayers.get(player).isInCombat()) {
            player.setHealth(0);
            final String broadcastMessage = config.getString("broadcast-message");
            if (broadcastMessage != null && !broadcastMessage.isEmpty())
                event.setQuitMessage(ChatColor.translateAlternateColorCodes(
                        '&',
                        broadcastMessage
                                .replaceAll("\\{displayname}", player.getDisplayName())
                                .replaceAll("\\{username}", player.getName())
                ));
        }
        if (handledPlayers.containsKey(player)) {
            handledPlayers.get(player).cleanUp();
            handledPlayers.remove(player);
        }
    }

    @EventHandler
    public void onDeath(final PlayerDeathEvent event){
        final CombatHandle combatHandle = handledPlayers.get(event.getEntity().getPlayer());
        if(combatHandle.isInCombat())
            combatHandle.reset();
    }
}