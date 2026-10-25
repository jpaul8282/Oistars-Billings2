package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.service.AiBillingCommunicationService
import com.example.data.service.InvoiceCommunicationRequest
import com.example.data.service.InvoiceCommunicationResult
import com.example.data.service.InvoiceCommunicationTone
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.StatusPaid
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiCommunicationGeneratorDialog(
    clientName: String,
    invoiceId: String,
    amount: Double,
    currency: String = "EUR",
    dueDate: String = "Net 14 Days",
    initialTone: InvoiceCommunicationTone = InvoiceCommunicationTone.FRIENDLY_WELCOME,
    onDismiss: () -> Unit,
    onApplyToInvoiceNotes: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val aiService = remember { AiBillingCommunicationService(context) }

    var selectedTone by remember { mutableStateOf(initialTone) }
    var customInstructions by remember { mutableStateOf("") }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedResult by remember { mutableStateOf<InvoiceCommunicationResult?>(null) }
    var editableOutputText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = bottomSheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(2.dp))
            )
        },
        modifier = Modifier.testTag("ai_comm_dialog")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(OceanBlue.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Generator",
                            tint = OceanBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Invoice Communication",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Server-side Genkit & Gemini generator",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Invoice Summary Pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = clientName.ifBlank { "Client Store" },
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Invoice: ${invoiceId.ifBlank { "INV-DRAFT" }} • Due: $dueDate",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = Formatters.formatCurrency(amount, currency),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tone Selection
            Text(
                text = "Select Tone & Communication Style",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                InvoiceCommunicationTone.values().forEach { tone ->
                    val isSelected = selectedTone == tone
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTone = tone },
                        label = { Text(tone.displayName, fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = OceanBlue.copy(alpha = 0.2f),
                            selectedLabelColor = OceanBlue
                        ),
                        modifier = Modifier.testTag("ai_tone_chip_${tone.key}")
                    )
                }
            }

            Text(
                text = selectedTone.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Custom Prompt / Instructions TextField
            OutlinedTextField(
                value = customInstructions,
                onValueChange = { customInstructions = it },
                label = { Text("Special instructions or terms (optional)") },
                placeholder = { Text("e.g. Include 5% early settlement discount if paid in 3 days") },
                maxLines = 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("ai_custom_instructions_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Generate Button
            Button(
                onClick = {
                    isGenerating = true
                    coroutineScope.launch {
                        val req = InvoiceCommunicationRequest(
                            clientName = clientName,
                            invoiceId = invoiceId,
                            amount = amount,
                            currency = currency,
                            dueDate = dueDate,
                            tone = selectedTone,
                            customInstructions = customInstructions
                        )
                        val result = aiService.generateCommunication(req)
                        generatedResult = result
                        editableOutputText = result.communicationText
                        isGenerating = false
                    }
                },
                enabled = !isGenerating,
                colors = ButtonDefaults.buttonColors(containerColor = OceanBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("ai_generate_communication_btn"),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generating Communication with Gemini...")
                } else {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (generatedResult == null) "Generate AI Communication" else "Regenerate Message")
                }
            }

            // Results Preview Section
            if (editableOutputText.isNotBlank()) {
                Spacer(modifier = Modifier.height(18.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, OceanBlue.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Generated Preview (Editable)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = OceanBlue
                            )
                            Text(
                                text = "Source: ${generatedResult?.source ?: "server"}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (generatedResult?.subject?.isNotBlank() == true) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Subject: ${generatedResult?.subject}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = editableOutputText,
                            onValueChange = { editableOutputText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_result_preview_text"),
                            minLines = 4,
                            maxLines = 8,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions: Insert into Invoice Notes / Copy / Share
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onApplyToInvoiceNotes != null) {
                                Button(
                                    onClick = {
                                        onApplyToInvoiceNotes(editableOutputText)
                                        Toast.makeText(context, "Note applied to invoice!", Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = StatusPaid),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("ai_insert_note_btn")
                                ) {
                                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Insert to Notes", fontSize = 12.sp)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Invoice Communication", editableOutputText)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ai_copy_note_btn")
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontSize = 12.sp)
                            }

                            IconButton(
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        putExtra(Intent.EXTRA_SUBJECT, generatedResult?.subject ?: "Invoice $invoiceId")
                                        putExtra(Intent.EXTRA_TEXT, editableOutputText)
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "Send Communication"))
                                },
                                modifier = Modifier.testTag("ai_share_note_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share", tint = OceanBlue)
                            }
                        }
                    }
                }
            }
        }
    }
}
