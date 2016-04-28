package pl.jeremi.antirelog;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Created by Jeremiasz N. on 2016-04-27.
 */
public class CombatTimeoutTask extends BukkitRunnable {
    CombatManager manager;
    Player player;

    public CombatTimeoutTask(CombatManager manager, Player player) {
        this.manager = manager;
        this.player = player;
    }

    public void run() {
        manager.setOffCombat(player);
    }
}
