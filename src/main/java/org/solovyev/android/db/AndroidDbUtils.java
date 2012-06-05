package org.solovyev.android.db;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.jetbrains.annotations.NotNull;
import org.solovyev.common.utils.StringUtils;

import java.util.Arrays;
import java.util.List;

/**
 * User: serso
 * Date: 4/15/12
 * Time: 6:01 PM
 */
public final class AndroidDbUtils {

    private AndroidDbUtils() {
        throw new AssertionError();
    }

    @NotNull
    public static <R> R doDbQuery(@NotNull SQLiteOpenHelper dbHelper, @NotNull DbQuery<R> query) {
        final R result;

        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();

            // database is cached inside SQLiteOpenHelper
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (db) {
                Cursor cursor = null;
                try {
                    // open cursor
                    cursor = query.createCursor(db);
                    // do operation
                    result = query.retrieveData(cursor);
                } finally {
                    // anyway if cursor was opened - close it
                    if (cursor != null) {
                        cursor.close();
                    }
                }
            }
        } finally {
            // if database was opened - close it
            if (db != null) {
                db.close();
            }
        }


        return result;
    }

    public static void doDbExec(@NotNull SQLiteOpenHelper dbHelper, @NotNull DbExec exec) {
        doDbExecs(dbHelper, Arrays.asList(exec));
    }

    public static void doDbExecs(@NotNull SQLiteOpenHelper dbHelper, @NotNull List<DbExec> execs) {

        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();

            // database is cached inside SQLiteOpenHelper
            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (db) {
                doDbTransaction(db, execs);
            }

        } finally {
            // if database was opened - close it
            if (db != null) {
                db.close();
            }
        }
    }

/*    // NOTE: currently DbUtils can handle only ONE database!!!

@NotNull
private static final ThreadLocal<SQLiteDatabase> threadLocalDb = new ThreadLocal<SQLiteDatabase>();

@NotNull
private static final Object DB_LOCK = new Object();

@NotNull
public static <R> R doDbQuery(@NotNull SQLiteOpenHelper dbHelper, @NotNull DbQuery<R> query) {
    final R result;

    synchronized (DB_LOCK) {
        SQLiteDatabase db = null;
        boolean wasOpened = false;
        try {
            db = threadLocalDb.get();
            if (db == null || !db.isOpen()) {
                // open database
                wasOpened = true;
                db = dbHelper.getWritableDatabase();
                threadLocalDb.set(db);
            }

            Cursor cursor = null;
            try {
                // open cursor
                cursor = query.createCursor(db);
                // do operation
                result = query.retrieveData(cursor);
            } finally {
                // anyway if cursor was opened - close it
                if (cursor != null) {
                    cursor.close();
                }
            }
        } finally {
            if (wasOpened) {
                threadLocalDb.set(null);

                // if database was opened - close it
                if (db != null) {
                    db.close();
                }
            }
        }
    }

    return result;
}

public static void doDbExec(@NotNull SQLiteOpenHelper dbHelper, @NotNull DbExec exec) {
    doDbExecs(dbHelper, Arrays.asList(exec));
}

public static void doDbExecs(@NotNull SQLiteOpenHelper dbHelper, @NotNull List<DbExec> execs) {

    synchronized (DB_LOCK) {
        SQLiteDatabase db = null;
        boolean wasOpened = false;
        try {
            db = threadLocalDb.get();
            if (db == null || !db.isOpen()) {
                // open database
                wasOpened = true;
                db = dbHelper.getWritableDatabase();
                threadLocalDb.set(db);
            }

            doDbTransaction(db, execs);
        } finally {
            if (wasOpened) {
                threadLocalDb.set(null);

                // if database was opened - close it
                if (db != null) {
                    db.close();
                }
            }
        }
    }
}*/

    private static void doDbTransaction(@NotNull SQLiteDatabase db, @NotNull List<DbExec> execs) {

        try {
            // start transaction
            db.beginTransaction();

            // do action
            for (DbExec exec : execs) {
                exec.exec(db);
            }

            // mark transaction successful
            db.setTransactionSuccessful();
        } finally {
            // end transaction
            db.endTransaction();
        }
    }

    @NotNull
    public static String[] inClauseValues(@NotNull List<?> objects, @NotNull String... beforeInValues) {
        final String[] result = new String[objects.size() + beforeInValues.length];

        for (int i = 0; i < result.length; i++) {
            if (i < beforeInValues.length) {
                result[i] = beforeInValues[i];
            } else {
                result[i] = objects.get(i - beforeInValues.length).toString();
            }
        }

        return result;
    }

    @NotNull
    public static String inClause(@NotNull List<?> objects) {
        final StringBuilder result = new StringBuilder(3 * objects.size());

        result.append("(");
        if (objects.size() == 1) {
            result.append("?");
        } else if (objects.size() > 1) {
            result.append("?");
            result.append(StringUtils.repeat(", ?", objects.size() - 1));
        } else {
            result.append("'foo'");
        }
        result.append(")");

        return result.toString();
    }
}
