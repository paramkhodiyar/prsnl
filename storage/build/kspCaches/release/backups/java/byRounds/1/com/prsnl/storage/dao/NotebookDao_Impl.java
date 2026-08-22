package com.prsnl.storage.dao;

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
import androidx.sqlite.db.SupportSQLiteStatement;
import com.prsnl.storage.entity.NotebookEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class NotebookDao_Impl implements NotebookDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<NotebookEntity> __insertionAdapterOfNotebookEntity;

  private final EntityDeletionOrUpdateAdapter<NotebookEntity> __deletionAdapterOfNotebookEntity;

  private final EntityDeletionOrUpdateAdapter<NotebookEntity> __updateAdapterOfNotebookEntity;

  private final SharedSQLiteStatement __preparedStmtOfUpdateNotebookFolderName;

  private final SharedSQLiteStatement __preparedStmtOfDeleteNotebookById;

  private final SharedSQLiteStatement __preparedStmtOfDeleteNotebooksByFolder;

  public NotebookDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfNotebookEntity = new EntityInsertionAdapter<NotebookEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `notebooks` (`id`,`title`,`createdAt`,`updatedAt`,`coverColor`,`coverStyle`,`folderName`,`pageIdsJson`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NotebookEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindLong(3, entity.getCreatedAt());
        statement.bindLong(4, entity.getUpdatedAt());
        statement.bindLong(5, entity.getCoverColor());
        statement.bindString(6, entity.getCoverStyle());
        statement.bindString(7, entity.getFolderName());
        statement.bindString(8, entity.getPageIdsJson());
      }
    };
    this.__deletionAdapterOfNotebookEntity = new EntityDeletionOrUpdateAdapter<NotebookEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `notebooks` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NotebookEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfNotebookEntity = new EntityDeletionOrUpdateAdapter<NotebookEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `notebooks` SET `id` = ?,`title` = ?,`createdAt` = ?,`updatedAt` = ?,`coverColor` = ?,`coverStyle` = ?,`folderName` = ?,`pageIdsJson` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final NotebookEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindLong(3, entity.getCreatedAt());
        statement.bindLong(4, entity.getUpdatedAt());
        statement.bindLong(5, entity.getCoverColor());
        statement.bindString(6, entity.getCoverStyle());
        statement.bindString(7, entity.getFolderName());
        statement.bindString(8, entity.getPageIdsJson());
        statement.bindString(9, entity.getId());
      }
    };
    this.__preparedStmtOfUpdateNotebookFolderName = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE notebooks SET folderName = ? WHERE folderName = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteNotebookById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM notebooks WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteNotebooksByFolder = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM notebooks WHERE folderName = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertNotebook(final NotebookEntity notebook,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfNotebookEntity.insert(notebook);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteNotebook(final NotebookEntity notebook,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfNotebookEntity.handle(notebook);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateNotebook(final NotebookEntity notebook,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfNotebookEntity.handle(notebook);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateNotebookFolderName(final String oldName, final String newName,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateNotebookFolderName.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, newName);
        _argIndex = 2;
        _stmt.bindString(_argIndex, oldName);
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
          __preparedStmtOfUpdateNotebookFolderName.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteNotebookById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteNotebookById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
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
          __preparedStmtOfDeleteNotebookById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteNotebooksByFolder(final String folderName,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteNotebooksByFolder.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, folderName);
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
          __preparedStmtOfDeleteNotebooksByFolder.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<NotebookEntity>> getAllNotebooks() {
    final String _sql = "SELECT * FROM notebooks ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notebooks"}, new Callable<List<NotebookEntity>>() {
      @Override
      @NonNull
      public List<NotebookEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfCoverColor = CursorUtil.getColumnIndexOrThrow(_cursor, "coverColor");
          final int _cursorIndexOfCoverStyle = CursorUtil.getColumnIndexOrThrow(_cursor, "coverStyle");
          final int _cursorIndexOfFolderName = CursorUtil.getColumnIndexOrThrow(_cursor, "folderName");
          final int _cursorIndexOfPageIdsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "pageIdsJson");
          final List<NotebookEntity> _result = new ArrayList<NotebookEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final NotebookEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final int _tmpCoverColor;
            _tmpCoverColor = _cursor.getInt(_cursorIndexOfCoverColor);
            final String _tmpCoverStyle;
            _tmpCoverStyle = _cursor.getString(_cursorIndexOfCoverStyle);
            final String _tmpFolderName;
            _tmpFolderName = _cursor.getString(_cursorIndexOfFolderName);
            final String _tmpPageIdsJson;
            _tmpPageIdsJson = _cursor.getString(_cursorIndexOfPageIdsJson);
            _item = new NotebookEntity(_tmpId,_tmpTitle,_tmpCreatedAt,_tmpUpdatedAt,_tmpCoverColor,_tmpCoverStyle,_tmpFolderName,_tmpPageIdsJson);
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
  public Object getNotebookById(final String id,
      final Continuation<? super NotebookEntity> $completion) {
    final String _sql = "SELECT * FROM notebooks WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<NotebookEntity>() {
      @Override
      @Nullable
      public NotebookEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfCoverColor = CursorUtil.getColumnIndexOrThrow(_cursor, "coverColor");
          final int _cursorIndexOfCoverStyle = CursorUtil.getColumnIndexOrThrow(_cursor, "coverStyle");
          final int _cursorIndexOfFolderName = CursorUtil.getColumnIndexOrThrow(_cursor, "folderName");
          final int _cursorIndexOfPageIdsJson = CursorUtil.getColumnIndexOrThrow(_cursor, "pageIdsJson");
          final NotebookEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final int _tmpCoverColor;
            _tmpCoverColor = _cursor.getInt(_cursorIndexOfCoverColor);
            final String _tmpCoverStyle;
            _tmpCoverStyle = _cursor.getString(_cursorIndexOfCoverStyle);
            final String _tmpFolderName;
            _tmpFolderName = _cursor.getString(_cursorIndexOfFolderName);
            final String _tmpPageIdsJson;
            _tmpPageIdsJson = _cursor.getString(_cursorIndexOfPageIdsJson);
            _result = new NotebookEntity(_tmpId,_tmpTitle,_tmpCreatedAt,_tmpUpdatedAt,_tmpCoverColor,_tmpCoverStyle,_tmpFolderName,_tmpPageIdsJson);
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
