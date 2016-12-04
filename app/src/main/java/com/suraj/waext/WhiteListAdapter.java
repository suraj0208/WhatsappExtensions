package com.suraj.waext;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by suraj on 4/12/16.
 */

public class WhiteListAdapter extends ArrayAdapter<String> {
    private List<String> whitelist = new ArrayList<>();
    private Context context;
    private HashMap<String,String> numberToNameHashMap;

    public WhiteListAdapter(Context context, List<String> contacts) {
        super(context, R.layout.contact_row);

        this.context=context;
        this.whitelist=contacts;
        this.numberToNameHashMap = new WhatsAppContactManager().getNumberToNameHashMap();
    }

    @Override
    public int getCount() {
        return this.whitelist.size();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View rowView = inflater.inflate(R.layout.contact_row, parent, false);

        TextView contactName = (TextView) rowView.findViewById(R.id.tvcontactname);

        if(numberToNameHashMap==null)
            return super.getView(position,convertView,parent);

        contactName.setText(numberToNameHashMap.get(whitelist.get(position)));

        return rowView;

    }
}
