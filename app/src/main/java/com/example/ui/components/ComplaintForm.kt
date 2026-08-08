package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.window.Dialog
import com.example.utils.CameraHelper
import com.example.ui.language.Language
import com.example.ui.viewmodel.ComplaintsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComplaintForm(
    complaintsViewModel: ComplaintsViewModel,
    currentUser: com.example.data.model.UserEntity? = null,
    lang: Language = Language.EN,
    onDismiss: () -> Unit = {},
    onSubmittedSuccessfully: () -> Unit = {}
) {
    val context = LocalContext.current
    val isSubmitting by complaintsViewModel.isSubmitting.collectAsState()

    var titleEn by remember { mutableStateOf("") }
    var titleBn by remember { mutableStateOf("") }
    var descEn by remember { mutableStateOf("") }
    var descBn by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Maintenance") }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var capturedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showSuccessBanner by remember { mutableStateOf(false) }
    var showCameraXViewfinder by remember { mutableStateOf(false) }

    val categories = listOf(
        "Maintenance" to "রক্ষণাবেক্ষণ",
        "Security" to "সিকিউরিটি",
        "Cleanliness" to "পরিচ্ছন্নতা",
        "Water & Electricity" to "পানি ও বিদ্যুৎ",
        "Noise Pollution" to "শব্দ দূষণ",
        "Other" to "অন্যান্য"
    )

    // Camera Capture Launcher (Bitmap)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            capturedPhotoUri = null
            Toast.makeText(
                context,
                if (lang == Language.BN) "ক্যামেরা ছবি সফলভাবে ধারণ করা হয়েছে" else "Camera photo captured successfully",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Camera Permission Launcher for quick snap
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch()
        } else {
            Toast.makeText(
                context,
                if (lang == Language.BN) "ক্যামেরা অনুমতি প্রয়োজন" else "Camera permission required to capture evidence",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // CameraX Permission Launcher
    val cameraXPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showCameraXViewfinder = true
        } else {
            Toast.makeText(
                context,
                if (lang == Language.BN) "ক্যামেরা লাইভ ভিউয়ের জন্য অনুমতি প্রয়োজন" else "Camera permission required for CameraX live view",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun triggerCameraCapture() {
        if (CameraHelper.hasCameraPermission(context)) {
            cameraLauncher.launch()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun triggerCameraXViewfinder() {
        if (CameraHelper.hasCameraPermission(context)) {
            showCameraXViewfinder = true
        } else {
            cameraXPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .testTag("complaint_form_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ReportProblem,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (lang == Language.BN) "অভিযোগ বার্তা জমা দিন (ফায়ারস্টোর)" else "Submit Complaint (Firestore)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Text(
                text = if (lang == Language.BN) "নতুন অভিযোগ প্রাথমিক 'Pending' স্ট্যাটাসে জমা হবে।" else "New complaint will be submitted with initial status 'Pending'.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(12.dp))

            // Category Chips
            Text(
                text = if (lang == Language.BN) "অভিযোগের ক্যাটাগরি" else "Complaint Category",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                items(categories) { (catEn, catBn) ->
                    val isSelected = selectedCategory == catEn
                    val label = if (lang == Language.BN) catBn else catEn
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategory = catEn },
                        label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null,
                        modifier = Modifier.testTag("complaint_category_$catEn")
                    )
                }
            }

            // Title Field (English)
            val isTitleValid = titleEn.trim().length >= 5
            val isTitleTouched = titleEn.isNotEmpty()

            OutlinedTextField(
                value = titleEn,
                onValueChange = { titleEn = it },
                label = { Text(if (lang == Language.BN) "শিরোনাম (English/বাংলা)" else "Complaint Title") },
                placeholder = { Text(if (lang == Language.BN) "যেমন: ৪ নম্বর রোডে স্ট্রিট লাইট নষ্ট" else "e.g., Street Light Damaged at Road 04") },
                isError = isTitleTouched && !isTitleValid,
                supportingText = {
                    if (isTitleTouched && !isTitleValid) {
                        Text(
                            text = if (lang == Language.BN) "শিরোনাম কমপক্ষে ৫ অক্ষরের হতে হবে" else "Title must be at least 5 characters long",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    } else if (isTitleValid) {
                        Text(
                            text = if (lang == Language.BN) "✓ সঠিক শিরোনাম" else "✓ Valid title length",
                            color = Color(0xFF2E7D32),
                            fontSize = 11.sp
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .testTag("complaint_title_field"),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            // Title Field (Bangla Optional)
            if (lang == Language.BN) {
                OutlinedTextField(
                    value = titleBn,
                    onValueChange = { titleBn = it },
                    label = { Text("শিরোনাম (বাংলা নির্দিষ্ট)") },
                    placeholder = { Text("বাংলায় শিরোনাম প্রদান করুন") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .testTag("complaint_title_bn_field"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Description Field
            val isDescValid = descEn.trim().length >= 10
            val isDescTouched = descEn.isNotEmpty()

            OutlinedTextField(
                value = descEn,
                onValueChange = { descEn = it },
                label = { Text(if (lang == Language.BN) "বিস্তারিত বিবরণ" else "Detailed Description") },
                placeholder = { Text(if (lang == Language.BN) "সমস্যার বিস্তারিত বিবরণ প্রদান করুন..." else "Explain the problem in detail...") },
                isError = isDescTouched && !isDescValid,
                supportingText = {
                    if (isDescTouched && !isDescValid) {
                        Text(
                            text = if (lang == Language.BN) "বিবরণ কমপক্ষে ১০ অক্ষরের হতে হবে" else "Description must be at least 10 characters long",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp
                        )
                    } else if (isDescValid) {
                        Text(
                            text = if (lang == Language.BN) "✓ সঠিক বিবরণ" else "✓ Valid description length",
                            color = Color(0xFF2E7D32),
                            fontSize = 11.sp
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .padding(vertical = 6.dp)
                    .testTag("complaint_desc_field"),
                maxLines = 4,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Camera Image Capture Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = RoundedCornerShape(12.dp)
                    ),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (lang == Language.BN) "ক্যামেরা প্রমাণ ছবি ধারণ (CameraX)" else "Evidence Image Capture (CameraX)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { triggerCameraXViewfinder() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("camerax_open_viewfinder_btn")
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lang == Language.BN) "ক্যামেরাএক্স ভিউ" else "CameraX View",
                                fontSize = 11.sp
                            )
                        }

                        OutlinedButton(
                            onClick = { triggerCameraCapture() },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("camera_capture_button")
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (capturedBitmap != null) {
                                    if (lang == Language.BN) "পুনরায় ছবি তুলুন" else "Retake Photo"
                                } else {
                                    if (lang == Language.BN) "কুইক স্ন্যাপ" else "Quick Snap"
                                },
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Captured Image Preview
                    if (capturedBitmap != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.1f))
                        ) {
                            Image(
                                bitmap = capturedBitmap!!.asImageBitmap(),
                                contentDescription = "Captured Evidence Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Clear captured photo button
                            IconButton(
                                onClick = { capturedBitmap = null },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                    .size(32.dp)
                                    .testTag("clear_captured_photo")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (lang == Language.BN) "কোন ছবি তোলা হয়নি (অপশনাল)" else "No camera image captured yet (Optional)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Success Confirmation Banner
            AnimatedVisibility(visible = showSuccessBanner) {
                Surface(
                    color = Color(0xFF2E7D32).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (lang == Language.BN) "অভিযোগ সফলভাবে 'Pending' স্ট্যাটাসে ফায়ারস্টোরে জমা হয়েছে!" else "Complaint successfully pushed to Firestore with status 'Pending'!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }

            // Submit Button
            val isFormValid = isTitleValid && isDescValid

            Button(
                onClick = {
                    if (!isTitleValid) {
                        Toast.makeText(
                            context,
                            if (lang == Language.BN) "শিরোনাম কমপক্ষে ৫ অক্ষরের হতে হবে" else "Title must be at least 5 characters long",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }
                    if (!isDescValid) {
                        Toast.makeText(
                            context,
                            if (lang == Language.BN) "বিবরণ কমপক্ষে ১০ অক্ষরের হতে হবে" else "Description must be at least 10 characters long",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@Button
                    }

                    val catBn = categories.find { it.first == selectedCategory }?.second ?: selectedCategory

                    val savedImageUrl = if (capturedBitmap != null) {
                        try {
                            val photoFile = CameraHelper.createTempImageFile(context)
                            val fos = java.io.FileOutputStream(photoFile)
                            capturedBitmap!!.compress(Bitmap.CompressFormat.JPEG, 90, fos)
                            fos.flush()
                            fos.close()
                            photoFile.toURI().toString()
                        } catch (e: Exception) {
                            "img_club_banner"
                        }
                    } else if (capturedPhotoUri != null) {
                        capturedPhotoUri.toString()
                    } else ""

                    // Submit to Firestore with status 'Pending'
                    complaintsViewModel.submitComplaint(
                        titleEn = titleEn,
                        titleBn = if (titleBn.isNotBlank()) titleBn else titleEn,
                        categoryEn = selectedCategory,
                        categoryBn = catBn,
                        descEn = descEn,
                        descBn = if (descBn.isNotBlank()) descBn else descEn,
                        imageUrl = savedImageUrl,
                        userNameEn = currentUser?.nameEn ?: "Resident Member",
                        userNameBn = currentUser?.nameBn ?: "আবাসিক নিবাসী",
                        holdingNo = currentUser?.holding ?: "Apt 4B"
                    )

                    showSuccessBanner = true
                    Toast.makeText(
                        context,
                        if (lang == Language.BN) "অভিযোগ সফলভাবে জমা হয়েছে" else "Complaint submitted to Firestore successfully",
                        Toast.LENGTH_LONG
                    ).show()

                    onSubmittedSuccessfully()
                },
                enabled = !isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_complaint_form_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                } else {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (lang == Language.BN) "ফায়ারস্টোরে অভিযোগ জমা দিন" else "Push Complaint to Firestore",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        if (showCameraXViewfinder) {
            CameraXViewfinderDialog(
                lang = lang,
                onDismiss = { showCameraXViewfinder = false },
                onPhotoCaptured = { bitmap ->
                    capturedBitmap = bitmap
                    capturedPhotoUri = null
                    Toast.makeText(
                        context,
                        if (lang == Language.BN) "ক্যামেরাএক্স প্রমাণ ছবি ধারণ করা হয়েছে" else "CameraX evidence photo captured!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }
}

@Composable
fun CameraXViewfinderDialog(
    lang: Language,
    onDismiss: () -> Unit,
    onPhotoCaptured: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { ContextCompat.getMainExecutor(context) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lang == Language.BN) "ক্যামেরাএক্স লাইভ ভিউফ্যাইন্ডার" else "CameraX Live Viewfinder",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { previewView ->
                                previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
                                CameraHelper.startCamera(
                                    context = ctx,
                                    lifecycleOwner = lifecycleOwner,
                                    previewView = previewView,
                                    onCameraReady = { capture ->
                                        imageCapture = capture
                                    }
                                )
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (lang == Language.BN) "বাতিল" else "Cancel")
                    }

                    Button(
                        onClick = {
                            val capture = imageCapture
                            if (capture != null && !isCapturing) {
                                isCapturing = true
                                CameraHelper.takePhotoAsBitmap(
                                    imageCapture = capture,
                                    executor = executor,
                                    onBitmapCaptured = { bitmap ->
                                        isCapturing = false
                                        onPhotoCaptured(bitmap)
                                        onDismiss()
                                    },
                                    onError = { exc ->
                                        isCapturing = false
                                        Toast.makeText(context, "CameraX Error: ${exc.message}", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        },
                        enabled = imageCapture != null && !isCapturing,
                        modifier = Modifier.weight(1f).testTag("camerax_take_photo_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Camera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (lang == Language.BN) "ছবি তুলুন" else "Snap Photo")
                    }
                }
            }
        }
    }
}
