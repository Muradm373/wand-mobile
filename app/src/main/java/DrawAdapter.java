package com.phatware.android;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.phatware.android.recotest.Wand;
import com.phatware.android.recotest.R;

import java.util.ArrayList;

/**
 * Created by muradm373 on 8/26/2016.
 */
public class DrawAdapter extends BaseAdapter{
    Context context;
    ArrayList<Drawable> imageId;
    private static LayoutInflater inflater=null;
    Wand mainAct;



    public DrawAdapter(Wand mainActivity, ArrayList<Drawable> prgmImages) {
        // TODO Auto-generated constructor stub

        context=mainActivity;
        imageId=prgmImages;
        mainAct = mainActivity;
        inflater = ( LayoutInflater )context.
                getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return imageId.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    public class Holder
    {
        ImageView img;
    }
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        Holder holder=new Holder();
        final View rowView;

        rowView = inflater.inflate(R.layout.componentcolor, null);


        holder.img=(ImageView) rowView.findViewById(R.id.image);

        ImageView round = (ImageView) rowView.findViewById(R.id.image);


        holder.img.setBackground(imageId.get(position));





        rowView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mainAct.setBrushColor(imageId.get(position));
            }
        });

        return rowView;
    }
}
