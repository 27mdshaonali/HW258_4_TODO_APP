package com.binarybirds.hw258_4.adapters;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.binarybirds.hw258_4.R;
import com.binarybirds.hw258_4.models.ToDoItem;

import java.util.List;

/**
 * RecyclerView adapter. Exposes a click listener to the activity so MainActivity
 * can handle toggles and long-clicks (delete/edit).
 */
public class ToDoAdapter extends RecyclerView.Adapter<ToDoAdapter.ToDoViewHolder> {

    private final Context context;
    private final List<ToDoItem> list;
    private final OnItemActionListener listener;

    public ToDoAdapter(Context context, List<ToDoItem> list, OnItemActionListener listener) {
        this.context = context;
        this.list = list;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ToDoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_todo, parent, false);
        return new ToDoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ToDoViewHolder holder, int position) {
        ToDoItem item = list.get(position);
        holder.textTodo.setText(item.getTodo());
        holder.textUserId.setText("User ID: " + item.getUserId());

        // Visuals for completed / pending
        if (item.isCompleted()) {
            holder.imageStatus.setImageResource(R.drawable.ic_done);
            holder.textTodo.setTextColor(ContextCompat.getColor(context, android.R.color.holo_green_dark));
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.todo_completed_bg));
            // strike-through
            holder.textTodo.setPaintFlags(holder.textTodo.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.imageStatus.setImageResource(R.drawable.ic_pending);
            holder.textTodo.setTextColor(ContextCompat.getColor(context, android.R.color.holo_red_dark));
            holder.cardView.setCardBackgroundColor(ContextCompat.getColor(context, R.color.todo_pending_bg));
            // remove strike-through
            holder.textTodo.setPaintFlags(holder.textTodo.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item, position);
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onItemLongClick(item, position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public ToDoItem getItemAt(int position) {
        return list.get(position);
    }

    public void removeAt(int position) {
        list.remove(position);
        notifyItemRemoved(position);
    }

    public void insertAt(int position, ToDoItem item) {
        list.add(position, item);
        notifyItemInserted(position);
    }

    public interface OnItemActionListener {
        void onItemClick(ToDoItem item, int position);      // toggle complete
        void onItemLongClick(ToDoItem item, int position);  // edit
    }

    static class ToDoViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imageStatus;
        TextView textTodo, textUserId;

        ToDoViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardToDo);
            imageStatus = itemView.findViewById(R.id.imageStatus);
            textTodo = itemView.findViewById(R.id.textTodo);
            textUserId = itemView.findViewById(R.id.textUserId);
        }
    }
}
