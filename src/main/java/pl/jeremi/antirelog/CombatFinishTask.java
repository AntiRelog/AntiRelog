package pl.jeremi.antirelog;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * Created by Jeremiasz N. on 2016-04-27.
 */
public class CombatFinishTask extends BukkitRunnable {
    CombatHandle handle;

    public CombatFinishTask(CombatHandle handle) {
        this.handle = handle;
    }

    public void run() {
        handle.endCombat();
    }
}
