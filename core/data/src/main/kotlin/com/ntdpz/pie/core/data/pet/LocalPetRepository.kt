package com.ntdpz.pie.core.data.pet

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ntdpz.pie.core.domain.pet.PetPosition
import com.ntdpz.pie.core.domain.pet.PetPreferences
import com.ntdpz.pie.core.domain.pet.PetRepository
import kotlinx.coroutines.flow.map

private val Context.companionPreferences by preferencesDataStore("pie_companion")

class LocalPetRepository(private val store: DataStore<Preferences>) : PetRepository {
    constructor(context: Context) : this(context.applicationContext.companionPreferences)

    private val enabled = booleanPreferencesKey("enabled")
    private val reducedMotion = booleanPreferencesKey("reduced_motion")
    private val positionX = floatPreferencesKey("position_x")
    private val positionY = floatPreferencesKey("position_y")

    override val preferences = store.data.map { values ->
        PetPreferences(
            enabled = values[enabled] ?: true,
            reducedMotion = values[reducedMotion] ?: false,
            position = PetPosition(
                coordinate(values[positionX], PetPosition().x),
                coordinate(values[positionY], PetPosition().y),
            ),
        )
    }

    override suspend fun setEnabled(enabled: Boolean) {
        store.edit { it[this.enabled] = enabled }
    }

    override suspend fun setReducedMotion(enabled: Boolean) {
        store.edit { it[this.reducedMotion] = enabled }
    }

    override suspend fun setPosition(position: PetPosition) {
        store.edit {
            it[positionX] = position.x
            it[positionY] = position.y
        }
    }

    private fun coordinate(value: Float?, fallback: Float): Float =
        value?.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: fallback
}
