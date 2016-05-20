package pl.jeremi.antirelog;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.inventivetalent.bossbar.BossBarAPI;

/**
 * Created by Jeremiasz N. on 2016-04-26.
 */
public class CombatHandle {
    int combatDuration, vanishTimeout;
    JavaPlugin plugin;
    String busyMessage, freeMessage;
    Player player;
    boolean inCombat, barVanished;
    BukkitTask barVanishTask, combatFinishTask;

    public CombatHandle(Player player, JavaPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        combatDuration = AntiRelog.config.getInt("combat-len");
        vanishTimeout = AntiRelog.config.getInt("vanish-timeout");
        busyMessage = ChatColor.translateAlternateColorCodes('&', AntiRelog.config.getString("busy-message"));
        freeMessage = ChatColor.translateAlternateColorCodes('&', AntiRelog.config.getString("free-message"));
        inCombat = false;
        barVanished = true;
    }

    public boolean isInCombat() {
        return inCombat;
    }

    public void startCombat() {
        inCombat = true;
        if (barVanishTask != null)
            barVanishTask.cancel();
        if (combatFinishTask != null)
            combatFinishTask.cancel();
        BossBarAPI.removeAllBars(player);
        BossBarAPI.addBar(player,
                new TextComponent(busyMessage),
                BossBarAPI.Color.RED,
                BossBarAPI.Style.PROGRESS,
                1f,
                combatDuration,
                1L);
        combatFinishTask =
                new CombatFinishTask(this).runTaskLater(plugin, combatDuration * 20);
    }

    public void endCombat() {
        inCombat = false;
        if (barVanishTask != null)
            barVanishTask.cancel();
        if (combatFinishTask != null)
            combatFinishTask.cancel();
        if (player.isOnline()) {
            BossBarAPI.removeAllBars(player);
            BossBarAPI.addBar(player,
                    new TextComponent(freeMessage),
                    BossBarAPI.Color.GREEN,
                    BossBarAPI.Style.PROGRESS,
                    1f);
            if (vanishTimeout >= 0) {
                barVanishTask =
                        new BarVanishTimeoutTask(player).runTaskLater(plugin, vanishTimeout * 20);
            }
        }
    }

    public void cleanUp() {
        if (barVanishTask != null)
            barVanishTask.cancel();
        if (combatFinishTask != null)
            combatFinishTask.cancel();
    }
}
