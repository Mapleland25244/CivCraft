# local-libs

Reserved for jars that cannot come from Maven Central. Currently unused: the third-party plugin jars
(`Vault.jar`, `Herochat.jar`, `WorldBorder.jar`, `NoCheatPlus.jar`, `TagAPI.jar`, `TitleAPI-1.7.4.jar`,
`CustomMobs-4.17.jar`) are expected in `civcraft/lib/` and are git-ignored.

## Spigot / CraftBukkit / NMS (v1_12_R1)

The code imports `net.minecraft.server.v1_12_R1.*` and `org.bukkit.craftbukkit.v1_12_R1.*`, so the
API jar alone is not enough. Install both into the local Maven repo with BuildTools (needs JDK 8 and git):

    java -jar BuildTools.jar --rev 1.12.2

This publishes `org.spigotmc:spigot-api` and `org.spigotmc:spigot` `1.12.2-R0.1-SNAPSHOT` to `~/.m2`.

Build:

    mvn -pl civcraft -am package
