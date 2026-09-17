package com.smileattendance.app.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
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
public final class EnrolledUserDao_Impl implements EnrolledUserDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<EnrolledUser> __insertionAdapterOfEnrolledUser;

  private final EmbeddingConverter __embeddingConverter = new EmbeddingConverter();

  private final EntityDeletionOrUpdateAdapter<EnrolledUser> __updateAdapterOfEnrolledUser;

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  public EnrolledUserDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfEnrolledUser = new EntityInsertionAdapter<EnrolledUser>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `enrolled_users` (`id`,`empCode`,`name`,`hrid`,`embedding`,`enrolledAtMillis`,`referencePhotoPath`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EnrolledUser entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getEmpCode());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getHrid());
        final byte[] _tmp = __embeddingConverter.fromFloatArray(entity.getEmbedding());
        statement.bindBlob(5, _tmp);
        statement.bindLong(6, entity.getEnrolledAtMillis());
        statement.bindString(7, entity.getReferencePhotoPath());
      }
    };
    this.__updateAdapterOfEnrolledUser = new EntityDeletionOrUpdateAdapter<EnrolledUser>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `enrolled_users` SET `id` = ?,`empCode` = ?,`name` = ?,`hrid` = ?,`embedding` = ?,`enrolledAtMillis` = ?,`referencePhotoPath` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EnrolledUser entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getEmpCode());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getHrid());
        final byte[] _tmp = __embeddingConverter.fromFloatArray(entity.getEmbedding());
        statement.bindBlob(5, _tmp);
        statement.bindLong(6, entity.getEnrolledAtMillis());
        statement.bindString(7, entity.getReferencePhotoPath());
        statement.bindLong(8, entity.getId());
      }
    };
    this.__preparedStmtOfDelete = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM enrolled_users WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final EnrolledUser user, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfEnrolledUser.insertAndReturnId(user);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final EnrolledUser user, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfEnrolledUser.handle(user);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDelete.acquire();
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
          __preparedStmtOfDelete.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<EnrolledUser>> observeAll() {
    final String _sql = "SELECT * FROM enrolled_users";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"enrolled_users"}, new Callable<List<EnrolledUser>>() {
      @Override
      @NonNull
      public List<EnrolledUser> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEmpCode = CursorUtil.getColumnIndexOrThrow(_cursor, "empCode");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfHrid = CursorUtil.getColumnIndexOrThrow(_cursor, "hrid");
          final int _cursorIndexOfEmbedding = CursorUtil.getColumnIndexOrThrow(_cursor, "embedding");
          final int _cursorIndexOfEnrolledAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "enrolledAtMillis");
          final int _cursorIndexOfReferencePhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "referencePhotoPath");
          final List<EnrolledUser> _result = new ArrayList<EnrolledUser>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EnrolledUser _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpEmpCode;
            _tmpEmpCode = _cursor.getInt(_cursorIndexOfEmpCode);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpHrid;
            _tmpHrid = _cursor.getString(_cursorIndexOfHrid);
            final float[] _tmpEmbedding;
            final byte[] _tmp;
            _tmp = _cursor.getBlob(_cursorIndexOfEmbedding);
            _tmpEmbedding = __embeddingConverter.toFloatArray(_tmp);
            final long _tmpEnrolledAtMillis;
            _tmpEnrolledAtMillis = _cursor.getLong(_cursorIndexOfEnrolledAtMillis);
            final String _tmpReferencePhotoPath;
            _tmpReferencePhotoPath = _cursor.getString(_cursorIndexOfReferencePhotoPath);
            _item = new EnrolledUser(_tmpId,_tmpEmpCode,_tmpName,_tmpHrid,_tmpEmbedding,_tmpEnrolledAtMillis,_tmpReferencePhotoPath);
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
  public Object getByEmpCode(final int empCode,
      final Continuation<? super EnrolledUser> $completion) {
    final String _sql = "SELECT * FROM enrolled_users WHERE empCode = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, empCode);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<EnrolledUser>() {
      @Override
      @Nullable
      public EnrolledUser call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEmpCode = CursorUtil.getColumnIndexOrThrow(_cursor, "empCode");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfHrid = CursorUtil.getColumnIndexOrThrow(_cursor, "hrid");
          final int _cursorIndexOfEmbedding = CursorUtil.getColumnIndexOrThrow(_cursor, "embedding");
          final int _cursorIndexOfEnrolledAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "enrolledAtMillis");
          final int _cursorIndexOfReferencePhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "referencePhotoPath");
          final EnrolledUser _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final int _tmpEmpCode;
            _tmpEmpCode = _cursor.getInt(_cursorIndexOfEmpCode);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpHrid;
            _tmpHrid = _cursor.getString(_cursorIndexOfHrid);
            final float[] _tmpEmbedding;
            final byte[] _tmp;
            _tmp = _cursor.getBlob(_cursorIndexOfEmbedding);
            _tmpEmbedding = __embeddingConverter.toFloatArray(_tmp);
            final long _tmpEnrolledAtMillis;
            _tmpEnrolledAtMillis = _cursor.getLong(_cursorIndexOfEnrolledAtMillis);
            final String _tmpReferencePhotoPath;
            _tmpReferencePhotoPath = _cursor.getString(_cursorIndexOfReferencePhotoPath);
            _result = new EnrolledUser(_tmpId,_tmpEmpCode,_tmpName,_tmpHrid,_tmpEmbedding,_tmpEnrolledAtMillis,_tmpReferencePhotoPath);
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
  public Object deleteMissing(final List<Integer> empCodes,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("DELETE FROM enrolled_users WHERE empCode NOT IN (");
        final int _inputSize = empCodes.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (int _item : empCodes) {
          _stmt.bindLong(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
