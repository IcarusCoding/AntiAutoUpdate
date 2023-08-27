package de.intelligence.antiautoupdate.activity;

import java.util.List;
import java.util.function.Consumer;

import com.topjohnwu.superuser.Shell;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import de.intelligence.antiautoupdate.R;
import de.intelligence.antiautoupdate.adapter.AppViewAdapter;
import de.intelligence.antiautoupdate.databinding.MainActivityBinding;
import de.intelligence.antiautoupdate.fragment.RootDialogFragment;
import de.intelligence.antiautoupdate.layout.AppEntrySpacing;
import de.intelligence.antiautoupdate.model.AppEntryModel;
import de.intelligence.antiautoupdate.model.AppEntryViewModel;
import de.intelligence.antiautoupdate.persistence.LocalDatabaseAccess;
import de.intelligence.antiautoupdate.root.RootUtils;
import de.intelligence.antiautoupdate.worker.WorkerService;

public final class MainActivity extends AppCompatActivity implements RootDialogFragment.RootDialogListener,
        AppViewAdapter.StateChangedListener {

    private MainActivityBinding binding;
    private RecyclerView recyclerView;
    private AppViewAdapter appViewAdapter;
    private LocalDatabaseAccess localDatabaseAccess;
    private AppEntryViewModel appEntryViewModel;
    private FragmentManager fragmentManager;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        //    final SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        //TODO maybe use the splashscreen idk

        super.onCreate(savedInstanceState);

        this.binding = MainActivityBinding.inflate(getLayoutInflater());
        setContentView(this.binding.getRoot());

        this.fragmentManager = super.getSupportFragmentManager();

        // root check
        //TODO the first part will never be called bc in the init phase we dont know if we have root
        RootUtils.executeIfRootedOrElse(this::initialize, () -> {
            // Ask user for initial root access
            new RootDialogFragment().show(this.fragmentManager, "root_dialog");
        });
    }

    private void initialize(Shell shell) {
        this.localDatabaseAccess = new LocalDatabaseAccess(this);

        this.appViewAdapter = new AppViewAdapter(this, this.localDatabaseAccess);

        this.binding.searchInputEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                MainActivity.this.appViewAdapter.getFilter().filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        this.recyclerView = this.binding.appView;
        this.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        this.recyclerView.setAdapter(this.appViewAdapter);
        this.recyclerView.addItemDecoration(new AppEntrySpacing(10));

        this.appEntryViewModel = new ViewModelProvider(this,
                new AppEntryViewModel.AppEntryViewModelFactory(this, this.localDatabaseAccess))
                .get(AppEntryViewModel.class);

        final Consumer<List<AppEntryModel>> updateLabels = entries -> {
            final long excludedApps = entries.stream().filter(AppEntryModel::isExcluded).count();
            final long allApps = entries.size();
            final String templateString = super.getResources().getString(R.string.count_template);
            this.binding.blockedCount.setText(String.format(templateString, excludedApps, allApps));
            this.binding.allowedCount.setText(String.format(templateString, allApps - excludedApps, allApps));
        };

        this.appEntryViewModel.getLiveData().observe(this, entries -> {
            this.binding.appProgressBar.setVisibility(View.GONE);
            this.binding.appView.setVisibility(View.VISIBLE);
            updateLabels.accept(entries);
            this.appViewAdapter.addEntries(entries);
            this.appViewAdapter.notifyDataSetChanged();
        });

        final SharedPreferences preferences = super.getPreferences(Context.MODE_PRIVATE);

        this.binding.cancelButton.setOnClickListener(l -> this.appViewAdapter.dismissChanges());
        this.binding.applyButton.setOnClickListener(l -> {
            this.appViewAdapter.saveChanges();
            updateLabels.accept(this.appViewAdapter.getAppEntries());
            if (WorkerService.isRunning()) {
                super.stopService(new Intent(this, WorkerService.class));
                super.startForegroundService(new Intent(this, WorkerService.class));
            }
        });

        if (preferences.getBoolean("bg-service-running", false)) {
            this.binding.serviceButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_pause));
            super.startForegroundService(new Intent(this, WorkerService.class));
        }

        this.binding.serviceButton.setOnClickListener(l -> {
            if (preferences.getBoolean("bg-service-running", false)) {
                preferences.edit().putBoolean("bg-service-running", false).apply();
                super.stopService(new Intent(this, WorkerService.class));
                this.binding.serviceButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_play));
            } else {
                preferences.edit().putBoolean("bg-service-running", true).apply();
                super.startForegroundService(new Intent(this, WorkerService.class));
                this.binding.serviceButton.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_pause));
            }
        });

        // request app entries
        this.appEntryViewModel.initializeAsync();
    }

    @Override
    public void onDialogGrantClicked(RootDialogFragment rootDialogFragment) {
        // Trigger root access dialog
        Shell.getShell(shell -> {
            final Dialog dialog = rootDialogFragment.getDialog();
            if (!shell.isRoot() && dialog != null) {
                // user did not grant root
                // TODO maybe create another warning dialog
                final TextView dialogMessage = dialog.findViewById(R.id.dialogMessage);
                dialogMessage.setText(R.string.application_root_denied);
                dialog.findViewById(R.id.rootButton).setVisibility(View.GONE);
            } else {
                // user granted root access
                if (dialog != null) {
                    dialog.cancel();
                }
                this.initialize(shell);
            }
        });
    }

    @Override
    public void onStateChanged(boolean changed) {
        this.binding.changeContainer.setVisibility(changed ? View.VISIBLE : View.GONE);
    }

}
