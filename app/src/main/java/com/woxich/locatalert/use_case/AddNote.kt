package com.woxich.locatalert.use_case

import com.woxich.locatalert.model.InvalidNoteException
import com.woxich.locatalert.model.Note
import com.woxich.locatalert.repository.NoteRepository


class AddNote(
    private val repository: NoteRepository
) {

    @Throws(InvalidNoteException::class)
    suspend operator fun invoke(note: Note) {
        if(note.title.isBlank()) {
            throw InvalidNoteException("The title of the note can't be empty.")
        }
        if(note.content.isBlank()) {
            throw InvalidNoteException("The content of the note can't be empty.")
        }
        repository.insertNote(note)
    }
}