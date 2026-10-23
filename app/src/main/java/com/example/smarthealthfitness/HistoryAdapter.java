package com.example.smarthealthfitness;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

/**
 * Custom ArrayAdapter to display daily activity history records in a ListView.
 */
public class HistoryAdapter extends ArrayAdapter<DailyRecord> {

    private final Context context;
    private final List<DailyRecord> recordList;

    public HistoryAdapter(@NonNull Context context, @NonNull List<DailyRecord> records) {
        super(context, R.layout.item_history_record, records);
        this.context = context;
        this.recordList = records;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_history_record, parent, false);
            holder = new ViewHolder();
            holder.tvDate = convertView.findViewById(R.id.tvHistoryItemDate);
            holder.tvSteps = convertView.findViewById(R.id.tvHistoryItemSteps);
            holder.tvDuration = convertView.findViewById(R.id.tvHistoryItemDuration);
            holder.tvGoalBadge = convertView.findViewById(R.id.tvHistoryItemGoalBadge);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        DailyRecord record = recordList.get(position);

        if (record != null) {
            holder.tvDate.setText(String.format(Locale.getDefault(), "Date: %s", record.getDate()));
            holder.tvSteps.setText(String.format(Locale.getDefault(), "%,d", record.getSteps()));
            holder.tvDuration.setText(record.getFormattedDuration());

            if (record.isGoalNotified()) {
                holder.tvGoalBadge.setVisibility(View.VISIBLE);
                holder.tvGoalBadge.setText("Goal Achieved");
            } else {
                holder.tvGoalBadge.setVisibility(View.GONE);
            }
        }

        return convertView;
    }

    private static class ViewHolder {
        TextView tvDate;
        TextView tvSteps;
        TextView tvDuration;
        TextView tvGoalBadge;
    }
}
