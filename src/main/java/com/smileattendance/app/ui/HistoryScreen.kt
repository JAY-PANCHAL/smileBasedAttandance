@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.smileattendance.app.ui

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEmotions
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.smileattendance.app.ui.theme.Success
import com.smileattendance.app.ui.theme.SuccessContainer
import com.smileattendance.app.ui.theme.Warning
import com.smileattendance.app.ui.theme.WarningContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                        enabled = records.isNotEmpty(),
                        onClick = {
                            val uri = AttendanceExporter.exportToCsv(context, records)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Export attendance"))
                        }
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Export to Excel/CSV")
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
        if (records.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.EventBusy,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No attendance records yet",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(records) { record: AttendanceRecord ->
                    AttendanceRow(record, dateFormat, photoByUserId[record.userId])
                }
            }
        }
    }
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
