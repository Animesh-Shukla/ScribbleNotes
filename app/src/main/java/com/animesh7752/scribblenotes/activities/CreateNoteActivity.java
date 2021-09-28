package com.animesh7752.scribblenotes.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.animesh7752.scribblenotes.R;
import com.animesh7752.scribblenotes.database.NotesDatabase;
import com.animesh7752.scribblenotes.entities.Note;
import com.google.android.material.bottomsheet.BottomSheetBehavior;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.BitSet;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

import top.defaults.colorpicker.ColorPickerPopup;

public class CreateNoteActivity extends AppCompatActivity {

    private EditText inputNoteTitle, inputNoteSubtitle, inputNoteText;
    private TextView textDateTime;
    private String selectedNoteColor;
    private String selectedImagePath;
    private View viewSubtitleIndicator;
    private ImageView imageNote;
    private TextView textWebURL;
    private LinearLayout layoutWebURL;
    private SeekBar seekBarTextSize;
    private static final int REQUEST_CODE_STORAGE_PERMISSION=1;
    private static final int REQUEST_CODE_SELECT_IMAGE=2;
    private int seekValue;
    private AlertDialog dialogAddURL;
    private AlertDialog dialogDeleteNote;
    private Note alreadyAvailableNote;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_note);
        ImageView imageback = findViewById(R.id.ImageBack);
        imageback.setOnClickListener(v -> onBackPressed());
        inputNoteTitle= findViewById(R.id.InputNoteTitle);
        inputNoteSubtitle= findViewById(R.id.InputNoteSubtitle);
        inputNoteText= findViewById(R.id.InputNote);
        textDateTime= findViewById(R.id.textDateTime);
        viewSubtitleIndicator= findViewById(R.id.ViewSubtitleIndicator);
        imageNote = findViewById(R.id.imageNote);
        textWebURL= findViewById(R.id.textWebURL);
        layoutWebURL= findViewById(R.id.layoutWebURL);
        textDateTime.setText(new SimpleDateFormat("EEEE, dd MMMM yyyy HH:mm a", Locale.getDefault()).format(new Date()));

        ImageView imageSave=findViewById(R.id.imageSave);
        imageSave.setOnClickListener(v -> saveNote());
        selectedNoteColor="#333333";
        selectedImagePath = "";

        if(getIntent().getBooleanExtra("isViewOrUpdate",false)){
            alreadyAvailableNote = (Note)getIntent().getSerializableExtra("note");
            setViewOrUpdateNote();
        }

        findViewById(R.id.imageRemoveWebURL).setOnClickListener(v -> {
            textWebURL.setText(null);
            layoutWebURL.setVisibility(View.GONE);
        });

        findViewById(R.id.rotateImage).setOnClickListener(v -> {
            //imageNote.setRotation(imageNote.getRotation()+90);
            //imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
            //Bitmap bitmap = BitmapFactory.decodeStream(selectedImagePath);
            Bitmap bitmap= BitmapFactory.decodeFile(selectedImagePath);
            Matrix matrix = new Matrix();
            matrix.setRotate(90f);
            imageNote.setImageBitmap(Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true));
            
        });
        findViewById(R.id.imageRemoveImage).setOnClickListener(v -> {
            imageNote.setImageBitmap(null);
            imageNote.setVisibility(View.GONE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.GONE);
            findViewById(R.id.rotateImage).setVisibility(View.GONE);
            selectedImagePath = "";
        });

        if(getIntent().getBooleanExtra("isFromQuickActions",false)){
            String type= getIntent().getStringExtra("quickActionsType");
            if(type!= null){
                if(type.equals("image")){
                    selectedImagePath= getIntent().getStringExtra("imagePath");
                    imageNote.setImageBitmap(BitmapFactory.decodeFile(selectedImagePath));
                    imageNote.setVisibility(View.VISIBLE);
                    findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
                }else if(type.equals("URL")){
                    textWebURL.setText(getIntent().getStringExtra("URL"));
                    layoutWebURL.setVisibility(View.VISIBLE);
                }
            }
        }
        seekBarTextSize=findViewById(R.id.seekBar);

        initMiscellaneous();
        setSubtitleIndicatorColor();
    }

    private void setViewOrUpdateNote(){
        inputNoteTitle.setText(alreadyAvailableNote.getTitle());
        inputNoteSubtitle.setText(alreadyAvailableNote.getSubtitle());
        inputNoteText.setText(alreadyAvailableNote.getNoteText());
        textDateTime.setText(alreadyAvailableNote.getDateTime());
        if(alreadyAvailableNote.getImagePath() != null && !alreadyAvailableNote.getImagePath().trim().isEmpty()){
            imageNote.setImageBitmap(BitmapFactory.decodeFile(alreadyAvailableNote.getImagePath()));
            imageNote.setVisibility(View.VISIBLE);
            findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);
            findViewById(R.id.rotateImage).setVisibility(View.VISIBLE);
            selectedImagePath = alreadyAvailableNote.getImagePath();
        }
        if(alreadyAvailableNote.getWebLink() != null && !alreadyAvailableNote.getWebLink().trim().isEmpty()){
            textWebURL.setText(alreadyAvailableNote.getWebLink());
            layoutWebURL.setVisibility(View.VISIBLE);

        }
    }

    private void saveNote(){
        if(inputNoteTitle.getText().toString().trim().isEmpty()){
            Toast.makeText(this,"Note title cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }else if(inputNoteSubtitle.getText().toString().trim().isEmpty() && inputNoteText.getText().toString().trim().isEmpty()){
            Toast.makeText(this,"Note cannot be empty!!", Toast.LENGTH_SHORT).show();
            return;
        }

        final Note note = new Note();
        note.setTitle(inputNoteTitle.getText().toString());
        note.setSubtitle(inputNoteSubtitle.getText().toString());
        note.setNoteText(inputNoteText.getText().toString());
        note.setDateTime(textDateTime.getText().toString());
        note.setColor(selectedNoteColor);
        note.setImagePath(selectedImagePath);

        if(layoutWebURL.getVisibility()== View.VISIBLE){
            note.setWebLink(textWebURL.getText().toString());
        }

        if(alreadyAvailableNote!= null ){
            note.setId(alreadyAvailableNote.getId());
        }
        @SuppressLint("StaticFieldLeak")
        class SaveNoteTask extends AsyncTask<Void,Void,Void>{
            @Override
            protected Void doInBackground(Void... voids) {
                NotesDatabase.getDatabase(getApplicationContext()).noteDao().insertNote(note);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                Intent intent=new Intent();
                setResult(RESULT_OK,intent);
                finish();
            }
        }
        new SaveNoteTask().execute();
    }
    private void initMiscellaneous() {
        final LinearLayout layoutMiscellaneous= findViewById(R.id.layoutMiscellaneous);
        final BottomSheetBehavior<LinearLayout> bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous);
        layoutMiscellaneous.findViewById(R.id.textMiscellaneous).setOnClickListener(v -> {
            if(bottomSheetBehavior.getState()!=BottomSheetBehavior.STATE_EXPANDED){
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }else{
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
        seekBarTextSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekValue=progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                inputNoteText.setTextSize(seekValue);
            }
        });
        final Button buttonColorPicker= layoutMiscellaneous.findViewById(R.id.colorPicker);
        buttonColorPicker.setOnClickListener(v -> new ColorPickerPopup.Builder(CreateNoteActivity.this).initialColor(Color.RED).enableAlpha(true)
                .okTitle("CHOOSE").cancelTitle("CANCLE")
                .showIndicator(true)
                .build().show(v, new ColorPickerPopup.ColorPickerObserver() {
            @Override
            public void onColorPicked(int color) {
                String hexColor = String.format("#%06X", (0xFFFFFF & color));
                selectedNoteColor=hexColor;
                setSubtitleIndicatorColor();
            }

            @Override
            public void onColor(int color, boolean fromUser) {
                String hexColor = String.format("#%06X", (0xFFFFFF & color));
                selectedNoteColor=hexColor;
                setSubtitleIndicatorColor();
            }
        }));
        layoutMiscellaneous.findViewById(R.id.layoutAddImage).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            if(ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(CreateNoteActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSION);
            }else{
                selectImage();
            }
        });
        layoutMiscellaneous.findViewById(R.id.layoutAddUrl).setOnClickListener(v -> {
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            showAddURLDialog();
        });

        if(alreadyAvailableNote!=null){
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setVisibility(View.VISIBLE);
            layoutMiscellaneous.findViewById(R.id.layoutDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    showDeleteNoteDialog();
                }
            });
        }
    }

    private void showDeleteNoteDialog(){
        if(dialogDeleteNote == null){
            AlertDialog.Builder builder= new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_delete_note,(ViewGroup)findViewById(R.id.layoutDeleteNoteContainer));
            builder.setView(view);
            dialogDeleteNote= builder.create();
            if(dialogDeleteNote.getWindow()!=null){
                dialogDeleteNote.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }
            view.findViewById(R.id.textDeleteNote).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    @SuppressLint("StaticFieldLeak")
                    class DeleteNoteTask extends AsyncTask<Void,Void,Void>{

                        @Override
                        protected Void doInBackground(Void... voids) {
                            NotesDatabase.getDatabase(getApplicationContext()).noteDao().deleteNote(alreadyAvailableNote);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            super.onPostExecute(aVoid);
                            Intent intent= new Intent();
                            intent.putExtra("isNoteDeleted",true);
                            setResult(RESULT_OK,intent);
                            finish();
                        }
                    }
                    new DeleteNoteTask().execute();
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(v -> dialogDeleteNote.dismiss());
        }
        dialogDeleteNote.show();
    }

    private void setSubtitleIndicatorColor(){
        GradientDrawable gradientDrawable=(GradientDrawable)viewSubtitleIndicator.getBackground();
        gradientDrawable.setColor(Color.parseColor(selectedNoteColor));
    }

    private void selectImage(){
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if(intent.resolveActivity(getPackageManager()) != null){
            startActivityForResult(intent, REQUEST_CODE_SELECT_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_CODE_STORAGE_PERMISSION && grantResults.length>0){
            if(grantResults[0]== PackageManager.PERMISSION_GRANTED){
                selectImage();
            }else{
                Toast.makeText(this,"Permission Denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE_SELECT_IMAGE && resultCode==RESULT_OK){
            if(data != null){
                Uri selectedImageUri = data.getData();
                if(selectedImageUri != null){
                    try{

                        InputStream inputStream= getContentResolver().openInputStream(selectedImageUri);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        imageNote.setImageBitmap(bitmap);
                        imageNote.setVisibility(View.VISIBLE);
                        findViewById(R.id.imageRemoveImage).setVisibility(View.VISIBLE);

                        selectedImagePath= getPathFromUri(selectedImageUri );
                    }catch (Exception exception){
                        Toast.makeText(this,exception.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    }

    private String getPathFromUri(Uri contentUri){
        String filePath;
        Cursor cursor = getContentResolver().query(contentUri,null,null,null,null);
        if(cursor == null){
            filePath = contentUri.getPath();
        }else{
            cursor.moveToFirst();
            int index = cursor.getColumnIndex("_data");
            filePath = cursor.getString(index);
            cursor.close();
        }
        return filePath;
    }

    private void showAddURLDialog(){
        if(dialogAddURL == null){
            AlertDialog.Builder builder= new AlertDialog.Builder(CreateNoteActivity.this);
            View view = LayoutInflater.from(this).inflate(R.layout.layout_add_url, findViewById(R.id.layoutAddUrlContainer));
            builder.setView(view);
            dialogAddURL = builder.create();
            if(dialogAddURL.getWindow()!= null){
                dialogAddURL.getWindow().setBackgroundDrawable(new ColorDrawable(0));
            }

            final EditText inputURL = view.findViewById(R.id.inputURL);
            inputURL.requestFocus();

            view.findViewById(R.id.textAdd).setOnClickListener(v -> {
                if(inputURL.getText().toString().trim().isEmpty()){
                    Toast.makeText(CreateNoteActivity.this,"URL not entered!",Toast.LENGTH_SHORT).show();
                }else if(!Patterns.WEB_URL.matcher(inputURL.getText().toString()).matches()){
                    Toast.makeText(CreateNoteActivity.this,"Enter valid URL",Toast.LENGTH_SHORT).show();
                }else{
                    textWebURL.setText(inputURL.getText().toString());
                    layoutWebURL.setVisibility(View.VISIBLE);
                    dialogAddURL.dismiss();
                }
            });

            view.findViewById(R.id.textCancel).setOnClickListener(v -> dialogAddURL.dismiss());
        }
        dialogAddURL.show();
    }
}