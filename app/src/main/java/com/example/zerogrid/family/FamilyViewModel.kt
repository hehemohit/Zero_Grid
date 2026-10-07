package com.example.zerogrid.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zerogrid.network.ChildLocationDto
import com.example.zerogrid.network.FamilyLinkDto
import com.example.zerogrid.network.FamilyRepository
import com.example.zerogrid.network.FamilyResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FamilyUiState {
    object Idle : FamilyUiState()
    object Loading : FamilyUiState()
    data class Success(val message: String? = null) : FamilyUiState()
    data class Error(val message: String) : FamilyUiState()
}

class FamilyViewModel(
    private val repository: FamilyRepository = FamilyRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<FamilyUiState>(FamilyUiState.Idle)
    val uiState: StateFlow<FamilyUiState> = _uiState.asStateFlow()

    private val _links = MutableStateFlow<List<FamilyLinkDto>>(emptyList())
    val links: StateFlow<List<FamilyLinkDto>> = _links.asStateFlow()

    private val _childLocation = MutableStateFlow<ChildLocationDto?>(null)
    val childLocation: StateFlow<ChildLocationDto?> = _childLocation.asStateFlow()

    fun resetState() {
        _uiState.value = FamilyUiState.Idle
    }

    fun loadLinks() {
        viewModelScope.launch {
            _uiState.value = FamilyUiState.Loading
            when (val result = repository.getFamilyLinks()) {
                is FamilyResult.Success -> {
                    _links.value = result.data
                    _uiState.value = FamilyUiState.Success()
                }
                is FamilyResult.Error -> {
                    _uiState.value = FamilyUiState.Error(result.message)
                }
            }
        }
    }

    fun requestLink(childEmail: String) {
        if (_uiState.value is FamilyUiState.Loading) return
        viewModelScope.launch {
            _uiState.value = FamilyUiState.Loading
            when (val result = repository.requestLink(childEmail.trim())) {
                is FamilyResult.Success -> {
                    result.data?.let { newLink ->
                        _links.value = listOf(newLink) + _links.value.filterNot { it.id == newLink.id }
                    }
                    _uiState.value = FamilyUiState.Success("Link request sent to $childEmail")
                }
                is FamilyResult.Error -> {
                    _uiState.value = FamilyUiState.Error(result.message)
                }
            }
        }
    }

    fun acceptLink(linkId: String) {
        viewModelScope.launch {
            when (val result = repository.acceptLink(linkId)) {
                is FamilyResult.Success -> {
                    _links.value = _links.value.map {
                        if (it.id == linkId) it.copy(status = "ACCEPTED") else it
                    }
                    _uiState.value = FamilyUiState.Success("Family link accepted")
                }
                is FamilyResult.Error -> {
                    _uiState.value = FamilyUiState.Error(result.message)
                }
            }
        }
    }

    fun revokeLink(linkId: String) {
        viewModelScope.launch {
            when (val result = repository.revokeLink(linkId)) {
                is FamilyResult.Success -> {
                    _links.value = _links.value.filterNot { it.id == linkId }
                    _uiState.value = FamilyUiState.Success("Family link revoked")
                }
                is FamilyResult.Error -> {
                    _uiState.value = FamilyUiState.Error(result.message)
                }
            }
        }
    }

    fun fetchChildLocation(childId: String) {
        viewModelScope.launch {
            when (val result = repository.getChildLocation(childId)) {
                is FamilyResult.Success -> {
                    _childLocation.value = result.data
                }
                is FamilyResult.Error -> {
                    _uiState.value = FamilyUiState.Error(result.message)
                }
            }
        }
    }
}
