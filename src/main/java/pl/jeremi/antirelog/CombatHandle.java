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
    static boolean enableBarApi;
    private int combatDuration, vanishTimeout;
    private JavaPlugin plugin;
    private String busyMessage, freeMessage, busyChat, freeChat;
    private BossBarAPI.Color busyColor, freeColor;
    private Player player;
    private boolean inCombat;
    private BukkitTask barVanishTask, combatFinishTask;

    CombatHandle(Player player, JavaPlugin plugin) {
        this.player = player;
        this.plugin = plugin;
        combatDuration = AntiRelog.config.getInt("combat-len");
        vanishTimeout = AntiRelog.config.getInt("vanish-timeout");
        busyMessage = ChatColor.translateAlternateColorCodes('&', AntiRelog.config.getString("busy-message").replaceAll("\\{displayname\\}", player.getDisplayName()).replaceAll("\\{username\\}", player.getName()).replaceAll("\\{combatdur\\}", String.valueOf(combatDuration)));
        freeMessage = ChatColor.translateAlternateColorCodes('&', AntiRelog.config.getString("free-message").replaceAll("\\{displayname\\}", player.getDisplayName()).replaceAll("\\{username\\}", player.getName()).replaceAll("\\{combatdur\\}", String.valueOf(combatDuration)));
        busyChat = ChatColor.translateAlternateColorCodes('&', AntiRelog.config.getString("busy-chat").replaceAll("\\{displayname\\}", player.getDisplayName()).replaceAll("\\{username\\}", player.getName()).replaceAll("\\{combatdur\\}", String.valueOf(combatDuration)));
        freeChat = ChatColor.translateAlternateColorCodes('&', AntiRelog.config.getString("free-chat").replaceAll("\\{displayname\\}", player.getDisplayName()).replaceAll("\\{username\\}", player.getName()).replaceAll("\\{combatdur\\}", String.valueOf(combatDuration)));
        if (enableBarApi) {
            freeColor = BossBarAPI.Color.valueOf(AntiRelog.config.getString("free-color").toUpperCase());
            busyColor = BossBarAPI.Color.valueOf(AntiRelog.config.getString("busy-color").toUpperCase());
        }
        inCombat = false;
    }

    boolean isInCombat() {
        return inCombat;
    }

    void startCombat() {
        if (enableBarApi) {
            if (barVanishTask != null)
                barVanishTask.cancel();
            BossBarAPI.removeAllBars(player);
            BossBarAPI.addBar(player,
                    new TextComponent(busyMessage),
                    busyColor,
                    BossBarAPI.Style.NOTCHED_20,
                    1f,
                    Bukkit.getBukkitVersion().contains("1.9") || Bukkit.getBukkitVersion().contains("1.10") ?
                            combatDuration * 20 : combatDuration, // NOTE: Inconsistent time units in BossBarAPI
                    1L);
        }
        if (!busyChat.isEmpty())
            player.sendMessage(busyChat);
        inCombat = true;
        if (combatFinishTask != null)
            combatFinishTask.cancel();
        combatFinishTask =
                new CombatFinishTask(this).runTaskLater(plugin, combatDuration * 20);
    }

    void endCombat() {
        if (enableBarApi) {
            if (barVanishTask != null)
                barVanishTask.cancel();
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
        if (!freeChat.isEmpty())
            player.sendMessage(freeChat);
        inCombat = false;
        if (combatFinishTask != null)
            combatFinishTask.cancel();
    }

    void cleanUp() {
        if (barVanishTask != null)
            barVanishTask.cancel();
        if (combatFinishTask != null)
            combatFinishTask.cancel();
    }
}
