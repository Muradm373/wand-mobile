package io_developers.sssemil.com.wand;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class CustomAdapter extends BaseAdapter {

    private int mImages[];
    private Context mContext;

    private OnColorClick mListener;

    CustomAdapter(Context context, int[] images, OnColorClick listener) {
        mContext = context;
        mImages = images;
        mListener = listener;
    }

    @Override
    public int getCount() {
        return mImages.length;
    }

    @Override
    public Object getItem(int position) {
        return mImages[position];
    }

    @Override
    public long getItemId(int position) {
        return mImages[position];
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Holder holder = new Holder();
        final View rowView;

        LayoutInflater inflater = (LayoutInflater) mContext.
                getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE);

        rowView = inflater.inflate(R.layout.componentcolor, null);

        holder.imageView = (ImageView) rowView.findViewById(R.id.image);

        Drawable shape = holder.imageView.getBackground();
        GradientDrawable colorShape = (GradientDrawable) shape;
        colorShape.setColor(mImages[position]);
        holder.imageView.setBackground(colorShape);

        rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mContext.setBrushColor(imageId.get(position));
                if (mListener != null) {
                    mListener.onColorClick(mImages[position]);
                }
            }
        });

        return rowView;
    }

    public interface OnColorClick {
        void onColorClick(int colorId);
    }

    public class Holder {
        ImageView imageView;
    }
}
