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
	private static List<HashMap<String, Object>> contactHashMaps;
	private static List<HashMap<String, String>> groupHashMaps;

	private static final int NUMBER_TO_NAME_HASHMAP_INDEX = 0;
	private static final int NAME_TO_NUMBER_HASHMAP_INDEX = 1;

	public static HashMap<String, Object> getNumberToNameHashMap() throws WhatsAppDBException {
		if (contactHashMaps == null)
			contactHashMaps = getContactsHashMaps();

		//0 - number to name
		return getContactsHashMaps().get(NUMBER_TO_NAME_HASHMAP_INDEX);
	}

	public static HashMap<String, Object> getNameToNumberHashMap() throws WhatsAppDBException {
		if (contactHashMaps == null)
			contactHashMaps = getContactsHashMaps();

		//1 - number to name
		return getContactsHashMaps().get(NAME_TO_NUMBER_HASHMAP_INDEX);
	}

	public static HashMap<String, String> getGroupNumberToNameHashMap() throws WhatsAppDBException {
		if (groupHashMaps == null)
			groupHashMaps = getGroupInfoHashMaps();

		//0 - number to name
		return groupHashMaps.get(NUMBER_TO_NAME_HASHMAP_INDEX);
	}

	public static HashMap<String, String> getGroupNameToNumberHashMap() throws WhatsAppDBException {
		if (groupHashMaps == null)
			groupHashMaps = getGroupInfoHashMaps();

		//1 - number to name
		return groupHashMaps.get(NAME_TO_NUMBER_HASHMAP_INDEX);
	}

	private static List<HashMap<String, String>> getGroupInfoHashMaps() throws WhatsAppDBException {
		String arr[] = WhatsAppDatabaseHelper.execSQL("/data/data/com.whatsapp/databases/wa.db", "select jid,display_name from wa_contacts where jid like " + '"' + "%@g.us" + '"');

		List<HashMap<String, String>> hashMaps = new ArrayList<>(2);

		hashMaps.add(new HashMap<String, String>());
		hashMaps.add(new HashMap<String, String>());

		for (String row : arr) {
			String splts[] = row.split("\\|", -1);

			Log.i("com.suraj.waext", "0: " + splts[0].replaceAll("[0-9]", "x"));

			if (splts.length > 1) {
				Log.i("com.suraj.waext", "1: " + splts[1].replaceAll("[0-9]", "x"));

				hashMaps.get(0).put(splts[0].split("@")[0], splts[1]);
				hashMaps.get(1).put(splts[1], splts[0].split("@")[0]);
			}
		}

		return hashMaps;
	}

	public static String[] execSQL(String dbName, String query) throws WhatsAppDBException {
		Process process;
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
			process.waitFor();

		} catch (IOException e) {
			Log.e(ExtModule.PACKAGE_NAME, e.getMessage());
			throw new WhatsAppDBException(e);
		} catch (InterruptedException e) {
			Log.e(ExtModule.PACKAGE_NAME, e.getMessage());
			throw new WhatsAppDBException(e);
		}

		final InputStreamReader errorStreamReader = new InputStreamReader(process.getErrorStream());

		if (process.exitValue() != 0) {
			try {
				String firstLine = null;
				BufferedReader bufferedReader = new BufferedReader(errorStreamReader);
				String s;
				while ((s = bufferedReader.readLine()) != null) {
					if (firstLine == null) {
						firstLine = s;
					}
					Log.e("com.suraj.waext", "WhatsAppDBHelper:" + s);
				}
				throw new WhatsAppDBException(firstLine);
			} catch (IOException ex) {
				Log.e("com.suraj.waext", ex.getMessage());
				throw new WhatsAppDBException(ex);
			}
		}

		try {
			BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String s;
			StringBuilder op = new StringBuilder();

			while ((s = bufferedReader.readLine()) != null) {
				op.append(s).append("\n");
			}

			return op.toString().split("\n");
		} catch (IOException ex) {
			throw new WhatsAppDBException(ex);
		}
	}

	private static List<HashMap<String, Object>> getContactsHashMaps() throws WhatsAppDBException {

		String[] arr = execSQL("/data/data/com.whatsapp/databases/wa.db", "Select display_name, jid FROM wa_contacts WHERE is_whatsapp_user=1 and jid like " + '"' + "%@s.whatsapp.net" + '"');

		List<HashMap<String, Object>> hashMaps = new ArrayList<>(2);

		hashMaps.add(new HashMap<String, Object>());
		hashMaps.add(new HashMap<String, Object>());

		//prevent any null pointer exceptions
		//not good but we've to deal with it this way

		for (String contact : arr) {
			String potential[] = contact.split("\\|");

			if (potential.length < 2)
				continue;

			// swap = true -> number to name hashmap
			hashMaps.get(0).put(potential[1].split("@")[0], potential[0]);

			// swap = false -> name to number hashmap
			Object value = hashMaps.get(1).get(potential[0]);

			if (value != null) {
				List<String> numbers = new ArrayList<>();

				if (value instanceof String) {
					numbers.add(value.toString());
					numbers.add(potential[1].split("@")[0]);
				} else if (value instanceof List) {
					numbers = (List<String>) value;
					numbers.add(potential[1].split("@")[0]);
				}
				hashMaps.get(1).put(potential[0], numbers);
			} else {
				hashMaps.get(1).put(potential[0], potential[1].split("@")[0]);
			}

		}
		return hashMaps;
	}

	public static void clearHashMaps() {
		groupHashMaps = null;
		contactHashMaps = null;
	}

	public static void clearNullItemsFromMessages(String contact) throws WhatsAppDBException {
		WhatsAppDatabaseHelper.execSQL("/data/data/com.whatsapp/databases/msgstore.db", "delete from messages where key_remote_jid = " + '"' + contact + '"' + " and data is null;");
	}
}