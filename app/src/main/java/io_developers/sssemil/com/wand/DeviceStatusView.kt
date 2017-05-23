package io_developers.sssemil.com.wand

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Created by emil on 8/05/17.
 */
class DeviceStatusView : LinearLayout {

    private var connectionStatus: Int = 0
    private var textView: TextView? = null
    private var iconView: ImageView? = null

    private val receiver = object : BroadcastReceiver() {
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
        connectionStatus = status
        when (connectionStatus) {
            DISCONNECTED -> {
                textView!!.setText(R.string.disconnected)
                iconView!!.setBackgroundResource(R.drawable.ic_disconnected)
            }
            CONNECTING -> {
                textView!!.setText(R.string.connecting)
                iconView!!.setBackgroundResource(R.drawable.ic_connecting)
            }
            CONNECTED -> {
                textView!!.setText(R.string.connected)
                iconView!!.setBackgroundResource(R.drawable.ic_connected)
            }
            ERROR -> {
                textView!!.setText(R.string.error)
                iconView!!.setBackgroundResource(R.drawable.ic_disconnected)
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
            connectionStatus = a.getInteger(R.styleable.DeviceStatusView_status, DISCONNECTED.toInt())
        } finally {
            a.recycle()
        }

        View.inflate(getContext(), R.layout.device_status_layout, this)

        this.iconView = findViewById(R.id.icon) as ImageView
        this.textView = findViewById(R.id.text) as TextView

        setStatus(connectionStatus)
    }

    internal fun unregisterReceiver() {
        if (mRegisteredReceiver) {
            context.unregisterReceiver(receiver)
            mRegisteredReceiver = false
        }
    }

    internal fun registerReceiver() {
        if (!mRegisteredReceiver) {
            context.registerReceiver(receiver, mFilter)
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
