package com.erthi.amanah.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.erthi.amanah.data.db.dao.AssetDao
import com.erthi.amanah.data.db.entity.AssetEntity
import com.erthi.amanah.data.db.entity.AssetType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AssetViewModel @Inject constructor(
    private val assetDao: AssetDao,
) : ViewModel() {

    val assets: StateFlow<List<AssetEntity>> = assetDao.getAllAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addAsset(name: String, type: AssetType, description: String, estimatedValue: Double?, location: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            assetDao.insertAsset(AssetEntity(name = name, type = type, description = description, estimatedValue = estimatedValue, location = location))
        }
    }

    fun delete(asset: AssetEntity) {
        viewModelScope.launch { assetDao.deleteAsset(asset) }
    }
}
