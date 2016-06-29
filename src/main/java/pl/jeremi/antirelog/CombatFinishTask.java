package pl.jeremi.antirelog;

import org.bukkit.scheduler.BukkitRunnable;

/**
 * Created by Jeremiasz N. on 2016-04-27.
 */
class CombatFinishTask extends BukkitRunnable {
    private CombatHandle handle;

    CombatFinishTask(CombatHandle handle) {
        this.handle = handle;
    }

    public void run() {
        handle.endCombat();
    }
}
