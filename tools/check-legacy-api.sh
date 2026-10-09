#!/usr/bin/env bash
# R2 ratchet: counts uses of legacy (pre-1.13) block/item APIs outside the ItemManager boundary.
# Usage: tools/check-legacy-api.sh [--list]
#   --list   print every offending line instead of only the per-category counts
# Exit code 1 when a category exceeds its entry in tools/legacy-api-baseline.txt (counts may only go down).
set -u
cd "$(dirname "$0")/.." || exit 2

SRC=civcraft/src
BASELINE=tools/legacy-api-baseline.txt
BOUNDARY='util/ItemManager.java|/compat/'

# name|regex  (extended regex, matched per line; comment-only lines are ignored)
CATEGORIES=(
	'raw-block-id|\.(getTypeId|setTypeId|setTypeIdAndData|getRawData)\('
	'raw-block-data|\.(getData|setData)\('
	'material-data|MaterialData|org\.bukkit\.material\.'
	'material-getid|(Material\.[A-Z_0-9]+|getType\(\)|getMaterial\(\)|getItemType\(\))\.getId\(\)'
	'durability-as-data|\.(getDurability|setDurability)\('
	'int-item-stack|new ItemStack\([^)]*(Id|id|ID)[,)]'
	'legacy-material-const|Material\.(CROPS|SIGN_POST|WALL_SIGN|STEP|DOUBLE_STEP|WOOD|WOOD_[A-Z]+|LOG_2|LEAVES|SKULL_ITEM|INK_SACK|MONSTER_EGG|RAW_[A-Z]+|GOLD_(BOOTS|CHESTPLATE|HELMET|LEGGINGS|PLATE|SWORD|PICKAXE|AXE|SPADE|HOE|BARDING|RECORD)|IRON_SPADE|DIAMOND_SPADE|STONE_SPADE|WATCH|EMPTY_MAP|BED_BLOCK|DOUBLE_PLANT|LONG_GRASS|SMOOTH_BRICK|SNOW_BLOCK|STAINED_[A-Z]+|HUGE_MUSHROOM_[0-9]|REDSTONE_TORCH_OFF|REDSTONE_TORCH_ON|SULPHUR|STATIONARY_[A-Z]+|SOIL|BURNING_FURNACE|MELON_BLOCK|THIN_GLASS|IRON_FENCE|FENCE|TRAP_DOOR|WOODEN_DOOR|IRON_DOOR_BLOCK|ENCHANTMENT_TABLE|MOB_SPAWNER|ENDER_PORTAL_FRAME|CLAY_BALL|CARROT_ITEM|POTATO_ITEM|LEASH|FIREWORK|SPECKLED_MELON|NETHER_BRICK|PORK|GRILLED_PORK|COOKED_[A-Z]+|COMMAND|PISTON_[A-Z_]+|HAY_BLOCK|DIODE[A-Z_]*)\b'
)

strip_comments() { grep -vE '^[^:]+:[0-9]+:[[:space:]]*(//|\*|/\*)'; }

# CivCraft's own block holders (SimpleBlock, BlockSnapshot) expose getType/getTypeId/getData with the
# same names as the Bukkit API; they are domain types, not legacy API. grep cannot see types, so
# receivers with these conventional names are skipped. Known limitation: a Bukkit object that happens
# to use one of these variable names would be missed.
domain_receivers() { grep -vE '(^|[^A-Za-z0-9_])(sb|bs|nextBs|nextBlock|commandBlock|block\.sb|blocks\[[^]]*\]\[[^]]*\]\[[^]]*\])\.(getTypeId|getData|getType)\('; }

list_hits() {
	grep -rnE --include=*.java "$1" "$SRC" 2>/dev/null \
		| grep -vE "$BOUNDARY" \
		| grep -v 'ItemManager\.' \
		| strip_comments \
		| domain_receivers
}

declare -A baseline
if [ -f "$BASELINE" ]; then
	while IFS='=' read -r k v; do
		v=${v%$'\r'}   # tolerate a CRLF checkout of the baseline
		[ -n "${k:-}" ] && baseline["$k"]="$v"
	done < "$BASELINE"
fi

fail=0
printf '%-24s %8s %9s\n' category count baseline
for entry in "${CATEGORIES[@]}"; do
	name=${entry%%|*}
	regex=${entry#*|}
	hits=$(list_hits "$regex" || true)
	count=$(printf '%s' "$hits" | grep -c . || true)
	base=${baseline[$name]:--}
	printf '%-24s %8s %9s\n' "$name" "$count" "$base"
	if [ "${1:-}" = "--list" ] && [ "$count" -gt 0 ]; then
		printf '%s\n' "$hits" | sed 's/^/    /'
	fi
	if [ "$base" != "-" ] && [ "$count" -gt "$base" ]; then
		echo "  ^ regression: $name grew from $base to $count" >&2
		fail=1
	elif [ "$base" != "-" ] && [ "$count" -lt "$base" ]; then
		echo "  note: $name improved ($base -> $count); lock it in with: tools/check-legacy-api.sh --write-baseline" >&2
	fi
done

if [ "${1:-}" = "--write-baseline" ]; then
	: > "$BASELINE"
	for entry in "${CATEGORIES[@]}"; do
		name=${entry%%|*}; regex=${entry#*|}
		c=$(list_hits "$regex" | grep -c . || true)
		echo "$name=$c" >> "$BASELINE"
	done
	echo "baseline written to $BASELINE"
fi
exit $fail
