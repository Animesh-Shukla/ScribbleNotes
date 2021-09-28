package com.animesh7752.scribblenotes.listeners;

import com.animesh7752.scribblenotes.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
}
