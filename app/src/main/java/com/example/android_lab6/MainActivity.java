package com.example.android_lab6;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etTitle;
    private TextInputEditText etContent;
    private TextView tvEmpty;
    private MaterialButton btnAdd;
    private NoteAdapter adapter;

    private final OkHttpClient client = new OkHttpClient();

    private static final String BASE_URL = "http://10.0.2.2:8080/api/notes";
    private static final MediaType JSON =
            MediaType.get("application/json; charset=utf-8");

    private Long editingNoteId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etTitle = findViewById(R.id.etTitle);
        etContent = findViewById(R.id.etContent);
        tvEmpty = findViewById(R.id.tvEmpty);

        btnAdd = findViewById(R.id.btnAdd);
        MaterialButton btnLoad = findViewById(R.id.btnLoad);
        RecyclerView recyclerView = findViewById(R.id.recyclerNotes);

        adapter = new NoteAdapter(new NoteAdapter.OnNoteActionListener() {
            @Override
            public void onDelete(Note note) {
                deleteNote(note.getId());
            }

            @Override
            public void onEdit(Note note) {
                startEditing(note);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnLoad.setOnClickListener(v -> loadNotes());

        btnAdd.setOnClickListener(v -> {
            if (editingNoteId == null) {
                addNote();
            } else {
                updateNote();
            }
        });

        loadNotes();
    }

    private void startEditing(Note note) {
        editingNoteId = note.getId();
        etTitle.setText(note.getTitle());
        etContent.setText(note.getContent());
        btnAdd.setText("Зберегти");
        Toast.makeText(this, "Режим редагування", Toast.LENGTH_SHORT).show();
    }

    private void resetEditingMode() {
        editingNoteId = null;
        etTitle.setText("");
        etContent.setText("");
        btnAdd.setText("Додати");
    }

    private void loadNotes() {
        Request request = new Request.Builder()
                .url(BASE_URL)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                                "Помилка з'єднання: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Помилка сервера: " + response.code(),
                                    Toast.LENGTH_LONG).show());
                    return;
                }

                String responseBody = response.body() != null ? response.body().string() : "";

                try {
                    JSONArray jsonArray = new JSONArray(responseBody);
                    List<Note> noteList = new ArrayList<>();

                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject obj = jsonArray.getJSONObject(i);

                        long id = obj.getLong("id");
                        String title = obj.getString("title");
                        String content = obj.getString("content");

                        noteList.add(new Note(id, title, content));
                    }

                    runOnUiThread(() -> {
                        adapter.setNotes(noteList);
                        tvEmpty.setVisibility(noteList.isEmpty() ? View.VISIBLE : View.GONE);
                    });

                } catch (Exception e) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Помилка JSON: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void addNote() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Заповни всі поля", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", System.currentTimeMillis());
            jsonObject.put("title", title);
            jsonObject.put("content", content);

            RequestBody body = RequestBody.create(jsonObject.toString(), JSON);

            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Помилка відправки: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                }

                @Override
                public void onResponse(Call call, Response response) {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() ->
                                Toast.makeText(MainActivity.this,
                                        "Сервер повернув: " + response.code(),
                                        Toast.LENGTH_LONG).show());
                        return;
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                "Нотатку додано",
                                Toast.LENGTH_SHORT).show();
                        resetEditingMode();
                        loadNotes();
                    });
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Помилка JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateNote() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";

        if (editingNoteId == null) {
            Toast.makeText(this, "Немає нотатки для редагування", Toast.LENGTH_SHORT).show();
            return;
        }

        if (title.isEmpty() || content.isEmpty()) {
            Toast.makeText(this, "Заповни всі поля", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            JSONObject json = new JSONObject();
            json.put("id", editingNoteId);
            json.put("title", title);
            json.put("content", content);

            RequestBody body = RequestBody.create(json.toString(), JSON);

            Request request = new Request.Builder()
                    .url(BASE_URL)
                    .put(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onResponse(Call call, Response response) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this,
                                "Нотатку оновлено",
                                Toast.LENGTH_SHORT).show();
                        resetEditingMode();
                        loadNotes();
                    });
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            Toast.makeText(MainActivity.this,
                                    "Помилка оновлення: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                }
            });

        } catch (Exception e) {
            Toast.makeText(this, "Помилка JSON: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteNote(long id) {
        Request request = new Request.Builder()
                .url(BASE_URL + "/" + id)
                .delete()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Call call, Response response) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this,
                            "Нотатку видалено",
                            Toast.LENGTH_SHORT).show();

                    if (editingNoteId != null && editingNoteId == id) {
                        resetEditingMode();
                    }

                    loadNotes();
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this,
                                "Помилка видалення: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
            }
        });
    }
}