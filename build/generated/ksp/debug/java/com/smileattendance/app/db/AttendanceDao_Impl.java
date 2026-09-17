package com.smileattendance.app.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AttendanceDao_Impl implements AttendanceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AttendanceRecord> __insertionAdapterOfAttendanceRecord;

  private final SharedSQLiteStatement __preparedStmtOfMarkSynced;

  public AttendanceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAttendanceRecord = new EntityInsertionAdapter<AttendanceRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `attendance_records` (`id`,`empCode`,`userName`,`hrid`,`timestampMillis`,`smileProbability`,`matchConfidence`,`photoPath`,`syncedToServer`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AttendanceRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getEmpCode());
        statement.bindString(3, entity.getUserName());
        statement.bindString(4, entity.getHrid());
        statement.bindLong(5, entity.getTimestampMillis());
        statement.bindDouble(6, entity.getSmileProbability());
        statement.bindDouble(7, entity.getMatchConfidence());
        statement.bindString(8, entity.getPhotoPath());
        final int _tmp = entity.getSyncedToServer() ? 1 : 0;
        statement.bindLong(9, _tmp);
      }
    };
    this.__preparedStmtOfMarkSynced = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE attendance_records SET syncedToServer = 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final AttendanceRecord record,
      final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfAttendanceRecord.insertAndReturnId(record);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object markSynced(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarkSynced.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfMarkSynced.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<AttendanceRecord>> observeAll() {
    final String _sql = "SELECT * FROM attendance_records ORDER BY timestampMillis DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"attendance_records"}, new Callable<List<AttendanceRecord>>() {
      @Override
      @NonNull
      public List<AttendanceRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEmpCode = CursorUtil.getColumnIndexOrThrow(_cursor, "empCode");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfHrid = CursorUtil.getColumnIndexOrThrow(_cursor, "hrid");
          final int _cursorIndexOfTimestampMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampMillis");
          final int _cursorIndexOfSmileProbability = CursorUtil.getColumnIndexOrThrow(_cursor, "smileProbability");
          final int _cursorIndexOfMatchConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "matchConfidence");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final int _cursorIndexOfSyncedToServer = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedToServer");
          final List<AttendanceRecord> _result = new ArrayList<AttendanceRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpEmpCode;
            _tmpEmpCode = _cursor.getInt(_cursorIndexOfEmpCode);
            final String _tmpUserName;
            _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            final String _tmpHrid;
            _tmpHrid = _cursor.getString(_cursorIndexOfHrid);
            final long _tmpTimestampMillis;
            _tmpTimestampMillis = _cursor.getLong(_cursorIndexOfTimestampMillis);
            final float _tmpSmileProbability;
            _tmpSmileProbability = _cursor.getFloat(_cursorIndexOfSmileProbability);
            final float _tmpMatchConfidence;
            _tmpMatchConfidence = _cursor.getFloat(_cursorIndexOfMatchConfidence);
            final String _tmpPhotoPath;
            _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            final boolean _tmpSyncedToServer;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSyncedToServer);
            _tmpSyncedToServer = _tmp != 0;
            _item = new AttendanceRecord(_tmpId,_tmpEmpCode,_tmpUserName,_tmpHrid,_tmpTimestampMillis,_tmpSmileProbability,_tmpMatchConfidence,_tmpPhotoPath,_tmpSyncedToServer);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getLastForEmployee(final int empCode,
      final Continuation<? super AttendanceRecord> $completion) {
    final String _sql = "SELECT * FROM attendance_records WHERE empCode = ? ORDER BY timestampMillis DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, empCode);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AttendanceRecord>() {
      @Override
      @Nullable
      public AttendanceRecord call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEmpCode = CursorUtil.getColumnIndexOrThrow(_cursor, "empCode");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfHrid = CursorUtil.getColumnIndexOrThrow(_cursor, "hrid");
          final int _cursorIndexOfTimestampMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampMillis");
          final int _cursorIndexOfSmileProbability = CursorUtil.getColumnIndexOrThrow(_cursor, "smileProbability");
          final int _cursorIndexOfMatchConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "matchConfidence");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final int _cursorIndexOfSyncedToServer = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedToServer");
          final AttendanceRecord _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpEmpCode;
            _tmpEmpCode = _cursor.getInt(_cursorIndexOfEmpCode);
            final String _tmpUserName;
            _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            final String _tmpHrid;
            _tmpHrid = _cursor.getString(_cursorIndexOfHrid);
            final long _tmpTimestampMillis;
            _tmpTimestampMillis = _cursor.getLong(_cursorIndexOfTimestampMillis);
            final float _tmpSmileProbability;
            _tmpSmileProbability = _cursor.getFloat(_cursorIndexOfSmileProbability);
            final float _tmpMatchConfidence;
            _tmpMatchConfidence = _cursor.getFloat(_cursorIndexOfMatchConfidence);
            final String _tmpPhotoPath;
            _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            final boolean _tmpSyncedToServer;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSyncedToServer);
            _tmpSyncedToServer = _tmp != 0;
            _result = new AttendanceRecord(_tmpId,_tmpEmpCode,_tmpUserName,_tmpHrid,_tmpTimestampMillis,_tmpSmileProbability,_tmpMatchConfidence,_tmpPhotoPath,_tmpSyncedToServer);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getUnsynced(final Continuation<? super List<AttendanceRecord>> $completion) {
    final String _sql = "SELECT * FROM attendance_records WHERE syncedToServer = 0 ORDER BY timestampMillis ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<AttendanceRecord>>() {
      @Override
      @NonNull
      public List<AttendanceRecord> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEmpCode = CursorUtil.getColumnIndexOrThrow(_cursor, "empCode");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfHrid = CursorUtil.getColumnIndexOrThrow(_cursor, "hrid");
          final int _cursorIndexOfTimestampMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampMillis");
          final int _cursorIndexOfSmileProbability = CursorUtil.getColumnIndexOrThrow(_cursor, "smileProbability");
          final int _cursorIndexOfMatchConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "matchConfidence");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final int _cursorIndexOfSyncedToServer = CursorUtil.getColumnIndexOrThrow(_cursor, "syncedToServer");
          final List<AttendanceRecord> _result = new ArrayList<AttendanceRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpEmpCode;
            _tmpEmpCode = _cursor.getInt(_cursorIndexOfEmpCode);
            final String _tmpUserName;
            _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            final String _tmpHrid;
            _tmpHrid = _cursor.getString(_cursorIndexOfHrid);
            final long _tmpTimestampMillis;
            _tmpTimestampMillis = _cursor.getLong(_cursorIndexOfTimestampMillis);
            final float _tmpSmileProbability;
            _tmpSmileProbability = _cursor.getFloat(_cursorIndexOfSmileProbability);
            final float _tmpMatchConfidence;
            _tmpMatchConfidence = _cursor.getFloat(_cursorIndexOfMatchConfidence);
            final String _tmpPhotoPath;
            _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            final boolean _tmpSyncedToServer;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSyncedToServer);
            _tmpSyncedToServer = _tmp != 0;
            _item = new AttendanceRecord(_tmpId,_tmpEmpCode,_tmpUserName,_tmpHrid,_tmpTimestampMillis,_tmpSmileProbability,_tmpMatchConfidence,_tmpPhotoPath,_tmpSyncedToServer);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
