package com.smileattendance.app.db;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile EnrolledUserDao _enrolledUserDao;

  private volatile AttendanceDao _attendanceDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `enrolled_users` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `uniqueNumber` TEXT NOT NULL, `embedding` BLOB NOT NULL, `enrolledAtMillis` INTEGER NOT NULL, `referencePhotoPath` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `attendance_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `userId` INTEGER NOT NULL, `userName` TEXT NOT NULL, `userUniqueNumber` TEXT NOT NULL, `timestampMillis` INTEGER NOT NULL, `type` TEXT NOT NULL, `smileProbability` REAL NOT NULL, `matchConfidence` REAL NOT NULL, `photoPath` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '8afb09669124da3914e1dbbbbbfd370c')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `enrolled_users`");
        db.execSQL("DROP TABLE IF EXISTS `attendance_records`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsEnrolledUsers = new HashMap<String, TableInfo.Column>(6);
        _columnsEnrolledUsers.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEnrolledUsers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEnrolledUsers.put("uniqueNumber", new TableInfo.Column("uniqueNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEnrolledUsers.put("embedding", new TableInfo.Column("embedding", "BLOB", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEnrolledUsers.put("enrolledAtMillis", new TableInfo.Column("enrolledAtMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEnrolledUsers.put("referencePhotoPath", new TableInfo.Column("referencePhotoPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEnrolledUsers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesEnrolledUsers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoEnrolledUsers = new TableInfo("enrolled_users", _columnsEnrolledUsers, _foreignKeysEnrolledUsers, _indicesEnrolledUsers);
        final TableInfo _existingEnrolledUsers = TableInfo.read(db, "enrolled_users");
        if (!_infoEnrolledUsers.equals(_existingEnrolledUsers)) {
          return new RoomOpenHelper.ValidationResult(false, "enrolled_users(com.smileattendance.app.db.EnrolledUser).\n"
                  + " Expected:\n" + _infoEnrolledUsers + "\n"
                  + " Found:\n" + _existingEnrolledUsers);
        }
        final HashMap<String, TableInfo.Column> _columnsAttendanceRecords = new HashMap<String, TableInfo.Column>(9);
        _columnsAttendanceRecords.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendanceRecords.put("userId", new TableInfo.Column("userId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendanceRecords.put("userName", new TableInfo.Column("userName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendanceRecords.put("userUniqueNumber", new TableInfo.Column("userUniqueNumber", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendanceRecords.put("timestampMillis", new TableInfo.Column("timestampMillis", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendanceRecords.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendanceRecords.put("smileProbability", new TableInfo.Column("smileProbability", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendanceRecords.put("matchConfidence", new TableInfo.Column("matchConfidence", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAttendanceRecords.put("photoPath", new TableInfo.Column("photoPath", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAttendanceRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAttendanceRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAttendanceRecords = new TableInfo("attendance_records", _columnsAttendanceRecords, _foreignKeysAttendanceRecords, _indicesAttendanceRecords);
        final TableInfo _existingAttendanceRecords = TableInfo.read(db, "attendance_records");
        if (!_infoAttendanceRecords.equals(_existingAttendanceRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "attendance_records(com.smileattendance.app.db.AttendanceRecord).\n"
                  + " Expected:\n" + _infoAttendanceRecords + "\n"
                  + " Found:\n" + _existingAttendanceRecords);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "8afb09669124da3914e1dbbbbbfd370c", "89aeb379255765413e85660bd30247f6");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "enrolled_users","attendance_records");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `enrolled_users`");
      _db.execSQL("DELETE FROM `attendance_records`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(EnrolledUserDao.class, EnrolledUserDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AttendanceDao.class, AttendanceDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public EnrolledUserDao enrolledUserDao() {
    if (_enrolledUserDao != null) {
      return _enrolledUserDao;
    } else {
      synchronized(this) {
        if(_enrolledUserDao == null) {
          _enrolledUserDao = new EnrolledUserDao_Impl(this);
        }
        return _enrolledUserDao;
      }
    }
  }

  @Override
  public AttendanceDao attendanceDao() {
    if (_attendanceDao != null) {
      return _attendanceDao;
    } else {
      synchronized(this) {
        if(_attendanceDao == null) {
          _attendanceDao = new AttendanceDao_Impl(this);
        }
        return _attendanceDao;
      }
    }
  }
}
