package com.debayan.ainotebook.core.result

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppResultTest {

    @Test
    fun map_transformsSuccessValue() {
        val result: AppResult<Int> = AppResult.Success(2)
        assertEquals(AppResult.Success(4), result.map { it * 2 })
    }

    @Test
    fun map_preservesFailure() {
        val failure: AppResult<Int> = AppResult.Failure(AppError.Unknown("boom"))
        assertTrue(failure.map { it * 2 } is AppResult.Failure)
    }

    @Test
    fun getOrNull_returnsValueOnSuccessAndNullOnFailure() {
        assertEquals(5, AppResult.Success(5).getOrNull())
        assertNull((AppResult.Failure(AppError.Unknown()) as AppResult<Int>).getOrNull())
    }

    @Test
    fun getOrDefault_usesFallbackOnFailure() {
        assertEquals(9, (AppResult.Failure(AppError.Unknown()) as AppResult<Int>).getOrDefault(9))
    }

    @Test
    fun fold_selectsCorrectBranch() {
        val success: AppResult<Int> = AppResult.Success(3)
        assertEquals("ok:3", success.fold(onSuccess = { "ok:$it" }, onFailure = { "err" }))

        val failure: AppResult<Int> = AppResult.Failure(AppError.Validation("bad"))
        assertEquals("err", failure.fold(onSuccess = { "ok:$it" }, onFailure = { "err" }))
    }
}
