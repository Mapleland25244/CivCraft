#!/usr/bin/env bash
# R4: server internals (net.minecraft.server, org.bukkit.craftbukkit) may only be used inside the nms package.
# Everything else must go through com.avrgaming.civcraft.nms.Nms. Exits non-zero and lists the offending lines otherwise.
set -u
cd "$(dirname "$0")/.." || exit 2

hits=$(grep -rnE 'net\.minecraft\.server|org\.bukkit\.craftbukkit|\bv1_[0-9]+_R[0-9]+\b' \
	--include=*.java civcraft/src civcraft_dynmap \
	| grep -v '^civcraft/src/com/avrgaming/civcraft/nms/' \
	| grep -vE '^[^:]+:[0-9]+:\s*(//|\*|/\*)')

if [ -n "$hits" ]; then
	echo "check-nms: server internals used outside the nms package (add it to NmsAdapter instead):" >&2
	echo "$hits" >&2
	exit 1
fi
echo "check-nms: OK"
