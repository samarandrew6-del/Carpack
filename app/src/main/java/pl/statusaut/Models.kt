package pl.statusaut

data class Car(
    val id: String,
    val name: String,
    val plate: String,
    val occupied: Boolean,
    val by: String,
    val phone: String,
    val time: String,
    val caretaker: String
)
