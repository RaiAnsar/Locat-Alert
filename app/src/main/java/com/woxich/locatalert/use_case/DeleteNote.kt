package com.woxich.locatalert.use_case

import com.woxich.locatalert.model.Note
import com.woxich.locatalert.repository.NoteRepository


class DeleteNote(
    private val repository: NoteRepository
) {

    suspend operator fun invoke(note: Note) {
        repository.deleteNote(note)
    }
}