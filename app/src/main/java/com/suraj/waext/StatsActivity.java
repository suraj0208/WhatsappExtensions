package com.suraj.waext;

import android.icu.text.SimpleDateFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.suraj.waext.data.ContributionCalendarDate;
import com.suraj.waext.views.ContributionCalendarView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class StatsActivity extends AppCompatActivity {
    private int median;
    private int diffMedian;
    private int contributionCalendarViewSquareSize;
    private int contributionCalendarViewSpacing;
    private int contributionCalendarViewWidth;
    private int contributionCalendarViewHeight;
    private int messagesHighestCount = 0;

    private String jid;
    private String[] messageTimestamps;

    private TreeMap<ContributionCalendarDate, Integer> dateCountTreeMap;
    private HashMap<String, MonthYearPair> availableMonths;

    private Spinner spinMonths;
    private TextView tvDay;
    RelativeLayout relativeLayoutMain;


    private ContributionCalendarView contributionCalendarView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        jid = getIntent().getStringExtra("jid");
        dateCountTreeMap = new TreeMap<>();
        availableMonths = new HashMap<>();
        spinMonths = (Spinner) findViewById(R.id.spinMonths);
        relativeLayoutMain = (RelativeLayout) findViewById(R.id.rlStatsMain);

        contributionCalendarViewSquareSize = 50;
        contributionCalendarViewSpacing = 10;
        contributionCalendarViewWidth = contributionCalendarViewSquareSize * 6 + contributionCalendarViewSpacing * 5;
        contributionCalendarViewHeight = contributionCalendarViewSquareSize * 7 + contributionCalendarViewSpacing * 6;

        if (jid == null) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            finish();
        }

        Utils.setContactNameFromDataase((TextView) findViewById(R.id.tvStatsContactName), jid);
        getMessagesCount((TextView) findViewById(R.id.tvMessagesReceived), (TextView) findViewById(R.id.tvMessagesSent), (TextView) findViewById(R.id.tvMessageTotal), jid);
        getMessagesTimeSpan((TextView) findViewById(R.id.tvMessagesTimeSpan), jid);

        spinMonths.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                MonthYearPair monthYearPair = availableMonths.get(spinMonths.getItemAtPosition(i).toString());
                displayContributionCalendar(messageTimestamps, monthYearPair.month, monthYearPair.year);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

    }

    private void getMessagesTimeSpan(final TextView textview, final String jid) {
        (new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... voids) {
                String[] arr = WhatsAppDatabaseHelper.execSQL("/data/data/com.whatsapp/databases/msgstore.db", "select timestamp from messages where key_remote_jid like " + '"' + jid + '"' + " and length(data) > 0 order by timestamp;");

                if (arr != null) {
                    return arr;
                }
                return null;
            }

            @Override
            protected void onPostExecute(String[] s) {
                super.onPostExecute(s);
                if (s == null) {
                    return;
                }

                if (s.length == 0)
                    return;

                String first;
                String second;

                try {
                    first = getDateFromTimeStamp(Long.parseLong(s[0]));
                    second = getDateFromTimeStamp(Long.parseLong(s[s.length - 1]));

                    textview.setText(getResources().getString(R.string.messagesTimespan, first, second));

                    String firstDate[] = first.split("/");
                    displayContributionCalendar(s, Integer.parseInt(firstDate[1]), Integer.parseInt(firstDate[2]));

                } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                    ex.printStackTrace();
                }


            }
        }).execute();

    }

    private void displayContributionCalendar(final String[] timestamps, final int month, final int year) {
        if (timestamps == null || timestamps.length == 0) {
            Log.i("com.suraj.waext", "timestamps array null");
            return;
        }

        (new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    for (final String data : timestamps) {
                        long timestamp = Long.parseLong(data);

                        ContributionCalendarDate date = new ContributionCalendarDate(timestamp);

                        Calendar c = Calendar.getInstance(Locale.getDefault());
                        c.setTime(date);

                        availableMonths.put(new SimpleDateFormat("MMM yyyy").format(date), new MonthYearPair(c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR)));

                        if (c.get(Calendar.MONTH) != month - 1 || c.get(Calendar.YEAR) != year)
                            continue;

                        Integer currentCount = dateCountTreeMap.get(date);

                        if (currentCount == null)
                            dateCountTreeMap.put(date, 1);
                        else
                            dateCountTreeMap.put(date, currentCount + 1);
                    }
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }

                ArrayList<Integer> counts = new ArrayList<>();
                ArrayList<Integer> diffs = new ArrayList<>();

                for (Date date : dateCountTreeMap.keySet()) {
                    counts.add(dateCountTreeMap.get(date));
                }

                Collections.sort(counts);

                if (counts.size() == 1)
                    diffs.add(counts.get(0));

                for (int i = 1; i < counts.size(); i++)
                    diffs.add(counts.get(i) - counts.get(i - 1));

                median = calculateMedian(counts);

                Collections.sort(diffs);

                diffMedian = calculateMedian(diffs);

                if (counts.size() > 0)
                    messagesHighestCount = counts.get(counts.size() - 1);

                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);

                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(contributionCalendarViewWidth, contributionCalendarViewHeight);
                layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
                layoutParams.addRule(RelativeLayout.BELOW, R.id.spinMonths);

                relativeLayoutMain.removeView(contributionCalendarView);

                contributionCalendarView = new ContributionCalendarView(StatsActivity.this, contributionCalendarViewSquareSize, contributionCalendarViewSpacing, median, diffMedian, dateCountTreeMap);
                contributionCalendarView.setId(View.generateViewId());
                relativeLayoutMain.addView(contributionCalendarView, layoutParams);

                RelativeLayout.LayoutParams layoutParamsDay = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                layoutParamsDay.addRule(RelativeLayout.ALIGN_TOP, contributionCalendarView.getId());
                layoutParamsDay.addRule(RelativeLayout.LEFT_OF, contributionCalendarView.getId());
                layoutParamsDay.setMargins(0, 0, 5, 0);
                relativeLayoutMain.removeView(tvDay);

                tvDay = new TextView(StatsActivity.this);
                tvDay.setText("Sun");
                tvDay.setTextSize(10);
                tvDay.setLayoutParams(layoutParamsDay);
                relativeLayoutMain.addView(tvDay);

                if (messageTimestamps == null) {
                    ArrayList<HashMap.Entry<String, MonthYearPair>> entries = new ArrayList<>(availableMonths.entrySet());

                    Collections.sort(entries, new Comparator<HashMap.Entry<String, MonthYearPair>>() {
                        @Override
                        public int compare(HashMap.Entry<String, MonthYearPair> left, HashMap.Entry<String, MonthYearPair> right) {
                            String thisDate = left.getValue().year + "/" + (Integer.toString(left.getValue().month).length() < 2 ? "0" + left.getValue().month : left.getValue().month);
                            String otherDate = right.getValue().year + "/" + (Integer.toString(right.getValue().month).length() < 2 ? "0" + right.getValue().month : right.getValue().month);

                            return thisDate.compareTo(otherDate);
                        }
                    });

                    ArrayList<String> spinnerData = new ArrayList<>();

                    for (Map.Entry<String, MonthYearPair> entry : entries)
                        spinnerData.add(entry.getKey());

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(StatsActivity.this, android.R.layout.simple_spinner_dropdown_item, spinnerData);
                    spinMonths.setAdapter(adapter);
                    messageTimestamps = timestamps;
                    spinMonths.setVisibility(View.VISIBLE);
                }

                System.out.println(median + " " + diffMedian);
                System.out.println(1 + " to " + (median - diffMedian <= 0 ? median : median - diffMedian));
                System.out.println(median - diffMedian <= 0 ? median : ((median - diffMedian) + " to " + (median + diffMedian)));
                System.out.println(median + diffMedian < messagesHighestCount ? ((median + diffMedian) + " to " + messagesHighestCount) : median + " to " + messagesHighestCount);

            }
        }).execute();
    }

    private int calculateMedian(List<Integer> counts) {
        int median;

        if (counts.size() == 0)
            return 0;

        if (counts.size() % 2 == 0)
            median = (counts.get((counts.size() / 2) - 1) + counts.get(counts.size() / 2)) / 2;
        else
            median = (counts.get(counts.size() / 2));

        return median;
    }

    public void getMessagesCount(final TextView tvTo, TextView tvFrom, final TextView tvTotal, final String jid) {
        final TextView[] textViews = {tvTo, tvFrom};

        (new AsyncTask<Void, Void, String[]>() {
            @Override
            protected String[] doInBackground(Void... voids) {
                String[] arr = WhatsAppDatabaseHelper.execSQL("/data/data/com.whatsapp/databases/msgstore.db", "select count(*),key_from_me from messages where key_remote_jid like " + '"' + jid + '"' + " and length(data) > 0 group by key_from_me;");

                if (arr != null) {
                    return arr;
                }

                return null;
            }

            @Override
            protected void onPostExecute(String[] s) {
                super.onPostExecute(s);
                if (s == null) {
                    return;
                }

                long total = 0;

                for (String data : s) {
                    String rowData[] = data.split("\\|");

                    try {
                        int num = Integer.parseInt(rowData[1]);
                        textViews[num].setText("" + rowData[0]);
                        total += Long.parseLong(rowData[0]);
                    } catch (NumberFormatException | ArrayIndexOutOfBoundsException ne) {
                        ne.printStackTrace();
                    }

                }
                tvTotal.setText("" + total);

            }
        }).execute();


    }

    public String getDateFromTimeStamp(long timestamp) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(timestamp));
    }

    class MonthYearPair {
        int month;
        int year;

        public MonthYearPair(int month, int year) {
            this.month = month;
            this.year = year;
        }
    }

}
