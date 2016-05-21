package pl.jeremi.antirelog;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.inventivetalent.bossbar.BossBarAPI;

/**
 * Created by Jeremiasz N. on 2016-04-28.
 */
class BarVanishTimeoutTask extends BukkitRunnable {
    Player player;

    BarVanishTimeoutTask(Player player) {
        this.player = player;
    }

    public void run() {
        BossBarAPI.removeAllBars(player);
    }
}
