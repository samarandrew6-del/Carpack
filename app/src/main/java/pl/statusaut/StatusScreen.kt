package pl.statusaut

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

private val Green = Color(0xFF34C759)
private val Red = Color(0xFFFF453A)

@Composable
fun ScreenTitle(text: String) {
    Text(
        text = text,
        fontSize = 34.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 24.dp, bottom = 12.dp, start = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(vm: StatusViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbar = remember { SnackbarHostState() }

    // Odświeżaj co 15 s, ale tylko gdy aplikacja jest widoczna.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                vm.refresh()
                delay(15_000)
            }
        }
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { vm.refresh(manual = true) },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item { ScreenTitle("Status aut") }
                if (state.loading && state.cars.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }
                val err = state.error
                if (err != null && state.cars.isEmpty()) {
                    item { ErrorBox(err) { vm.refresh(manual = true) } }
                }
                items(state.cars, key = { it.id }) { car ->
                    CarCard(car, vm, busy = state.busyId == car.id)
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(16.dp))
    }

    if (state.needBoardCode) {
        TextDialog(
            title = "Kod dostępu do tablicy",
            label = "Kod dostępu",
            confirm = "Zapisz",
            onConfirm = { vm.submitBoardCode(it) },
            onDismiss = { vm.dismissBoardCode() }
        )
    }
}

@Composable
private fun ErrorBox(text: String, onRetry: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Błąd połączenia z serwerem", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onRetry, shape = CircleShape) { Text("Spróbuj ponownie") }
        }
    }
}

@Composable
private fun StatusChip(free: Boolean) {
    val color = if (free) Green else Red
    Surface(shape = CircleShape, color = color.copy(alpha = 0.16f)) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.size(8.dp))
            Text(if (free) "Wolne" else "Zajęte", color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CarCard(car: Car, vm: StatusViewModel, busy: Boolean) {
    var name by rememberSaveable(car.id) { mutableStateOf(Prefs.name) }
    var phone by rememberSaveable(car.id) { mutableStateOf(Prefs.phone) }
    var code by rememberSaveable(car.id) { mutableStateOf("") }
    var showFree by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(car.name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            car.plate,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                StatusChip(free = !car.occupied)
            }

            if (car.occupied) {
                Text(
                    "Zajęte przez ${car.by}" + if (car.time.isNotBlank()) " od ${car.time}" else "",
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (car.phone.isNotBlank()) {
                    Text(
                        car.phone,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + car.phone.filter { it.isDigit() || it == '+' })))
                        }
                    )
                }
                if (car.caretaker.isNotBlank()) {
                    Text("Opiekun: ${car.caretaker}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = { showFree = true },
                    enabled = !busy,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Zwolnij", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            } else {
                Field("Twoje imię", name) { name = it }
                Field("Numer telefonu", phone, KeyboardType.Phone) { phone = it }
                Field("Ustaw kod do zwolnienia", code) { code = it }
                Button(
                    onClick = {
                        vm.take(car, name, phone, code)
                        code = ""
                    },
                    enabled = !busy,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    if (busy) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = Color.White)
                    else Text("Zajmij", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = { /* "Zajmij później" (rezerwacja) powstanie razem z ekranem Planera */ },
                    enabled = false,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("Zajmij później (wkrótce)") }
            }
        }
    }

    if (showFree) {
        TextDialog(
            title = "Zwolnij ${car.name}",
            label = "Kod zwolnienia",
            confirm = "Zwolnij",
            onConfirm = { vm.free(car, it); showFree = false },
            onDismiss = { showFree = false }
        )
    }
}

@Composable
private fun Field(
    label: String,
    value: String,
    keyboard: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun TextDialog(
    title: String,
    label: String,
    confirm: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Field(label, text) { text = it } },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text(confirm) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
        shape = MaterialTheme.shapes.large
    )
}
