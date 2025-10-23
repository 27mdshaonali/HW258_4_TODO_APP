package com.binarybirds.hw258_4.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Room Entity representing a todo item (also used as the app model).
 */
@Entity(tableName = "todos")
public class ToDoItem {

    @PrimaryKey
    private int id;            // Use server id when available; for local-only items we will generate ids

    private String todo;
    private boolean completed;
    private int userId;

    // Default constructor required by Room
    public ToDoItem() { }

    public ToDoItem(int id, String todo, boolean completed, int userId) {
        this.id = id;
        this.todo = todo;
        this.completed = completed;
        this.userId = userId;
    }

    // --- Getters & Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTodo() { return todo; }
    public void setTodo(String todo) { this.todo = todo; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}
