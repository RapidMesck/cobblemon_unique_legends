package com.nbp.unique_legends.config

data class UniqueLegendsConfig(
    val enabled: Boolean = true,
    val mode: String = "SPECIES",
    val trackedPokemons: MutableSet<String> = mutableSetOf(),
    val untrackedPokemons: MutableSet<String> = mutableSetOf(),
    val trackedLabels: MutableSet<String> = mutableSetOf("legendary", "mythical", "ultra_beast"),
    val untrackedLabels: MutableSet<String> = mutableSetOf(),
    val ignoreLockIfShiny: Boolean = false,
    val captureLimitPerSpecies: Int = 1,
    val releaseOnInactive: Boolean = true,
    val inactiveDaysToRelease: Int = 30,
    val checkInactiveOnServerStart: Boolean = true,
    val checkInactiveIntervalMinutes: Int = 60,
    val scanExistingPokemonOnServerStart: Boolean = true,
    val scanExistingPokemonOnlyWhenRegistryEmpty: Boolean = true,
    val messages: UniqueLegendsMessages = UniqueLegendsMessages(),
    val broadcastCapture: Boolean = true,
    val titleBroadcastCapture: Boolean = true,
    val broadcastReleaseByInactivity: Boolean = true,
    val debug: Boolean = false
)

data class UniqueLegendsMessages(
    val prefix: String = "&6[Unique Legends]&r ",
    val blockCapture: String = "&cThis unique Pokemon already belongs to &e{owner}&c.",
    val releasedByInactivity: String = "&eYour unique Pokemon &b{species}&e was released due to inactivity.",
    val broadcastCapture: String = "&6{player}&e captured the unique Pokemon &b{species}&e.",
    val captureTitle: String = "&6Unique Pokemon Captured",
    val captureSubtitle: String = "&e{player}&7 claimed &b{species}",
    val broadcastReleaseByInactivity: String = "&b{species}&e was released due to &6{owner}&e's inactivity.",
    val releaseUnlocked: String = "&a{species}&7 was removed from the unique list because &e{player}&7 released it.",
    val commandReload: String = "&aConfig and storage reloaded.",
    val commandListEmpty: String = "&7No species are currently locked.",
    val commandListEntry: String = "&b{species}&7 -> &e{owner}&7 (&8{pokemon_uuid}&7)",
    val commandInfoUnlocked: String = "&b{species}&7 is not locked.",
    val commandInfoEntry: String = "&b{species}&7 belongs to &e{owner}&7 (&8{owner_uuid}&7), Pokemon &8{pokemon_uuid}&7.",
    val commandUnlockMissing: String = "&b{species}&7 was not registered.",
    val commandUnlockSuccess: String = "&a{species}&7 was manually unlocked.",
    val commandCheckInactive: String = "&aCheck complete.&7 Released: &e{released}&7.",
    val commandScanSummary: String = "&aScan complete.&7 Players: &e{players}&7. Pokemon scanned: &e{scanned}&7. Registered: &e{registered}&7. Stale locks released: &e{stale_locks}&7. Owners updated: &e{ownership_updates}&7. Duplicates: &e{duplicates}&7.",
    val commandScanDuplicate: String = "&cDuplicate:&7 {duplicate}",
    val commandDebugStatus: String = "&7enabled=&e{enabled}&7, tracked=&e{tracked}&7, untracked=&e{untracked}&7, labels=&e{tracked_labels}&7, locks=&e{locks}&7"
)
