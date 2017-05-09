package io_developers.sssemil.com.wand;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.UUID;

/**
 * Created by emil on 8/05/17.
 */
public class DeviceStatusView extends RelativeLayout {

    public static final int DISCONNECTED = 0;
    public static final int CONNECTING = 1;
    public static final int CONNECTED = 2;

    private int mConnectionStatus;
    private TextView mTextView;
    private ImageView mIconView;

    public DeviceStatusView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public DeviceStatusView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    public void setStatus(int status) {
        mConnectionStatus = status;
        switch (mConnectionStatus) {
            case DISCONNECTED:
                mTextView.setText(R.string.disconnected);
                mIconView.setBackgroundResource(R.drawable.ic_disconnected);
                break;
            case CONNECTING:
                mTextView.setText(R.string.connecting);
                mIconView.setBackgroundResource(R.drawable.ic_connecting);
                break;
            case CONNECTED:
                mTextView.setText(R.string.connected);
                mIconView.setBackgroundResource(R.drawable.ic_connected);
                break;

        }
        invalidate();
        requestLayout();
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.DeviceStatusView,
                0, 0);

        try {
            mConnectionStatus = a.getInteger(R.styleable.DeviceStatusView_status, DISCONNECTED);
        } finally {
            a.recycle();
        }

        inflate(getContext(), R.layout.device_status_layout, this);
        this.mTextView = (TextView) findViewById(R.id.text);
        this.mIconView = (ImageView) findViewById(R.id.icon);

        setStatus(mConnectionStatus);
    }
}
