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
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.prsnl.storage.entity.PageEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
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
public final class PageDao_Impl implements PageDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PageEntity> __insertionAdapterOfPageEntity;

  private final EntityDeletionOrUpdateAdapter<PageEntity> __deletionAdapterOfPageEntity;

  private final EntityDeletionOrUpdateAdapter<PageEntity> __updateAdapterOfPageEntity;

  public PageDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPageEntity = new EntityInsertionAdapter<PageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `pages` (`id`,`notebookId`,`pageIndex`,`width`,`height`,`backgroundType`,`lineSpacing`,`colorLight`,`colorDark`,`pdfSourceRef`,`elementFilePath`,`schemaVersion`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PageEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getNotebookId());
        statement.bindLong(3, entity.getPageIndex());
        statement.bindDouble(4, entity.getWidth());
        statement.bindDouble(5, entity.getHeight());
        statement.bindString(6, entity.getBackgroundType());
        if (entity.getLineSpacing() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getLineSpacing());
        }
        statement.bindLong(8, entity.getColorLight());
        statement.bindLong(9, entity.getColorDark());
        if (entity.getPdfSourceRef() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getPdfSourceRef());
        }
        statement.bindString(11, entity.getElementFilePath());
        statement.bindLong(12, entity.getSchemaVersion());
      }
    };
    this.__deletionAdapterOfPageEntity = new EntityDeletionOrUpdateAdapter<PageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `pages` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PageEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfPageEntity = new EntityDeletionOrUpdateAdapter<PageEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `pages` SET `id` = ?,`notebookId` = ?,`pageIndex` = ?,`width` = ?,`height` = ?,`backgroundType` = ?,`lineSpacing` = ?,`colorLight` = ?,`colorDark` = ?,`pdfSourceRef` = ?,`elementFilePath` = ?,`schemaVersion` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PageEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getNotebookId());
        statement.bindLong(3, entity.getPageIndex());
        statement.bindDouble(4, entity.getWidth());
        statement.bindDouble(5, entity.getHeight());
        statement.bindString(6, entity.getBackgroundType());
        if (entity.getLineSpacing() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getLineSpacing());
        }
        statement.bindLong(8, entity.getColorLight());
        statement.bindLong(9, entity.getColorDark());
        if (entity.getPdfSourceRef() == null) {
          statement.bindNull(10);
        } else {
          statement.bindString(10, entity.getPdfSourceRef());
        }
        statement.bindString(11, entity.getElementFilePath());
        statement.bindLong(12, entity.getSchemaVersion());
        statement.bindString(13, entity.getId());
      }
    };
  }

  @Override
  public Object insertPage(final PageEntity page, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPageEntity.insert(page);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePage(final PageEntity page, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfPageEntity.handle(page);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePage(final PageEntity page, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPageEntity.handle(page);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PageEntity>> getPagesForNotebook(final String notebookId) {
    final String _sql = "SELECT * FROM pages WHERE notebookId = ? ORDER BY pageIndex ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, notebookId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"pages"}, new Callable<List<PageEntity>>() {
      @Override
      @NonNull
      public List<PageEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNotebookId = CursorUtil.getColumnIndexOrThrow(_cursor, "notebookId");
          final int _cursorIndexOfPageIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "pageIndex");
          final int _cursorIndexOfWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "width");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfBackgroundType = CursorUtil.getColumnIndexOrThrow(_cursor, "backgroundType");
          final int _cursorIndexOfLineSpacing = CursorUtil.getColumnIndexOrThrow(_cursor, "lineSpacing");
          final int _cursorIndexOfColorLight = CursorUtil.getColumnIndexOrThrow(_cursor, "colorLight");
          final int _cursorIndexOfColorDark = CursorUtil.getColumnIndexOrThrow(_cursor, "colorDark");
          final int _cursorIndexOfPdfSourceRef = CursorUtil.getColumnIndexOrThrow(_cursor, "pdfSourceRef");
          final int _cursorIndexOfElementFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "elementFilePath");
          final int _cursorIndexOfSchemaVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "schemaVersion");
          final List<PageEntity> _result = new ArrayList<PageEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PageEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNotebookId;
            _tmpNotebookId = _cursor.getString(_cursorIndexOfNotebookId);
            final int _tmpPageIndex;
            _tmpPageIndex = _cursor.getInt(_cursorIndexOfPageIndex);
            final float _tmpWidth;
            _tmpWidth = _cursor.getFloat(_cursorIndexOfWidth);
            final float _tmpHeight;
            _tmpHeight = _cursor.getFloat(_cursorIndexOfHeight);
            final String _tmpBackgroundType;
            _tmpBackgroundType = _cursor.getString(_cursorIndexOfBackgroundType);
            final Float _tmpLineSpacing;
            if (_cursor.isNull(_cursorIndexOfLineSpacing)) {
              _tmpLineSpacing = null;
            } else {
              _tmpLineSpacing = _cursor.getFloat(_cursorIndexOfLineSpacing);
            }
            final int _tmpColorLight;
            _tmpColorLight = _cursor.getInt(_cursorIndexOfColorLight);
            final int _tmpColorDark;
            _tmpColorDark = _cursor.getInt(_cursorIndexOfColorDark);
            final String _tmpPdfSourceRef;
            if (_cursor.isNull(_cursorIndexOfPdfSourceRef)) {
              _tmpPdfSourceRef = null;
            } else {
              _tmpPdfSourceRef = _cursor.getString(_cursorIndexOfPdfSourceRef);
            }
            final String _tmpElementFilePath;
            _tmpElementFilePath = _cursor.getString(_cursorIndexOfElementFilePath);
            final int _tmpSchemaVersion;
            _tmpSchemaVersion = _cursor.getInt(_cursorIndexOfSchemaVersion);
            _item = new PageEntity(_tmpId,_tmpNotebookId,_tmpPageIndex,_tmpWidth,_tmpHeight,_tmpBackgroundType,_tmpLineSpacing,_tmpColorLight,_tmpColorDark,_tmpPdfSourceRef,_tmpElementFilePath,_tmpSchemaVersion);
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
  public Object getPageById(final String id, final Continuation<? super PageEntity> $completion) {
    final String _sql = "SELECT * FROM pages WHERE id = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PageEntity>() {
      @Override
      @Nullable
      public PageEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfNotebookId = CursorUtil.getColumnIndexOrThrow(_cursor, "notebookId");
          final int _cursorIndexOfPageIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "pageIndex");
          final int _cursorIndexOfWidth = CursorUtil.getColumnIndexOrThrow(_cursor, "width");
          final int _cursorIndexOfHeight = CursorUtil.getColumnIndexOrThrow(_cursor, "height");
          final int _cursorIndexOfBackgroundType = CursorUtil.getColumnIndexOrThrow(_cursor, "backgroundType");
          final int _cursorIndexOfLineSpacing = CursorUtil.getColumnIndexOrThrow(_cursor, "lineSpacing");
          final int _cursorIndexOfColorLight = CursorUtil.getColumnIndexOrThrow(_cursor, "colorLight");
          final int _cursorIndexOfColorDark = CursorUtil.getColumnIndexOrThrow(_cursor, "colorDark");
          final int _cursorIndexOfPdfSourceRef = CursorUtil.getColumnIndexOrThrow(_cursor, "pdfSourceRef");
          final int _cursorIndexOfElementFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "elementFilePath");
          final int _cursorIndexOfSchemaVersion = CursorUtil.getColumnIndexOrThrow(_cursor, "schemaVersion");
          final PageEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpNotebookId;
            _tmpNotebookId = _cursor.getString(_cursorIndexOfNotebookId);
            final int _tmpPageIndex;
            _tmpPageIndex = _cursor.getInt(_cursorIndexOfPageIndex);
            final float _tmpWidth;
            _tmpWidth = _cursor.getFloat(_cursorIndexOfWidth);
            final float _tmpHeight;
            _tmpHeight = _cursor.getFloat(_cursorIndexOfHeight);
            final String _tmpBackgroundType;
            _tmpBackgroundType = _cursor.getString(_cursorIndexOfBackgroundType);
            final Float _tmpLineSpacing;
            if (_cursor.isNull(_cursorIndexOfLineSpacing)) {
              _tmpLineSpacing = null;
            } else {
              _tmpLineSpacing = _cursor.getFloat(_cursorIndexOfLineSpacing);
            }
            final int _tmpColorLight;
            _tmpColorLight = _cursor.getInt(_cursorIndexOfColorLight);
            final int _tmpColorDark;
            _tmpColorDark = _cursor.getInt(_cursorIndexOfColorDark);
            final String _tmpPdfSourceRef;
            if (_cursor.isNull(_cursorIndexOfPdfSourceRef)) {
              _tmpPdfSourceRef = null;
            } else {
              _tmpPdfSourceRef = _cursor.getString(_cursorIndexOfPdfSourceRef);
            }
            final String _tmpElementFilePath;
            _tmpElementFilePath = _cursor.getString(_cursorIndexOfElementFilePath);
            final int _tmpSchemaVersion;
            _tmpSchemaVersion = _cursor.getInt(_cursorIndexOfSchemaVersion);
            _result = new PageEntity(_tmpId,_tmpNotebookId,_tmpPageIndex,_tmpWidth,_tmpHeight,_tmpBackgroundType,_tmpLineSpacing,_tmpColorLight,_tmpColorDark,_tmpPdfSourceRef,_tmpElementFilePath,_tmpSchemaVersion);
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
