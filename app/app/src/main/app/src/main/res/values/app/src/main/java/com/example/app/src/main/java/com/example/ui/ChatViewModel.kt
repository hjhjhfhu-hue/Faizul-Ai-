package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.api.GeminiRepository
import com.example.data.db.AppDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.ChatSessionEntity
import com.example.util.LanguageDetector
import com.example.util.TextToSpeechHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val chatDao = db.chatDao()
    val repository = GeminiRepository(application)
    val ttsHelper = TextToSpeechHelper(application)

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val allSessions: StateFlow<List<ChatSessionEntity>> = combine(
        chatDao.getAllSessions(),
        _searchQuery
    ) { sessions, query ->
        if (query.isBlank()) {
            sessions
        } else {
            sessions.filter { it.title.contains(query, ignoreCase = true) }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _isImageMode = MutableStateFlow(false)
    val isImageMode: StateFlow<Boolean> = _isImageMode.asStateFlow()

    fun toggleImageMode() {
        _isImageMode.value = !_isImageMode.value
    }

    private val _selectedVoice = MutableStateFlow("Kore (Female - Warm)")
    val selectedVoice: StateFlow<String> = _selectedVoice.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow("guest@faizul.ai")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _accountType = MutableStateFlow("Free Guest Plan")
    val accountType: StateFlow<String> = _accountType.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _userName = MutableStateFlow("User")
    val userName: StateFlow<String> = _userName.asStateFlow()

    init {
        createNewChat()
    }

    fun setDarkMode(dark: Boolean) {
        _isDarkMode.value = dark
    }

    fun createNewChat(): String {
        val newSessionId = UUID.randomUUID().toString()
        _currentSessionId.value = newSessionId
        _messages.value = emptyList()

        viewModelScope.launch {
            val session = ChatSessionEntity(
                id = newSessionId,
                title = "New Conversation"
            )
            chatDao.insertSession(session)
            observeMessages(newSessionId)
        }
        return newSessionId
    }

    fun selectSession(sessionId: String) {
        _currentSessionId.value = sessionId
        observeMessages(sessionId)
    }

    private fun observeMessages(sessionId: String) {
        viewModelScope.launch {
            chatDao.getMessagesForSession(sessionId).collect { list ->
                _messages.value = list
            }
        }
    }

    fun sendMessage(userText: String, imageUri: Uri? = null) {
        val text = userText.trim()
        if (text.isEmpty() && imageUri == null) return
        if (_isGenerating.value) return

        var sessionId = _currentSessionId.value ?: createNewChat()

        viewModelScope.launch {
            _isGenerating.value = true
            val lang = LanguageDetector.detectLanguage(text).name.lowercase()

            val userMsg = ChatMessageEntity(
                sessionId = sessionId,
                sender = "user",
                text = if (text.isEmpty() && imageUri != null) "Uploaded Photo 📷" else text,
                language = lang,
                imageUrl = imageUri?.toString()
            )
            chatDao.insertMessage(userMsg)

            val historyMsgs = chatDao.getMessagesListForSession(sessionId)
            val historyTurns = historyMsgs.map { it.sender to it.text }

            val (aiResponseText, isOffline) = repository.generateResponse(text, historyTurns, null)

            val aiMsg = ChatMessageEntity(
                sessionId = sessionId,
                sender = "ai",
                text = aiResponseText,
                language = lang,
                isOffline = isOffline
            )
            chatDao.insertMessage(aiMsg)

            _isGenerating.value = false
        }
    }

    fun speakMessage(messageId: String, text: String) {
        ttsHelper.speak(messageId, text)
    }

    fun stopSpeaking() {
        ttsHelper.stop()
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
    }
}
