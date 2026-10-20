package com.example.myspacepersonalhub.ui.feature.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspacepersonalhub.core.database.entity.ContactEntity
import com.example.myspacepersonalhub.core.model.PhoneNumber
import com.example.myspacepersonalhub.core.util.ContactVCardUtils
import com.example.myspacepersonalhub.data.repository.ContactRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class ContactSortOrder {
    NAME_ASC,
    RECENTLY_UPDATED,
    RECENTLY_ADDED,
    FAVORITES_FIRST
}

data class ContactListUiState(
    val contacts: List<ContactEntity> = emptyList(),
    val filteredContacts: List<ContactEntity> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: ContactSortOrder = ContactSortOrder.NAME_ASC,
    val filterFavoritesOnly: Boolean = false,
    val selectedTag: String? = null,
    val availableTags: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ContactsViewModel(private val contactRepository: ContactRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactListUiState())
    val uiState: StateFlow<ContactListUiState> = _uiState.asStateFlow()

    init {
        loadContacts()
    }

    private fun loadContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            contactRepository.getAllContacts().collect { contactsList ->
                val allTags = contactsList.flatMap { it.tags }.distinct().sorted()
                _uiState.update { state ->
                    val filtered = applyFilterAndSort(
                        contacts = contactsList,
                        query = state.searchQuery,
                        sort = state.sortOrder,
                        favoritesOnly = state.filterFavoritesOnly,
                        tag = state.selectedTag
                    )
                    state.copy(
                        contacts = contactsList,
                        filteredContacts = filtered,
                        availableTags = allTags,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = applyFilterAndSort(
                contacts = state.contacts,
                query = query,
                sort = state.sortOrder,
                favoritesOnly = state.filterFavoritesOnly,
                tag = state.selectedTag
            )
            state.copy(searchQuery = query, filteredContacts = filtered)
        }
    }

    fun onSortOrderChange(sortOrder: ContactSortOrder) {
        _uiState.update { state ->
            val filtered = applyFilterAndSort(
                contacts = state.contacts,
                query = state.searchQuery,
                sort = sortOrder,
                favoritesOnly = state.filterFavoritesOnly,
                tag = state.selectedTag
            )
            state.copy(sortOrder = sortOrder, filteredContacts = filtered)
        }
    }

    fun toggleFavoritesFilter() {
        _uiState.update { state ->
            val newFav = !state.filterFavoritesOnly
            val filtered = applyFilterAndSort(
                contacts = state.contacts,
                query = state.searchQuery,
                sort = state.sortOrder,
                favoritesOnly = newFav,
                tag = state.selectedTag
            )
            state.copy(filterFavoritesOnly = newFav, filteredContacts = filtered)
        }
    }

    fun onSelectTag(tag: String?) {
        _uiState.update { state ->
            val newTag = if (state.selectedTag == tag) null else tag
            val filtered = applyFilterAndSort(
                contacts = state.contacts,
                query = state.searchQuery,
                sort = state.sortOrder,
                favoritesOnly = state.filterFavoritesOnly,
                tag = newTag
            )
            state.copy(selectedTag = newTag, filteredContacts = filtered)
        }
    }

    fun toggleFavorite(contact: ContactEntity) {
        viewModelScope.launch {
            contactRepository.saveContact(contact.copy(isFavorite = !contact.isFavorite))
        }
    }

    fun deleteContact(id: String) {
        viewModelScope.launch {
            contactRepository.deleteContact(id)
        }
    }

    fun getContactById(id: String): Flow<ContactEntity?> {
        return contactRepository.getContactById(id)
    }

    fun saveContact(
        contact: ContactEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = contactRepository.saveContact(contact)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Validation failed")
            }
        }
    }

    fun exportToVCard(): String {
        return ContactVCardUtils.exportToVCard(_uiState.value.contacts)
    }

    fun exportToJson(): String {
        return ContactVCardUtils.exportToJson(_uiState.value.contacts)
    }

    fun importFromJson(jsonString: String, onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            try {
                val list = ContactVCardUtils.importFromJson(jsonString)
                contactRepository.importContacts(list)
                onComplete(list.size)
            } catch (_: Exception) {
                onComplete(0)
            }
        }
    }

    private fun applyFilterAndSort(
        contacts: List<ContactEntity>,
        query: String,
        sort: ContactSortOrder,
        favoritesOnly: Boolean,
        tag: String?
    ): List<ContactEntity> {
        var result = contacts

        // Search Query
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            result = result.filter { c ->
                c.displayName.lowercase().contains(q) ||
                (c.nickname?.lowercase()?.contains(q) == true) ||
                (c.email?.lowercase()?.contains(q) == true) ||
                (c.company?.lowercase()?.contains(q) == true) ||
                (c.jobTitle?.lowercase()?.contains(q) == true) ||
                (c.personalNotes?.lowercase()?.contains(q) == true) ||
                c.phoneNumbers.any { it.number.contains(q) } ||
                c.tags.any { it.lowercase().contains(q) } ||
                (c.city?.lowercase()?.contains(q) == true)
            }
        }

        // Favorites filter
        if (favoritesOnly) {
            result = result.filter { it.isFavorite }
        }

        // Tag filter
        if (tag != null) {
            result = result.filter { it.tags.contains(tag) }
        }

        // Sort
        return when (sort) {
            ContactSortOrder.NAME_ASC -> result.sortedWith(
                compareByDescending<ContactEntity> { it.isPinned }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
            )
            ContactSortOrder.RECENTLY_UPDATED -> result.sortedByDescending { it.updatedAt }
            ContactSortOrder.RECENTLY_ADDED -> result.sortedByDescending { it.createdAt }
            ContactSortOrder.FAVORITES_FIRST -> result.sortedWith(
                compareByDescending<ContactEntity> { it.isFavorite }
                    .thenByDescending { it.isPinned }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayName }
            )
        }
    }
}
