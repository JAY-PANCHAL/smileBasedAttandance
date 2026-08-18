@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.smileattendance.app.ui

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterAltOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.smileattendance.app.data.AttendanceExporter
import com.smileattendance.app.db.AttendanceRecord
import com.smileattendance.app.db.AttendanceType
import com.smileattendance.app.db.EnrolledUser
import com.smileattendance.app.ui.theme.Success
import com.smileattendance.app.ui.theme.SuccessContainer
import com.smileattendance.app.ui.theme.Warning
import com.smileattendance.app.ui.theme.WarningContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun HistoryScreen(
    viewModel: AttendanceViewModel,
    onBack: () -> Unit
) {
    val records by viewModel.records.collectAsState()
    val users by viewModel.users.collectAsState()
    val photoByUserId = remember(users) { users.associate { it.id to it.referencePhotoPath } }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d · HH:mm:ss", Locale.getDefault()) }
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf<Long?>(null) }
    var selectedDayStartMillis by remember { mutableStateOf<Long?>(null) }
    var showPersonPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val filteredRecords = remember(records, searchQuery, selectedUserId, selectedDayStartMillis) {
        val query = searchQuery.trim()
        records.filter { record ->
            val matchesQuery = query.isEmpty() ||
                record.userName.contains(query, ignoreCase = true) ||
                record.userUniqueNumber.contains(query, ignoreCase = true)
            val matchesPerson = selectedUserId == null || record.userId == selectedUserId
            val matchesDate = selectedDayStartMillis == null || isSameDay(record.timestampMillis, selectedDayStartMillis!!)
            matchesQuery && matchesPerson && matchesDate
        }
    }

    val selectedUserName = users.firstOrNull { it.id == selectedUserId }?.name
    val shortDateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val selectedDateLabel = selectedDayStartMillis?.let { shortDateFormat.format(Date(it)) }
    val hasActiveFilters = searchQuery.isNotBlank() || selectedUserId != null || selectedDayStartMillis != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance History", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        enabled = filteredRecords.isNotEmpty(),
                        onClick = {
                            val uri = AttendanceExporter.exportToCsv(context, filteredRecords)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export attendance"))
                        }
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Export filtered results to CSV")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name or ID") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedUserId != null,
                        onClick = { showPersonPicker = true },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text(selectedUserName ?: "All people") }
                    )
                    FilterChip(
                        selected = selectedDayStartMillis != null,
                        onClick = { showDatePicker = true },
                        leadingIcon = { Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        label = { Text(selectedDateLabel ?: "All dates") }
                    )
                    if (hasActiveFilters) {
                        IconButton(onClick = {
                            searchQuery = ""
                            selectedUserId = null
                            selectedDayStartMillis = null
                        }) {
                            Icon(Icons.Filled.FilterAltOff, contentDescription = "Clear all filters")
                        }
                    }
                }

                Text(
                    "${filteredRecords.size} of ${records.size} record${if (records.size == 1) "" else "s"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (records.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState("No attendance records yet")
                }
            } else if (filteredRecords.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    EmptyState("No records match these filters")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRecords) { record: AttendanceRecord ->
                        AttendanceRow(record, dateFormat, photoByUserId[record.userId])
                    }
                }
            }
        }
    }

    if (showPersonPicker) {
        PersonPickerDialog(
            users = users,
            selectedUserId = selectedUserId,
            onSelect = { selectedUserId = it; showPersonPicker = false },
            onDismiss = { showPersonPicker = false }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDayStartMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedDayStartMillis = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    selectedDayStartMillis = null
                    showDatePicker = false
                }) { Text("Clear") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Filled.EventBusy,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun PersonPickerDialog(
    users: List<EnrolledUser>,
    selectedUserId: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(users, query) {
        if (query.isBlank()) users
        else users.filter { it.name.contains(query, true) || it.uniqueNumber.contains(query, true) }
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by person") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search people") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                LazyColumn(modifier = Modifier.height(320.dp).padding(top = 8.dp)) {
                    item {
                        PersonRow("All people", selectedUserId == null) { onSelect(null) }
                    }
                    items(filtered) { user ->
                        PersonRow("${user.name} · ${user.uniqueNumber}", selectedUserId == user.id) {
                            onSelect(user.id)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun PersonRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private val dayKeyFormatLocal = SimpleDateFormat("yyyy-MM-dd", Locale.US)
private val dayKeyFormatUtc = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
    timeZone = TimeZone.getTimeZone("UTC")
}

private fun isSameDay(recordTimestampMillis: Long, pickedDayStartMillisUtc: Long): Boolean {
    // Material3's DatePicker always reports the picked day as UTC midnight, regardless of the
    // device's timezone, while attendance timestamps are real local-time instants. Formatting
    // each side with the timezone it actually means, then comparing the resulting "yyyy-MM-dd"
    // strings, is what correctly matches the calendar day the admin clicked in the picker.
    val recordDayKey = dayKeyFormatLocal.format(Date(recordTimestampMillis))
    val pickedDayKey = dayKeyFormatUtc.format(Date(pickedDayStartMillisUtc))
    return recordDayKey == pickedDayKey
}

@Composable
private fun AttendanceRow(record: AttendanceRecord, dateFormat: SimpleDateFormat, photoPath: String?) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileThumbnail(photoPath)
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${record.userName} · ${record.userUniqueNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    TypeBadge(record.type)
                }
                Text(dateFormat.format(Date(record.timestampMillis)), style = MaterialTheme.typography.bodySmall)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.EmojiEmotions,
                        contentDescription = null,
                        tint = Success,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        " smile ${(record.smileProbability * 100).toInt()}% · match ${(record.matchConfidence * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = Success
                    )
                }
            }
        }
    }
}

@Composable
private fun TypeBadge(type: AttendanceType) {
    val (label, color, container) = if (type == AttendanceType.CHECK_IN)
        Triple("IN", Success, SuccessContainer) else Triple("OUT", Warning, WarningContainer)
    Surface(shape = RoundedCornerShape(8.dp), color = container) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun ProfileThumbnail(photoPath: String?) {
    val bitmap = remember(photoPath) {
        photoPath?.let { BitmapFactory.decodeFile(it)?.asImageBitmap() }
    }
    Surface(shape = CircleShape, color = SuccessContainer, modifier = Modifier.size(44.dp)) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Success)
            }
        }
    }
}
