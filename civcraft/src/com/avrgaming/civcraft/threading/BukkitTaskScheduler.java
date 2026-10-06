package com.avrgaming.civcraft.threading;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import com.avrgaming.civcraft.util.BukkitObjects;

/** {@link TaskScheduler} on top of the Bukkit scheduler. The name maps are shared with async threads. */
public class BukkitTaskScheduler implements TaskScheduler {

	private final ConcurrentHashMap<String, BukkitTask> tasks = new ConcurrentHashMap<String, BukkitTask>();
	private final ConcurrentHashMap<String, BukkitTask> timers = new ConcurrentHashMap<String, BukkitTask>();

	private Plugin plugin() {
		return BukkitObjects.getPlugin();
	}

	private BukkitScheduler scheduler() {
		return BukkitObjects.getScheduler();
	}

	private static boolean isTracked(String name) {
		return name != null && !name.isEmpty();
	}

	@Override
	public void syncTask(Runnable runnable, long delay) {
		scheduler().runTaskLater(plugin(), runnable, delay);
	}

	@Override
	public void syncTimer(String name, Runnable runnable, long delay, long repeat) {
		addTimer(name, scheduler().runTaskTimer(plugin(), runnable, delay, repeat));
	}

	@Override
	public void asyncTask(String name, Runnable runnable, long delay) {
		BukkitTask task = scheduler().runTaskLaterAsynchronously(plugin(), runnable, delay);
		if (isTracked(name)) {
			tasks.put(name, task);
		}
	}

	@Override
	public void asyncTimer(String name, Runnable runnable, long delay, long repeat) {
		addTimer(name, scheduler().runTaskTimerAsynchronously(plugin(), runnable, delay, repeat));
	}

	private void addTimer(String name, BukkitTask timer) {
		if (!isTracked(name)) {
			return;
		}
		BukkitTask previous = timers.put(name, timer);
		if (previous != null) {
			previous.cancel();
		}
	}

	@Override
	public void cancelTask(String name) {
		BukkitTask task = tasks.remove(name);
		if (task != null) {
			task.cancel();
		}
	}

	@Override
	public void cancelTimer(String name) {
		BukkitTask timer = timers.remove(name);
		if (timer != null) {
			timer.cancel();
		}
	}

	@Override
	public boolean hasTask(String name) {
		BukkitTask task = tasks.get(name);
		if (task == null) {
			return false;
		}

		BukkitScheduler scheduler = scheduler();
		if (scheduler.isCurrentlyRunning(task.getTaskId()) || scheduler.isQueued(task.getTaskId())) {
			return true;
		}

		tasks.remove(name, task);
		return false;
	}

	@Override
	public List<String> getTimerNames() {
		return new ArrayList<String>(timers.keySet());
	}

	@Override
	public void stopAll() {
		scheduler().cancelTasks(plugin());
		tasks.clear();
		timers.clear();
	}
}
