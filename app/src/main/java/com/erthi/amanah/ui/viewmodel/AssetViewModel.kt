package com.erthi.amanah.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.AssetDao
import com.erthi.amanah.data.db.entity.AssetEntity
import com.erthi.amanah.data.db.entity.AssetType
import com.erthi.amanah.data.security.FieldCipher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssetViewModel @Inject constructor(
    private val assetDao: AssetDao,
    private val cipher: FieldCipher,
) : ViewModel() {

    val assets: StateFlow<List<AssetEntity>> = assetDao.getAllAssets()
        .map { list -> list.map(cipher::decrypt) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addAsset(name: String, type: AssetType, description: String, estimatedValue: Double?, location: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            assetDao.insertAsset(cipher.encrypt(
                AssetEntity(name = name, type = type, description = description, estimatedValue = estimatedValue, location = location)
            ))
        }
    }

    fun delete(asset: AssetEntity) {
        viewModelScope.launch { assetDao.deleteAsset(asset) }
    }
}
