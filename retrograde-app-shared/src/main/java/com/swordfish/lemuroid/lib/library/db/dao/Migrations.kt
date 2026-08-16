package com.swordfish.lemuroid.lib.library.db.dao

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object Migrations {
    val VERSION_9_10: Migration =
        object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `games` ADD COLUMN `isHidden` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `games` ADD COLUMN `timePlayed` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `games` ADD COLUMN `description` TEXT")
                database.execSQL("ALTER TABLE `games` ADD COLUMN `genre` TEXT")
                database.execSQL("ALTER TABLE `games` ADD COLUMN `year` TEXT")
                database.execSQL("ALTER TABLE `games` ADD COLUMN `rating` TEXT")
                database.execSQL("ALTER TABLE `games` ADD COLUMN `screenshotUrl` TEXT")
                database.execSQL("ALTER TABLE `games` ADD COLUMN `coreOverride` TEXT")
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_games_isHidden` ON `games` (`isHidden`)
                    """.trimIndent(),
                )
            }
        }

    val VERSION_8_9: Migration =
        object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `datafiles`(
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `gameId` INTEGER NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `fileUri` TEXT NOT NULL,
                        `lastIndexedAt` INTEGER NOT NULL,
                        `path` TEXT, FOREIGN KEY(`gameId`
                    ) REFERENCES `games`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )
                    """.trimIndent(),
                )

                database.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_datafiles_id` ON `datafiles` (`id`)
                    """.trimIndent(),
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_datafiles_fileUri` ON `datafiles` (`fileUri`)
                    """.trimIndent(),
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_datafiles_gameId` ON `datafiles` (`gameId`)
                    """.trimIndent(),
                )

                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS `index_datafiles_lastIndexedAt` ON `datafiles` (`lastIndexedAt`)
                    """.trimIndent(),
                )
            }
        }
}
