package de.intelligence.antiautoupdate.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;

import de.intelligence.antiautoupdate.model.AppEntryModel;

public final class LocalDatabaseAccess extends JDBCSQLiteHelper implements ILocalDatabaseAccess {

    private static final String DB_NAME = "anti_auto_update.db";
    private static final String TABLE_NAME = "app_states";

    public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS %s (app TEXT PRIMARY KEY, excluded INTEGER, changed INTEGER)";
    public static final String INSERT_IF_NOT_EXIST = "INSERT OR IGNORE INTO %s (app, excluded, changed) VALUES (?, ?, ?)";
    public static final String IS_EXCLUDED = "SELECT excluded FROM %s WHERE app = ?";
    public static final String UPDATE = "UPDATE %s SET excluded = ?, changed = 1 WHERE app = ?";
    public static final String GET_CHANGES = "SELECT excluded, app FROM %s WHERE changed = 1 OR excluded = 1";
    public static final String HAS_EXCLUSIONS_OR_CHANGES = "SELECT COUNT(*) FROM %s WHERE changed = 1 OR excluded = 1";
    public static final String REMOVE_CHANGES = "UPDATE %s SET changed = 0 WHERE changed = 1";

    public LocalDatabaseAccess(Context context) {
        super(context.getDatabasePath(DB_NAME), TABLE_NAME);
        super.executeUpdate(super.prepareTable(CREATE_TABLE));
    }

    @Override
    public void insertIfNotExist(List<String> packages) {
        for (final String p : packages) {
            super.executeUpdate(super.prepareTable(INSERT_IF_NOT_EXIST), p, false, false);
        }
    }

    @Override
    public boolean isExcluded(String packageName) {
        return super.executeQuery(super.prepareTable(IS_EXCLUDED), rs -> rs.getBoolean(1), packageName);
    }

    @Override
    public void update(List<AppEntryModel> entries) {
        for (final AppEntryModel entry : entries) {
            super.executeUpdate(super.prepareTable(UPDATE), entry.isExcluded(), entry.getPackageName());
        }
    }

    @Override
    public boolean hasExclusionsOrChanges() {
        return super.executeQuery(super.prepareTable(HAS_EXCLUSIONS_OR_CHANGES), rs -> rs.getInt(1) != 0);
    }

    @Override
    public Map<Boolean, String> getChangedAndExcluded() {
        return super.executeQuery(super.prepareTable(GET_CHANGES), rs -> {
            final Map<Boolean, String> changes = new HashMap<>();
            while (rs.next()) {
                changes.put(rs.getBoolean(1), rs.getString(2));
            }
            return changes;
        });
    }

    @Override
    public void removeChanges() {
        super.executeUpdate(super.prepareTable(REMOVE_CHANGES));
    }

}
