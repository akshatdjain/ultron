package com.akshatdjain.ultron.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.akshatdjain.ultron.ble.ConnectionState

@Composable
fun ConnectionStatusBar(
    connectionState: ConnectionState,
    onConnectClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val label = when (connectionState) {
        ConnectionState.Disconnected -> "Tap to connect"
        ConnectionState.Connecting -> "Scanning..."
        ConnectionState.Connected -> "Connecting..."
        ConnectionState.Ready -> "Connected to device"
    }
    val dotColor = when (connectionState) {
        ConnectionState.Disconnected -> MaterialTheme.colorScheme.outline
        ConnectionState.Connecting, ConnectionState.Connected -> MaterialTheme.colorScheme.tertiary
        ConnectionState.Ready -> MaterialTheme.colorScheme.primary
    }
    val isBusy = connectionState == ConnectionState.Connecting || connectionState == ConnectionState.Connected

    val infiniteTransition = rememberInfiniteTransition(label = "statusPulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseScale"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(enabled = connectionState == ConnectionState.Disconnected) { onConnectClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .scale(if (isBusy) pulseScale else 1f)
                .clip(CircleShape)
                .background(if (isBusy) dotColor.copy(alpha = pulseAlpha) else dotColor)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
