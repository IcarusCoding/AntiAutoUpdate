package de.intelligence.antiautoupdate.persistence;

import java.util.List;

public interface IVendingDatabaseAccess {

    List<String> getAllPackageNames();

    void excludePackage(String packageName);

    List<String> getAllExcluded();

    void allowPackage(String packageName);

}
