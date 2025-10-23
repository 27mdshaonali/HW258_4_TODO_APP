package com.binarybirds.hw258_4;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.CheckBox;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.RequestQueue;
import com.binarybirds.hw258_4.adapters.ToDoAdapter;
import com.binarybirds.hw258_4.database.ToDoDao;
import com.binarybirds.hw258_4.database.ToDoDatabase;
import com.binarybirds.hw258_4.models.ToDoItem;
import com.makeramen.roundedimageview.RoundedImageView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * MainActivity — offline-first TODO app using Room + Volley.
 * Added: Add/Edit dialogs include userId (number) and a 'Mark as Completed' checkbox.
 */
public class MainActivity extends AppCompatActivity {

    // UI
    private RecyclerView recyclerView;
    private Spinner filterSpinner, sortSpinner;
    private RoundedImageView addToDo;

    // Data + adapter
    private final ArrayList<ToDoItem> todoList = new ArrayList<>();
    private final ArrayList<ToDoItem> filteredList = new ArrayList<>();
    private ToDoAdapter adapter;

    // DB & network
    private ToDoDatabase db;
    private ToDoDao toDoDao;
    private RequestQueue queue;
    private final Executor executor = Executors.newSingleThreadExecutor();

    // API
    private static final String API_URL = "https://dummyjson.com/todos";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // bind views
        recyclerView = findViewById(R.id.recyclerViewStatus);
        filterSpinner = findViewById(R.id.filterStatus);
        sortSpinner = findViewById(R.id.sortStatus);
        addToDo = findViewById(R.id.addToDo);

        // DB and network
        db = ToDoDatabase.getInstance(this);
        toDoDao = db.toDoDao();
        queue = Volley.newRequestQueue(this);

        // RecyclerView + Adapter
        adapter = new ToDoAdapter(this, filteredList, new ToDoAdapter.OnItemActionListener() {
            @Override
            public void onItemClick(ToDoItem item, int position) {
                toggleCompleted(item, position);
            }

            @Override
            public void onItemLongClick(ToDoItem item, int position) {
                showEditDialog(item, position);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // Swipe to delete
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView rv, RecyclerView.ViewHolder vh, RecyclerView.ViewHolder target) { return false; }

            @Override
            public void onSwiped(RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getAdapterPosition();
                ToDoItem item = adapter.getItemAt(pos);
                deleteTodoLocal(item, pos);
            }
        };
        new ItemTouchHelper(simpleCallback).attachToRecyclerView(recyclerView);

        // Spinners
        setupSpinners();

        // Load local data or network
        loadLocalData();

        // Add button
        addToDo.setOnClickListener(v -> showAddDialogInflated());
    }

    // ==== DB & Network helpers ====

    private void loadLocalData() {
        executor.execute(() -> {
            List<ToDoItem> fromDb = toDoDao.getAllTodos();
            if (fromDb == null || fromDb.isEmpty()) {
                fetchFromNetworkAndSave();
            } else {
                todoList.clear();
                todoList.addAll(fromDb);
                runOnUiThread(this::applyFilterCurrent);
            }
        });
    }

    private void fetchFromNetworkAndSave() {
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, API_URL, null, response -> {
            try {
                JSONArray arr = response.getJSONArray("todos");
                List<ToDoItem> items = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.getJSONObject(i);
                    ToDoItem t = new ToDoItem();
                    t.setId(o.getInt("id"));
                    t.setTodo(o.getString("todo"));
                    t.setCompleted(o.getBoolean("completed"));
                    t.setUserId(o.getInt("userId"));
                    items.add(t);
                }

                executor.execute(() -> {
                    toDoDao.deleteAll();
                    toDoDao.insertAll(items);
                    List<ToDoItem> fromDb = toDoDao.getAllTodos();
                    todoList.clear();
                    todoList.addAll(fromDb);
                    runOnUiThread(this::applyFilterCurrent);
                });

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }, error -> {
            runOnUiThread(() -> Toast.makeText(this, "Network load failed", Toast.LENGTH_SHORT).show());
            executor.execute(() -> {
                List<ToDoItem> fromDb = toDoDao.getAllTodos();
                todoList.clear();
                todoList.addAll(fromDb);
                runOnUiThread(this::applyFilterCurrent);
            });
        });

        queue.add(request);
    }

    private void insertTodoLocal(ToDoItem item) {
        executor.execute(() -> {
            List<ToDoItem> current = toDoDao.getAllTodos();
            int maxId = 0;
            for (ToDoItem t : current) if (t.getId() > maxId) maxId = t.getId();
            if (item.getId() == 0) item.setId(maxId + 1);

            toDoDao.insert(item);
            List<ToDoItem> fromDb = toDoDao.getAllTodos();
            todoList.clear();
            todoList.addAll(fromDb);

            runOnUiThread(() -> {
                applyFilterCurrent();
                recyclerView.scrollToPosition(0);
                Toast.makeText(this, "Saved locally", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void updateTodoLocal(ToDoItem item, int position) {
        executor.execute(() -> {
            toDoDao.update(item);
            List<ToDoItem> fromDb = toDoDao.getAllTodos();
            todoList.clear();
            todoList.addAll(fromDb);
            runOnUiThread(() -> {
                applyFilterCurrent();
                Toast.makeText(this, "Updated locally", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void deleteTodoLocal(ToDoItem item, int position) {
        executor.execute(() -> {
            toDoDao.delete(item);
            List<ToDoItem> fromDb = toDoDao.getAllTodos();
            todoList.clear();
            todoList.addAll(fromDb);
            runOnUiThread(() -> {
                adapter.removeAt(position);
                applyFilterCurrent();
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
            });
        });
    }

    // ==== Add & Edit dialogs (inflated layout) ====

    private void showAddDialogInflated() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_todo, null);
        EditText inputTodo = view.findViewById(R.id.inputTodoText);
        EditText inputUserId = view.findViewById(R.id.inputUserId);
        CheckBox inputCompleted = view.findViewById(R.id.inputCompleted);

        new AlertDialog.Builder(this)
                .setTitle("Add New ToDo")
                .setView(view)
                .setPositiveButton("Add", (d, w) -> {
                    String text = inputTodo.getText().toString().trim();
                    if (text.isEmpty()) {
                        Toast.makeText(this, "Please enter a task", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int userId = parseUserId(inputUserId.getText().toString());
                    boolean completed = inputCompleted.isChecked();
                    ToDoItem newItem = new ToDoItem(0, text, completed, userId);
                    insertTodoLocal(newItem);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditDialog(ToDoItem item, int position) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_todo, null);
        EditText inputTodo = view.findViewById(R.id.inputTodoText);
        EditText inputUserId = view.findViewById(R.id.inputUserId);
        CheckBox inputCompleted = view.findViewById(R.id.inputCompleted);

        // populate
        inputTodo.setText(item.getTodo());
        inputUserId.setText(String.valueOf(item.getUserId()));
        inputCompleted.setChecked(item.isCompleted());

        new AlertDialog.Builder(this)
                .setTitle("Edit ToDo")
                .setView(view)
                .setPositiveButton("Save", (d, w) -> {
                    String text = inputTodo.getText().toString().trim();
                    if (text.isEmpty()) { Toast.makeText(this, "Please enter a task", Toast.LENGTH_SHORT).show(); return; }
                    int userId = parseUserId(inputUserId.getText().toString());
                    boolean completed = inputCompleted.isChecked();

                    item.setTodo(text);
                    item.setUserId(userId);
                    item.setCompleted(completed);

                    updateTodoLocal(item, position);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private int parseUserId(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return 1; // default if invalid
        }
    }

    // ==== Item actions ====

    private void toggleCompleted(ToDoItem item, int position) {
        item.setCompleted(!item.isCompleted());
        updateTodoLocal(item, position);
    }

    // ==== Filtering & Sorting ====

    private void setupSpinners() {
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(this,
                R.array.filter_options, android.R.layout.simple_spinner_item);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(filterAdapter);

        ArrayAdapter<CharSequence> sortAdapter = ArrayAdapter.createFromResource(this,
                R.array.sort_options, android.R.layout.simple_spinner_item);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(sortAdapter);

        filterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { applyFilterCurrent(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        sortSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) { applyFilterCurrent(); }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void applyFilterCurrent() {
        String filter = (String) filterSpinner.getSelectedItem();
        String sort = (String) sortSpinner.getSelectedItem();

        filteredList.clear();
        if ("Completed".equals(filter)) {
            for (ToDoItem t : todoList) if (t.isCompleted()) filteredList.add(t);
        } else if ("Pending".equals(filter)) {
            for (ToDoItem t : todoList) if (!t.isCompleted()) filteredList.add(t);
        } else {
            filteredList.addAll(todoList);
        }

        if ("Newest First".equals(sort)) {
            Collections.sort(filteredList, (a, b) -> b.getId() - a.getId());
        } else if ("Oldest First".equals(sort)) {
            Collections.sort(filteredList, Comparator.comparingInt(ToDoItem::getId));
        } else if ("Alphabetical".equals(sort)) {
            Collections.sort(filteredList, (a, b) -> a.getTodo().compareToIgnoreCase(b.getTodo()));
        }

        adapter.notifyDataSetChanged();
    }
}
