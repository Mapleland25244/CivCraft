package com.avrgaming.civcraft.threading;

import java.util.List;

/**
 * Scheduling boundary. Business code reaches it through the static {@link TaskMaster} facade; only the
 * implementation (currently {@link BukkitTaskScheduler}) may touch the server's scheduler, so a later
 * server version only has to provide another implementation.
 *
 * Threading contract: "sync" runs on the main server thread, "async" runs on a worker thread and must not
 * touch the world, entities, inventories or other non-thread-safe server state directly.
 *
 * Names identify tasks for cancellation. An empty name means "anonymous": the task is not tracked and is
 * only cancelled by {@link #stopAll()}. Methods may be called from any thread.
 */
public interface TaskScheduler {

	void syncTask(Runnable runnable, long delay);

	/** Registers the timer under name; an existing timer with the same name is cancelled first. */
	void syncTimer(String name, Runnable runnable, long delay, long repeat);

	void asyncTask(String name, Runnable runnable, long delay);

	/** Registers the timer under name; an existing timer with the same name is cancelled first. */
	void asyncTimer(String name, Runnable runnable, long delay, long repeat);

	/** Cancels a tracked task that has not started yet. A run already in progress is not interrupted. */
	void cancelTask(String name);

	void cancelTimer(String name);

	/** True while the named task is queued or running. */
	boolean hasTask(String name);

	List<String> getTimerNames();

	/** Cancels every task and timer owned by the plugin, tracked or not. */
	void stopAll();
}
