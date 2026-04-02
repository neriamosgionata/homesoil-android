package com.homesoil.app.ui.screens.scripts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.homesoil.app.data.models.Script
import com.homesoil.app.data.repository.HomesoilRepository
import com.homesoil.app.util.ScriptError
import com.homesoil.app.util.ScriptValidator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ScriptsViewModel(
    private val repository: HomesoilRepository
) : ViewModel() {

    val scripts: StateFlow<Map<Int, Script>> = repository.scripts

    init {
        repository.getAllScripts()
    }

    fun runScript(scriptId: Int) {
        repository.runScript(scriptId)
    }

    fun addScript(title: String, code: String) {
        repository.addScript(title, code)
    }

    fun modifyScript(scriptId: Int, title: String, code: String) {
        repository.modifyScript(scriptId, title, code)
    }

    fun removeScript(scriptId: Int) {
        repository.removeScript(scriptId)
    }

    fun addSchedule(scriptId: Int, schedule: String) {
        repository.addScriptSchedule(scriptId, schedule)
    }

    fun removeSchedule(scriptId: Int) {
        repository.removeScriptSchedule(scriptId)
    }
}

class ScriptEditorViewModel(
    private val repository: HomesoilRepository,
    private val scriptId: Int?
) : ViewModel() {

    private val existingScript: StateFlow<Script?> = repository.scripts.map { scripts ->
        scriptId?.let { scripts[it] }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title

    private val _code = MutableStateFlow("")
    val code: StateFlow<String> = _code

    val validationErrors: StateFlow<List<ScriptError>> = _code.map { code ->
        if (code.isBlank()) emptyList() else ScriptValidator.validate(code)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val hasErrors: StateFlow<Boolean> = validationErrors.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _schedule = MutableStateFlow("")
    val schedule: StateFlow<String> = _schedule

    val isNewScript = scriptId == null

    init {
        if (scriptId != null) {
            viewModelScope.launch {
                existingScript.filterNotNull().first().let { script ->
                    _title.value = script.title
                    _code.value = script.code
                    _schedule.value = script.schedule ?: ""
                }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun updateCode(newCode: String) {
        _code.value = newCode
    }

    fun updateSchedule(newSchedule: String) {
        _schedule.value = newSchedule
    }

    fun save(onSaved: () -> Unit) {
        if (_title.value.isBlank() || _code.value.isBlank()) return

        if (scriptId == null) {
            repository.addScript(_title.value, _code.value)
        } else {
            repository.modifyScript(scriptId, _title.value, _code.value)
        }
        onSaved()
    }

    fun saveSchedule() {
        scriptId?.let { id ->
            if (_schedule.value.isNotBlank()) {
                repository.addScriptSchedule(id, _schedule.value)
            } else {
                repository.removeScriptSchedule(id)
            }
        }
    }

    fun run() {
        scriptId?.let { repository.runScript(it) }
    }

    fun delete(onDeleted: () -> Unit) {
        scriptId?.let {
            repository.removeScript(it)
            onDeleted()
        }
    }
}
