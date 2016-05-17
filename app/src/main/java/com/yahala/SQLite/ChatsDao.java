package com.yahala.SQLite;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

import com.yahala.SQLite.Chats;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * DAO for table CHATS.
 */
public class ChatsDao extends AbstractDao<Chats, Void> {

    public static final String TABLENAME = "CHATS";

    /**
     * Properties of entity Chats.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Jid = new Property(0, String.class, "jid", false, "JID");
        public final static Property Name = new Property(1, String.class, "name", false, "NAME");
    }

    ;


    public ChatsDao(DaoConfig config) {
        super(config);
    }

    public ChatsDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /**
     * Creates the underlying database table.
     */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists ? "IF NOT EXISTS " : "";
        db.execSQL("CREATE TABLE " + constraint + "'CHATS' (" + //
                "'JID' TEXT," + // 0: jid
                "'NAME' TEXT);"); // 1: name
    }

    /**
     * Drops the underlying database table.
     */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "'CHATS'";
        db.execSQL(sql);
    }

    /**
     * @inheritdoc
     */
    @Override
    protected void bindValues(SQLiteStatement stmt, Chats entity) {
        stmt.clearBindings();

        String jid = entity.getJid();
        if (jid != null) {
            stmt.bindString(1, jid);
        }

        String name = entity.getName();
        if (name != null) {
            stmt.bindString(2, name);
        }
    }

    /**
     * @inheritdoc
     */
    @Override
    public Void readKey(Cursor cursor, int offset) {
        return null;
    }

    /**
     * @inheritdoc
     */
    @Override
    public Chats readEntity(Cursor cursor, int offset) {
        Chats entity = new Chats( //
                cursor.isNull(offset + 0) ? null : cursor.getString(offset + 0), // jid
                cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1) // name
        );
        return entity;
    }

    /**
     * @inheritdoc
     */
    @Override
    public void readEntity(Cursor cursor, Chats entity, int offset) {
        entity.setJid(cursor.isNull(offset + 0) ? null : cursor.getString(offset + 0));
        entity.setName(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
    }

    /**
     * @inheritdoc
     */
    @Override
    protected Void updateKeyAfterInsert(Chats entity, long rowId) {
        // Unsupported or missing PK type
        return null;
    }

    /**
     * @inheritdoc
     */
    @Override
    public Void getKey(Chats entity) {
        return null;
    }

    /**
     * @inheritdoc
     */
    @Override
    protected boolean isEntityUpdateable() {
        return true;
    }

}