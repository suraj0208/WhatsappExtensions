package com.suraj.waext;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Created by suraj on 2/12/16.
 */
public class WhatsAppContactManager {

    public HashMap<String, Object> getNumberToNameHashMap() {
        return this.getHashMap(true);
    }

    public HashMap<String, Object> getNameToNumberHashMap() {
        return this.getHashMap(false);
    }

    private HashMap<String, Object> getHashMap(boolean swap) {
        Process process = null;
        Runtime runtime = Runtime.getRuntime();
        OutputStreamWriter outputStreamWriter;

        HashMap<String, Object> hashMap = new HashMap<>();

        try {

            String command = "/data/data/com.whatsapp/databases/wa.db 'Select display_name, jid FROM wa_contacts WHERE is_whatsapp_user=1';";
            process = runtime.exec("su");

            outputStreamWriter = new OutputStreamWriter(process.getOutputStream());

            outputStreamWriter.write("sqlite3 " + command);

            outputStreamWriter.flush();
            outputStreamWriter.close();
            outputStreamWriter.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String s;
            StringBuilder op = new StringBuilder();

            while ((s = bufferedReader.readLine()) != null) {
                op.append(s).append("\n");
            }

            String arr[] = op.toString().split("\n");

            Arrays.sort(arr);

            Log.i("com.suraj","in contact mngr arr len :"+arr.length);


            for (String contact : arr) {
                String potential[] = contact.split("\\|");

                if (potential.length < 2)
                    continue;

                if (swap)        // swap = true -> number to name hashmap
                    hashMap.put(potential[1].split("@")[0], potential[0]);
                else {            // swap = true -> name to number hashmap
                    Object value = hashMap.get(potential[0]);

                    if (value != null) {
                        List<String> numbers = new ArrayList<>();

                        Log.i("com.suraj","building list");


                        if (value instanceof String) {
                            numbers.add(value.toString());
                            numbers.add(potential[1].split("@")[0]);
                        } else if (value instanceof List) {
                            numbers = (List<String>)value;
                            numbers.add(potential[1].split("@")[0]);
                        }
                        hashMap.put(potential[0],numbers);


                    } else {
                        hashMap.put(potential[0], potential[1].split("@")[0]);
                        Log.i("com.suraj","inserting single");

                    }


                }
            }


        }  catch (IOException e) {
            Log.e("com.suraj","in mgnr io exception");
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
            Log.e("com.suraj","in mgnr io exception");
        }

        return hashMap;
    }

}