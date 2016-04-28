package pl.jeremi.antirelog;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.util.logging.Level;

/**
 * Created by Jeremiasz N. on 2016-04-26.
 */
public class AntiRelog extends JavaPlugin implements Listener {
    YamlConfiguration config;
    File configFile;
    CombatManager manager;

    @Override
    public void onEnable() {
        configFile = new File(getDataFolder(), "config.yml");
        try {
            if(!configFile.exists()){
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

        getServer().getPluginManager().registerEvents(this, this);
        manager = new CombatManager(config.getInt("combat-len"), config.getInt("vanish-timeout"), getConfig().getString("busy-message").replaceAll("(&([a-f0-9]))", "\u00A7$2"), getConfig().getString("free-message").replaceAll("(&([a-f0-9]))", "\u00A7$2"), this);
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

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event)
    {
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (!player.hasPermission("antirelog.bypass"))
                manager.setOnCombat(player);
        }
        if (event.getDamager() instanceof Player && isHostile(event.getEntity())) {
            Player player = (Player) event.getDamager();
            if (!player.hasPermission("antirelog.bypass"))
                manager.setOnCombat(player);
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
        manager.setOffCombat(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onQuit(PlayerQuitEvent event)
    {
        Player player =  event.getPlayer();
        if (!player.hasPermission("antirelog.bypass") && manager.isInCombat(player)) {
            player.setHealth(0);
            getLogger().log(Level.INFO, player.getName() + " has left server while in combat!");
        }
    }
}
