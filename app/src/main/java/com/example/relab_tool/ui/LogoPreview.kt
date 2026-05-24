package com.example.relab_tool.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun RelabLogo(modifier: Modifier = Modifier) {
    val relabBlue = Color(0xFF4385f4)
    Box(
        modifier = modifier
            .size(200.dp)
            .background(relabBlue),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(160.dp)) {
            // Future path drawing here
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val style = TextStyle(
                color = Color.White,
                fontSize = 50.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.SansSerif
            )
            Text(text = "rel", style = style)
            Box(contentAlignment = Alignment.Center) {
                Text(text = "a", style = style)
                // Gear icon
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier
                        .size(18.dp)
                        .offset(x = (-0.5).dp, y = 5.dp),
                    tint = relabBlue
                )
            }
            Text(text = "b", style = style)
        }
    }
}

@Preview
@Composable
fun PreviewRelabLogo() {
    RelabLogo()
}
