package com.woxich.locatalert.presentation.notes

import com.woxich.locatalert.model.Note
import com.woxich.locatalert.utils.NoteOrder
import com.woxich.locatalert.utils.OrderType

data class NotesState(
    val notes: List<Note> = emptyList(),
    val noteOrder: NoteOrder = NoteOrder.Date(OrderType.Descending),
    val isOrderSectionVisible: Boolean = false
)
