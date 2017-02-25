package com.phatware.android;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import com.phatware.android.recotest.R;
import com.phatware.android.recotest.Wand;

import java.util.ArrayList;

/**
 * Created by muradm373 on 8/26/2016.
 */
public class CustomAdapter extends BaseAdapter {
    private static LayoutInflater inflater = null;
    Context context;
    ArrayList<Integer> imageId;
    Wand mainAct;


    public CustomAdapter(Wand mainActivity, ArrayList<Integer> prgmImages) {
        // TODO Auto-generated constructor stub

        context = mainActivity;
        imageId = prgmImages;
        mainAct = mainActivity;
        inflater = (LayoutInflater) context.
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

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        Holder holder = new Holder();
        final View rowView;

        rowView = inflater.inflate(R.layout.componentcolor, null);


        holder.img = (ImageView) rowView.findViewById(R.id.image);

        ImageView round = (ImageView) rowView.findViewById(R.id.image);

        Drawable shape = round.getBackground();
        GradientDrawable colorShape = (GradientDrawable) shape;
        colorShape.setColor(imageId.get(position));
        holder.img.setBackground(colorShape);


        rowView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                mainAct.setBrushColor(imageId.get(position));
            }
        });

        return rowView;
    }

    public class Holder {
        ImageView img;
    }
}
