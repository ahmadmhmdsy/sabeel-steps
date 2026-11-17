package com.sabeel.steps.data.repo

import com.sabeel.steps.data.db.WalkDao
import com.sabeel.steps.data.db.toDomain
import com.sabeel.steps.data.db.toEntity
import com.sabeel.steps.domain.Walk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WalkRepository(private val dao: WalkDao) {
    fun walksFlow(): Flow<List<Walk>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    suspend fun get(id: Long): Walk? = dao.get(id)?.toDomain()

    suspend fun save(walk: Walk): Long = dao.insert(walk.toEntity())

    suspend fun clearAll() = dao.clear()
}