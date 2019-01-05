package com.suraj.waext;

import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

/**
 * Created by suraj on 4/1/17.
 */
public class WhatsAppChatBackupManager {
    /*public static final String WHATSAPP_MESSAGEDB_PATH = "/data/data/com.whatsapp/databases/msgstore.db";
    public static final String WAEXT_MESSAGEDB_PATH = "/sdcard/waext/databases/backup.db";

    public WhatsAppChatBackupManager(){
        File directory = new File(Environment.getExternalStorageDirectory()+File.separator+"waext"+File.separator + "databases");

        if(!directory.exists())
            directory.mkdirs();
    }

    public void backupChat(String jid) {

        BackupDatabaseHelper backupDatabaseHelper = new BackupDatabaseHelper();

        backupDatabaseHelper.execSQL("CREATE TABLE IF NOT EXISTS messages (_id INTEGER PRIMARY KEY AUTOINCREMENT, key_remote_jid TEXT NOT NULL, key_from_me INTEGER, key_id TEXT NOT NULL, status INTEGER, needs_push INTEGER, data TEXT, timestamp INTEGER, media_url TEXT, media_mime_type TEXT, media_wa_type TEXT, media_size INTEGER, media_name TEXT, media_caption TEXT, media_hash TEXT, media_duration INTEGER, origin INTEGER, latitude REAL, longitude REAL, thumb_image TEXT, remote_resource TEXT, received_timestamp INTEGER, send_timestamp INTEGER, receipt_server_timestamp INTEGER, receipt_device_timestamp INTEGER, read_device_timestamp INTEGER, played_device_timestamp INTEGER, raw_data BLOB, recipient_count INTEGER, participant_hash TEXT, starred INTEGER, quoted_row_id INTEGER, mentioned_jids TEXT, multicast_id TEXT);;");

        String[] values = prepareValuesForMessageTable(WhatsAppChatBackupManager.WHATSAPP_MESSAGEDB_PATH, jid);
        backupDatabaseHelper.execSQL("delete from messages where key_remote_jid like " + '"' + jid + '"' + ';');

        for (String value : values) {
            String query = "INSERT OR IGNORE INTO messages (key_remote_jid, key_from_me, key_id, status, needs_push, data, timestamp, media_url, media_mime_type, media_wa_type, media_size, media_name, media_caption, media_hash, media_duration, origin, latitude, longitude, thumb_image, remote_resource, received_timestamp, send_timestamp, receipt_server_timestamp, receipt_device_timestamp, read_device_timestamp,played_device_timestamp, raw_data, recipient_count, participant_hash,starred,quoted_row_id, mentioned_jids, multicast_id) VALUES (" + value + ");";
            backupDatabaseHelper.execSQL(query);
        }

        backupChatListData(jid);

    }

    private void backupChatListData(String jid) {
        BackupDatabaseHelper backupDatabaseHelper = new BackupDatabaseHelper();
        backupDatabaseHelper.execSQL("CREATE TABLE IF NOT EXISTS chat_list (_id INTEGER PRIMARY KEY AUTOINCREMENT, key_remote_jid TEXT UNIQUE, message_table_id INTEGER, subject TEXT, creation INTEGER, last_read_message_table_id INTEGER, last_read_receipt_sent_message_table_id INTEGER, archived INTEGER, sort_timestamp INTEGER, mod_tag INTEGER, gen REAL, my_messages INTEGER, plaintext_disabled BOOLEAN, last_message_table_id INTEGER, unseen_message_count INTEGER, unseen_missed_calls_count INTEGER, unseen_row_count INTEGER);");

        String[] values = prepareValuesForChatListTable(WhatsAppChatBackupManager.WHATSAPP_MESSAGEDB_PATH, jid);
        backupDatabaseHelper.execSQL("delete from chat_list where key_remote_jid like " + '"' + jid + '"' + ';');

        for (String value : values) {
            String query = "INSERT OR IGNORE INTO chat_list (key_remote_jid , message_table_id , subject , creation , last_read_message_table_id , last_read_receipt_sent_message_table_id , archived , sort_timestamp , mod_tag , gen , my_messages , plaintext_disabled , last_message_table_id , unseen_message_count , unseen_missed_calls_count, unseen_row_count) VALUES (" + value + ");";
            backupDatabaseHelper.execSQL(query);
        }
    }

    public void restoreChat(String jid) {

        String[] values = prepareValuesForMessageTable(WhatsAppChatBackupManager.WAEXT_MESSAGEDB_PATH, jid);

        StringBuilder query = new StringBuilder();

        for (String value : values) {

            //appending multiple sql queries in single string -- avoiding multiple su calls
            //assuming length will not exceed 2^31-1
            //if it does... umm well lets hope it doesnt

            query.append("INSERT OR IGNORE INTO messages (key_remote_jid, key_from_me, key_id, status, needs_push, data, timestamp, media_url, media_mime_type, media_wa_type, media_size, media_name, media_caption, media_hash, media_duration, origin, latitude, longitude, thumb_image, remote_resource, received_timestamp, send_timestamp, receipt_server_timestamp, receipt_device_timestamp, read_device_timestamp,played_device_timestamp, raw_data, recipient_count, participant_hash,starred,quoted_row_id, mentioned_jids, multicast_id) VALUES (").append(value).append(");");
        }
        WhatsAppDatabaseHelper.execSQL(WhatsAppChatBackupManager.WHATSAPP_MESSAGEDB_PATH, "delete from messages where key_remote_jid like " + '"' + jid + '"' + ';');
        WhatsAppDatabaseHelper.execSQL(WhatsAppChatBackupManager.WHATSAPP_MESSAGEDB_PATH, query.toString());

        restoreChatListData(jid);
    }

    private void restoreChatListData(String jid) {
        String[] values = prepareValuesForChatListTable(WhatsAppChatBackupManager.WAEXT_MESSAGEDB_PATH, jid);

        StringBuilder query = new StringBuilder();

        for (String value : values) {
            query.append("INSERT OR IGNORE INTO chat_list (key_remote_jid , message_table_id , subject , creation , last_read_message_table_id , last_read_receipt_sent_message_table_id , archived , sort_timestamp , mod_tag , gen , my_messages , plaintext_disabled , last_message_table_id , unseen_message_count , unseen_missed_calls_count, unseen_row_count) VALUES (").append(value).append(");");
        }

        WhatsAppDatabaseHelper.execSQL(WhatsAppChatBackupManager.WHATSAPP_MESSAGEDB_PATH, "delete from chat_list where key_remote_jid like " + '"' + jid + '"' + ';');
        WhatsAppDatabaseHelper.execSQL(WhatsAppChatBackupManager.WHATSAPP_MESSAGEDB_PATH, query.toString());
    }

    private String[] prepareValuesForChatListTable(String dbname, String jid) {

        String[] message_rows = WhatsAppDatabaseHelper.execSQL(dbname, "select key_remote_jid , message_table_id , subject , creation , last_read_message_table_id , last_read_receipt_sent_message_table_id , archived , sort_timestamp , mod_tag , gen , my_messages , plaintext_disabled , last_message_table_id , unseen_message_count , unseen_missed_calls_count, unseen_row_count from chat_list where key_remote_jid like " + '"' + '%' + jid + '%' + '"' + ";");

        for (int j = 0; j < message_rows.length; j++) {

            String[] fields = message_rows[j].split("\\|", -1);

            StringBuilder values = new StringBuilder();


            for (int i = 0; i < fields.length; i++) {
                if (fields[i].length() == 0) {
                    values.append("NULL");
                } else if (fields[i].matches("[A-Za-z0-9@\\.\\-]+")) {
                    values.append('"').append(fields[i]).append('"');
                } else {
                    values.append(fields[i]);
                }

                if (i != fields.length - 1)
                    values.append(",");
            }
            message_rows[j] = values.toString();
        }

        return message_rows;
    }

    private String[] prepareValuesForMessageTable(String dbname, String jid) {


        String[] table_info = WhatsAppDatabaseHelper.execSQL(WhatsAppChatBackupManager.WHATSAPP_MESSAGEDB_PATH, "pragma table_info(messages);");

        HashSet<Integer> textFields = new HashSet<>();

        for (int i = 0; i < table_info.length; i++) {
            String[] fields = table_info[i].split("\\|", -1);
            if (fields.length > 2 && fields[2].equals("TEXT")) {
                textFields.add(i);
            }
        }

        String[] message_rows = WhatsAppDatabaseHelper.execSQL(dbname, "select * from messages where key_remote_jid like " + '"' + '%' + jid + '%' + '"' + ";");


        for (int j = 0; j < message_rows.length; j++) {

            String[] fields = message_rows[j].split("\\|", -1);

            StringBuilder values = new StringBuilder();

             for (int i=1; i < fields.length; i++) {
                if (fields[i].length() == 0) {
                    values.append("NULL");
                } else if (textFields.contains(i)) {
                    values.append('"').append(fields[i]).append('"');
                } else if (i == 27) {
                    values.append("NULL");
                } else {
                    values.append(fields[i]);
                }

                if (i != fields.length - 1)
                    values.append(",");
            }

            message_rows[j] = values.toString();

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
*/
}
