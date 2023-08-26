package de.intelligence.antiautoupdate.persistence;

import java.util.List;
import java.util.Map;

import de.intelligence.antiautoupdate.model.AppEntryModel;

public interface ILocalDatabaseAccess {

    void insertIfNotExist(List<String> packages);

    boolean isExcluded(String packageName);

    void update(List<AppEntryModel> entries);

    boolean hasExclusionsOrChanges();

    Map<Boolean, String> getChangedAndExcluded();

    void removeChanges();

}
