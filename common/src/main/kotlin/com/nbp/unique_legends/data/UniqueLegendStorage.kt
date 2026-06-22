package com.nbp.unique_legends.data

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.nbp.unique_legends.UniqueLegends
import net.minecraft.server.MinecraftServer
import net.minecraft.world.level.storage.LevelResource
import java.io.Reader
import java.io.Writer
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object UniqueLegendStorage {
    private val gson: Gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    private var storagePath: Path? = null

    fun load(server: MinecraftServer) {
        val root = server.getWorldPath(LevelResource.ROOT)
        storagePath = root.resolve("data").resolve(UniqueLegends.MOD_ID).resolve("unique_legends.json")
        Files.createDirectories(path().parent)

        if (!Files.exists(path())) {
            UniqueLegendRegistry.replaceAll(emptyList())
            save()
            return
        }

        val file = try {
            Files.newBufferedReader(path()).use { reader: Reader ->
                gson.fromJson(reader, UniqueLegendStorageFile::class.java) ?: UniqueLegendStorageFile()
            }
        } catch (exception: Exception) {
            backupCorruptStorage()
            UniqueLegends.logger.error("Failed to load Unique Legends storage, starting with an empty registry.", exception)
            UniqueLegendStorageFile()
        }

        UniqueLegendRegistry.replaceAll(file.entries.values)
        save()
    }

    fun save() {
        val currentPath = storagePath ?: return
        Files.createDirectories(currentPath.parent)
        backupExistingStorage()

        val file = UniqueLegendStorageFile(
            entries = UniqueLegendRegistry.all(includeInactive = true).associateBy { it.speciesId }.toMutableMap()
        )

        Files.newBufferedWriter(currentPath).use { writer: Writer ->
            gson.toJson(file, writer)
        }
    }

    fun isLoaded(): Boolean = storagePath != null

    private fun path(): Path {
        return storagePath ?: error("Unique Legends storage path is not initialized.")
    }

    private fun backupExistingStorage() {
        val currentPath = storagePath ?: return
        if (!Files.exists(currentPath)) {
            return
        }

        runCatching {
            Files.copy(currentPath, currentPath.resolveSibling("unique_legends.json.bak"), StandardCopyOption.REPLACE_EXISTING)
        }
    }

    private fun backupCorruptStorage() {
        val currentPath = storagePath ?: return
        if (!Files.exists(currentPath)) {
            return
        }

        runCatching {
            val backupPath = currentPath.resolveSibling("unique_legends.json.corrupt-${System.currentTimeMillis()}")
            Files.copy(currentPath, backupPath, StandardCopyOption.REPLACE_EXISTING)
        }
    }
}

data class UniqueLegendStorageFile(
    val version: Int = 1,
    val entries: MutableMap<String, UniqueLegendEntry> = mutableMapOf()
)
