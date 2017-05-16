package io_developers.sssemil.com.wand;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import static io_developers.sssemil.com.wand.MainActivity.BT_DEVICE_NAME;

/**
 * Created by emil on 8/05/17.
 */
public class DeviceStatusView extends RelativeLayout {

    public static final byte ERROR = -1;
    public static final byte DISCONNECTED = 0;
    public static final byte CONNECTING = 1;
    public static final byte CONNECTED = 2;

    private int mConnectionStatus;
    private TextView mTextView;
    private ImageView mIconView;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                //Device is now connected
                if (device != null && device.getName().equals(BT_DEVICE_NAME)) {
                    setStatus(CONNECTED);
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                //Device has disconnected
                if (device != null && device.getName().equals(BT_DEVICE_NAME)) {
                    setStatus(DISCONNECTED);
                }
            }
        }
    };
    private IntentFilter mFilter;
    private boolean mRegisteredReceiver = false;

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
            case ERROR:
                mTextView.setText(R.string.error);
                mIconView.setBackgroundResource(R.drawable.ic_disconnected);
                break;

        }
        invalidate();
        requestLayout();
    }

    private void init(Context context, AttributeSet attrs) {
        mFilter = new IntentFilter();
        mFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        mFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);

        registerReceiver();

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

    void unregisterReceiver() {
        if (mRegisteredReceiver) {
            getContext().unregisterReceiver(mReceiver);
            mRegisteredReceiver = false;
        }
    }

    void registerReceiver() {
        if (!mRegisteredReceiver) {
            getContext().registerReceiver(mReceiver, mFilter);
            mRegisteredReceiver = true;
        }
    }
}
