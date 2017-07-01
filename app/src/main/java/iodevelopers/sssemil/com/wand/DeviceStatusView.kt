package iodevelopers.sssemil.com.wand

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
class DeviceStatusView : android.widget.LinearLayout {

    private var connectionStatus: Int = 0
    private var textView: android.widget.TextView? = null
    private var iconView: android.widget.ImageView? = null

    constructor(context: android.content.Context, attrs: android.util.AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: android.content.Context, attrs: android.util.AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        init(context, attrs)
    }

    fun setStatus(status: Int) {
        connectionStatus = status
        when (connectionStatus) {
            DeviceStatusView.Companion.DISCONNECTED -> {
                textView!!.setText(R.string.disconnected)
                iconView!!.setBackgroundResource(R.drawable.ic_disconnected)
            }
            DeviceStatusView.Companion.CONNECTING -> {
                textView!!.setText(R.string.connecting)
                iconView!!.setBackgroundResource(R.drawable.ic_connecting)
            }
            DeviceStatusView.Companion.CONNECTED -> {
                textView!!.setText(R.string.connected)
                iconView!!.setBackgroundResource(R.drawable.ic_connected)
            }
            DeviceStatusView.Companion.ERROR -> {
                textView!!.setText(R.string.error)
                iconView!!.setBackgroundResource(R.drawable.ic_disconnected)
            }
        }
        invalidate()
        requestLayout()
    }

    private fun init(context: android.content.Context, attrs: android.util.AttributeSet) {
        val a = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.DeviceStatusView,
                0, 0)

        try {
            connectionStatus = a.getInteger(R.styleable.DeviceStatusView_status, DeviceStatusView.Companion.DISCONNECTED.toInt())
        } finally {
            a.recycle()
        }

        android.view.View.inflate(getContext(), R.layout.device_status_layout, this)

        this.iconView = findViewById<ImageView>(R.id.icon)
        this.textView = findViewById<TextView>(R.id.text)

        setStatus(connectionStatus)
    }


    companion object {
        val ERROR: Int = -1
        val DISCONNECTED: Int = 0
        val CONNECTING: Int = 1
        val CONNECTED: Int = 2
    }
}
