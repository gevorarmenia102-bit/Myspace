package com.example.myspacepersonalhub.ui.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myspacepersonalhub.core.database.entity.NoteEntity
import com.example.myspacepersonalhub.data.repository.NoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NotesUiState(
    val notes: List<NoteEntity> = emptyList(),
    val filteredNotes: List<NoteEntity> = emptyList(),
    val trashNotes: List<NoteEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedTab: Int = 0, // 0: All Notes, 1: Journal, 2: Pinned, 3: Trash
    val selectedCategory: String? = null,
    val availableCategories: List<String> = emptyList(),
    val isLoading: Boolean = false
)

class NotesViewModel(private val noteRepository: NoteRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesUiState())
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            combine(
                noteRepository.getAllNotes(),
                noteRepository.getTrashNotes()
            ) { allNotes, trash ->
                val categories = allNotes.map { it.category }.distinct().sorted()
                Pair(allNotes, trash) to categories
            }.collect { (notesPair, categories) ->
                val (allNotes, trash) = notesPair
                _uiState.update { state ->
                    val filtered = applyFilter(allNotes, trash, state.searchQuery, state.selectedTab, state.selectedCategory)
                    state.copy(
                        notes = allNotes,
                        trashNotes = trash,
                        filteredNotes = filtered,
                        availableCategories = categories,
                        isLoading = false
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { state ->
            val filtered = applyFilter(state.notes, state.trashNotes, query, state.selectedTab, state.selectedCategory)
            state.copy(searchQuery = query, filteredNotes = filtered)
        }
    }

    fun onTabSelected(tabIndex: Int) {
        _uiState.update { state ->
            val filtered = applyFilter(state.notes, state.trashNotes, state.searchQuery, tabIndex, state.selectedCategory)
            state.copy(selectedTab = tabIndex, filteredNotes = filtered)
        }
    }

    fun onCategorySelected(category: String?) {
        _uiState.update { state ->
            val newCat = if (state.selectedCategory == category) null else category
            val filtered = applyFilter(state.notes, state.trashNotes, state.searchQuery, state.selectedTab, newCat)
            state.copy(selectedCategory = newCat, filteredNotes = filtered)
        }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.saveNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun toggleFavorite(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.saveNote(note.copy(isFavorite = !note.isFavorite))
        }
    }

    fun moveToTrash(id: String) {
        viewModelScope.launch {
            noteRepository.moveToTrash(id)
        }
    }

    fun restoreFromTrash(id: String) {
        viewModelScope.launch {
            noteRepository.restoreFromTrash(id)
        }
    }

    fun deletePermanently(note: NoteEntity) {
        viewModelScope.launch {
            noteRepository.deletePermanently(note)
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteRepository.emptyTrash()
        }
    }

    fun getNoteById(id: String): Flow<NoteEntity?> {
        return noteRepository.getNoteById(id)
    }

    fun saveNote(
        note: NoteEntity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = noteRepository.saveNote(note)
            if (result.isSuccess) {
                onSuccess()
            } else {
                onError(result.exceptionOrNull()?.message ?: "Save failed")
            }
        }
    }

    private fun applyFilter(
        allNotes: List<NoteEntity>,
        trashNotes: List<NoteEntity>,
        query: String,
        tab: Int,
        category: String?
    ): List<NoteEntity> {
        var baseList = when (tab) {
            0 -> allNotes // All
            1 -> allNotes.filter { it.isJournal } // Journal only
            2 -> allNotes.filter { it.isPinned || it.isFavorite } // Pinned / Favorites
            3 -> trashNotes // Trash
            else -> allNotes
        }

        if (category != null && tab != 3) {
            baseList = baseList.filter { it.category == category }
        }

        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            baseList = baseList.filter {
                it.title.lowercase().contains(q) ||
                it.body.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.tags.any { tag -> tag.lowercase().contains(q) }
            }
        }

        return baseList
    }
}
