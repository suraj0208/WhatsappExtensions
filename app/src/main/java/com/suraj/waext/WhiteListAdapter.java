package com.suraj.waext;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by suraj on 4/12/16.
 */

public class WhiteListAdapter extends ArrayAdapter<String> {
    private List<String> whitelist = new ArrayList<>();
    private Context context;
    private WhiteListContactRowManager whiteListContactRowManager;

    public WhiteListAdapter(Context context, List<String> contacts, WhiteListContactRowManager whiteListContactRowManager) {
        super(context, R.layout.contact_row);

        this.context = context;
        this.whitelist = contacts;
        this.whiteListContactRowManager = whiteListContactRowManager;
    }


    @Override
    public int getCount() {
        return this.whitelist.size();
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.contact_row, parent, false);
        this.whiteListContactRowManager.onInflateContactRow(rowView,whitelist,position);
        return rowView;
    }
}

interface WhiteListContactRowManager
{
    void onInflateContactRow(View view,List<String> whitelist,int position);
}
