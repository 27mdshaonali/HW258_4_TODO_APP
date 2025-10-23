package com.binarybirds.hw258_4.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.binarybirds.hw258_4.models.ToDoItem;

import java.util.List;

@Dao
public interface ToDoDao {

    @Query("SELECT * FROM todos ORDER BY id DESC")
    List<ToDoItem> getAllTodos();

    @Insert
    void insert(ToDoItem todo);

    @Insert
    void insertAll(List<ToDoItem> todos);

    @Update
    void update(ToDoItem todo);

    @Delete
    void delete(ToDoItem todo);

    @Query("DELETE FROM todos")
    void deleteAll();
}
