package com.binarybirds.hw258_4.models;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a ToDo item stored in the local Room database.
 */
@Entity(tableName = "todos")
public class ToDoEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String todo;
    private boolean completed;
    private int userId;

    public ToDoEntity(String todo, boolean completed, int userId) {
        this.todo = todo;
        this.completed = completed;
        this.userId = userId;
    }

    // --- Getters and Setters ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTodo() { return todo; }
    public void setTodo(String todo) { this.todo = todo; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
}
