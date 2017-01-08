package com.suraj.waext;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by suraj on 2/12/16.
 */
public class WhatsAppDatabaseHelper {

    public static HashMap<String, Object> getNumberToNameHashMap() {
        return getContactsHashMap(true);
    }

    public static HashMap<String, Object> getNameToNumberHashMap() {
        return getContactsHashMap(false);
    }

    public static List<HashMap<String,String>> getGroupInfoHashMaps(){
        String arr[] = WhatsAppDatabaseHelper.execSQL("/data/data/com.whatsapp/databases/wa.db","select jid,display_name from wa_contacts where jid like "+ '"' + "%@g.us" + '"');

        List<HashMap<String,String>> hashMaps = new ArrayList<>(2);

        hashMaps.add(new HashMap<String, String>());
        hashMaps.add(new HashMap<String, String>());

        for(String row:arr){
            String splts[] = row.split("\\|",-1);

            if(splts.length > 0) {
                hashMaps.get(0).put(splts[0].split("@")[0], splts[1]);
                hashMaps.get(1).put(splts[1],splts[0].split("@")[0]);
            }
        }

        return hashMaps;

    }

    public static String[] execSQL(String dbName, String query) {
        Process process = null;
        Runtime runtime = Runtime.getRuntime();
        OutputStreamWriter outputStreamWriter;

        try {
            String command = dbName + " " + "'" + query + "'" + ";";
            process = runtime.exec("su");

            outputStreamWriter = new OutputStreamWriter(process.getOutputStream());

            outputStreamWriter.write("sqlite3 " + command);

            outputStreamWriter.flush();
            outputStreamWriter.close();
            outputStreamWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        final InputStreamReader errorStreamReader = new InputStreamReader(process.getErrorStream());

        (new Thread() {
            @Override
            public void run() {
                try {

                    BufferedReader bufferedReader = new BufferedReader(errorStreamReader);
                    String s;
                    while ((s = bufferedReader.readLine()) != null) {
                        Log.d("com.suraj.waext", "WhatsAppDBHelper:" + s);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

        }).start();

        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String s;
            StringBuilder op = new StringBuilder();

            while ((s = bufferedReader.readLine()) != null) {
                op.append(s).append("\n");
            }

            return op.toString().split("\n");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;

    }

    private static HashMap<String, Object> getContactsHashMap(boolean swap) {
        HashMap<String, Object> hashMap = new HashMap<>();

        String[] arr = execSQL("/data/data/com.whatsapp/databases/wa.db", "Select display_name, jid FROM wa_contacts WHERE is_whatsapp_user=1 and jid like " + '"' + "%@s.whatsapp.net" + '"');

        if (arr == null)
            return null;

        for (String contact : arr) {
            String potential[] = contact.split("\\|");

            if (potential.length < 2)
                continue;

            if (swap)        // swap = true -> number to name hashmap
                hashMap.put(potential[1].split("@")[0], potential[0]);
            else {            // swap = false -> name to number hashmap
                Object value = hashMap.get(potential[0]);

                if (value != null) {
                    List<String> numbers = new ArrayList<>();

                    if (value instanceof String) {
                        numbers.add(value.toString());
                        numbers.add(potential[1].split("@")[0]);
                    } else if (value instanceof List) {
                        numbers = (List<String>) value;
                        numbers.add(potential[1].split("@")[0]);
                    }
                    hashMap.put(potential[0], numbers);
                } else {
                    hashMap.put(potential[0], potential[1].split("@")[0]);
                }
            }
        }
        return hashMap;
    }

}