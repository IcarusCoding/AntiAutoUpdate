package de.intelligence.antiautoupdate.adapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import android.annotation.SuppressLint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import de.intelligence.antiautoupdate.R;
import de.intelligence.antiautoupdate.model.AppEntryModel;
import de.intelligence.antiautoupdate.persistence.ILocalDatabaseAccess;

public final class AppViewAdapter extends RecyclerView.Adapter<AppViewAdapter.AppViewHolder> implements Filterable {

    public interface StateChangedListener {

        void onStateChanged(boolean changed);

    }

    private final Map<String, Boolean> appEntriesOriginal;
    private final List<AppEntryModel> appEntries;
    private final List<AppEntryModel> appEntriesFiltered;
    private final Map<String, Integer> changedEntries;
    private final StateChangedListener listener;
    private final ILocalDatabaseAccess localDatabaseAccess;

    public AppViewAdapter(StateChangedListener listener, ILocalDatabaseAccess localDatabaseAccess) {
        this.appEntriesOriginal = new HashMap<>();
        this.appEntries = new ArrayList<>();
        this.appEntriesFiltered = new ArrayList<>();
        this.changedEntries = new HashMap<>();
        this.listener = listener;
        this.localDatabaseAccess = localDatabaseAccess;
    }

    public void addEntries(List<AppEntryModel> entries) {
        this.updateOriginals(entries);
        this.appEntries.addAll(entries);
        this.appEntriesFiltered.addAll(this.appEntries);
    }

    public void updateOriginals(List<AppEntryModel> entries) {
        this.appEntriesOriginal.clear();
        for (final AppEntryModel entryModel : entries) {
            this.appEntriesOriginal.put(entryModel.getPackageName(), entryModel.isExcluded());
        }
    }

    public List<AppEntryModel> getAppEntries() {
        return this.appEntries;
    }

    public void dismissChanges() {
        for (final Map.Entry<String, Integer> entry : this.changedEntries.entrySet()) {
            final String packageName = entry.getKey();
            this.appEntries.stream().filter(e -> e.getPackageName().equals(packageName)).findFirst().ifPresent(e -> {
                final Boolean originalExcluded = this.appEntriesOriginal.get(packageName);
                if (originalExcluded != null) {
                    e.setExcluded(originalExcluded);
                    super.notifyItemChanged(entry.getValue());
                }
            });
        }
        this.changedEntries.clear();
        this.listener.onStateChanged(false);
    }

    public void saveChanges() {
        this.localDatabaseAccess.update(this.appEntries.stream()
                .filter(e -> this.changedEntries.containsKey(e.getPackageName()))
                .collect(Collectors.toList()));
        this.updateOriginals(this.appEntries);
        this.changedEntries.clear();
        this.listener.onStateChanged(false);
    }

    @NonNull
    @NotNull
    @Override
    public AppViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        final View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_entry, parent, false);
        return new AppViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull AppViewHolder holder, int position) {
        final AppEntryModel entry = this.appEntriesFiltered.get(position);

        holder.appName.setText(entry.getAppName());
        holder.appPackageName.setText(entry.getPackageName());
        holder.appIcon.setImageDrawable(entry.getAppIcon());

        holder.appToggleSwitch.setChecked(entry.isExcluded());
        holder.appToggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            entry.setExcluded(isChecked);
            if (Boolean.TRUE.equals(this.appEntriesOriginal.get(entry.getPackageName())) != isChecked) {
                this.changedEntries.put(entry.getPackageName(), position);
                if (this.changedEntries.size() == 1) {
                    this.listener.onStateChanged(true);
                }
            } else {
                this.changedEntries.remove(entry.getPackageName());
                if (this.changedEntries.isEmpty()) {
                    this.listener.onStateChanged(false);
                }
            }
        });
    }

    @Override
    public void onViewRecycled(@NonNull @NotNull AppViewHolder holder) {
        super.onViewRecycled(holder);
        holder.appToggleSwitch.setOnCheckedChangeListener(null);
    }

    @Override
    public int getItemCount() {
        return this.appEntriesFiltered.size();
    }

    @Override
    public Filter getFilter() {
        return new AppViewFilter();
    }

    public static final class AppViewHolder extends RecyclerView.ViewHolder {

        private final ImageView appIcon;
        private final TextView appName;
        private final TextView appPackageName;
        private final SwitchCompat appToggleSwitch;

        public AppViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            this.appIcon = itemView.findViewById(R.id.appIcon);
            this.appName = itemView.findViewById(R.id.appName);
            this.appPackageName = itemView.findViewById(R.id.appPackageName);
            this.appToggleSwitch = itemView.findViewById(R.id.appToggleSwitch);
        }

    }

    public final class AppViewFilter extends Filter {

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            final String filterCleaned = constraint.toString().toLowerCase().trim();
            final List<AppEntryModel> filtered;
            if (filterCleaned.isEmpty()) {
                filtered = AppViewAdapter.this.appEntries;
            } else {
                filtered = new ArrayList<>();
                for (final AppEntryModel entry : AppViewAdapter.this.appEntries) {
                    if (entry.getAppName().toLowerCase().contains(filterCleaned) ||
                            entry.getPackageName().toLowerCase().contains(filterCleaned)) {
                        filtered.add(entry);
                    }
                }
            }
            final FilterResults results = new FilterResults();
            results.values = filtered;
            return results;
        }


        @Override
        @SuppressWarnings("unchecked")
        @SuppressLint("NotifyDataSetChanged")
        protected void publishResults(CharSequence constraint, FilterResults results) {
            AppViewAdapter.this.appEntriesFiltered.clear();
            AppViewAdapter.this.appEntriesFiltered.addAll((List<AppEntryModel>) results.values);
            AppViewAdapter.super.notifyDataSetChanged();
        }

    }

}
