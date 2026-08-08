package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FinancialRecordEntity
import com.example.ui.language.Language

data class MonthlyFinanceData(
    val monthLabelEn: String,
    val monthLabelBn: String,
    val duesCollected: Double,
    val maintenanceExpense: Double,
    val netSurplus: Double = duesCollected - maintenanceExpense
)

data class ExpenseCategoryData(
    val categoryEn: String,
    val categoryBn: String,
    val amount: Double,
    val percentage: Float,
    val color: Color
)

@Composable
fun VisualSpendingDashboard(
    financials: List<FinancialRecordEntity>,
    lang: Language,
    formatMoney: (Double) -> String
) {
    var selectedTimeframe by remember { mutableStateOf("6M") } // "3M", "6M", "YTD"
    var selectedMonthIndex by remember { mutableStateOf<Int?>(5) } // Default to latest month

    // Sample/Aggregated trend data for Committee Dashboard
    val monthlyDataList = remember(financials, selectedTimeframe) {
        val baseList = listOf(
            MonthlyFinanceData("Feb 26", "ফেব্রু ২৬", 42000.0, 28000.0),
            MonthlyFinanceData("Mar 26", "মার্চ ২৬", 48000.0, 31000.0),
            MonthlyFinanceData("Apr 26", "এপ্রিল ২৬", 45000.0, 35000.0),
            MonthlyFinanceData("May 26", "মে ২৬", 52000.0, 29000.0),
            MonthlyFinanceData("Jun 26", "জুন ২৬", 56000.0, 38000.0),
            MonthlyFinanceData("Jul 26", "জুলাই ২৬", 62000.0, 32000.0)
        )
        when (selectedTimeframe) {
            "3M" -> baseList.takeLast(3)
            "YTD" -> baseList
            else -> baseList
        }
    }

    val totalCollected = monthlyDataList.sumOf { it.duesCollected }
    val totalExpense = monthlyDataList.sumOf { it.maintenanceExpense }
    val netReserve = totalCollected - totalExpense
    val collectionRate = if (totalCollected > 0) ((totalCollected / (totalCollected + 15000.0)) * 100).toInt() else 85

    // Category breakdown
    val categories = remember {
        listOf(
            ExpenseCategoryData("Security & Wages", "নিরাপত্তা ও গার্ড বেতন", 45000.0, 0.38f, Color(0xFF1E88E5)),
            ExpenseCategoryData("Park & Mosque", "পার্ক ও মসজিদ সংস্কার", 32000.0, 0.27f, Color(0xFF43A047)),
            ExpenseCategoryData("Utilities & Lighting", "বিদ্যুৎ ও স্ট্রিটলাইট", 22000.0, 0.18f, Color(0xFFFB8C00)),
            ExpenseCategoryData("Waste Management", "বর্জ্য ব্যবস্থাপনা", 12000.0, 0.10f, Color(0xFF8E24AA)),
            ExpenseCategoryData("Repairs & Misc", "জরুরি মেরামত ও অন্যান্য", 8000.0, 0.07f, Color(0xFFE53935))
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("visual_spending_dashboard"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- HEADER & TIME HORIZON CHIPS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.QueryStats,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (lang == Language.BN) "কমিটি স্পেন্ডিং ও কালেকশন ড্যাশবোর্ড" else "Committee Spending & Dues Dashboard",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (lang == Language.BN) "চাঁদা আদায় বনাম ক্লাব রক্ষণাবেক্ষণ ব্যয় বিশ্লেষণ" else "Collection Trends vs Maintenance Expenses Analytics",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Time Range Selector
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (lang == Language.BN) "সময়কাল:" else "Period:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FilterChip(
                        selected = selectedTimeframe == "3M",
                        onClick = { selectedTimeframe = "3M"; selectedMonthIndex = 2 },
                        label = { Text("3 Months", fontSize = 11.sp) },
                        modifier = Modifier.testTag("period_3m_chip")
                    )
                    FilterChip(
                        selected = selectedTimeframe == "6M",
                        onClick = { selectedTimeframe = "6M"; selectedMonthIndex = 5 },
                        label = { Text("6 Months", fontSize = 11.sp) },
                        modifier = Modifier.testTag("period_6m_chip")
                    )
                    FilterChip(
                        selected = selectedTimeframe == "YTD",
                        onClick = { selectedTimeframe = "YTD"; selectedMonthIndex = 5 },
                        label = { Text("2026 YTD", fontSize = 11.sp) },
                        modifier = Modifier.testTag("period_ytd_chip")
                    )
                }
            }
        }

        // --- KEY FINANCIAL KPI CARDS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Net Fund Surplus Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_net_reserve_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (lang == Language.BN) "নিট ফান্ড উদ্বৃত্ত" else "Net Reserve Surplus",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatMoney(netReserve),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = if (lang == Language.BN) "+১৪.২% বৃদ্ধি" else "+14.2% Growth",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            }

            // Collection Efficiency Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_collection_rate_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (lang == Language.BN) "চাঁদা আদায় দক্ষতা" else "Collection Efficiency",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$collectionRate%",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (lang == Language.BN) "টার্গেট: ৯০% সাফল্য" else "Target: 90% Success",
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- VISUAL CHART 1: DUES COLLECTION VS MAINTENANCE EXPENSE BAR CHART ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("dues_vs_expenses_chart_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (lang == Language.BN) "মাসিক আদায় ও ব্যয় তুলনা" else "Monthly Dues vs. Expense Trend",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (lang == Language.BN) "বার চার্টে ক্লিক করে বিস্তারিত দেখুন" else "Tap on any month bar to inspect breakdown",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Legend
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFF2E7D32), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(if (lang == Language.BN) "আদায়" else "Dues", fontSize = 9.sp)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFE53935), RoundedCornerShape(2.dp)))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(if (lang == Language.BN) "ব্যয়" else "Expense", fontSize = 9.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Canvas Bar Chart Component
                val maxVal = 70000.0
                val chartHeight = 160.dp

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(chartHeight)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val width = size.width
                        val height = size.height
                        val itemCount = monthlyDataList.size
                        val sectionWidth = width / itemCount

                        // Grid lines
                        val gridLines = 4
                        for (i in 0..gridLines) {
                            val y = height * (i.toFloat() / gridLines)
                            drawLine(
                                color = Color.LightGray.copy(alpha = 0.4f),
                                start = Offset(0f, y),
                                end = Offset(width, y),
                                strokeWidth = 1f
                            )
                        }

                        // Render Bars for each month
                        monthlyDataList.forEachIndexed { index, item ->
                            val centerX = sectionWidth * index + sectionWidth / 2
                            val barWidth = (sectionWidth * 0.28f).coerceAtMost(28f)

                            val duesHeight = ((item.duesCollected / maxVal) * height).toFloat()
                            val expenseHeight = ((item.maintenanceExpense / maxVal) * height).toFloat()

                            val isSelected = selectedMonthIndex == index

                            // Dues Bar (Green)
                            val duesX = centerX - barWidth - 2f
                            val duesY = height - duesHeight
                            drawRoundRect(
                                color = if (isSelected) Color(0xFF2E7D32) else Color(0xFF4CAF50).copy(alpha = 0.85f),
                                topLeft = Offset(duesX, duesY),
                                size = Size(barWidth, duesHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                            )

                            // Expense Bar (Red)
                            val expX = centerX + 2f
                            val expY = height - expenseHeight
                            drawRoundRect(
                                color = if (isSelected) Color(0xFFC62828) else Color(0xFFEF5350).copy(alpha = 0.85f),
                                topLeft = Offset(expX, expY),
                                size = Size(barWidth, expenseHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                            )
                        }
                    }

                    // Clickable Overlay Row for months
                    Row(modifier = Modifier.fillMaxSize()) {
                        monthlyDataList.forEachIndexed { index, item ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { selectedMonthIndex = index },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                if (selectedMonthIndex == index) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .width(2.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // X-Axis Labels Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    monthlyDataList.forEachIndexed { index, item ->
                        val isSelected = selectedMonthIndex == index
                        Text(
                            text = if (lang == Language.BN) item.monthLabelBn else item.monthLabelEn,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .clickable { selectedMonthIndex = index }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }

                // Interactive Selected Month Inspection Card
                if (selectedMonthIndex != null && selectedMonthIndex!! in monthlyDataList.indices) {
                    val activeMonth = monthlyDataList[selectedMonthIndex!!]
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("selected_month_detail_box")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "📊 ${if (lang == Language.BN) activeMonth.monthLabelBn else activeMonth.monthLabelEn} (কমিটি অডিট)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${if (lang == Language.BN) "আদায়:" else "Collected:"} ${formatMoney(activeMonth.duesCollected)} • ${if (lang == Language.BN) "ব্যয়:" else "Expense:"} ${formatMoney(activeMonth.maintenanceExpense)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "Surplus: ${formatMoney(activeMonth.netSurplus)}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B5E20),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- VISUAL CHART 2: MAINTENANCE EXPENSE CATEGORY BREAKDOWN (DONUT / PIE) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("expense_category_breakdown_card"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (lang == Language.BN) "ক্লাব রক্ষণাবেক্ষণ ব্যয় বন্টন (ক্যাটেগরি)" else "Maintenance Expense Category Allocation",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (lang == Language.BN) "সর্বশেষ অডিট কমিটি অনুমোদিত বাজেট অনুপাতে" else "Based on latest Committee approved allocations",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Donut Chart Canvas
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            var startAngle = -90f
                            val strokeWidth = 22f

                            categories.forEach { cat ->
                                val sweepAngle = cat.percentage * 360f
                                drawArc(
                                    color = cat.color,
                                    startAngle = startAngle,
                                    sweepAngle = sweepAngle,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                                )
                                startAngle += sweepAngle
                            }
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "৳ 1.25L",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (lang == Language.BN) "মোট ব্যয়" else "Total",
                                fontSize = 8.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Categories Legend List
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.forEach { cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(cat.color, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (lang == Language.BN) cat.categoryBn else cat.categoryEn,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = "${(cat.percentage * 100).toInt()}%",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
