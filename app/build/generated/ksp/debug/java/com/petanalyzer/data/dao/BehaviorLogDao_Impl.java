package com.petanalyzer.data.dao;

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
import com.petanalyzer.data.entity.BehaviorLogEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class BehaviorLogDao_Impl implements BehaviorLogDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BehaviorLogEntity> __insertionAdapterOfBehaviorLogEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteOlderThan;

  public BehaviorLogDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBehaviorLogEntity = new EntityInsertionAdapter<BehaviorLogEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `behavior_logs` (`id`,`timestamp`,`date_key`,`pet_type`,`behavior`,`emotion`,`confidence`,`description`,`snapshot_path`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BehaviorLogEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getTimestamp());
        statement.bindString(3, entity.getDateKey());
        statement.bindString(4, entity.getPetType());
        statement.bindString(5, entity.getBehavior());
        statement.bindString(6, entity.getEmotion());
        statement.bindDouble(7, entity.getConfidence());
        statement.bindString(8, entity.getDescription());
        if (entity.getSnapshotPath() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getSnapshotPath());
        }
      }
    };
    this.__preparedStmtOfDeleteOlderThan = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM behavior_logs WHERE timestamp < ?";
        return _query;
      }
    };
  }

  @Override
  public Object insert(final BehaviorLogEntity log, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfBehaviorLogEntity.insertAndReturnId(log);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteOlderThan(final long before, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteOlderThan.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, before);
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
          __preparedStmtOfDeleteOlderThan.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<BehaviorLogEntity>> getLogsByDate(final String dateKey) {
    final String _sql = "SELECT * FROM behavior_logs WHERE date_key = ? ORDER BY timestamp DESC LIMIT 200";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, dateKey);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"behavior_logs"}, new Callable<List<BehaviorLogEntity>>() {
      @Override
      @NonNull
      public List<BehaviorLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDateKey = CursorUtil.getColumnIndexOrThrow(_cursor, "date_key");
          final int _cursorIndexOfPetType = CursorUtil.getColumnIndexOrThrow(_cursor, "pet_type");
          final int _cursorIndexOfBehavior = CursorUtil.getColumnIndexOrThrow(_cursor, "behavior");
          final int _cursorIndexOfEmotion = CursorUtil.getColumnIndexOrThrow(_cursor, "emotion");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfSnapshotPath = CursorUtil.getColumnIndexOrThrow(_cursor, "snapshot_path");
          final List<BehaviorLogEntity> _result = new ArrayList<BehaviorLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BehaviorLogEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpDateKey;
            _tmpDateKey = _cursor.getString(_cursorIndexOfDateKey);
            final String _tmpPetType;
            _tmpPetType = _cursor.getString(_cursorIndexOfPetType);
            final String _tmpBehavior;
            _tmpBehavior = _cursor.getString(_cursorIndexOfBehavior);
            final String _tmpEmotion;
            _tmpEmotion = _cursor.getString(_cursorIndexOfEmotion);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpSnapshotPath;
            if (_cursor.isNull(_cursorIndexOfSnapshotPath)) {
              _tmpSnapshotPath = null;
            } else {
              _tmpSnapshotPath = _cursor.getString(_cursorIndexOfSnapshotPath);
            }
            _item = new BehaviorLogEntity(_tmpId,_tmpTimestamp,_tmpDateKey,_tmpPetType,_tmpBehavior,_tmpEmotion,_tmpConfidence,_tmpDescription,_tmpSnapshotPath);
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
  public Flow<List<String>> getRecentDates(final int limit) {
    final String _sql = "SELECT DISTINCT date_key FROM behavior_logs ORDER BY date_key DESC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"behavior_logs"}, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
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
  public Flow<Integer> getLogCountForDate(final String dateKey) {
    final String _sql = "SELECT COUNT(*) FROM behavior_logs WHERE date_key = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, dateKey);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"behavior_logs"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Object getActivityCountsForDate(final String dateKey,
      final Continuation<? super List<ActivityCount>> $completion) {
    final String _sql = "\n"
            + "        SELECT behavior, COUNT(*) as cnt FROM behavior_logs\n"
            + "        WHERE date_key = ? AND behavior IN ('WALKING', 'RUNNING', 'PLAYING', 'SLEEPING', 'EATING', 'SITTING', 'LYING_DOWN')\n"
            + "        GROUP BY behavior\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, dateKey);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<ActivityCount>>() {
      @Override
      @NonNull
      public List<ActivityCount> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfBehavior = 0;
          final int _cursorIndexOfCnt = 1;
          final List<ActivityCount> _result = new ArrayList<ActivityCount>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ActivityCount _item;
            final String _tmpBehavior;
            _tmpBehavior = _cursor.getString(_cursorIndexOfBehavior);
            final int _tmpCnt;
            _tmpCnt = _cursor.getInt(_cursorIndexOfCnt);
            _item = new ActivityCount(_tmpBehavior,_tmpCnt);
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
  public Flow<List<DailyCount>> getActivityDailyCounts(final String startDate) {
    final String _sql = "\n"
            + "        SELECT date_key, COUNT(*) as cnt FROM behavior_logs\n"
            + "        WHERE date_key >= ? AND behavior IN ('WALKING', 'RUNNING', 'PLAYING')\n"
            + "        GROUP BY date_key ORDER BY date_key ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, startDate);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"behavior_logs"}, new Callable<List<DailyCount>>() {
      @Override
      @NonNull
      public List<DailyCount> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDateKey = 0;
          final int _cursorIndexOfCnt = 1;
          final List<DailyCount> _result = new ArrayList<DailyCount>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DailyCount _item;
            final String _tmpDateKey;
            _tmpDateKey = _cursor.getString(_cursorIndexOfDateKey);
            final int _tmpCnt;
            _tmpCnt = _cursor.getInt(_cursorIndexOfCnt);
            _item = new DailyCount(_tmpDateKey,_tmpCnt);
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
  public Object getDominantEmotion(final String dateKey,
      final Continuation<? super EmotionCount> $completion) {
    final String _sql = "\n"
            + "        SELECT emotion, COUNT(*) as cnt FROM behavior_logs\n"
            + "        WHERE date_key = ?\n"
            + "        GROUP BY emotion ORDER BY cnt DESC LIMIT 1\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, dateKey);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<EmotionCount>() {
      @Override
      @Nullable
      public EmotionCount call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfEmotion = 0;
          final int _cursorIndexOfCnt = 1;
          final EmotionCount _result;
          if (_cursor.moveToFirst()) {
            final String _tmpEmotion;
            _tmpEmotion = _cursor.getString(_cursorIndexOfEmotion);
            final int _tmpCnt;
            _tmpCnt = _cursor.getInt(_cursorIndexOfCnt);
            _result = new EmotionCount(_tmpEmotion,_tmpCnt);
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
  public Object getEmotionCountsInRange(final String startDate, final String endDate,
      final Continuation<? super List<DateEmotionCount>> $completion) {
    final String _sql = "\n"
            + "        SELECT date_key, emotion, COUNT(*) as cnt FROM behavior_logs\n"
            + "        WHERE date_key >= ? AND date_key <= ?\n"
            + "        GROUP BY date_key, emotion ORDER BY date_key ASC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, startDate);
    _argIndex = 2;
    _statement.bindString(_argIndex, endDate);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<DateEmotionCount>>() {
      @Override
      @NonNull
      public List<DateEmotionCount> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfDateKey = 0;
          final int _cursorIndexOfEmotion = 1;
          final int _cursorIndexOfCnt = 2;
          final List<DateEmotionCount> _result = new ArrayList<DateEmotionCount>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final DateEmotionCount _item;
            final String _tmpDateKey;
            _tmpDateKey = _cursor.getString(_cursorIndexOfDateKey);
            final String _tmpEmotion;
            _tmpEmotion = _cursor.getString(_cursorIndexOfEmotion);
            final int _tmpCnt;
            _tmpCnt = _cursor.getInt(_cursorIndexOfCnt);
            _item = new DateEmotionCount(_tmpDateKey,_tmpEmotion,_tmpCnt);
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
  public Object countScratchingSince(final long since,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM behavior_logs WHERE timestamp > ? AND behavior = 'SCRATCHING'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Object countSleepingSince(final long since,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM behavior_logs WHERE timestamp > ? AND behavior = 'SLEEPING'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Object countSadSince(final long since, final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM behavior_logs WHERE timestamp > ? AND emotion = 'SAD'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Object countActiveSince(final long since,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM behavior_logs WHERE timestamp > ? AND behavior IN ('WALKING', 'RUNNING', 'PLAYING')";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Object getLogsSince(final long since,
      final Continuation<? super List<BehaviorLogEntity>> $completion) {
    final String _sql = "SELECT * FROM behavior_logs WHERE timestamp > ? AND pet_type != 'UNKNOWN'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, since);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BehaviorLogEntity>>() {
      @Override
      @NonNull
      public List<BehaviorLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDateKey = CursorUtil.getColumnIndexOrThrow(_cursor, "date_key");
          final int _cursorIndexOfPetType = CursorUtil.getColumnIndexOrThrow(_cursor, "pet_type");
          final int _cursorIndexOfBehavior = CursorUtil.getColumnIndexOrThrow(_cursor, "behavior");
          final int _cursorIndexOfEmotion = CursorUtil.getColumnIndexOrThrow(_cursor, "emotion");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfSnapshotPath = CursorUtil.getColumnIndexOrThrow(_cursor, "snapshot_path");
          final List<BehaviorLogEntity> _result = new ArrayList<BehaviorLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BehaviorLogEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpDateKey;
            _tmpDateKey = _cursor.getString(_cursorIndexOfDateKey);
            final String _tmpPetType;
            _tmpPetType = _cursor.getString(_cursorIndexOfPetType);
            final String _tmpBehavior;
            _tmpBehavior = _cursor.getString(_cursorIndexOfBehavior);
            final String _tmpEmotion;
            _tmpEmotion = _cursor.getString(_cursorIndexOfEmotion);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpSnapshotPath;
            if (_cursor.isNull(_cursorIndexOfSnapshotPath)) {
              _tmpSnapshotPath = null;
            } else {
              _tmpSnapshotPath = _cursor.getString(_cursorIndexOfSnapshotPath);
            }
            _item = new BehaviorLogEntity(_tmpId,_tmpTimestamp,_tmpDateKey,_tmpPetType,_tmpBehavior,_tmpEmotion,_tmpConfidence,_tmpDescription,_tmpSnapshotPath);
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
  public Object getLogsForDate(final String dateKey,
      final Continuation<? super List<BehaviorLogEntity>> $completion) {
    final String _sql = "SELECT * FROM behavior_logs WHERE date_key = ? ORDER BY timestamp ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, dateKey);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BehaviorLogEntity>>() {
      @Override
      @NonNull
      public List<BehaviorLogEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfDateKey = CursorUtil.getColumnIndexOrThrow(_cursor, "date_key");
          final int _cursorIndexOfPetType = CursorUtil.getColumnIndexOrThrow(_cursor, "pet_type");
          final int _cursorIndexOfBehavior = CursorUtil.getColumnIndexOrThrow(_cursor, "behavior");
          final int _cursorIndexOfEmotion = CursorUtil.getColumnIndexOrThrow(_cursor, "emotion");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfSnapshotPath = CursorUtil.getColumnIndexOrThrow(_cursor, "snapshot_path");
          final List<BehaviorLogEntity> _result = new ArrayList<BehaviorLogEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BehaviorLogEntity _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final String _tmpDateKey;
            _tmpDateKey = _cursor.getString(_cursorIndexOfDateKey);
            final String _tmpPetType;
            _tmpPetType = _cursor.getString(_cursorIndexOfPetType);
            final String _tmpBehavior;
            _tmpBehavior = _cursor.getString(_cursorIndexOfBehavior);
            final String _tmpEmotion;
            _tmpEmotion = _cursor.getString(_cursorIndexOfEmotion);
            final float _tmpConfidence;
            _tmpConfidence = _cursor.getFloat(_cursorIndexOfConfidence);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpSnapshotPath;
            if (_cursor.isNull(_cursorIndexOfSnapshotPath)) {
              _tmpSnapshotPath = null;
            } else {
              _tmpSnapshotPath = _cursor.getString(_cursorIndexOfSnapshotPath);
            }
            _item = new BehaviorLogEntity(_tmpId,_tmpTimestamp,_tmpDateKey,_tmpPetType,_tmpBehavior,_tmpEmotion,_tmpConfidence,_tmpDescription,_tmpSnapshotPath);
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
  public Flow<Integer> getBehaviorCountForDate(final String dateKey, final String behavior) {
    final String _sql = "SELECT COUNT(*) FROM behavior_logs WHERE date_key = ? AND behavior = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, dateKey);
    _argIndex = 2;
    _statement.bindString(_argIndex, behavior);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"behavior_logs"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
