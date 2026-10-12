package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantState
import com.example.ui.theme.NothingBlack
import com.example.ui.theme.NothingCardBg
import com.example.ui.theme.NothingCardBorder
import com.example.ui.theme.NothingGreyDark
import com.example.ui.theme.NothingGreyLight
import com.example.ui.theme.NothingGreyMuted
import com.example.ui.theme.NothingRed
import com.example.ui.theme.NothingWhite
import com.example.ui.theme.StatusSuccess

enum class NavTab(val title: String) {
    ASSISTANT("EMILY"),
    PERMISSIONS("ACCESS"),
    SETTINGS("CONFIG")
}

@Composable
fun NothingTopBar(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    assistantState: AssistantState,
    isServiceActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(NothingBlack)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header Row with Time/Branding & Privacy Dot
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Signature Nothing OS Red Dot Indicator
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isServiceActive) NothingRed else NothingGreyDark)
                        .testTag("privacy_mic_indicator")
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EMILY // AI-JARVIS",
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite,
                    letterSpacing = 1.2.sp
                )
            }

            // Status chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(NothingCardBg)
                    .border(1.dp, NothingCardBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isServiceActive) "MIC ACTIVE" else "STANDBY",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (isServiceActive) StatusSuccess else NothingGreyMuted,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.size(14.dp))

        // Tab Navigation Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(NothingCardBg)
                .border(1.dp, NothingCardBorder, RoundedCornerShape(8.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            NavTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) NothingWhite else NothingBlack.copy(alpha = 0f))
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 8.dp)
                        .testTag("tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.title,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) NothingBlack else NothingGreyLight,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}
