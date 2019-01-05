package com.suraj.waext;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.graphics.Color;
import android.icu.text.SimpleDateFormat;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class StatsActivity extends AppCompatActivity {
    private int contributionCalendarViewSquareSize;
    private int contributionCalendarViewSpacing;
    private int contributionCalendarViewWidth;
    private int contributionCalendarViewHeight;

    private String jid;

    ArrayList<String> spinnerData;


    private HashMap<String, MonthYearPair> availableMonths;
    private HashMap<String, TreeMap<ContributionCalendarDate, Integer>> dateStringTreeMapHashMap;
    private HashMap<String, DistributionPair> dateStringMedianPairHashMap;

    private Spinner spinMonths;
    private TextView tvDay;
    private RelativeLayout relativeLayoutMain;
    private LinearLayout linearLayoutLegend;

    private ImageView imgViewActivities[];
    private TextView tvActivities[];

    private ContributionCalendarView contributionCalendarView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        jid = getIntent().getStringExtra("jid");

        availableMonths = new HashMap<>();

        dateStringTreeMapHashMap = new HashMap<>();
        dateStringMedianPairHashMap = new HashMap<>();

        spinMonths = (Spinner) findViewById(R.id.spinMonths);
        spinnerData = new ArrayList<>();

        relativeLayoutMain = (RelativeLayout) findViewById(R.id.rlStatsMain);
        linearLayoutLegend = (LinearLayout) findViewById(R.id.llLegend);

        imgViewActivities = new ImageView[]{(ImageView) findViewById(R.id.imgViewLowActivity), (ImageView) findViewById(R.id.imgViewMediumActivity), (ImageView) findViewById(R.id.imgViewHighActivity)};
        tvActivities = new TextView[]{(TextView) findViewById(R.id.tvLowActivity), (TextView) findViewById(R.id.tvMediumActivity), (TextView) findViewById(R.id.tvHighActivity)};

        contributionCalendarViewSquareSize = 50;
        contributionCalendarViewSpacing = 10;

        contributionCalendarViewWidth = contributionCalendarViewSquareSize * 6 + contributionCalendarViewSpacing * 5;
        contributionCalendarViewHeight = contributionCalendarViewSquareSize * 7 + contributionCalendarViewSpacing * 6;

        if (jid == null) {
            Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
            finish();
        }

        setContactNameFromDatabase(jid);
        getMessagesCount((TextView) findViewById(R.id.tvMessagesReceived), (TextView) findViewById(R.id.tvMessagesSent), (TextView) findViewById(R.id.tvMessageTotal), jid);
        getMessagesTimeSpan((TextView) findViewById(R.id.tvMessagesTimeSpan), jid);


    }

    @SuppressLint("StaticFieldLeak")
    public void setContactNameFromDatabase(final String jid) {
        (new AsyncTask<Void, Void, Object>() {

            @Override
            protected Object doInBackground(Void... voids) {
                try {
                    return Utils.getContactNameFromDatabase(jid);
                } catch (WhatsAppDBException e) {
                    return e;
                }
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);

                if (Utils.toastAndExitIfWaDbException(o, StatsActivity.this)) {
                    return;
                }

                ((TextView) findViewById(R.id.tvStatsContactName)).setText(o.toString());
            }
        }).execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void getMessagesTimeSpan(final TextView textview, final String jid) {
        (new AsyncTask<Void, Void, Object>() {
            @TargetApi(Build.VERSION_CODES.N)
            @Override
            protected Object doInBackground(Void... voids) {
                String[] arr;
                try {
                    arr = WhatsAppDatabaseHelper.execSQL("/data/data/com.whatsapp/databases/msgstore.db", "select timestamp from messages where key_remote_jid like " + '"' + jid + '"' + " and length(data) > 0 order by timestamp;");
                } catch (WhatsAppDBException e) {
                    return e;
                }

                Calendar c = Calendar.getInstance(Locale.getDefault());

                ArrayList<Integer> medianList = new ArrayList<>();

                TreeMap<ContributionCalendarDate, Integer> currentTreeMap = null;

                String prevMonthYearName = null;
                ContributionCalendarDate prevDate = null;

                Long[] timestampsFromDatabase = new Long[arr.length];

                for (int i = 0; i < arr.length; i++) {
                    timestampsFromDatabase[i] = Long.parseLong(arr[i]);

                    c.setTime(new Date(timestampsFromDatabase[i]));

                    String monthYearName = new SimpleDateFormat("MMM yyyy").format(c.getTime());

                    ContributionCalendarDate currentDate = new ContributionCalendarDate(timestampsFromDatabase[i]);
                    availableMonths.put(monthYearName, new MonthYearPair(c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR)));

                    currentTreeMap = dateStringTreeMapHashMap.get(monthYearName);

                    if (currentTreeMap == null) {
                        if (i != 0) {
                            Collections.sort(medianList);
                            int intervalStart = medianList.size() / 3;
                            int intervalEnd = intervalStart + intervalStart;

                            if (medianList.size() > 0 && intervalEnd >= medianList.size() - 1) {
                                dateStringMedianPairHashMap.put(prevMonthYearName, new DistributionPair(medianList.get(intervalStart), medianList.get(medianList.size() - 1), medianList.get(medianList.size() - 1) + 1));
                            } else if (medianList.size() > 0)
                                dateStringMedianPairHashMap.put(prevMonthYearName, new DistributionPair(medianList.get(intervalStart), medianList.get(intervalEnd), medianList.get(medianList.size() - 1)));
                            else {
                                dateStringMedianPairHashMap.put(prevMonthYearName, new DistributionPair(1, 1, 1));
                            }

                            medianList.clear();
                        }

                        currentTreeMap = new TreeMap<>();
                        currentTreeMap.put(currentDate, 1);
                        dateStringTreeMapHashMap.put(monthYearName, currentTreeMap);
                    } else {
                        Integer current = currentTreeMap.get(currentDate);

                        if (current == null) {
                            currentTreeMap.put(currentDate, 1);
                            medianList.add(currentTreeMap.get(prevDate));
                        } else
                            currentTreeMap.put(currentDate, current + 1);

                    }
                    prevMonthYearName = monthYearName;
                    prevDate = currentDate;
                }

                medianList.add(currentTreeMap.get(prevDate));
                Collections.sort(medianList);
                int intervalStart = medianList.size() / 3;
                int intervalEnd = intervalStart + intervalStart;

                if (intervalEnd >= medianList.size() - 1) {
                    dateStringMedianPairHashMap.put(prevMonthYearName, new DistributionPair(medianList.get(intervalStart), medianList.get(medianList.size() - 1), medianList.get(medianList.size() - 1) + 1));
                } else
                    dateStringMedianPairHashMap.put(prevMonthYearName, new DistributionPair(medianList.get(intervalStart), medianList.get(intervalEnd), medianList.get(medianList.size() - 1)));

                ArrayList<HashMap.Entry<String, MonthYearPair>> entries = new ArrayList<>(availableMonths.entrySet());

                Collections.sort(entries, new Comparator<HashMap.Entry<String, MonthYearPair>>() {
                    @Override
                    public int compare(HashMap.Entry<String, MonthYearPair> left, HashMap.Entry<String, MonthYearPair> right) {
                        String thisDate = left.getValue().year + "/" + (Integer.toString(left.getValue().month).length() < 2 ? "0" + left.getValue().month : left.getValue().month);
                        String otherDate = right.getValue().year + "/" + (Integer.toString(right.getValue().month).length() < 2 ? "0" + right.getValue().month : right.getValue().month);

                        return thisDate.compareTo(otherDate);
                    }
                });

                for (Map.Entry<String, MonthYearPair> entry : entries)
                    spinnerData.add(entry.getKey());

                return timestampsFromDatabase;
            }

            @TargetApi(Build.VERSION_CODES.N)
            @Override
            protected void onPostExecute(Object object) {
                super.onPostExecute(object);

                if (Utils.toastAndExitIfWaDbException(object, StatsActivity.this)) {
                    return;
                }

                Long[] s = (Long[]) object;

                if (s == null) {
                    return;
                }

                if (s.length == 0)
                    return;

                String first;
                String second;

                try {
                    first = getDateFromTimeStamp(s[0]);
                    second = getDateFromTimeStamp(s[s.length - 1]);

                    textview.setText(getResources().getString(R.string.messagesTimespan, first, second));

                    String monthYearName = new SimpleDateFormat("MMM yyyy").format(s[0]);

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(StatsActivity.this, android.R.layout.simple_spinner_dropdown_item, spinnerData);
                    spinMonths.setAdapter(adapter);
                    spinMonths.setVisibility(View.VISIBLE);

                    displayContributionCalendar(monthYearName);

                    spinMonths.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            displayContributionCalendar(spinMonths.getItemAtPosition(i).toString());
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });

                    imgViewActivities[0].setBackgroundColor(Color.parseColor(contributionCalendarView.getLowColor()));
                    imgViewActivities[1].setBackgroundColor(Color.parseColor(contributionCalendarView.getMediumColor()));
                    imgViewActivities[2].setBackgroundColor(Color.parseColor(contributionCalendarView.getHighColor()));

                } catch (NumberFormatException | ArrayIndexOutOfBoundsException ex) {
                    ex.printStackTrace();
                }


            }
        }).execute();

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void displayContributionCalendar(String dateString) {
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(contributionCalendarViewWidth, contributionCalendarViewHeight);
        layoutParams.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        layoutParams.addRule(RelativeLayout.BELOW, R.id.spinMonths);

        relativeLayoutMain.removeView(contributionCalendarView);

        DistributionPair distributionPair = dateStringMedianPairHashMap.get(dateString);

        contributionCalendarView = new ContributionCalendarView(StatsActivity.this, contributionCalendarViewSquareSize, contributionCalendarViewSpacing, distributionPair.middleStart, distributionPair.middleEnd, dateStringTreeMapHashMap.get(dateString));
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

        RelativeLayout.LayoutParams layoutParamsLegend = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        layoutParamsLegend.addRule(RelativeLayout.BELOW, contributionCalendarView.getId());
        layoutParamsLegend.addRule(RelativeLayout.CENTER_HORIZONTAL, RelativeLayout.TRUE);
        layoutParamsLegend.setMargins(0, 20, 0, 0);
        setLegend(dateString);
        linearLayoutLegend.setVisibility(View.VISIBLE);
        linearLayoutLegend.setLayoutParams(layoutParamsLegend);

    }

    private void setLegend(String dateString) {
        DistributionPair distributionPair = dateStringMedianPairHashMap.get(dateString);
        int median = distributionPair.middleStart;
        int diffMedian = distributionPair.middleEnd;
        int highLimit = distributionPair.highest;

        tvActivities[0].setText(1 + "-" + median);
        tvActivities[1].setText("" + (median + 1) + "-" + diffMedian);
        tvActivities[2].setText(diffMedian + 1 + " - " + highLimit);

    }

    @SuppressLint("StaticFieldLeak")
    public void getMessagesCount(final TextView tvTo, TextView tvFrom, final TextView tvTotal, final String jid) {
        final TextView[] textViews = {tvTo, tvFrom};

        (new AsyncTask<Void, Void, Object>() {
            @Override
            protected Object doInBackground(Void... voids) {
                String[] arr = new String[0];
                try {
                    arr = WhatsAppDatabaseHelper.execSQL("/data/data/com.whatsapp/databases/msgstore.db", "select count(*),key_from_me from messages where key_remote_jid like " + '"' + jid + '"' + " and length(data) > 0 group by key_from_me;");
                } catch (WhatsAppDBException e) {
                    return e;
                }

                return arr;

            }

            @Override
            protected void onPostExecute(Object object) {
                super.onPostExecute(object);

                if(Utils.toastAndExitIfWaDbException(object, StatsActivity.this)){
                    return;
                }

                String[] s = (String[]) object;
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

    @TargetApi(Build.VERSION_CODES.N)
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

    class DistributionPair {
        int middleStart;
        int middleEnd;
        int highest;

        public DistributionPair(int middleStart, int middleEnd, int highest) {
            this.middleStart = middleStart;
            this.middleEnd = middleEnd;
            this.highest = highest;
        }
    }

}
