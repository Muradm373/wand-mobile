package io_developers.sssemil.com.wand

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView

/**
 * Created by emil on 8/05/17.
 */
class DeviceStatusView : RelativeLayout {

    private var mConnectionStatus: Int = 0
    private var mTextView: TextView? = null
    private var mIconView: ImageView? = null

    private val mReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

            if (BluetoothDevice.ACTION_ACL_CONNECTED == action) {
                //Device is now connected
                if (device != null && device.name == MainActivity.BT_DEVICE_NAME) {
                    setStatus(CONNECTED.toInt())
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                //Device has disconnected
                if (device != null && device.name == MainActivity.BT_DEVICE_NAME) {
                    setStatus(DISCONNECTED.toInt())
                }
            }
        }
    }
    private var mFilter: IntentFilter? = null
    private var mRegisteredReceiver = false

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    fun setStatus(status: Int) {
        mConnectionStatus = status
        when (mConnectionStatus) {
            DISCONNECTED -> {
                mTextView!!.setText(R.string.disconnected)
                mIconView!!.setBackgroundResource(R.drawable.ic_disconnected)
            }
            CONNECTING -> {
                mTextView!!.setText(R.string.connecting)
                mIconView!!.setBackgroundResource(R.drawable.ic_connecting)
            }
            CONNECTED -> {
                mTextView!!.setText(R.string.connected)
                mIconView!!.setBackgroundResource(R.drawable.ic_connected)
            }
            ERROR -> {
                mTextView!!.setText(R.string.error)
                mIconView!!.setBackgroundResource(R.drawable.ic_disconnected)
            }
        }
        invalidate()
        requestLayout()
    }

    private fun init(context: Context, attrs: AttributeSet) {
        mFilter = IntentFilter()
        mFilter!!.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        mFilter!!.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        mFilter!!.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)

        registerReceiver()

        val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.DeviceStatusView,
                0, 0)

        try {
            mConnectionStatus = a.getInteger(R.styleable.DeviceStatusView_status, DISCONNECTED.toInt())
        } finally {
            a.recycle()
        }

        View.inflate(getContext(), R.layout.device_status_layout, this)

        this.mTextView = findViewById(R.id.text) as TextView
        this.mIconView = findViewById(R.id.icon) as ImageView

        setStatus(mConnectionStatus)
    }

    internal fun unregisterReceiver() {
        if (mRegisteredReceiver) {
            context.unregisterReceiver(mReceiver)
            mRegisteredReceiver = false
        }
    }

    internal fun registerReceiver() {
        if (!mRegisteredReceiver) {
            context.registerReceiver(mReceiver, mFilter)
            mRegisteredReceiver = true
        }
    }

    companion object {

        val ERROR: Int = -1
        val DISCONNECTED: Int = 0
        val CONNECTING: Int = 1
        val CONNECTED: Int = 2
    }
}
