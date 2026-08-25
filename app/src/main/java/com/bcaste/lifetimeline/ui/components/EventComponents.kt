package com.bcaste.lifetimeline.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bcaste.lifetimeline.data.local.entity.EventWithDetails
import com.bcaste.lifetimeline.ui.utils.getIconByName
import com.bcaste.lifetimeline.ui.utils.toColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimelineEventCard(
    eventWithDetails: EventWithDetails,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onImageClick: (Int) -> Unit,
    dayText: String,
    monthText: String
) {
    val event = eventWithDetails.event
    val category = eventWithDetails.categories.firstOrNull()
    val categoryColor = category?.color?.toColor() ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F171F))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Indicador vertical izquierdo
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(categoryColor)
            )

            Column(modifier = Modifier.padding(6.dp).weight(1f).animateContentSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Date Block
                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 68.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(categoryColor.copy(alpha = 0.15f))
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dayText,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = categoryColor,
                                    fontSize = 22.sp
                                )
                            )
                            Text(
                                text = monthText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = categoryColor.copy(alpha = 0.9f),
                                    fontSize = 10.sp
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Content
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = getIconByName(category?.icon ?: "event"),
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        if (category != null) {
                            Text(
                                text = category.name,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = categoryColor,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        
                        Text(
                            text = event.description,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            maxLines = if (isExpanded) Int.MAX_VALUE else 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Image
                    if (eventWithDetails.images.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        AsyncImage(
                            model = eventWithDetails.images.first().imageUri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImageClick(0) },
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // More images if any
                    if (eventWithDetails.images.size > 1) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            eventWithDetails.images.drop(1).forEachIndexed { index, image ->
                                AsyncImage(
                                    model = image.imageUri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { onImageClick(index + 1) },
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
