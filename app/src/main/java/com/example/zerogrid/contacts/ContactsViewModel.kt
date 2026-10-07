package com.example.zerogrid.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.zerogrid.network.ContactDto
import com.example.zerogrid.network.ContactsRepository
import com.example.zerogrid.network.ContactsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ContactsUiState {
    object Idle : ContactsUiState()
    object Loading : ContactsUiState()
    data class Success(val contacts: List<ContactDto>) : ContactsUiState()
    data class Error(val message: String) : ContactsUiState()
}

sealed class AddContactState {
    object Idle : AddContactState()
    object Loading : AddContactState()
    data class Success(val contact: ContactDto) : AddContactState()
    data class Error(val message: String) : AddContactState()
}

class ContactsViewModel(
    private val repository: ContactsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ContactsUiState>(ContactsUiState.Idle)
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private val _addState = MutableStateFlow<AddContactState>(AddContactState.Idle)
    val addState: StateFlow<AddContactState> = _addState.asStateFlow()

    private val _contacts = MutableStateFlow<List<ContactDto>>(emptyList())
    val contacts: StateFlow<List<ContactDto>> = _contacts.asStateFlow()

    fun resetAddState() {
        _addState.value = AddContactState.Idle
    }

    fun loadContacts() {
        viewModelScope.launch {
            _uiState.value = ContactsUiState.Loading
            when (val result = repository.getContacts()) {
                is ContactsResult.Success -> {
                    _contacts.value = result.data
                    _uiState.value = ContactsUiState.Success(result.data)
                }
                is ContactsResult.Error -> {
                    _uiState.value = ContactsUiState.Error(result.message)
                }
            }
        }
    }

    fun addContact(emailOrPhone: String, label: String) {
        if (_addState.value is AddContactState.Loading) return
        viewModelScope.launch {
            _addState.value = AddContactState.Loading
            when (val result = repository.addContact(emailOrPhone, label)) {
                is ContactsResult.Success -> {
                    val updated = listOf(result.data) + _contacts.value.filterNot { it.id == result.data.id }
                    _contacts.value = updated
                    _uiState.value = ContactsUiState.Success(updated)
                    _addState.value = AddContactState.Success(result.data)
                }
                is ContactsResult.Error -> {
                    _addState.value = AddContactState.Error(result.message)
                }
            }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            when (val result = repository.deleteContact(contactId)) {
                is ContactsResult.Success -> {
                    val updated = _contacts.value.filterNot { it.id == contactId }
                    _contacts.value = updated
                    _uiState.value = ContactsUiState.Success(updated)
                }
                is ContactsResult.Error -> {
                    // Retain list and emit error
                    _uiState.value = ContactsUiState.Error(result.message)
                }
            }
        }
    }
}
