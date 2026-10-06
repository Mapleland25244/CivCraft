/*************************************************************************
 *
 * AVRGAMING LLC
 * __________________
 *
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.threading;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.avrgaming.civcraft.main.CivMessage;

/** Static entry point for scheduling; the work is done by the {@link TaskScheduler} behind it. */
public class TaskMaster {

	private static final TaskScheduler scheduler = new BukkitTaskScheduler();

	public static long getTicksTilDate(Date date) {
		Calendar c = Calendar.getInstance();

		if (c.getTime().after(date)) {
			return 0;
		}

		long timeInSeconds = (date.getTime() - c.getTime().getTime() ) / 1000;
		return timeInSeconds*20;
	}

	public static long getTicksToNextHour() {
		Calendar c = Calendar.getInstance();
		Date now = c.getTime();

		c.add(Calendar.HOUR_OF_DAY, 1);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);

		Date nextHour = c.getTime();

		long timeInSeconds = (nextHour.getTime() - now.getTime())/1000;
		return timeInSeconds*20;
	}

	public static void syncTask(Runnable runnable) {
		scheduler.syncTask(runnable, 0);
	}

	public static void syncTask(Runnable runnable, long l) {
		scheduler.syncTask(runnable, l);
	}

	public static void asyncTimer(String name, Runnable runnable,
			long delay, long repeat) {
		scheduler.asyncTimer(name, runnable, delay, repeat);
	}

	public static void asyncTimer(String name, Runnable runnable, long time) {
		scheduler.asyncTimer(name, runnable, time, time);
	}

	public static void asyncTask(String name, Runnable runnable, long delay) {
		scheduler.asyncTask(name, runnable, delay);
	}

	public static void asyncTask(Runnable runnable, long delay) {
		scheduler.asyncTask("", runnable, delay);
	}

	public static void syncTimer(String name, Runnable runnable, long time) {
		scheduler.syncTimer(name, runnable, time, time);
	}

	public static void syncTimer(String name, Runnable runnable, long delay, long repeat) {
		scheduler.syncTimer(name, runnable, delay, repeat);
	}

	public static void stopAll() {
		scheduler.stopAll();
	}

	public static void cancelTask(String name) {
		scheduler.cancelTask(name);
	}

	public static void cancelTimer(String name) {
		scheduler.cancelTimer(name);
	}

	public static boolean hasTask(String key) {
		return scheduler.hasTask(key);
	}

	public static List<String> getTimersList() {
		List<String> out = new ArrayList<String>();

		out.add(CivMessage.buildTitle("Timers Running"));
		for (String name : scheduler.getTimerNames()) {
			out.add("Timer: "+name+" running.");
		}

		return out;
	}

}
