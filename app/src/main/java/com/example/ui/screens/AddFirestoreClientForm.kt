package com.example.ui.screens

import android.util.Patterns
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.firebase.FirestoreSyncManager
import com.example.data.model.ClientAccountStatus
import com.example.data.model.FirestoreClient
import com.example.ui.theme.AccentGold
import com.example.ui.theme.NavyDark
import com.example.ui.theme.OceanBlue
import com.example.ui.theme.StatusOverdue
import com.example.ui.theme.StatusPaid
import kotlinx.coroutines.launch

/**
 * A dedicated Composable form component that allows users to input a client's
 * name and email (along with optional contact person, phone, and address details),
 * and provides a button to save this data directly to the Firestore 'clients' collection.
 */
@Composable
fun AddFirestoreClientForm(
    userId: String,
    firestoreSyncManager: FirestoreSyncManager,
    onClientSaved: (FirestoreClient) -> Unit,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    // Form Field States
    var clientName by remember { mutableStateOf("") }
    var clientEmail by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("Netherlands") }
    var notes by remember { mutableStateOf("") }

    // Validation & State Feedback
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var serverError by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showOptionalFields by remember { mutableStateOf(false) }

    fun validateForm(): Boolean {
        var isValid = true
        val trimmedName = clientName.trim()
        val trimmedEmail = clientEmail.trim()

        if (trimmedName.isBlank()) {
            nameError = "Client name is required"
            isValid = false
        } else if (trimmedName.length < 2) {
            nameError = "Name must be at least 2 characters"
            isValid = false
        } else {
            nameError = null
        }

        if (trimmedEmail.isBlank()) {
            emailError = "Client contact email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            emailError = "Please enter a valid email address (e.g. name@company.com)"
            isValid = false
        } else {
            emailError = null
        }

        return isValid
    }

    fun submitForm() {
        if (!validateForm()) return

        val trimmedName = clientName.trim()
        val trimmedEmail = clientEmail.trim()
        val newDocId = "CLI-${System.currentTimeMillis() % 1000000}"

        val clientToSave = FirestoreClient(
            id = newDocId,
            userId = userId,
            name = trimmedName,
            contactPerson = contactPerson.trim(),
            email = trimmedEmail,
            phone = phoneNumber.trim(),
            address = address.trim(),
            country = country.trim().ifBlank { "Netherlands" },
            currency = "EUR",
            paymentTermsDays = 14,
            notes = notes.trim(),
            accountStatus = ClientAccountStatus.ACTIVE,
            totalInvoiced = 0.0,
            totalPaid = 0.0,
            outstandingBalance = 0.0,
            openInvoicesCount = 0,
            createdAt = System.currentTimeMillis()
        )

        isSubmitting = true
        serverError = null
        successMessage = null

        coroutineScope.launch {
            val result = firestoreSyncManager.saveClientToFirestore(userId, clientToSave)
            isSubmitting = false
            result.onSuccess {
                successMessage = "Client '$trimmedName' successfully saved to Firestore!"
                onClientSaved(clientToSave)
                // Clear fields if staying on screen
                clientName = ""
                clientEmail = ""
                contactPerson = ""
                phoneNumber = ""
                address = ""
                notes = ""
            }.onFailure { ex ->
                serverError = ex.localizedMessage ?: "Failed to save client to Firestore. Please try again."
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("add_firestore_client_form"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header: Title and Firestore Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(NavyDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Add Firestore Client",
                            tint = AccentGold,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "New Firestore Client",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Saves to 'clients' collection in Firestore",
                            fontSize = 11.sp,
                            color = OceanBlue,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (onDismiss != null) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("btn_close_firestore_client_form")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Form",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))

            // Success feedback message
            AnimatedVisibility(
                visible = successMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = StatusPaid.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = StatusPaid,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = successMessage ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = StatusPaid
                        )
                    }
                }
            }

            // Server / Cloud error feedback message
            AnimatedVisibility(
                visible = serverError != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = StatusOverdue.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = StatusOverdue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = serverError ?: "",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = StatusOverdue
                        )
                    }
                }
            }

            // 1. Client Name Input (Required)
            OutlinedTextField(
                value = clientName,
                onValueChange = {
                    clientName = it
                    if (nameError != null) nameError = null
                },
                label = { Text("Client Name *") },
                placeholder = { Text("e.g. Acme Corporation or Superstore Amsterdam") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Business,
                        contentDescription = "Client Name Icon",
                        tint = if (nameError != null) MaterialTheme.colorScheme.error else OceanBlue
                    )
                },
                isError = nameError != null,
                supportingText = {
                    if (nameError != null) {
                        Text(text = nameError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Company, organization, or brand name")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_firestore_client_name"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OceanBlue,
                    focusedLabelColor = OceanBlue
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Client Email Input (Required)
            OutlinedTextField(
                value = clientEmail,
                onValueChange = {
                    clientEmail = it
                    if (emailError != null) emailError = null
                },
                label = { Text("Contact Email *") },
                placeholder = { Text("e.g. billing@acme.com") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Contact Email Icon",
                        tint = if (emailError != null) MaterialTheme.colorScheme.error else OceanBlue
                    )
                },
                isError = emailError != null,
                supportingText = {
                    if (emailError != null) {
                        Text(text = emailError!!, color = MaterialTheme.colorScheme.error)
                    } else {
                        Text("Used for electronic billing & Firestore notifications")
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = if (showOptionalFields) ImeAction.Next else ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) },
                    onDone = {
                        focusManager.clearFocus()
                        submitForm()
                    }
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_firestore_client_email"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = OceanBlue,
                    focusedLabelColor = OceanBlue
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Toggle Optional Detailed Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showOptionalFields) "Additional Client Info" else "Need to add address or contact person?",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedButton(
                    onClick = { showOptionalFields = !showOptionalFields },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = ButtonDefaults.ContentPadding
                ) {
                    Text(
                        text = if (showOptionalFields) "Hide Details" else "+ More Details",
                        fontSize = 12.sp
                    )
                }
            }

            AnimatedVisibility(visible = showOptionalFields) {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    // Contact Person
                    OutlinedTextField(
                        value = contactPerson,
                        onValueChange = { contactPerson = it },
                        label = { Text("Contact Person") },
                        placeholder = { Text("e.g. Sarah Jenkins") },
                        leadingIcon = {
                            Icon(Icons.Default.Person, contentDescription = null, tint = OceanBlue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_firestore_client_contact"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Phone Number
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("e.g. +31 20 123 4567") },
                        leadingIcon = {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = OceanBlue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_firestore_client_phone"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Address & Country
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Business Address") },
                        placeholder = { Text("e.g. Keizersgracht 421, 1016 EK Amsterdam") },
                        leadingIcon = {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = OceanBlue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_firestore_client_address"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Country
                    OutlinedTextField(
                        value = country,
                        onValueChange = { country = it },
                        label = { Text("Country") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_firestore_client_country"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Account Notes
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Account Notes") },
                        placeholder = { Text("Optional notes or billing instructions") },
                        leadingIcon = {
                            Icon(Icons.Default.Notes, contentDescription = null, tint = OceanBlue)
                        },
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_firestore_client_notes"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onDismiss != null) {
                    OutlinedButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("btn_cancel_firestore_client"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel")
                    }
                }

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        submitForm()
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OceanBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(if (onDismiss != null) 1.5f else 1f)
                        .height(50.dp)
                        .testTag("btn_save_firestore_client"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Saving to Firestore...")
                    } else {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = "Save Client",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save to Firestore",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dialog wrapper for [AddFirestoreClientForm] so it can be presented seamlessly as a modal
 * anywhere across the application.
 */
@Composable
fun AddFirestoreClientDialog(
    userId: String,
    firestoreSyncManager: FirestoreSyncManager,
    onDismiss: () -> Unit,
    onClientSaved: (FirestoreClient) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(vertical = 24.dp)
        ) {
            AddFirestoreClientForm(
                userId = userId,
                firestoreSyncManager = firestoreSyncManager,
                onClientSaved = { client ->
                    onClientSaved(client)
                    onDismiss()
                },
                onDismiss = onDismiss
            )
        }
    }
}
