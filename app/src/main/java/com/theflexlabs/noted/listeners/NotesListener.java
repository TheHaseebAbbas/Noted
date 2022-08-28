package com.theflexlabs.noted.listeners;

import com.theflexlabs.noted.entities.Note;

public interface NotesListener {
    void onNoteClicked(Note note, int position);
}
