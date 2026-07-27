package com.debayan.ainotebook.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.debayan.ainotebook.data.local.room.AiNotebookDatabase
import com.debayan.ainotebook.data.local.room.migration.DatabaseMigrations
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Validates the v1 → v2 migration (adds the `models` table) against the exported schemas.
 * Instrumented — runs on a device/emulator.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AiNotebookDatabase::class.java,
    )

    @Test
    fun migrate1To2_addsModelsTable() {
        helper.createDatabase(TEST_DB, 1).close()
        helper.runMigrationsAndValidate(TEST_DB, 2, true, DatabaseMigrations.MIGRATION_1_2)
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
