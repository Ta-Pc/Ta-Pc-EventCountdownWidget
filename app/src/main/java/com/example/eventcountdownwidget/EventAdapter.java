package com.example.eventcountdownwidget;

import android.content.Context;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private List<EventSelectionActivity.CalendarEvent> events;
    private OnEventClickListener listener;

    public interface OnEventClickListener {
        void onEventClick(EventSelectionActivity.CalendarEvent event);
    }

    public EventAdapter(List<EventSelectionActivity.CalendarEvent> events, OnEventClickListener listener) {
        this.events = events;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.event_list_item, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventSelectionActivity.CalendarEvent event = events.get(position);
        holder.eventTitle.setText(event.getTitle());
        holder.eventDate.setText(event.getFormattedDate());

        // Make the card clickable with proper haptic feedback
        holder.cardView.setOnClickListener(v -> {
            if (listener != null) {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                listener.onEventClick(event);
            }
        });
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    public void updateList(List<EventSelectionActivity.CalendarEvent> newList) {
        this.events = newList;
        notifyDataSetChanged();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView eventTitle;
        TextView eventDate;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = (MaterialCardView) itemView;
            eventTitle = itemView.findViewById(R.id.event_title);
            eventDate = itemView.findViewById(R.id.event_date);
        }
    }
}
