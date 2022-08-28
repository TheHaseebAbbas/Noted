package com.theflexlabs.noted.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.theflexlabs.noted.R;
import com.theflexlabs.noted.database.NotesDatabase;
import com.theflexlabs.noted.databinding.ActivityCreateNoteBinding;
import com.theflexlabs.noted.databinding.LayoutAddUrlBinding;
import com.theflexlabs.noted.databinding.LayoutDialogBinding;
import com.theflexlabs.noted.entities.Note;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CreateNoteActivity extends AppCompatActivity {

    private static final String TAG = "MyTag";
    public static final String IS_NOTE_DELETED = "isNoteDeleted";
    private ActivityCreateNoteBinding binding;
    private String selectedNoteColor;
    private String selectedImagePath;

    private AlertDialog dialogAddUrl;
    private AlertDialog dialogDeleteNote;

    private Note alreadyAvailableNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCreateNoteBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        init();
        setListeners();

        initMiscellaneous();
        setSubtitleIndicatorColor();
    }

    private void init() {
        binding.textDateTime.setText(
                new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault())
                        .format(new Date())
        );

        selectedNoteColor = "#333333";
        selectedImagePath = "";

        if (getIntent().getBooleanExtra(MainActivity.EXTRA_IS_VIEW_OR_UPDATE, false)) {
            alreadyAvailableNote = (Note) getIntent().getSerializableExtra(MainActivity.EXTRA_NOTE);
            setViewOrUpdateNote();
        }

        if (getIntent().getBooleanExtra(MainActivity.EXTRA_IS_FROM_QUICK_ACTIONS, false)){
            String type = getIntent().getStringExtra(MainActivity.EXTRA_QUICK_ACTION_TYPE);
            if (type != null) {
                if (type.equals(MainActivity.TYPE_IMAGE)){
                    selectedImagePath = getIntent().getStringExtra(MainActivity.EXTRA_IMAGE_PATH);
                    binding.imageNote.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath));
                    binding.imageNote.setVisibility(View.VISIBLE);
                    binding.imageRemoveImage.setVisibility(View.VISIBLE);
                } else if (type.equals(MainActivity.TYPE_URL)) {
                    binding.textWebURL.setText(getIntent().getStringExtra(MainActivity.EXTRA_URL));
                    binding.layoutWebURL.setVisibility(View.VISIBLE);
                }
            }
        }
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(view -> onBackPressed());
        binding.imageSave.setOnClickListener(view -> saveNote());
        binding.imageRemoveWebURL.setOnClickListener(view -> {
            binding.textWebURL.setText(null);
            binding.layoutWebURL.setVisibility(View.GONE);
        });

        binding.imageRemoveImage.setOnClickListener(view -> {
            binding.imageNote.setImageBitmap(null);
            binding.imageNote.setVisibility(View.GONE);
            binding.imageRemoveImage.setVisibility(View.GONE);
            selectedImagePath = "";
        });
    }

    private void setViewOrUpdateNote() {
        binding.inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        binding.inputNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
        binding.inputNoteText.setText(alreadyAvailableNote.getNoteText());
        binding.textDateTime.setText(alreadyAvailableNote.getDateTime());

        if (alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().trim().isEmpty()) {
            binding.imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
            binding.imageNote.setVisibility(View.VISIBLE);
            binding.imageRemoveImage.setVisibility(View.VISIBLE);
            selectedImagePath = alreadyAvailableNote.getImagePath();
        }

        if (alreadyAvailableNote.getWebLink() != null && !alreadyAvailableNote.getWebLink().trim().isEmpty()) {
            Log.d(TAG, "setViewOrUpdateNote: " + alreadyAvailableNote.getWebLink());
            binding.textWebURL.setText(alreadyAvailableNote.getWebLink());
            binding.layoutWebURL.setVisibility(View.VISIBLE);
        }
    }

    private void saveNote() {
        if (binding.inputNoteTitle.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note title can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        } else if (binding.inputNoteSubtitle.getText().toString().trim().isEmpty()
                && binding.inputNoteText.getText().toString().trim().isEmpty()) {
            Toast.makeText(this, "Note can't be empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        final Note note = new Note();
        note.setTitle(binding.inputNoteTitle.getText().toString());
        note.setSubtitle(binding.inputNoteSubtitle.getText().toString());
        note.setNoteText(binding.inputNoteText.getText().toString());
        note.setDateTime(binding.textDateTime.getText().toString());
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);

        if (binding.layoutWebURL.getVisibility() == View.VISIBLE)
            note.setWebLink(binding.textWebURL.getText().toString());

        if (alreadyAvailableNote != null)
            note.setId(alreadyAvailableNote.getId());


        @SuppressWarnings("deprecation")
        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void, Void, Void> {

            @Nullable
            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase.getNotesDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);
                Intent intent = new Intent();
                if (alreadyAvailableNote == null)
                    intent.putExtra(MainActivity.EXTRA_NOTE, MainActivity.REQUEST_CODE_ADD_NOTE);
                else
                    intent.putExtra(MainActivity.EXTRA_NOTE, MainActivity.REQUEST_CODE_UPDATE_NOTE);
                setResult(RESULT_OK, intent);
                finish();
            }
        }
        new SaveNoteTask().execute();
    }

    private void initMiscellaneous() {
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(binding.layoutMiscellaneous.getRoot());
        binding.layoutMiscellaneous.textMiscellaneous.setOnClickListener(view -> {
            if (bottomSheetBehavior.getState() != BottomSheetBehavior.STATE_EXPANDED)
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            else
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        });

        binding.layoutMiscellaneous.viewColor1.setOnClickListener(view -> {
            selectedNoteColor = "#333333";
            binding.layoutMiscellaneous.imageColor1.setImageResource(R.drawable.ic_done);
            binding.layoutMiscellaneous.imageColor2.setImageResource(0);
            binding.layoutMiscellaneous.imageColor3.setImageResource(0);
            binding.layoutMiscellaneous.imageColor4.setImageResource(0);
            binding.layoutMiscellaneous.imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        binding.layoutMiscellaneous.viewColor2.setOnClickListener(view -> {
            selectedNoteColor = "#FDBE3E";
            binding.layoutMiscellaneous.imageColor1.setImageResource(0);
            binding.layoutMiscellaneous.imageColor2.setImageResource(R.drawable.ic_done);
            binding.layoutMiscellaneous.imageColor3.setImageResource(0);
            binding.layoutMiscellaneous.imageColor4.setImageResource(0);
            binding.layoutMiscellaneous.imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        binding.layoutMiscellaneous.viewColor3.setOnClickListener(view -> {
            selectedNoteColor = "#FF4842";
            binding.layoutMiscellaneous.imageColor1.setImageResource(0);
            binding.layoutMiscellaneous.imageColor2.setImageResource(0);
            binding.layoutMiscellaneous.imageColor3.setImageResource(R.drawable.ic_done);
            binding.layoutMiscellaneous.imageColor4.setImageResource(0);
            binding.layoutMiscellaneous.imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        binding.layoutMiscellaneous.viewColor4.setOnClickListener(view -> {
            selectedNoteColor = "#3A52FC";
            binding.layoutMiscellaneous.imageColor1.setImageResource(0);
            binding.layoutMiscellaneous.imageColor2.setImageResource(0);
            binding.layoutMiscellaneous.imageColor3.setImageResource(0);
            binding.layoutMiscellaneous.imageColor4.setImageResource(R.drawable.ic_done);
            binding.layoutMiscellaneous.imageColor5.setImageResource(0);
            setSubtitleIndicatorColor();
        });

        binding.layoutMiscellaneous.viewColor5.setOnClickListener(view -> {
            selectedNoteColor = "#000000";
            binding.layoutMiscellaneous.imageColor1.setImageResource(0);
            binding.layoutMiscellaneous.imageColor2.setImageResource(0);
            binding.layoutMiscellaneous.imageColor3.setImageResource(0);
            binding.layoutMiscellaneous.imageColor4.setImageResource(0);
            binding.layoutMiscellaneous.imageColor5.setImageResource(R.drawable.ic_done);
            setSubtitleIndicatorColor();
        });

        if (alreadyAvailableNote != null && alreadyAvailableNote.getColor() != null && !alreadyAvailableNote.getColor().trim().isEmpty()) {
            switch (alreadyAvailableNote.getColor()) {
                case "#FDBE3E":
                    binding.layoutMiscellaneous.viewColor2.performClick();
                    break;
                case "#FF4842":
                    binding.layoutMiscellaneous.viewColor3.performClick();
                    break;
                case "#3A52FC":
                    binding.layoutMiscellaneous.viewColor4.performClick();
                    break;
                case "#000000":
                    binding.layoutMiscellaneous.viewColor5.performClick();
                    break;
            }
        }

        binding.layoutMiscellaneous.layoutAddImage.setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            selectImage();
        });

        binding.layoutMiscellaneous.layoutAddUrl.setOnClickListener(view -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showAddUrlDialog();
        });

        if (alreadyAvailableNote != null) {
            binding.layoutMiscellaneous.layoutDeleteNote.setVisibility(View.VISIBLE);
            binding.layoutMiscellaneous.layoutDeleteNote.setOnClickListener(view -> {
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                showDeleteNoteDialog();
            });
        }
    }

    private void setSubtitleIndicatorColor() {
        GradientDrawable gradientDrawable = (GradientDrawable) binding.viewSubtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

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
        AlertDialog dialog = new AlertDialog.Builder(CreateNoteActivity.this)
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
        AlertDialog dialog = new AlertDialog.Builder(CreateNoteActivity.this)
                .setView(dialogBinding.getRoot())
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));

        dialogBinding.textDialogPositive.setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }

    private void showDeleteNoteDialog() {
        if (dialogDeleteNote == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            LayoutDialogBinding deleteNoteBinding = LayoutDialogBinding.inflate(LayoutInflater.from(this));
            deleteNoteBinding.imageDialog.setImageResource(R.drawable.ic_delete);
            deleteNoteBinding.textDialogTitle.setText(getString(R.string.delete_note));
            deleteNoteBinding.textDialogMessage.setText(getString(R.string.are_you_sure_you_want_to_delete_this_note));
            deleteNoteBinding.textDialogPositive.setText(getString(R.string.delete_note));
            deleteNoteBinding.textDialogNegative.setText(getString(android.R.string.cancel));
            builder.setView(deleteNoteBinding.getRoot());

            dialogDeleteNote = builder.create();

            if (dialogDeleteNote.getWindow() != null)
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));

            deleteNoteBinding.textDialogPositive.setOnClickListener(view -> {

                @SuppressWarnings("deprecation")
                @SuppressLint("StaticFieldLeak")
                class DeleteNoteTask extends AsyncTask<Void, Void, Void> {

                    @Nullable
                    @Override
                    protected Void doInBackground(Void... voids) {
                        NotesDatabase
                                .getNotesDatabase(getApplicationContext())
                                .noteDao().deleteNote(alreadyAvailableNote);
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void unused) {
                        super.onPostExecute(unused);
                        Intent intent = new Intent();
                        intent.putExtra(IS_NOTE_DELETED, true);
                        intent.putExtra(MainActivity.EXTRA_NOTE, MainActivity.REQUEST_CODE_UPDATE_NOTE);
                        setResult(RESULT_OK, intent);
                        finish();
                    }
                }
                new DeleteNoteTask().execute();
            });
            deleteNoteBinding.textDialogNegative.setOnClickListener(view -> dialogDeleteNote.dismiss());
        }
        dialogDeleteNote.show();
    }

    private final ActivityResultLauncher<Intent> selectImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageNote.setImageBitmap(bitmap);
                            binding.imageNote.setVisibility(View.VISIBLE);
                            binding.imageRemoveImage.setVisibility(View.VISIBLE);

                            selectedImagePath = getPathFromUri(selectedImageUri);

                        } catch (Exception e) {
                            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
    );

    private void selectImage() {
        if (ContextCompat.checkSelfPermission(
                getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            selectImageLauncher.launch(intent);
        } else {
            requestStorageReadPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
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

    private void showAddUrlDialog() {
        if (dialogAddUrl == null) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CreateNoteActivity.this);
            LayoutAddUrlBinding addUrlBinding = LayoutAddUrlBinding.inflate(LayoutInflater.from(this));
            if (alreadyAvailableNote != null)
                addUrlBinding.inputURL.setText(alreadyAvailableNote.getWebLink());
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
                    binding.textWebURL.setText(addUrlBinding.inputURL.getText().toString());
                    binding.layoutWebURL.setVisibility(View.VISIBLE);
                    dialogAddUrl.dismiss();
                }
            });
            addUrlBinding.textCancel.setOnClickListener(view -> dialogAddUrl.dismiss());
        }
        dialogAddUrl.show();
    }
}