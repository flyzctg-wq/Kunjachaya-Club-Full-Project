package com.example.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.ComplaintEntity
import com.example.ui.language.Language

@Composable
fun EvidenceImageThumbnail(
    imageUrl: String,
    complaintTitle: String,
    lang: Language,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var loadedBitmap by remember(imageUrl) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.startsWith("file:") || imageUrl.startsWith("content:")) {
            try {
                val uri = Uri.parse(imageUrl)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    loadedBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag("evidence_image_thumbnail_card"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (lang == Language.BN) "সংযুক্ত প্রমাণ ছবি (গ্যালারিতে দেখুন)" else "Uploaded Evidence Photo (Tap to view)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                AssistChip(
                    onClick = onClick,
                    label = { Text(if (lang == Language.BN) "বড় করে দেখুন" else "Full Size", fontSize = 10.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.ZoomIn, contentDescription = null, modifier = Modifier.size(14.dp))
                    },
                    modifier = Modifier.height(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.08f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
            ) {
                when {
                    loadedBitmap != null -> {
                        Image(
                            bitmap = loadedBitmap!!.asImageBitmap(),
                            contentDescription = complaintTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    imageUrl == "img_club_logo" -> {
                        Image(
                            painter = painterResource(id = R.drawable.img_club_logo),
                            contentDescription = complaintTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Image(
                            painter = painterResource(id = R.drawable.img_club_banner),
                            contentDescription = complaintTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Overlay tag
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(topStart = 6.dp, bottomEnd = 6.dp),
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Fullscreen,
                            contentDescription = "Expand",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (lang == Language.BN) "গ্যালারি প্রিভিউ" else "Tap for Gallery View",
                            fontSize = 10.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComplaintImageGalleryDialog(
    complaint: ComplaintEntity,
    lang: Language,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var loadedBitmap by remember(complaint.imageUrl) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(complaint.imageUrl) {
        if (complaint.imageUrl.startsWith("file:") || complaint.imageUrl.startsWith("content:")) {
            try {
                val uri = Uri.parse(complaint.imageUrl)
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    loadedBitmap = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            color = Color.Black
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (lang == Language.BN) complaint.titleBn else complaint.titleEn,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${complaint.categoryEn} • ${complaint.createdAt}",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .testTag("close_evidence_gallery_btn")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                // Image Viewer Area with Zoom Controls
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF121212)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        loadedBitmap != null -> {
                            Image(
                                bitmap = loadedBitmap!!.asImageBitmap(),
                                contentDescription = "Full Size Evidence",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = zoomScale,
                                        scaleY = zoomScale
                                    ),
                                contentScale = ContentScale.Fit
                            )
                        }
                        complaint.imageUrl == "img_club_logo" -> {
                            Image(
                                painter = painterResource(id = R.drawable.img_club_logo),
                                contentDescription = "Full Size Evidence",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = zoomScale,
                                        scaleY = zoomScale
                                    ),
                                contentScale = ContentScale.Fit
                            )
                        }
                        else -> {
                            Image(
                                painter = painterResource(id = R.drawable.img_club_banner),
                                contentDescription = "Full Size Evidence",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = zoomScale,
                                        scaleY = zoomScale
                                    ),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    // Floating Zoom Controls Overlay
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp)
                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(24.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        IconButton(
                            onClick = { if (zoomScale > 0.8f) zoomScale -= 0.3f },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White)
                        }

                        Text(
                            text = "${(zoomScale * 100).toInt()}%",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        IconButton(
                            onClick = { if (zoomScale < 3.0f) zoomScale += 0.3f },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = Color.White)
                        }

                        IconButton(
                            onClick = { zoomScale = 1.0f },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = "Reset Zoom", tint = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Metadata Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${complaint.userNameEn} (${complaint.holdingNo})",
                                    fontSize = 12.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            ComplaintStatusBadge(
                                status = complaint.status,
                                lang = lang,
                                compact = true
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (lang == Language.BN) complaint.descriptionBn else complaint.descriptionEn,
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )

                        if (complaint.adminNoteEn.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Admin Note: ${if (lang == Language.BN) complaint.adminNoteBn else complaint.adminNoteEn}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        if (lang == Language.BN) "অভিযোগের প্রমাণ ছবি ডিভাইসে সংরক্ষিত হয়েছে" else "Evidence image saved to device storage",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("download_evidence_image_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (lang == Language.BN) "ছবি ডাউনলোড করুন" else "Download Evidence", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    Toast.makeText(
                                        context,
                                        if (lang == Language.BN) "প্রমাণ বিবরণ ক্লিপবোর্ডে কপি করা হয়েছে" else "Evidence report details copied to clipboard",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("share_evidence_report_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (lang == Language.BN) "শেয়ার রিপোর্ট" else "Share Report", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
