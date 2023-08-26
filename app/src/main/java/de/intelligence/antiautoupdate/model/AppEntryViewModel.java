package de.intelligence.antiautoupdate.model;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import de.intelligence.antiautoupdate.AntiAutoUpdate;
import de.intelligence.antiautoupdate.persistence.ILocalDatabaseAccess;

public final class AppEntryViewModel extends ViewModel {

    private final MutableLiveData<List<AppEntryModel>> liveData;
    private final ILocalDatabaseAccess localDatabaseAccess;
    private final PackageManager pm;

    public AppEntryViewModel(Context context, ILocalDatabaseAccess localDatabaseAccess) {
        this.liveData = new MutableLiveData<>();
        this.localDatabaseAccess = localDatabaseAccess;
        this.pm = context.getPackageManager();
    }

    public LiveData<List<AppEntryModel>> getLiveData() {
        return this.liveData;
    }

    public void initializeAsync() {
        Executors.newSingleThreadExecutor().execute(() -> {
            // load all apps installed by the PlayStore
            final List<ApplicationInfo> appInfos = this.pm.getInstalledApplications(PackageManager.GET_META_DATA).stream()
                    .filter(appInfo -> !appInfo.packageName.equals(appInfo.loadLabel(this.pm).toString()))
                    .collect(Collectors.toList());

            // Add to own database if not yet done
            this.localDatabaseAccess.insertIfNotExist(appInfos.stream()
                    .map(a -> a.packageName)
                    .collect(Collectors.toList()));

            // create model objects and populate live data
            this.liveData.postValue(appInfos.stream()
                    .map(appInfo -> new AppEntryModel(appInfo.loadLabel(this.pm).toString(),
                            appInfo.loadIcon(this.pm), appInfo.packageName,
                            this.localDatabaseAccess.isExcluded(appInfo.packageName)))
                    .sorted(Comparator.comparing(a -> a.getAppName().toUpperCase()))
                    .collect(Collectors.toList()));
        });
    }

    public static final class AppEntryViewModelFactory implements ViewModelProvider.Factory {

        private final Context context;
        private final ILocalDatabaseAccess localDatabaseAccess;

        public AppEntryViewModelFactory(Context context, ILocalDatabaseAccess localDatabaseAccess) {
            this.context = context;
            this.localDatabaseAccess = localDatabaseAccess;

        }

        @NonNull
        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull @NotNull Class<T> modelClass) {
            return (T) new AppEntryViewModel(this.context, this.localDatabaseAccess);
        }

    }

}
