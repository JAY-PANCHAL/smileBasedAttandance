package com.smileattendance.app.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
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
public final class EnrolledUserDao_Impl implements EnrolledUserDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<EnrolledUser> __insertionAdapterOfEnrolledUser;

  private final EmbeddingConverter __embeddingConverter = new EmbeddingConverter();

  private final SharedSQLiteStatement __preparedStmtOfDelete;

  public EnrolledUserDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfEnrolledUser = new EntityInsertionAdapter<EnrolledUser>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `enrolled_users` (`id`,`name`,`uniqueNumber`,`embedding`,`enrolledAtMillis`,`referencePhotoPath`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final EnrolledUser entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getUniqueNumber());
        final byte[] _tmp = __embeddingConverter.fromFloatArray(entity.getEmbedding());
        statement.bindBlob(4, _tmp);
        statement.bindLong(5, entity.getEnrolledAtMillis());
        statement.bindString(6, entity.getReferencePhotoPath());
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
  public Object getAll(final Continuation<? super List<EnrolledUser>> $completion) {
    final String _sql = "SELECT * FROM enrolled_users";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<EnrolledUser>>() {
      @Override
      @NonNull
      public List<EnrolledUser> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUniqueNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "uniqueNumber");
          final int _cursorIndexOfEmbedding = CursorUtil.getColumnIndexOrThrow(_cursor, "embedding");
          final int _cursorIndexOfEnrolledAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "enrolledAtMillis");
          final int _cursorIndexOfReferencePhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "referencePhotoPath");
          final List<EnrolledUser> _result = new ArrayList<EnrolledUser>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EnrolledUser _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUniqueNumber;
            _tmpUniqueNumber = _cursor.getString(_cursorIndexOfUniqueNumber);
            final float[] _tmpEmbedding;
            final byte[] _tmp;
            _tmp = _cursor.getBlob(_cursorIndexOfEmbedding);
            _tmpEmbedding = __embeddingConverter.toFloatArray(_tmp);
            final long _tmpEnrolledAtMillis;
            _tmpEnrolledAtMillis = _cursor.getLong(_cursorIndexOfEnrolledAtMillis);
            final String _tmpReferencePhotoPath;
            _tmpReferencePhotoPath = _cursor.getString(_cursorIndexOfReferencePhotoPath);
            _item = new EnrolledUser(_tmpId,_tmpName,_tmpUniqueNumber,_tmpEmbedding,_tmpEnrolledAtMillis,_tmpReferencePhotoPath);
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
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUniqueNumber = CursorUtil.getColumnIndexOrThrow(_cursor, "uniqueNumber");
          final int _cursorIndexOfEmbedding = CursorUtil.getColumnIndexOrThrow(_cursor, "embedding");
          final int _cursorIndexOfEnrolledAtMillis = CursorUtil.getColumnIndexOrThrow(_cursor, "enrolledAtMillis");
          final int _cursorIndexOfReferencePhotoPath = CursorUtil.getColumnIndexOrThrow(_cursor, "referencePhotoPath");
          final List<EnrolledUser> _result = new ArrayList<EnrolledUser>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EnrolledUser _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpUniqueNumber;
            _tmpUniqueNumber = _cursor.getString(_cursorIndexOfUniqueNumber);
            final float[] _tmpEmbedding;
            final byte[] _tmp;
            _tmp = _cursor.getBlob(_cursorIndexOfEmbedding);
            _tmpEmbedding = __embeddingConverter.toFloatArray(_tmp);
            final long _tmpEnrolledAtMillis;
            _tmpEnrolledAtMillis = _cursor.getLong(_cursorIndexOfEnrolledAtMillis);
            final String _tmpReferencePhotoPath;
            _tmpReferencePhotoPath = _cursor.getString(_cursorIndexOfReferencePhotoPath);
            _item = new EnrolledUser(_tmpId,_tmpName,_tmpUniqueNumber,_tmpEmbedding,_tmpEnrolledAtMillis,_tmpReferencePhotoPath);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
