package pl.jeremi.antirelog;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.inventivetalent.bossbar.BossBar;
import org.inventivetalent.bossbar.BossBarAPI;

import java.util.Hashtable;

/**
 * Created by Jeremiasz N. on 2016-04-26.
 */
public class CombatManager {
    int combatDuration;
    Hashtable<Player, BukkitTask> playersInCombat;
    Hashtable<Player, BossBar> bossBars;
    JavaPlugin plugin;
    String busyMessage, freeMessage;

    public CombatManager(int combatDuration, String busyMessage, String freeMessage, JavaPlugin plugin) {
        this.combatDuration = combatDuration;
        this.plugin = plugin;
        this.busyMessage = busyMessage;
        this.freeMessage = freeMessage;
        playersInCombat = new Hashtable<Player, BukkitTask>();
    }

    public boolean isInCombat(Player player) {
        return playersInCombat.containsKey(player);
    }

    public void setOnCombat(Player player) {
        if(playersInCombat.containsKey(player))
        {
            playersInCombat.get(player).cancel();
        }
        BossBarAPI.removeAllBars(player);
        BossBarAPI.addBar(player,
            new TextComponent(busyMessage),
            BossBarAPI.Color.RED,
            BossBarAPI.Style.PROGRESS,
            1f,
            combatDuration,
            1L);
        playersInCombat.put(player,
                new CombatTimeoutTask(this, plugin, player).runTaskLater(plugin, combatDuration));
    }

    public void setOffCombat(Player player) {
        if (player.isOnline())
        {
            BossBarAPI.removeAllBars(player);
            BossBarAPI.addBar(player,
                    new TextComponent(freeMessage),
                    BossBarAPI.Color.GREEN,
                    BossBarAPI.Style.PROGRESS,
                    1f);
            if (playersInCombat.containsKey(player))
                playersInCombat.remove(player);
        }
    }
}
