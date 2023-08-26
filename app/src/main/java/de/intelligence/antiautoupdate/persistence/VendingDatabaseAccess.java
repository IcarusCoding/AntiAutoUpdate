package de.intelligence.antiautoupdate.persistence;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class VendingDatabaseAccess extends JDBCSQLiteHelper implements IVendingDatabaseAccess {

    private static final String GET_ALL_PACKAGE_NAMES = "SELECT doc_id FROM %s";
    private static final String GET_ALL_EXCLUDED = "SELECT doc_id FROM %s WHERE library_id = 'u-wl'";
    private static final String EXCLUDE_PACKAGE = "UPDATE %s SET library_id = 'u-wl' WHERE doc_id = ?";
    private static final String ALLOW_PACKAGE = "UPDATE %s SET library_id = '3' WHERE doc_id = ?";

    public VendingDatabaseAccess(File file) {
        super(file, "ownership");
    }

    @Override
    public List<String> getAllPackageNames() {
        return this.extractStrings(GET_ALL_PACKAGE_NAMES);
    }

    @Override
    public void excludePackage(String packageName) {
        super.executeUpdate(super.prepareTable(EXCLUDE_PACKAGE), packageName);
    }

    @Override
    public List<String> getAllExcluded() {
        return this.extractStrings(GET_ALL_EXCLUDED);
    }

    @Override
    public void allowPackage(String packageName) {
        super.executeUpdate(super.prepareTable(ALLOW_PACKAGE), packageName);
    }

    private List<String> extractStrings(String query) {
        return super.executeQuery(super.prepareTable(query), rs -> {
            final List<String> packageNames = new ArrayList<>();
            while(rs.next()) {
                packageNames.add(rs.getString(1));
            }
            return packageNames;
        });
    }

}
