package com.example.hersafe.ui.features.reports;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.hersafe.R;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GovernorateAdapter extends RecyclerView.Adapter<GovernorateAdapter.ViewHolder> {

    private final List<Map.Entry<String, Integer>> governorates = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String governorateName, int count);
    }

    public GovernorateAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setData(Map<String, Integer> data) {
        governorates.clear();
        governorates.addAll(data.entrySet());
        // Sort by count descending
        governorates.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_governorate, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map.Entry<String, Integer> entry = governorates.get(position);
        holder.bind(entry.getKey(), entry.getValue(), listener);
    }

    @Override
    public int getItemCount() {
        return governorates.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvCount;

        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvGovernorateName);
            tvCount = itemView.findViewById(R.id.tvAlertCount);
        }

        void bind(String name, int count, OnItemClickListener listener) {
            tvName.setText(name);
            tvCount.setText(count + " بلاغ");
            
            // Adjust badge color based on count
            // Simple visual cue: > 10 is very bad, > 0 is warn
            
            itemView.setOnClickListener(v -> listener.onItemClick(name, count));
        }
    }
}
