package org.phenoapps.verify;

import android.content.Context;
import android.graphics.Typeface;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class CustomExpandableListAdapter extends BaseExpandableListAdapter {

    private Context context;
    private SparseArray<String> parentData;
    private ArrayList<String> childData, auxData;

    private Boolean displayAuxValues = false;


    public CustomExpandableListAdapter(Context context, SparseArray<String> data){
        this.context = context;
        this.parentData = data;
    }

    @Override
    public int getGroupCount() {
        return this.parentData.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return this.childData.size();
    }

    @Override
    public Object getGroup(int i) {
        return this.parentData.get(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return this.childData.get(0);
    }

    @Override
    public long getGroupId(int i) {
        return i;
    }

    @Override
    public long getChildId(int i, int i1) {
        return i1;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        String listTitle = (String) getGroup(i);
        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.list_group, null);
        }
        TextView listTitleTextView = (TextView) view.findViewById(R.id.listTitle);
        listTitleTextView.setTypeface(null, Typeface.BOLD);
        listTitleTextView.setText(listTitle);

        return view;
    }

    public void setAuxDisplay(Boolean value){
        this.displayAuxValues = value;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
        String listContent = (String) getChild(i, i1);
        if (view == null) {
            LayoutInflater layoutInflater = (LayoutInflater) this.context.
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = layoutInflater.inflate(R.layout.list_item, null);
        }

        TextView listChildTextView = (TextView) view.findViewById(R.id.expandedListItem);
        TextView listChildAux = (TextView) view.findViewById(R.id.auxValueView);


        if (this.displayAuxValues) {
            listChildAux.setVisibility(View.VISIBLE);

        } else {
            listChildAux.setVisibility(View.GONE);
        }

        listChildTextView.setText(listContent);
        listChildAux.setText(this.auxData.get(0));
        return view;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return false;
    }

    public void setChildData(ArrayList<String> childData) {
        this.childData = childData;
    }

    public void setAuxData(ArrayList<String> auxData){ this.auxData = auxData; }
}
