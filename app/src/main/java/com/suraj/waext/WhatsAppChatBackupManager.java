package com.suraj.waext;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.io.IOException;
import java.util.HashSet;

/**
 * Created by suraj on 4/1/17.
 */
public class WhatsAppChatBackupManager {
    public static final String WHATSAPP_MESSAGEDB_PATH = "/data/data/com.whatsapp/databases/msgstore.db";
    public static final String WAEXT_MESSAGEDB_PATH = "/sdcard/test.db";

    private long max_message_id = 0;

    public void backupChat(String jid) {

        BackupDatabaseHelper backupDatabaseHelper = new BackupDatabaseHelper();

        backupDatabaseHelper.execSQL("CREATE TABLE IF NOT EXISTS messages (_id INTEGER PRIMARY KEY AUTOINCREMENT, key_remote_jid TEXT NOT NULL, key_from_me INTEGER, key_id TEXT NOT NULL, status INTEGER, needs_push INTEGER, data TEXT, timestamp INTEGER, media_url TEXT, media_mime_type TEXT, media_wa_type TEXT, media_size INTEGER, media_name TEXT, media_caption TEXT, media_hash TEXT, media_duration INTEGER, origin INTEGER, latitude REAL, longitude REAL, thumb_image TEXT, remote_resource TEXT, received_timestamp INTEGER, send_timestamp INTEGER, receipt_server_timestamp INTEGER, receipt_device_timestamp INTEGER, read_device_timestamp INTEGER, played_device_timestamp INTEGER, raw_data BLOB, recipient_count INTEGER, participant_hash TEXT, starred INTEGER, quoted_row_id INTEGER, mentioned_jids TEXT, multicast_id TEXT);;");

        String[] values = prepareValues(WhatsAppChatBackupManager.WHATSAPP_MESSAGEDB_PATH,jid,false);

        for(String value:values) {
            String query = "INSERT OR IGNORE INTO messages VALUES (" + value + ");";
            //backupDatabaseHelper.execSQL(query);
        }

    }

    public void restoreChat(String jid){

        String[] values = prepareValues(WhatsAppChatBackupManager.WAEXT_MESSAGEDB_PATH,jid,true);

        StringBuilder query = new StringBuilder();

        for(String value:values){

            //appending multiple sql queries in single string -- avoiding multiple su calls
            //assuming length will not exceed 2^31-1
            //if it does... umm well lets hope it doesnt

            query.append("INSERT OR IGNORE INTO messages VALUES (").append(value).append(");");
        }

        WhatsAppDatabaseHelper.execSQL(WhatsAppChatBackupManager.WHATSAPP_MESSAGEDB_PATH,query.toString());


    }

    private String[] prepareValues(String dbname, String jid, boolean includeID){

        String[] last_msg_row = WhatsAppDatabaseHelper.execSQL(WhatsAppChatBackupManager.WHATSAPP_MESSAGEDB_PATH, "select * from messages order by _id desc limit 1");

        if (last_msg_row.length > 0) {
            max_message_id = Long.parseLong(last_msg_row[0].split("\\|")[0]);
        } else {
            Log.i("com.suraj.waext", "unable to set max_message_id");
        }

        String[] table_info = WhatsAppDatabaseHelper.execSQL(WhatsAppChatBackupManager.WHATSAPP_MESSAGEDB_PATH, "pragma table_info(messages);");

        HashSet<Integer> textFields = new HashSet<>();

        for (int i = 0; i < table_info.length; i++) {
            String[] fields = table_info[i].split("\\|", -1);
            if (fields.length > 2 && fields[2].equals("TEXT")) {
                textFields.add(i);
            }
        }

        String[] message_rows = WhatsAppDatabaseHelper.execSQL(dbname, "select * from messages where key_remote_jid like " + '"' + '%' + jid + '%' + '"' + ";");


        for (int j=0;j<message_rows.length;j++) {

            String[] fields = message_rows[j].split("\\|", -1);

            StringBuilder values = new StringBuilder();

            int i = 0;

            if(includeID) {
                max_message_id++;
                values.append(max_message_id);
                i++;
                values.append(",");
            }

            for (;i < fields.length; i++) {
                if (fields[i].length() == 0) {
                    values.append("NULL");
                } else if (textFields.contains(i)) {
                    values.append('"').append(fields[i]).append('"');
                } else if (i == 27) {
                    values.append("NULL");
                } else {
                    values.append(fields[i]);
                }

                if(i!=fields.length-1)
                    values.append(",");
            }

            message_rows[j]=values.toString();

        }

        return message_rows;
    }
}

class BackupDatabaseHelper {
    SQLiteDatabase backupDatabase;

    BackupDatabaseHelper() {
        backupDatabase = SQLiteDatabase.openDatabase(WhatsAppChatBackupManager.WAEXT_MESSAGEDB_PATH, null, SQLiteDatabase.CREATE_IF_NECESSARY);
    }

    public void execSQL(String query) {
        backupDatabase.execSQL(query);
    }


}
