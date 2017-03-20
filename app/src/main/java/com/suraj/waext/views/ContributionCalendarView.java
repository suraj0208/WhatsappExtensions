package com.suraj.waext.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import com.suraj.waext.data.ContributionCalendarDate;

import java.util.Calendar;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.TreeMap;

/**
 * Created by suraj on 22/2/17.
 */
public class ContributionCalendarView extends View {
    private int squareSide;
    private int spacing;
    private int lowerLimit;
    private int upperLimit;

    private String lowColor;
    private String mediumColor;
    private String highColor;
    private String emptyColor;

    public String getLowColor() {
        return lowColor;
    }

    public String getMediumColor() {
        return mediumColor;
    }

    public String getHighColor() {
        return highColor;
    }

    public String getEmptyColor() {
        return emptyColor;
    }

    private TreeMap<ContributionCalendarDate, Integer> dateCountTreeMap;

    private Paint belowAvgPaint;
    private Paint avgPaint;
    private Paint aboveAvgPaint;
    private Paint emptyPaint;
    private Paint textPaint;


    public void setLowColor(String lowColor) {
        this.lowColor = lowColor;
    }

    public void setMediumColor(String mediumColor) {
        this.mediumColor = mediumColor;
    }

    public void setHighColor(String highColor) {
        this.highColor = highColor;
    }

    public void setSquareSide(int squareSide) {
        this.squareSide = squareSide;
    }

    public int getSpacing() {
        return spacing;
    }

    public int getSquareSide() {
        return squareSide;
    }

    public void setSpacing(int spacing) {
        this.spacing = spacing;
    }

    public ContributionCalendarView(Context context, int side, int spacing, int intervalStart, int intervalEnd, TreeMap<ContributionCalendarDate, Integer> dateCountTreeMap) {
        super(context);
        this.squareSide = side;
        this.spacing = spacing;
        this.dateCountTreeMap = new TreeMap<>(dateCountTreeMap);

        lowerLimit = intervalStart;
        upperLimit = intervalEnd;

        setLowColor("#D6E685");
        setMediumColor("#8CC665");
        setHighColor("#1E6823");
        setEmptyColor("#ffcccccc");


        init();
    }

    public ContributionCalendarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ContributionCalendarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void init() {
        belowAvgPaint = setPaint(Color.parseColor(lowColor));
        avgPaint = setPaint(Color.parseColor(mediumColor));
        aboveAvgPaint = setPaint(Color.parseColor(highColor));
        emptyPaint = setPaint(Color.parseColor(emptyColor));

        textPaint = setPaint(Color.parseColor("#D6E685"));
        textPaint.setTextSize(20);
        textPaint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        try {
            Calendar c = Calendar.getInstance(Locale.getDefault());
            c.setTime(dateCountTreeMap.firstKey());
            c.set(Calendar.DAY_OF_MONTH, 1);

            int firstDay = c.get(Calendar.DAY_OF_WEEK);

            int j;
            int k = squareSide / 2;

            int day = 1;

            int col = 0;

            for (col = 0; col < 6; col++) {
                j = squareSide / 2;
                for (int row = 0; row < 7; row++) {

                    if (firstDay > 1) {
                        canvas.drawPoint(k, j, emptyPaint);
                        firstDay--;
                        j += squareSide + spacing;
                        continue;
                    }

                    if (dateCountTreeMap.size() > 0 && day == dateCountTreeMap.firstKey().getDate()) {

                        int count = dateCountTreeMap.remove(dateCountTreeMap.firstKey());

                        if (count >= lowerLimit && count <= upperLimit) {
                            canvas.drawPoint(k, j, avgPaint);
                            textPaint.setColor(aboveAvgPaint.getColor());
                        } else if (count < lowerLimit) {
                            canvas.drawPoint(k, j, belowAvgPaint);
                            textPaint.setColor(aboveAvgPaint.getColor());
                        } else {
                            canvas.drawPoint(k, j, aboveAvgPaint);
                            textPaint.setColor(belowAvgPaint.getColor());
                        }

                        canvas.drawText("" + day, k - 10, j + 10, textPaint);

                    } else {
                        canvas.drawPoint(k, j, emptyPaint);
                    }

                    j += squareSide + spacing;
                    day++;

                    if (day > 31)
                        break;

                }
                k += squareSide + spacing;

                if (day > 31)
                    break;
            }
        } catch (NoSuchElementException e) {
            e.printStackTrace();

        }
    }

    public Paint setPaint(int color) {
        Paint paint = new Paint();
        paint.setColor(color);
        paint.setStrokeWidth(squareSide);
        paint.setStyle(Paint.Style.STROKE);
        return paint;
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        Calendar c = Calendar.getInstance(Locale.getDefault());
        c.setTime(dateCountTreeMap.firstKey());
        c.set(Calendar.DAY_OF_MONTH, 1);

        int firstDay = c.get(Calendar.DAY_OF_WEEK);

        if (firstDay <= 5) {
            params.width = params.width - getSpacing() - getSquareSide();
        }
        super.setLayoutParams(params);
    }

    public void setEmptyColor(String emptyColor) {
        this.emptyColor = emptyColor;
    }
}
