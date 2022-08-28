package com.theflexlabs.noted.adapters;

import android.annotation.SuppressLint;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.theflexlabs.noted.databinding.ItemContainerNoteBinding;
import com.theflexlabs.noted.entities.Note;
import com.theflexlabs.noted.listeners.NotesListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<Note> notes;
    private final List<Note> notesSource;
    private final NotesListener notesListener;
    private Timer timer;

    public NotesAdapter(List<Note> notes, NotesListener notesListener) {
        this.notes = notes;
        this.notesListener = notesListener;
        this.notesSource = notes;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NoteViewHolder(
                ItemContainerNoteBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        final Note note = notes.get(position);
        holder.setNote(note);
        holder.binding.getRoot().setOnClickListener(view -> notesListener.onNoteClicked(note, position));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class NoteViewHolder extends RecyclerView.ViewHolder {

        private final ItemContainerNoteBinding binding;

        public NoteViewHolder(@NonNull ItemContainerNoteBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void setNote(@NonNull Note note) {
            binding.textTitle.setText(note.getTitle());
            if (note.getSubtitle().trim().isEmpty())
                binding.textSubtitle.setVisibility(View.GONE);
            else
                binding.textSubtitle.setText(note.getSubtitle());
            try {
                Date date = new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).parse(note.getDateTime());
                assert date != null;
                String formattedDate = new SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(date);
                binding.textDateTime.setText(formattedDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            GradientDrawable gradientDrawable = (GradientDrawable) binding.getRoot().getBackground();
            if (note.getColor() != null)
                gradientDrawable.setColor(Color.parseColor(note.getColor()));
            else
                gradientDrawable.setColor(Color.parseColor("#333333"));

            if (note.getImagePath() != null) {
                binding.imageNote.setImageBitmap(BitmapFactory.decodeFile(note.getImagePath()));
                binding.imageNote.setVisibility(View.VISIBLE);
            } else
                binding.imageNote.setVisibility(View.GONE);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void searchNote(final String searchKeyword) {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (searchKeyword.trim().isEmpty())
                    notes = notesSource;
                else {
                    ArrayList<Note> temp = new ArrayList<>();
                    for (Note note : notesSource) {
                        if (note.getTitle().toLowerCase().contains(searchKeyword.toLowerCase())
                                || note.getSubtitle().toLowerCase().contains(searchKeyword.toLowerCase())
                                || note.getNoteText().toLowerCase().contains(searchKeyword.toLowerCase())) {
                            temp.add(note);
                        }
                    }
                    notes = temp;
                }
                new Handler(Looper.getMainLooper()).post(() -> notifyDataSetChanged());
            }
        }, 500);
    }

    public void cancelTimer() {
        if (timer != null)
            timer.cancel();
    }
}
