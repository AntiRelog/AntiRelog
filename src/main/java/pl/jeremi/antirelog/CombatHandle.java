package pl.jeremi.antirelog;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.inventivetalent.bossbar.BossBarAPI;

/**
 * Created by Jeremiasz N. on 2016-04-26.
 */
class CombatHandle {
    private int combatDuration, vanishTimeout;
    private JavaPlugin plugin;
    private String busyMessage, freeMessage;
    private BossBarAPI.Color busyColor, freeColor;
    private Player player;
    private boolean inCombat;
    private BukkitTask barVanishTask, combatFinishTask;

    CombatHandle(Player player, JavaPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        combatDuration = AntiRelog.config.getInt("combat-len");
        vanishTimeout = AntiRelog.config.getInt("vanish-timeout");
        busyMessage = ChatColor.translateAlternateColorCodes('&', AntiRelog.config.getString("busy-message"));
        freeMessage = ChatColor.translateAlternateColorCodes('&', AntiRelog.config.getString("free-message"));
        freeColor = BossBarAPI.Color.valueOf(AntiRelog.config.getString("free-color").toUpperCase());
        busyColor = BossBarAPI.Color.valueOf(AntiRelog.config.getString("busy-color").toUpperCase());
        inCombat = false;
    }

    boolean isInCombat() {
        return inCombat;
    }

    void startCombat() {
        inCombat = true;
        if (barVanishTask != null)
            barVanishTask.cancel();
        if (combatFinishTask != null)
            combatFinishTask.cancel();
        BossBarAPI.removeAllBars(player);
        BossBarAPI.addBar(player,
                new TextComponent(busyMessage),
                busyColor,
                BossBarAPI.Style.NOTCHED_20,
                1f,
                Bukkit.getBukkitVersion().contains("1.9") ? combatDuration * 20 : combatDuration, // HACK: I don't know what is happening with BossBarAPI, but this fixes the problem.
                1L);
        combatFinishTask =
                new CombatFinishTask(this).runTaskLater(plugin, combatDuration * 20);
    }

    void endCombat() {
        inCombat = false;
        if (barVanishTask != null)
            barVanishTask.cancel();
        if (combatFinishTask != null)
            combatFinishTask.cancel();
        if (player.isOnline()) {
            BossBarAPI.removeAllBars(player);
            BossBarAPI.addBar(player,
                    new TextComponent(freeMessage),
                    freeColor,
                    BossBarAPI.Style.NOTCHED_20,
                    1f);
            if (vanishTimeout >= 0) {
                barVanishTask =
                        new BarVanishTimeoutTask(player).runTaskLater(plugin, vanishTimeout * 20);
            }
        }
    }

    void cleanUp() {
        if (barVanishTask != null)
            barVanishTask.cancel();
        if (combatFinishTask != null)
            combatFinishTask.cancel();
    }
}
