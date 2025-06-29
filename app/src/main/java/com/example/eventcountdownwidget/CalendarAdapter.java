package com.example.eventcountdownwidget;

import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private List<CalendarSettingsActivity.CalendarInfo> calendars;
    private OnCalendarCheckListener listener;

    public interface OnCalendarCheckListener {
        void onCalendarChecked(CalendarSettingsActivity.CalendarInfo calendarInfo, boolean isChecked);
    }

    public CalendarAdapter(List<CalendarSettingsActivity.CalendarInfo> calendars, OnCalendarCheckListener listener) {
        this.calendars = calendars;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.calendar_list_item, parent, false);
        return new CalendarViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        CalendarSettingsActivity.CalendarInfo calendar = calendars.get(position);

        holder.calendarName.setText(calendar.getDisplayName());
        holder.accountName.setText(calendar.getAccountName());

        // Set color indicator
        GradientDrawable colorCircle = (GradientDrawable) holder.colorIndicator.getBackground();
        colorCircle.setColor(calendar.getColor());

        // Set checkbox without triggering listener
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(calendar.isSelected());

        // Set click listeners
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            calendar.setSelected(isChecked);
            if (listener != null) {
                listener.onCalendarChecked(calendar, isChecked);
            }
        });

        // Make the whole row clickable
        holder.itemView.setOnClickListener(v -> {
            boolean newState = !holder.checkBox.isChecked();
            holder.checkBox.setChecked(newState);
        });
    }

    @Override
    public int getItemCount() {
        return calendars.size();
    }

    static class CalendarViewHolder extends RecyclerView.ViewHolder {
        TextView calendarName;
        TextView accountName;
        View colorIndicator;
        CheckBox checkBox;

        public CalendarViewHolder(@NonNull View itemView) {
            super(itemView);
            calendarName = itemView.findViewById(R.id.calendar_name);
            accountName = itemView.findViewById(R.id.account_name);
            colorIndicator = itemView.findViewById(R.id.color_indicator);
            checkBox = itemView.findViewById(R.id.calendar_checkbox);
        }
    }
}

