package com.woxich.locatalert.use_case

import com.woxich.locatalert.model.Note
import com.woxich.locatalert.repository.NoteRepository


class GetNote(
    private val repository: NoteRepository
) {

    suspend operator fun invoke(id: Int): Note? {
        return repository.getNoteById(id)
    }
}