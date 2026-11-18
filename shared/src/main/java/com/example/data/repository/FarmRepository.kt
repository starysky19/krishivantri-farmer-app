package com.example.data.repository

import com.example.data.local.KrishivantriDao
import com.example.data.model.FarmBoundaryEntity
import com.example.data.model.LatLngPoint
import com.example.data.remote.SupabaseClient
import com.example.data.remote.SupabaseCoordDto
import com.example.data.remote.SupabaseFarmBoundaryDto
import com.example.domain.GeoPolygonCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class FarmRepository(private val dao: KrishivantriDao) {

    fun observeFarmerBoundaries(farmerId: String): Flow<List<FarmBoundaryEntity>> =
        dao.observeFarmBoundariesForFarmer(farmerId)

    fun observeAllBoundaries(): Flow<List<FarmBoundaryEntity>> =
        dao.observeAllFarmBoundaries()

    /**
     * Automatically calculates area in both acres and hectares, persists locally in Room,
     * and saves to Supabase farm_boundaries table when configured.
     */
    suspend fun saveFarmBoundary(
        farmerId: String,
        farmName: String,
        polygonPoints: List<LatLngPoint>
    ): FarmBoundaryEntity = withContext(Dispatchers.IO) {
        val acres = GeoPolygonCalculator.calculatePolygonAreaAcres(polygonPoints)
        val hectares = GeoPolygonCalculator.calculatePolygonAreaHectares(polygonPoints)
        val center = GeoPolygonCalculator.calculatePolygonCenter(polygonPoints)
            ?: polygonPoints.firstOrNull()
            ?: LatLngPoint(20.0059, 73.7897)

        val entity = FarmBoundaryEntity(
            id = UUID.randomUUID().toString(),
            farmerId = farmerId,
            farmName = farmName.trim().ifBlank { "Farm Plot" },
            polygonCoordinatesJson = GeoPolygonCalculator.encodePolygon(polygonPoints),
            areaAcres = acres,
            areaHectares = hectares,
            centerLat = center.lat,
            centerLng = center.lng,
            isSyncedToSupabase = false
        )

        dao.insertFarmBoundary(entity)

        val synced = pushBoundaryToSupabase(entity, polygonPoints)
        if (synced) {
            val updated = entity.copy(isSyncedToSupabase = true)
            dao.updateFarmBoundary(updated)
            updated
        } else {
            entity
        }
    }

    suspend fun deleteFarmBoundary(boundaryId: String) = withContext(Dispatchers.IO) {
        dao.deleteFarmBoundary(boundaryId)
    }

    suspend fun syncWithSupabase(farmerId: String): Int = withContext(Dispatchers.IO) {
        val api = SupabaseClient.api ?: return@withContext 0
        var syncedCount = 0

        val unsynced = dao.getUnsyncedFarmBoundaries(farmerId)
        for (item in unsynced) {
            val pts = GeoPolygonCalculator.decodePolygon(item.polygonCoordinatesJson)
            if (pushBoundaryToSupabase(item, pts)) {
                dao.updateFarmBoundary(item.copy(isSyncedToSupabase = true))
                syncedCount++
            }
        }

        // Pull remote boundaries for this farmer from Supabase
        runCatching {
            val resp = api.getFarmBoundariesForFarmer("eq.$farmerId")
            if (resp.isSuccessful) {
                resp.body()?.forEach { dto ->
                    val coords = dto.polygon_coordinates.map { LatLngPoint(it.lat, it.lng) }
                    dao.insertFarmBoundary(
                        FarmBoundaryEntity(
                            id = dto.id,
                            farmerId = dto.farmer_id,
                            farmName = dto.farm_name,
                            polygonCoordinatesJson = GeoPolygonCalculator.encodePolygon(coords),
                            areaAcres = dto.area_acres,
                            areaHectares = dto.area_hectares,
                            centerLat = dto.center_lat,
                            centerLng = dto.center_lng,
                            isSyncedToSupabase = true
                        )
                    )
                }
            }
        }
        syncedCount
    }

    private suspend fun pushBoundaryToSupabase(
        entity: FarmBoundaryEntity,
        points: List<LatLngPoint>
    ): Boolean {
        val api = SupabaseClient.api ?: return false
        return runCatching {
            val dto = SupabaseFarmBoundaryDto(
                id = entity.id,
                farmer_id = entity.farmerId,
                farm_name = entity.farmName,
                polygon_coordinates = points.map { SupabaseCoordDto(it.lat, it.lng) },
                area_acres = entity.areaAcres,
                area_hectares = entity.areaHectares,
                center_lat = entity.centerLat,
                center_lng = entity.centerLng
            )
            api.upsertFarmBoundary(dto).isSuccessful
        }.getOrDefault(false)
    }
}
