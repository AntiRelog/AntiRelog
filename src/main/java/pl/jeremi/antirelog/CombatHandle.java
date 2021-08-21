package pl.jeremi.antirelog;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Instrument;
import org.bukkit.Note;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created by Jeremiasz N. on 2016-04-26.
 */
class CombatHandle {
    static boolean enableBar;
    private int combatTimeLeft;
    private final int combatTimeOut = AntiRelog.config.getInt("combat-len");
    private final int vanishTimeOut = AntiRelog.config.getInt("vanish-timeout");
    private final String busyChat = formatCombatChatMessage("busy-chat");
    private final String freeChat = formatCombatChatMessage("free-chat");
    private BossBar busyBar, freeBar;
    private final Player player;
    private final JavaPlugin plugin;
    private boolean inCombat;
    private int combatTickTask = -1;

    CombatHandle(Player player, JavaPlugin plugin) {
        this.player = player;
        this.plugin = plugin;

        if (enableBar) {
            busyBar = Bukkit.createBossBar(formatCombatChatMessage("busy-message"),
                    BarColor.valueOf(AntiRelog.getConfigString("busy-color").toUpperCase()),
                    BarStyle.valueOf(AntiRelog.getConfigString("busy-style").toUpperCase()));
            busyBar.addPlayer(player);
            busyBar.setVisible(false);

            freeBar = Bukkit.createBossBar(formatCombatChatMessage("free-message"),
                    BarColor.valueOf(AntiRelog.getConfigString("free-color").toUpperCase()),
                    BarStyle.valueOf(AntiRelog.getConfigString("free-style").toUpperCase()));
            freeBar.addPlayer(player);
            freeBar.setVisible(false);
        }

        inCombat = false;
    }

    private String formatCombatChatMessage(String message) {
        return ChatColor.translateAlternateColorCodes('&', AntiRelog.getConfigString(message))
                .replaceAll("\\{displayname}", player.getDisplayName())
                .replaceAll("\\{username}", player.getName())
                .replaceAll("\\{timeleft}", String.valueOf(combatTimeLeft))
                .replaceAll("\\{timeout}", String.valueOf(combatTimeOut));
    }


    boolean isInCombat() {
        return inCombat;
        //return combatTimeLeft != 0;
    }

    void startCombat() {
        if (enableBar) {
            busyBar.setTitle(formatCombatChatMessage("busy-message"));
            busyBar.setVisible(true);
            busyBar.setProgress(1d);
            freeBar.setVisible(false);
        }
        if (!busyChat.isEmpty() && !inCombat)
            player.sendMessage(busyChat);
        combatTimeLeft = combatTimeOut;
        if (combatTickTask != -1)
            plugin.getServer().getScheduler().cancelTask(combatTickTask);

        combatTickTask = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (combatTimeLeft == 0) {
                CombatHandle.this.endCombat();

                return;
            }

            player.playNote(player.getLocation(), Instrument.PIANO, Note.sharp(1, Note.Tone.C));
            if (enableBar) {
                busyBar.setProgress((double) combatTimeLeft / combatTimeOut);
                // Update time in message
                busyBar.setTitle(CombatHandle.this.formatCombatChatMessage("busy-message"));
            }

            combatTimeLeft--;
        }, 0, 20);
        //combatTickTask.timedRun(true);
        inCombat = true;
    }

    void endCombat() {
        if (enableBar) {
            busyBar.setVisible(false);

            freeBar.setVisible(true);
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,
                    () -> freeBar.setVisible(false), vanishTimeOut * 20L);
        }
        plugin.getServer().getScheduler().cancelTask(combatTickTask);
        combatTickTask = -1;
        player.playNote(player.getLocation(), Instrument.PIANO, Note.natural(1, Note.Tone.G));
        if (!freeChat.isEmpty())
            player.sendMessage(freeChat);

        inCombat = false;
    }

    // In case, if player re-joins
    void cleanUp() {
        //combatTickTask.cancel();
        if (enableBar) {
            busyBar.removeAll();
            freeBar.removeAll();
        }
    }
    void reset(){
        endCombat();
        combatTimeLeft = 0;
    }
}