package com.example.snapsort

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * SNAPSORT: Smart Media Organizer
 * Refined UI Version with premium aesthetics and improved UX.
 */

class MainActivity : ComponentActivity() {

    private var isProcessing = mutableStateOf(false)
    private var logEntries = mutableStateListOf<LogEntry>()
    private var processedCount = mutableStateOf(0)

    data class LogEntry(val id: String = UUID.randomUUID().toString(), val message: String, val type: LogType)
    enum class LogType { INFO, SUCCESS, ERROR }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SnapSortTheme {
                SnapSortScreen()
            }
        }
    }

    @Composable
    fun SnapSortScreen() {
        val gradient = Brush.verticalGradient(
            colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
        )

        Scaffold(
            bottomBar = {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        Button(
                            onClick = { checkPermissions() },
                            enabled = !isProcessing.value,
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            if (isProcessing.value) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Organizing Storage...")
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Select Folder & Organize", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Header Area with Gradient Background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(gradient)
                        .padding(24.dp),
                    contentAlignment = Alignment.BottomStart
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "SnapSort",
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text(
                            text = "Auto-organize your chaos into months",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
                    // Status Overview Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatusCard(
                            label = "Processed",
                            value = processedCount.value.toString(),
                            icon = Icons.Default.CheckCircle,
                            modifier = Modifier.weight(1f)
                        )
                        StatusCard(
                            label = "Status",
                            value = if (isProcessing.value) "Active" else "Idle",
                            icon = if (isProcessing.value) Icons.Default.PlayArrow else Icons.Default.Notifications,
                            modifier = Modifier.weight(1f),
                            highlight = isProcessing.value
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        "Live Activity Log",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Log Surface
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        if (logEntries.isEmpty()) {
                            EmptyState()
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                reverseLayout = true,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(logEntries, key = { it.id }) { log ->
                                    LogItem(log)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun StatusCard(label: String, value: String, icon: ImageVector, modifier: Modifier, highlight: Boolean = false) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = if (highlight) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    @Composable
    fun LogItem(log: LogEntry) {
        val color = when (log.type) {
            LogType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
            LogType.SUCCESS -> Color(0xFF2E7D32)
            LogType.ERROR -> MaterialTheme.colorScheme.error
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall,
                color = color,
                lineHeight = 16.sp
            )
        }
    }

    @Composable
    fun EmptyState() {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No activity yet.\nReady to organize!",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    // --- Core Logic ---

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.all { it }) pickFolder() else showToast("Permission required.")
    }

    private val folderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            contentResolver.takePersistableUriPermission(
                it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            runSort(it)
        }
    }

    private fun checkPermissions() {
        val perms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (perms.all { ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }) {
            pickFolder()
        } else {
            permissionLauncher.launch(perms)
        }
    }

    private fun pickFolder() = folderLauncher.launch(null)

    private fun runSort(rootUri: Uri) {
        isProcessing.value = true
        processedCount.value = 0
        logEntries.clear()
        addLog("Analyzing storage structure...", LogType.INFO)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val rootDoc = DocumentFile.fromTreeUri(this@MainActivity, rootUri) ?: return@launch
                processRecursive(rootDoc, rootDoc)
                withContext(Dispatchers.Main) {
                    addLog("Successfully organized all media!", LogType.SUCCESS)
                    isProcessing.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    addLog("Operation interrupted: ${e.localizedMessage}", LogType.ERROR)
                    isProcessing.value = false
                }
            }
        }
    }

    private suspend fun processRecursive(current: DocumentFile, root: DocumentFile) {
        val children = current.listFiles()
        val format = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        for (file in children) {
            if (file.isDirectory) {
                if (!isOrganizedFolder(file.name)) processRecursive(file, root)
                continue
            }

            val monthYear = format.format(Date(file.lastModified()))
            if (current.name == monthYear) continue

            withContext(Dispatchers.Main) {
                processedCount.value += 1
                addLog("Relocated: ${file.name} ➔ $monthYear", LogType.INFO)
            }

            moveFile(file, monthYear, root)
        }
    }

    private fun moveFile(file: DocumentFile, folderName: String, root: DocumentFile) {
        var target = root.findFile(folderName)
        if (target == null || !target.isDirectory) {
            target = root.createDirectory(folderName)
        }

        target?.let { targetDir ->
            try {
                val sourceParent = file.parentFile?.uri ?: return
                DocumentsContract.moveDocument(contentResolver, file.uri, sourceParent, targetDir.uri)
            } catch (e: Exception) { }
        }
    }

    private fun isOrganizedFolder(name: String?): Boolean {
        val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        return months.any { name?.contains(it, ignoreCase = true) == true }
    }

    private fun addLog(msg: String, type: LogType) {
        logEntries.add(LogEntry(message = msg, type = type))
    }

    private fun showToast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    @Composable
    fun SnapSortTheme(content: @Composable () -> Unit) {
        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = Color(0xFF673AB7),
                secondary = Color(0xFF00BFA5),
                tertiary = Color(0xFF512DA8),
                background = Color(0xFFF8F9FA),
                surface = Color.White,
                onSurface = Color(0xFF1C1B1F),
                surfaceVariant = Color(0xFFF1F0F4)
            ),
            content = content
        )
    }
}