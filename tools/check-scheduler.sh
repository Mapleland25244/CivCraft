#!/usr/bin/env bash
# R3: the Bukkit scheduler may only be used inside the threading package (BukkitTaskScheduler).
# Everything else must go through TaskMaster. Exits non-zero and lists the offending lines otherwise.
set -u
cd "$(dirname "$0")/.." || exit 2

hits=$(grep -rnE 'getScheduler\(\)|BukkitScheduler|BukkitRunnable|BukkitTask\b|\.runTask\w*\(|scheduleSync\w*\(|scheduleAsync\w*\(' \
	--include=*.java civcraft/src \
	| grep -v '^civcraft/src/com/avrgaming/civcraft/threading/BukkitTaskScheduler\.java:' \
	| grep -v '^civcraft/src/com/avrgaming/civcraft/util/BukkitObjects\.java:' \
	| grep -vE '^[^:]+:[0-9]+:\s*//')

if [ -n "$hits" ]; then
	echo "check-scheduler: Bukkit scheduler used outside threading/BukkitTaskScheduler (use TaskMaster):" >&2
	echo "$hits" >&2
	exit 1
fi
echo "check-scheduler: OK"
