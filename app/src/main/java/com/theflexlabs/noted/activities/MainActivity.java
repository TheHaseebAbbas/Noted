package com.theflexlabs.noted.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.theflexlabs.noted.R;
import com.theflexlabs.noted.adapters.NotesAdapter;
import com.theflexlabs.noted.database.NotesDatabase;
import com.theflexlabs.noted.databinding.ActivityMainBinding;
import com.theflexlabs.noted.databinding.LayoutAddUrlBinding;
import com.theflexlabs.noted.databinding.LayoutDialogBinding;
import com.theflexlabs.noted.entities.Note;
import com.theflexlabs.noted.listeners.NotesListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NotesListener {

    public static final int REQUEST_CODE_SHOW_NOTES = 1;
    public static final int REQUEST_CODE_ADD_NOTE = 2;
    public static final int REQUEST_CODE_UPDATE_NOTE = 3;

    public static final String EXTRA_IS_VIEW_OR_UPDATE = "isViewOrUpdate";
    public static final String EXTRA_IS_FROM_QUICK_ACTIONS = "isFromQuickActions";
    public static final String EXTRA_NOTE = "note";
    public static final String EXTRA_QUICK_ACTION_TYPE = "quickActionType";
    public static final String EXTRA_IMAGE_PATH = "imagePath";
    public static final String EXTRA_URL = "url";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_URL = "webLink";

    private ActivityMainBinding binding;

    private List<Note> noteList;
    private NotesAdapter notesAdapter;
    private int noteClickedPosition = -1;

    private AlertDialog dialogAddUrl;

    private final ActivityResultLauncher<Intent> notesLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    assert result.getData() != null;
                    Intent intent = result.getData();
                    getNotes(intent.getIntExtra(EXTRA_NOTE, -1),
                            intent.getBooleanExtra(CreateNoteActivity.IS_NOTE_DELETED, false));
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
        setListeners();

    }

    private void init() {
        binding.notesRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        noteList = new ArrayList<>();
        notesAdapter = new NotesAdapter(noteList, this);
        binding.notesRecyclerView.setAdapter(notesAdapter);

        getNotes(REQUEST_CODE_SHOW_NOTES, false);

        binding.inputSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                notesAdapter.cancelTimer();
            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (noteList.size() != 0)
                    notesAdapter.searchNote(editable.toString());
            }
        });
    }

    private void setListeners() {
        binding.imageAddNoteMain.setOnClickListener(view ->
                notesLauncher.launch(new Intent(getApplicationContext(), CreateNoteActivity.class)));

        binding.imageAddNote.setOnClickListener(view ->
                notesLauncher.launch(new Intent(getApplicationContext(), CreateNoteActivity.class)));

        binding.imageAddImage.setOnClickListener(view -> selectImage());

        binding.imageAddWebLink.setOnClickListener(view -> showAddUrlDialog());
    }

    private void selectImage() {
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            selectImageLauncher.launch(intent);
        } else {
            requestStorageReadPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void showAddUrlDialog() {
        if (dialogAddUrl == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            LayoutAddUrlBinding addUrlBinding = LayoutAddUrlBinding.inflate(LayoutInflater.from(this));
            builder.setView(addUrlBinding.getRoot());

            dialogAddUrl = builder.create();
            if (dialogAddUrl.getWindow() != null)
                dialogAddUrl.getWindow().setBackgroundDrawable(new ColorDrawable(0));

            addUrlBinding.inputURL.requestFocus();
            addUrlBinding.textAdd.setOnClickListener(view -> {
                if (addUrlBinding.inputURL.getText().toString().trim().isEmpty())
                    Toast.makeText(this, "Enter URL", Toast.LENGTH_SHORT).show();
                else if (!Patterns.WEB_URL.matcher(addUrlBinding.inputURL.getText().toString()).matches())
                    Toast.makeText(this, "Enter valid URL", Toast.LENGTH_SHORT).show();
                else {
                    dialogAddUrl.dismiss();
                    Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                    intent.putExtra(EXTRA_IS_FROM_QUICK_ACTIONS, true);
                    intent.putExtra(EXTRA_QUICK_ACTION_TYPE, TYPE_URL);
                    intent.putExtra(EXTRA_URL, addUrlBinding.inputURL.getText().toString());
                    notesLauncher.launch(intent);
                }
            });
            addUrlBinding.textCancel.setOnClickListener(view -> dialogAddUrl.dismiss());
        }
        dialogAddUrl.show();
    }


    private final ActivityResultLauncher<Intent> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        try {
                            String selectedImagePath = getPathFromUri(selectedImageUri);
                            Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
                            intent.putExtra(EXTRA_IS_FROM_QUICK_ACTIONS, true);
                            intent.putExtra(EXTRA_QUICK_ACTION_TYPE, TYPE_IMAGE);
                            intent.putExtra(EXTRA_IMAGE_PATH, selectedImagePath);
                            notesLauncher.launch(intent);
                        } catch (Exception e) {
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    private final ActivityResultLauncher<String> requestStorageReadPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            result -> {
                if (result)
                    selectImage();
                else {
                    boolean permissionRationaleStatus = shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE);
                    if (permissionRationaleStatus) {
                        // denied first time
                        showDialogPermissionDenied();
                    } else {
                        // denied permanently
                        showDialogPermissionPermanentlyDenied();
                    }
                }
            }
    );

    private void showDialogPermissionDenied() {
        LayoutDialogBinding dialogBinding = LayoutDialogBinding.inflate(LayoutInflater.from(this));
        dialogBinding.imageDialog.setImageResource(R.drawable.ic_storage);
        dialogBinding.textDialogTitle.setText(getString(R.string.permission_denied));
        dialogBinding.textDialogMessage.setText(getString(R.string.storage_read_permission_required_to_add_images));
        dialogBinding.textDialogPositive.setText(getString(android.R.string.ok));
        dialogBinding.textDialogNegative.setVisibility(View.GONE);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));

        dialogBinding.textDialogPositive.setOnClickListener(view -> {
            dialog.dismiss();
            selectImage();
        });
        dialog.show();
    }

    private void showDialogPermissionPermanentlyDenied() {
        LayoutDialogBinding dialogBinding = LayoutDialogBinding.inflate(LayoutInflater.from(this));
        dialogBinding.imageDialog.setImageResource(R.drawable.ic_storage);
        dialogBinding.textDialogTitle.setText(getString(R.string.permission_required));
        dialogBinding.textDialogMessage.setText(getString(R.string.storage_read_permission_required_to_get_images_go_to_app_settings_and_enable_permission_manually));
        dialogBinding.textDialogPositive.setText(getString(android.R.string.ok));
        dialogBinding.textDialogNegative.setVisibility(View.GONE);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogBinding.getRoot())
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));

        dialogBinding.textDialogPositive.setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }

    private String getPathFromUri(Uri contentUri) {
        String filePath;
        Cursor cursor = getContentResolver()
                .query(contentUri, null, null, null, null);
        if (cursor == null)
            filePath = contentUri.getPath();
        else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    private void getNotes(final int requestCode, final boolean isNoteDeleted) {

        @SuppressWarnings("deprecation")
        @SuppressLint("StaticFieldLeak")
        class GetNotesTask extends AsyncTask<Void, Void, List<Note>> {

            @Override
            protected List<Note> doInBackground(Void... voids) {
                return NotesDatabase
                        .getNotesDatabase(getApplicationContext())
                        .noteDao().getAllNotes();
            }

            @SuppressLint("NotifyDataSetChanged")
            @Override
            protected void onPostExecute(List<Note> notes) {
                super.onPostExecute(notes);
                if (requestCode == REQUEST_CODE_SHOW_NOTES) {
                    noteList.addAll(notes);
                    notesAdapter.notifyDataSetChanged();
                } else if (requestCode == REQUEST_CODE_ADD_NOTE) {
                    noteList.add(0, notes.get(0));
                    notesAdapter.notifyItemInserted(0);
                    binding.notesRecyclerView.smoothScrollToPosition(0);
                } else if (requestCode == REQUEST_CODE_UPDATE_NOTE) {
                    noteList.remove(noteClickedPosition);
                    if (isNoteDeleted)
                        notesAdapter.notifyItemRemoved(noteClickedPosition);
                    else {
                        noteList.add(noteClickedPosition, notes.get(noteClickedPosition));
                        notesAdapter.notifyItemChanged(noteClickedPosition);
                    }
                }
            }
        }

        new GetNotesTask().execute();
    }

    @Override
    public void onNoteClicked(Note note, int position) {
        noteClickedPosition = position;
        Intent intent = new Intent(getApplicationContext(), CreateNoteActivity.class);
        intent.putExtra(EXTRA_IS_VIEW_OR_UPDATE, true);
        intent.putExtra(EXTRA_NOTE, note);
        notesLauncher.launch(intent);
    }
}