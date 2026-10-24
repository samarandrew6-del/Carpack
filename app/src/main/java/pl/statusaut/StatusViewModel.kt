package pl.statusaut

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StatusState(
    val cars: List<Car> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
    val busyId: String? = null,
    val needBoardCode: Boolean = false,
    val message: String? = null
)

class StatusViewModel : ViewModel() {
    private val _state = MutableStateFlow(StatusState())
    val state: StateFlow<StatusState> = _state

    fun refresh(manual: Boolean = false) {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = manual, loading = it.cars.isEmpty()) }
            try {
                val cars = Api.getCars()
                _state.update { it.copy(cars = cars, loading = false, refreshing = false, error = null) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(loading = false, refreshing = false, error = e.message ?: "Błąd połączenia z serwerem")
                }
            }
        }
    }

    fun take(car: Car, name: String, phone: String, code: String) {
        viewModelScope.launch {
            _state.update { it.copy(busyId = car.id) }
            try {
                val cars = Api.take(car.id, name.trim(), phone.trim(), code.trim(), Prefs.boardCode)
                Prefs.name = name.trim()
                Prefs.phone = phone.trim()
                _state.update { it.copy(cars = cars, busyId = null, message = "Auto zajęte") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    fun free(car: Car, code: String) {
        viewModelScope.launch {
            _state.update { it.copy(busyId = car.id) }
            try {
                val cars = Api.free(car.id, code.trim(), Prefs.boardCode)
                _state.update { it.copy(cars = cars, busyId = null, message = "Auto zwolnione") }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleError(e)
            }
        }
    }

    private fun handleError(e: Exception) {
        if (e is ApiException && e.code == "board_code_required") {
            _state.update { it.copy(busyId = null, needBoardCode = true) }
        } else {
            _state.update { it.copy(busyId = null, message = e.message ?: "Błąd") }
        }
    }

    fun submitBoardCode(code: String) {
        Prefs.boardCode = code.trim()
        _state.update { it.copy(needBoardCode = false, message = "Zapisano kod dostępu. Spróbuj ponownie.") }
    }

    fun dismissBoardCode() = _state.update { it.copy(needBoardCode = false) }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
