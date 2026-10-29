package com.midicommander.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.midicommander.app.data.model.Bank
import com.midicommander.app.ui.theme.MidiColors

// ─── Bank Selector Strip ─────────────────────────────────────────────────────

@Composable
fun BankSelector(
    banks: List<Bank>,
    selectedBankId: String,
    modifier: Modifier = Modifier,
    onSelectBank: (Bank) -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        banks.forEach { bank ->
            val isSelected = bank.id == selectedBankId
            val bgColor = if (isSelected) MidiColors.PurpleGlow else MidiColors.SurfaceCard
            val textColor = if (isSelected) Color.White else MidiColors.TextSecondary
            val borderColor = if (isSelected) MidiColors.PurpleLight else MidiColors.CardBorder

            Box(
                modifier = Modifier
                    .height(32.dp)
                    .defaultMinSize(minWidth = 36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                    .clickable { onSelectBank(bank) }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bank.label,
                    color = textColor,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Normal,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                )
            }
        }
    }
}
