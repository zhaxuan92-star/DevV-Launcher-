package com.example.ui

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Project
import com.example.data.ProjectRepository
import com.example.interpreter.DrawCommand
import com.example.interpreter.GeminiClient
import com.example.interpreter.PythonInterpreter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// Visual entity representing items in the drag-and-drop designer
data class VisualEntity(
    var id: String,
    var name: String,
    var type: String, // "rect", "circle", "text"
    var x: Float,
    var y: Float,
    var w: Float = 60f,
    var h: Float = 60f,
    var r: Float = 30f,
    var color: String = "WHITE",
    var text: String = ""
) {
    fun toJsonObject(): JSONObject {
        val obj = JSONObject()
        obj.put("id", id)
        obj.put("name", name)
        obj.put("type", type)
        obj.put("x", x.toDouble())
        obj.put("y", y.toDouble())
        obj.put("w", w.toDouble())
        obj.put("h", h.toDouble())
        obj.put("r", r.toDouble())
        obj.put("color", color)
        obj.put("text", text)
        return obj
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): VisualEntity {
            return VisualEntity(
                id = obj.optString("id", System.currentTimeMillis().toString()),
                name = obj.optString("name", "entity"),
                type = obj.optString("type", "rect"),
                x = obj.optDouble("x", 100.0).toFloat(),
                y = obj.optDouble("y", 100.0).toFloat(),
                w = obj.optDouble("w", 60.0).toFloat(),
                h = obj.optDouble("h", 60.0).toFloat(),
                r = obj.optDouble("r", 30.0).toFloat(),
                color = obj.optString("color", "WHITE"),
                text = obj.optString("text", "")
            )
        }
    }
}

class ProjectViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProjectRepository
    val allProjects: StateFlow<List<Project>>

    // Active project state
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    // Editor live states
    val editorCode = mutableStateOf("")
    val consoleLogs = mutableStateListOf<String>()
    val runtimeError = mutableStateOf<String?>(null)

    // Run / Runtime status
    val isRunning = mutableStateOf(false)
    val interpreter = PythonInterpreter()
    private var gameLoopJob: Job? = null

    // Visual Designer Entities
    val visualEntities = mutableStateListOf<VisualEntity>()

    // AI Assist states
    val aiResponse = mutableStateOf("")
    val aiLoading = mutableStateOf(false)

    // Selected visual entity for properties editor
    val selectedEntity = mutableStateOf<VisualEntity?>(null)

    // Audio tone generator for real in-game sound effects
    private var toneGenerator: ToneGenerator? = null

    init {
        val database = AppDatabase.getDatabase(application, viewModelScope)
        repository = ProjectRepository(database.projectDao())
        
        val projectsFlow = MutableStateFlow<List<Project>>(emptyList())
        allProjects = projectsFlow
        
        viewModelScope.launch {
            repository.allProjects.collect { list ->
                projectsFlow.value = list
            }
        }

        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
        } catch (e: Exception) {
            Log.e("ProjectViewModel", "Failed to init ToneGenerator: ${e.message}")
        }
    }

    fun selectProject(project: Project?) {
        _currentProject.value = project
        if (project == null) return
        editorCode.value = project.pythonCode
        loadVisualEntities(project.visualDesignJson)
        stopGame()
        interpreter.reset()
        runtimeError.value = null
        consoleLogs.clear()
        selectedEntity.value = null
        aiResponse.value = ""
    }

    fun createNewProject(name: String, description: String, isGame: Boolean) {
        val defaultCode = if (isGame) {
            """# Game mới của tôi
# Viết code game Python của bạn ở đây!

player_x = 170
player_y = 500
player_size = 40

def on_start():
    global player_x, player_y
    player_x = 170
    player_y = 500

def on_update():
    global player_x, player_y
    # Vẽ nền & nhân vật
    draw_text("GAME CỦA TÔI", 120, 80, 24, "CYAN")
    draw_rect(player_x, player_y, player_size, player_size, "GREEN")
    draw_text("Chạm màn hình để di chuyển!", 80, 560, 14, "GRAY")

def on_touch(tx, ty):
    global player_x
    # Trượt phi thuyền theo ngón tay
    player_x = tx - player_size / 2
"""
        } else {
            """# Chương trình Python mới
# Viết script của bạn ở đây!

print("Xin chào từ PyDev Studio!")
name = "Lập trình viên"
print("Chào mừng " + name + " đến với IDE Lập trình Mini.")

def on_start():
    print("Script chạy thành công!")
    draw_text("CHƯƠNG TRÌNH PYTHON", 60, 150, 24, "YELLOW")
    draw_text("Nhấn chạy để xem kết quả log bên dưới", 50, 200, 16, "WHITE")
"""
        }

        val defaultVisual = if (isGame) {
            """[{"id":"player","name":"player","type":"rect","x":170,"y":500,"w":40,"h":40,"color":"GREEN"}]"""
        } else {
            "[]"
        }

        viewModelScope.launch {
            val newProj = Project(
                name = name,
                description = description,
                pythonCode = defaultCode,
                visualDesignJson = defaultVisual
            )
            val newId = repository.insertProject(newProj)
            val insertedProj = newProj.copy(id = newId.toInt())
            selectProject(insertedProj)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProjectById(project.id)
            if (_currentProject.value?.id == project.id) {
                _currentProject.value = null
            }
        }
    }

    fun updateCurrentCode(code: String) {
        editorCode.value = code
        _currentProject.value?.let { proj ->
            _currentProject.value = proj.copy(pythonCode = code)
            viewModelScope.launch {
                repository.updateProject(proj.copy(pythonCode = code, lastModified = System.currentTimeMillis()))
            }
        }
    }

    // Load visual components from database JSON
    private fun loadVisualEntities(jsonStr: String) {
        visualEntities.clear()
        if (jsonStr.isEmpty() || jsonStr == "[]") return
        try {
            val arr = JSONArray(jsonStr)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                visualEntities.add(VisualEntity.fromJsonObject(obj))
            }
        } catch (e: Exception) {
            Log.e("ProjectViewModel", "Error parsing visual design json", e)
        }
    }

    // Save visual components to DB and synchronize python variable declarations
    fun saveVisualEntities() {
        val arr = JSONArray()
        for (entity in visualEntities) {
            arr.put(entity.toJsonObject())
        }
        val jsonStr = arr.toString()
        
        _currentProject.value?.let { proj ->
            _currentProject.value = proj.copy(visualDesignJson = jsonStr)
            viewModelScope.launch {
                repository.updateProject(proj.copy(visualDesignJson = jsonStr, lastModified = System.currentTimeMillis()))
            }
        }

        // Generate variables from designer to inject in code if they do not exist
        var code = editorCode.value
        var updated = false
        for (entity in visualEntities) {
            val nameX = "${entity.name}_x"
            val nameY = "${entity.name}_y"
            val nameW = "${entity.name}_w"
            val nameH = "${entity.name}_h"
            val nameR = "${entity.name}_r"

            if (entity.type == "rect") {
                if (!code.contains("$nameX =")) {
                    code = "$nameX = ${entity.x}\n$nameY = ${entity.y}\n$nameW = ${entity.w}\n$nameH = ${entity.h}\n" + code
                    updated = true
                }
            } else if (entity.type == "circle") {
                if (!code.contains("$nameX =")) {
                    code = "$nameX = ${entity.x}\n$nameY = ${entity.y}\n$nameR = ${entity.r}\n" + code
                    updated = true
                }
            }
        }
        if (updated) {
            updateCurrentCode(code)
        }
    }

    fun addVisualEntity(type: String) {
        val name = when (type) {
            "rect" -> "rect_${visualEntities.size + 1}"
            "circle" -> "circle_${visualEntities.size + 1}"
            else -> "text_${visualEntities.size + 1}"
        }
        val newEntity = VisualEntity(
            id = System.currentTimeMillis().toString(),
            name = name,
            type = type,
            x = 150f,
            y = 200f,
            w = if (type == "rect") 80f else 60f,
            h = if (type == "rect") 40f else 60f,
            r = 25f,
            color = "CYAN",
            text = if (type == "text") "Hello" else ""
        )
        visualEntities.add(newEntity)
        selectedEntity.value = newEntity
        saveVisualEntities()
    }

    fun updateEntityProperties(entity: VisualEntity, name: String, color: String, x: Float, y: Float, w: Float = 0f, h: Float = 0f, r: Float = 0f, text: String = "") {
        entity.name = name
        entity.color = color
        entity.x = x
        entity.y = y
        if (w > 0) entity.w = w
        if (h > 0) entity.h = h
        if (r > 0) entity.r = r
        if (text.isNotEmpty()) entity.text = text
        
        // Sync back to lists
        val index = visualEntities.indexOfFirst { it.id == entity.id }
        if (index != -1) {
            visualEntities[index] = entity
        }
        saveVisualEntities()
    }

    fun deleteVisualEntity(entity: VisualEntity) {
        visualEntities.removeIf { it.id == entity.id }
        if (selectedEntity.value?.id == entity.id) {
            selectedEntity.value = null
        }
        saveVisualEntities()
    }

    // Execute Python Script
    fun startGame() {
        stopGame()
        consoleLogs.clear()
        interpreter.reset()
        runtimeError.value = null
        
        // Load visual drawings initially if present
        for (entity in visualEntities) {
            if (entity.type == "rect") {
                interpreter.transientDrawings.add(DrawCommand.Rect(entity.x, entity.y, entity.w, entity.h, entity.color))
            } else if (entity.type == "circle") {
                interpreter.transientDrawings.add(DrawCommand.Circle(entity.x, entity.y, entity.r, entity.color))
            } else {
                interpreter.transientDrawings.add(DrawCommand.Text(entity.text, entity.x, entity.y, 16f, entity.color))
            }
        }

        // Load script
        interpreter.loadScript(editorCode.value)
        
        // Check for loading error
        if (interpreter.runtimeError.value != null) {
            runtimeError.value = interpreter.runtimeError.value
            return
        }

        isRunning.value = true
        
        // Call initial script setup
        interpreter.callFunction("on_start")
        syncLogsAndErrors()

        // 60 FPS Game Render Loop
        gameLoopJob = viewModelScope.launch(Dispatchers.Main) {
            while (isRunning.value) {
                delay(16) // ~60fps
                
                // Clear transient frame drawing buffer
                interpreter.transientDrawings.clear()
                
                // Call periodic update
                interpreter.callFunction("on_update")
                
                // Play sounds triggered inside script
                playToneTriggers()
                
                syncLogsAndErrors()
                
                if (interpreter.runtimeError.value != null) {
                    runtimeError.value = interpreter.runtimeError.value
                    isRunning.value = false
                    break
                }
            }
        }
    }

    private fun syncLogsAndErrors() {
        if (interpreter.consoleLogs.size != consoleLogs.size) {
            consoleLogs.clear()
            consoleLogs.addAll(interpreter.consoleLogs)
        }
        if (interpreter.runtimeError.value != null) {
            runtimeError.value = interpreter.runtimeError.value
        }
    }

    private fun playToneTriggers() {
        if (interpreter.soundTriggers.isNotEmpty()) {
            val sound = interpreter.soundTriggers.removeAt(0)
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    when (sound.lowercase()) {
                        "coin", "point" -> {
                            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_A, 100)
                            delay(80)
                            toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 100)
                        }
                        "hit", "explode" -> {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                        }
                        "wing", "jump" -> {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 80)
                        }
                        "die", "game_over" -> {
                            toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 350)
                        }
                        else -> {
                            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ProjectViewModel", "Tone play exception: ${e.message}")
                }
            }
        }
    }

    fun handleCanvasTouch(tx: Float, ty: Float) {
        if (isRunning.value) {
            interpreter.callFunction("on_touch", listOf(tx, ty))
            
            // Mirror coordinates dynamically to editor vars if game updates
            // E.g., if there's a variable corresponding to a visual entity,
            // let's update visual entities positions in real-time so dragging on code works too!
            for (entity in visualEntities) {
                val valX = interpreter.globals["${entity.name}_x"]
                val valY = interpreter.globals["${entity.name}_y"]
                if (valX is Number) entity.x = valX.toFloat()
                if (valY is Number) entity.y = valY.toFloat()
            }
        }
    }

    fun stopGame() {
        isRunning.value = false
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    // AI Features integration
    fun fixCodeWithAI() {
        aiLoading.value = true
        viewModelScope.launch {
            val response = GeminiClient.fixPythonCode(editorCode.value, runtimeError.value)
            aiResponse.value = response
            
            // Extract the python code block if present
            val extractedCode = extractPythonCode(response)
            if (extractedCode.isNotEmpty()) {
                updateCurrentCode(extractedCode)
                logConsole("AI: Đã tự động sửa lỗi và cập nhật mã nguồn thành công!")
                runtimeError.value = null
            }
            aiLoading.value = false
        }
    }

    fun askAI(prompt: String) {
        aiLoading.value = true
        viewModelScope.launch {
            val fullPrompt = "$prompt\nMã nguồn hiện tại:\n```python\n${editorCode.value}\n```"
            val response = GeminiClient.generateGameFromPrompt(fullPrompt)
            aiResponse.value = response
            
            val extractedCode = extractPythonCode(response)
            if (extractedCode.isNotEmpty()) {
                updateCurrentCode(extractedCode)
                logConsole("AI: Đã tải mã nguồn mới được tạo vào trình soạn thảo!")
            }
            aiLoading.value = false
        }
    }

    private fun extractPythonCode(response: String): String {
        val startTag = "```python"
        val endTag = "```"
        val startIdx = response.indexOf(startTag)
        if (startIdx != -1) {
            val sub = response.substring(startIdx + startTag.length)
            val endIdx = sub.indexOf(endTag)
            if (endIdx != -1) {
                return sub.substring(0, endIdx).trim()
            }
        }
        return ""
    }

    private fun logConsole(message: String) {
        consoleLogs.add(message)
    }

    fun playPreviewSound(soundName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                when (soundName.lowercase()) {
                    "coin", "point" -> {
                        toneGenerator?.startTone(ToneGenerator.TONE_DTMF_A, 100)
                        delay(80)
                        toneGenerator?.startTone(ToneGenerator.TONE_DTMF_9, 100)
                    }
                    "hit", "explode" -> {
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
                    }
                    "wing", "jump" -> {
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 80)
                    }
                    "die", "game_over" -> {
                        toneGenerator?.startTone(ToneGenerator.TONE_SUP_ERROR, 350)
                    }
                    else -> {
                        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 100)
                    }
                }
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "Tone play exception: ${e.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopGame()
        toneGenerator?.release()
    }
}
