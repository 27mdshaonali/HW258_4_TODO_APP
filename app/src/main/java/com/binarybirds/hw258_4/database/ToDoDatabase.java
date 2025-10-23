package com.binarybirds.hw258_4.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.binarybirds.hw258_4.models.ToDoItem;

@Database(entities = {ToDoItem.class}, version = 1, exportSchema = false)
public abstract class ToDoDatabase extends RoomDatabase {
    public abstract ToDoDao toDoDao();

    private static volatile ToDoDatabase INSTANCE;

    public static ToDoDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (ToDoDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    ToDoDatabase.class, "todo_database")
                            // For simplicity in this homework example only:
                            // allowMainThreadQueries() is avoided; we use Executors instead.
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
