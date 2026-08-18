package com.smileattendance.app.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
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
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class AttendanceDao_Impl implements AttendanceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<AttendanceRecord> __insertionAdapterOfAttendanceRecord;

  private final AttendanceTypeConverter __attendanceTypeConverter = new AttendanceTypeConverter();

  public AttendanceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfAttendanceRecord = new EntityInsertionAdapter<AttendanceRecord>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `attendance_records` (`id`,`userId`,`userName`,`userUniqueNumber`,`timestampMillis`,`type`,`smileProbability`,`matchConfidence`,`photoPath`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final AttendanceRecord entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getUserId());
        statement.bindString(3, entity.getUserName());
        statement.bindString(4, entity.getUserUniqueNumber());
        statement.bindLong(5, entity.getTimestampMillis());
        final String _tmp = __attendanceTypeConverter.fromType(entity.getType());
        statement.bindString(6, _tmp);
        statement.bindDouble(7, entity.getSmileProbability());
        statement.bindDouble(8, entity.getMatchConfidence());
        statement.bindString(9, entity.getPhotoPath());
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
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfUserUniqueNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "userUniqueNumber");
          final int _cursorIndexOfTimestampMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampMillis");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSmileProbability = CursorUtil.getColumnIndexOrThrow(_cursor, "smileProbability");
          final int _cursorIndexOfMatchConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "matchConfidence");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final List<AttendanceRecord> _result = new ArrayList<AttendanceRecord>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final AttendanceRecord _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpUserId;
            _tmpUserId = _cursor.getLong(_cursorIndexOfUserId);
            final String _tmpUserName;
            _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            final String _tmpUserUniqueNumber;
            _tmpUserUniqueNumber = _cursor.getString(_cursorIndexOfUserUniqueNumber);
            final long _tmpTimestampMillis;
            _tmpTimestampMillis = _cursor.getLong(_cursorIndexOfTimestampMillis);
            final AttendanceType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __attendanceTypeConverter.toType(_tmp);
            final float _tmpSmileProbability;
            _tmpSmileProbability = _cursor.getFloat(_cursorIndexOfSmileProbability);
            final float _tmpMatchConfidence;
            _tmpMatchConfidence = _cursor.getFloat(_cursorIndexOfMatchConfidence);
            final String _tmpPhotoPath;
            _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            _item = new AttendanceRecord(_tmpId,_tmpUserId,_tmpUserName,_tmpUserUniqueNumber,_tmpTimestampMillis,_tmpType,_tmpSmileProbability,_tmpMatchConfidence,_tmpPhotoPath);
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
  public Object getLastForUserSince(final long userId, final long sinceMillis,
      final Continuation<? super AttendanceRecord> $completion) {
    final String _sql = "SELECT * FROM attendance_records WHERE userId = ? AND timestampMillis >= ? ORDER BY timestampMillis DESC LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, userId);
    _argIndex = 2;
    _statement.bindLong(_argIndex, sinceMillis);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<AttendanceRecord>() {
      @Override
      @Nullable
      public AttendanceRecord call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfUserName = CursorUtil.getColumnIndexOrThrow(_cursor, "userName");
          final int _cursorIndexOfUserUniqueNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "userUniqueNumber");
          final int _cursorIndexOfTimestampMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "timestampMillis");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfSmileProbability = CursorUtil.getColumnIndexOrThrow(_cursor, "smileProbability");
          final int _cursorIndexOfMatchConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "matchConfidence");
          final int _cursorIndexOfPhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "photoPath");
          final AttendanceRecord _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpUserId;
            _tmpUserId = _cursor.getLong(_cursorIndexOfUserId);
            final String _tmpUserName;
            _tmpUserName = _cursor.getString(_cursorIndexOfUserName);
            final String _tmpUserUniqueNumber;
            _tmpUserUniqueNumber = _cursor.getString(_cursorIndexOfUserUniqueNumber);
            final long _tmpTimestampMillis;
            _tmpTimestampMillis = _cursor.getLong(_cursorIndexOfTimestampMillis);
            final AttendanceType _tmpType;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfType);
            _tmpType = __attendanceTypeConverter.toType(_tmp);
            final float _tmpSmileProbability;
            _tmpSmileProbability = _cursor.getFloat(_cursorIndexOfSmileProbability);
            final float _tmpMatchConfidence;
            _tmpMatchConfidence = _cursor.getFloat(_cursorIndexOfMatchConfidence);
            final String _tmpPhotoPath;
            _tmpPhotoPath = _cursor.getString(_cursorIndexOfPhotoPath);
            _result = new AttendanceRecord(_tmpId,_tmpUserId,_tmpUserName,_tmpUserUniqueNumber,_tmpTimestampMillis,_tmpType,_tmpSmileProbability,_tmpMatchConfidence,_tmpPhotoPath);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
