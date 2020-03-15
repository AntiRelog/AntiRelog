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
@SuppressWarnings("FieldCanBeLocal")
class CombatHandle {
    static boolean enableBar;
    private int combatTimeLeft;
    private int combatTimeOut, vanishTimeOut;
    private JavaPlugin plugin;
    private String busyChat, freeChat;
    private BarColor busyColor, freeColor;
    private BarStyle barStyle;
    private BossBar busyBar, freeBar;
    private Player player;
    private boolean inCombat;
    private int combatTickTask = -1;

    CombatHandle(Player player, JavaPlugin plugin) {
        this.player = player;
        this.plugin = plugin;

        combatTimeOut = AntiRelog.config.getInt("combat-len");
        vanishTimeOut = AntiRelog.config.getInt("vanish-timeout");

        busyChat = formatCombatChatMessage("busy-chat");
        freeChat = formatCombatChatMessage("free-chat");

        if (enableBar) {
            busyColor = BarColor.valueOf(AntiRelog.getConfigString("busy-color").toUpperCase());
            freeColor = BarColor.valueOf(AntiRelog.getConfigString("free-color").toUpperCase());
            barStyle = BarStyle.valueOf(AntiRelog.getConfigString("bar-style").toUpperCase());

            busyBar = Bukkit.createBossBar(formatCombatChatMessage("busy-message"), busyColor, barStyle);
            busyBar.addPlayer(player);
            busyBar.setVisible(false);

            freeBar = Bukkit.createBossBar(formatCombatChatMessage("free-message"), freeColor, barStyle);
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


    boolean shouldBePunished() {
        return inCombat;
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
        if (combatTickTask != -1) plugin.getServer().getScheduler().cancelTask(combatTickTask);
        combatTickTask = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            @Override
            public void run() {
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
            }
        }, 0, 20);
        //combatTickTask.timedRun(true);
        inCombat = true;
    }

    void endCombat() {
        if (enableBar) {
            busyBar.setVisible(false);

            freeBar.setVisible(true);
            freeBar.setProgress(1d);
            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    freeBar.setVisible(false);
                }
            }, vanishTimeOut * 20);
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
}
