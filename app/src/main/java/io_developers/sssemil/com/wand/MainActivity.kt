package io_developers.sssemil.com.wand

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import com.fastaccess.permission.base.PermissionHelper
import com.phatware.android.WritePadFlagManager
import com.phatware.android.WritePadManager
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import io_developers.sssemil.com.wand.Account.ApiHelper
import io_developers.sssemil.com.wand.Account.ApiHelper.*
import io_developers.sssemil.com.wand.Account.LoginActivity
import io_developers.sssemil.com.wand.Account.SignupActivity
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, SharedPreferences.OnSharedPreferenceChangeListener {
    internal var boundService: RecognizerService? = null
    private var handler: Handler? = null
    private var connectedThread: ConnectedThread? = null
    private val bufferArray = ArrayList<Byte>()
    private var prevX: Int = 0
    private var prevY: Int = 0
    private var connection: ServiceConnection? = null
    private var recoInit: Boolean = false

    private var logoutMenuItem: MenuItem? = null
    private var signupMenuItem: MenuItem? = null
    private var loginMenuItem: MenuItem? = null

    private var sharedPreferences: SharedPreferences? = null
    private var headerEmailView: TextView? = null
    private var headerNameView: TextView? = null

    @SuppressLint("HandlerLeak")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        val toggle = ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById(R.id.nav_view) as NavigationView
        navigationView.setNavigationItemSelectedListener(this)

        headerEmailView = navigationView.getHeaderView(0).findViewById(R.id.userid) as TextView
        headerNameView = navigationView.getHeaderView(0).findViewById(R.id.username) as TextView

        val navMenu = navigationView.menu

        logoutMenuItem = navMenu.findItem(R.id.nav_logout)

        loginMenuItem = navMenu.findItem(R.id.nav_login)
        signupMenuItem = navMenu.findItem(R.id.nav_sign_up)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)

        setLoggedIn(sharedPreferences!!.getString(PREF_TOKEN, null) != null)

        // initialize ink inkView class
        val lName = WritePadManager.getLanguageName()
        WritePadManager.setLanguage(lName, this)

        val defaultDisplay = windowManager.defaultDisplay
        val size = Point()
        defaultDisplay.getSize(size)
        val screenHeight = size.y
        val textViewHeight = 15 * screenHeight / 100

        val colorList = intArrayOf(resources.getColor(R.color.green),
                resources.getColor(R.color.light_green),
                resources.getColor(R.color.lime),
                resources.getColor(R.color.yellow),
                resources.getColor(R.color.orange),
                resources.getColor(R.color.red),
                resources.getColor(R.color.pink),
                resources.getColor(R.color.cyan),
                resources.getColor(R.color.blue),
                resources.getColor(R.color.purple),
                resources.getColor(R.color.indigo),
                resources.getColor(R.color.black))


        ink_view!!.brushColor = colorList.last()
        layoutline.setBackgroundColor(colorList.last())
        supportActionBar?.setBackgroundDrawable(ColorDrawable(colorList.last()))


        val gridView = findViewById(R.id.gridView) as GridView

        val ca = CustomAdapter(this, colorList, CustomAdapter.OnColorClick { colorId ->
            ink_view!!.brushColor = colorId
            layoutline.setBackgroundColor(colorId)
            supportActionBar?.setBackgroundDrawable(ColorDrawable(colorId))
        })

        gridView.adapter = ca

        ink_view!!.setRecognizedTextContainer(recognized_text)

        connection = object : ServiceConnection {
            override fun onServiceConnected(className: ComponentName, service: IBinder) {
                // This is called when the connection with the service has been
                // established, giving us the service object we can use to
                // interact with the service.  Because we have bound to a explicit
                // service that we know is running in our own process, we can
                // cast its IBinder to a concrete class and directly access it.
                boundService = (service as RecognizerService.RecognizerBinder).service
                boundService!!.mHandler = ink_view!!.handler
            }

            override fun onServiceDisconnected(className: ComponentName) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                // Because it is running in our same process, we should never
                // see this happen.
                boundService = null
            }
        }

        bindService(Intent(this, RecognizerService::class.java), connection, android.content.Context.BIND_AUTO_CREATE)

        handler = object : Handler() {
            override fun handleMessage(msg: android.os.Message) {
                val sbprint = msg.obj as String

                Log.d("BT1", sbprint)

                var spl = sbprint.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (spl.size == 4) {
                    spl = Arrays.copyOfRange(spl, 1, 4)
                }
                if (spl.size >= 2 || spl.size == 3) {
                    try {
                        val x = -Integer.valueOf(spl[0])
                        val y = Integer.valueOf(spl[1])!!

                        var state = 1

                        if (spl.size >= 3) {
                            state = Integer.valueOf(spl[2])!!
                        }

                        if (x != prevX || y != prevY) {
                            if (ink_view != null) {
                                /*if (state == 1) {
                                    ink_view.addLine(x, y, false);
                                } else if (state == 2) {
                                    //ink_view.touch_up();
                                    ink_view.addLine(x, y, true);
                                }*/

                                ink_view!!.onWandEvent(x.toFloat(), y.toFloat(), state == 1)
                            }
                            prevX = x
                            prevY = y
                        }
                    } catch (ignored: NumberFormatException) {
                    }

                }
            }
        }

        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun registerReceivers() {
        if (deviceState != null) {
            deviceState!!.registerReceiver()
        }
    }

    private fun unregisterReceivers() {
        if (deviceState != null) {
            deviceState!!.unregisterReceiver()
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        when (id) {
            R.id.action_settings -> {
                if (sliding_layout!!.panelState != SlidingUpPanelLayout.PanelState.EXPANDED) {
                    sliding_layout!!.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
                } else {
                    sliding_layout!!.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
                }
                return true
            }
            R.id.action_save -> {
                saveDrawings()
                return true
            }
            R.id.action_share -> {
                shareDrawings()
                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        grantResults.forEach {
            if (it == PackageManager.PERMISSION_DENIED) {
                AlertDialog.Builder(this)
                        .setMessage(getString(R.string.permission_denied))
                        .setNeutralButton(android.R.string
                                .ok, null)
                        .show()
                return
            }
        }

        when (requestCode) {
            REQUEST_READ_EXTERNAL_STORAGE_SAVE -> {
                saveDrawings()
            }
            REQUEST_READ_EXTERNAL_STORAGE_SHARE -> {
                shareDrawings()
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun shareDrawings() {
        val postfix = saveDrawings(REQUEST_READ_EXTERNAL_STORAGE_SHARE)

        val path: File = File(String.format("%s/Wand/wand_%s.jpg", Environment
                .getExternalStorageDirectory().path, postfix))
        val mime: MimeTypeMap = MimeTypeMap.getSingleton()
        val ext = path.name.substring(path.name.lastIndexOf(".") + 1)
        val sharingIntent = Intent(Intent.ACTION_SEND)
        sharingIntent.type = mime.getMimeTypeFromExtension(ext)
        sharingIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(path))
        sharingIntent.putExtra(Intent.EXTRA_TEXT, recognized_text.text);
        startActivity(Intent.createChooser(sharingIntent, getString(R.string.share_using)))
    }

    private fun saveDrawings(): String? {
        return saveDrawings(REQUEST_READ_EXTERNAL_STORAGE_SAVE)
    }

    private fun saveDrawings(requestCode: Int): String? {
        if (!PermissionHelper.isPermissionGranted(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), requestCode)
            return null
        } else {
            val c: Calendar = Calendar.getInstance()

            val postfix: String = SimpleDateFormat("dd-MMM-yyyy hh:mm:ss aa").format(c.time) +
                    Random().nextInt()

            val startPath: File = File(String.format("%s/Wand/", Environment
                    .getExternalStorageDirectory().path))

            if (!startPath.exists()) {
                startPath.mkdirs()
            }
            val b: Bitmap = ink_view.getBitmap()

            val allpixels: IntArray = kotlin.IntArray(b.width * b.height)

            b.getPixels(allpixels, 0, b.width, 0, 0, b.width, b.height)

            for (i in 0..(allpixels.size - 1)) {
                if (allpixels[i] == Color.TRANSPARENT) {
                    allpixels[i] = Color.WHITE
                }
            }

            b.setPixels(allpixels, 0, b.width, 0, 0, b.width,
                    b.height)

            //b.eraseColor(Color.TRANSPARENT)
            b.compress(Bitmap.CompressFormat.JPEG, 100, FileOutputStream(
                    String.format("%s/wand_%s.jpg", startPath.path, postfix)))

            val out: PrintWriter = PrintWriter(String.format("%s/wand_%s.txt", startPath.path,
                    postfix))
            out.print(recognized_text.text)
            out.flush()

            Toast.makeText(applicationContext, getString(R.string.saved_to) + startPath.path +
                    "/wand_" + postfix, Toast.LENGTH_LONG).show()

            return postfix
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        when (id) {
            R.id.nav_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out)
            }
            R.id.nav_license -> {
                startActivity(Intent(this, LicenseActivity::class.java))
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out)
            }
            R.id.nav_login -> {
                startActivity(Intent(this, LoginActivity::class.java))
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out)
            }
            R.id.nav_sign_up -> {
                startActivity(Intent(this, SignupActivity::class.java))
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out)
            }
            R.id.nav_logout -> ApiHelper.logOut(sharedPreferences)
            else -> {
            }
        }

        val drawer = findViewById(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    public override fun onResume() {
        if (ink_view != null) {
            ink_view!!.cleanView(true)
        }

        WritePadFlagManager.initialize(this)

        super.onResume()

        Thread(Runnable { findWand() }).start()

        registerReceivers()
    }

    public override fun onPause() {
        super.onPause()
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }

        unregisterReceivers()
    }

    override fun onDestroy() {
        unbindService(connection)
        super.onDestroy()
        if (recoInit) {
            WritePadManager.recoFree()
        }
        recoInit = false
    }

    private fun findWand() {
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }

        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            runOnUiThread {
                Toast.makeText(applicationContext, "Device doesn't Support Bluetooth :(", Toast.LENGTH_SHORT).show()
                deviceState!!.setStatus(DeviceStatusView.ERROR.toInt())
            }

            return
        }

        if (!bluetoothAdapter.isEnabled) {
            val enableAdapter = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableAdapter, 0)
        }

        val bondedDevices = bluetoothAdapter.bondedDevices

        var device: BluetoothDevice? = null

        if (bondedDevices.isEmpty()) {
            runOnUiThread {
                Toast.makeText(applicationContext, getString(R.string.pair_request), Toast.LENGTH_SHORT).show()
                //deviceState.setStatus(DeviceStatusView.DISCONNECTED);
            }
        } else {
            for (iterator in bondedDevices) {
                Log.i("name", iterator.name)
                if (iterator.name == BT_DEVICE_NAME) {
                    device = iterator //device is an object of type BluetoothDevice
                    break
                }
            }
        }

        if (device != null) {
            /*runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //deviceState.setStatus(DeviceStatusView.CONNECTING);
                }
            });*/
            try {
                val bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)

                try {
                    bluetoothSocket!!.connect()
                    runOnUiThread {
                        //Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                        deviceState!!.setStatus(DeviceStatusView.CONNECTED.toInt())
                    }
                } catch (e: IOException) {
                    try {
                        bluetoothSocket!!.close()
                    } catch (e2: IOException) {
                        Log.e("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.message + ".")
                        runOnUiThread { deviceState!!.setStatus(DeviceStatusView.ERROR.toInt()) }
                        return
                    }

                }

                if (connectedThread != null) {
                    connectedThread!!.cancel()
                    connectedThread = null
                }

                connectedThread = ConnectedThread(bluetoothSocket)
                connectedThread!!.start()
            } catch (e: IOException) {
                Log.e("Fatal Error", "In onResume() and socket create failed: " + e.message + ".")

                runOnUiThread { deviceState!!.setStatus(DeviceStatusView.ERROR.toInt()) }
                return
            }
        }
    }

    private fun setLoggedIn(menuLoggedIn: Boolean) {
        logoutMenuItem!!.isVisible = menuLoggedIn

        loginMenuItem!!.isVisible = !menuLoggedIn
        signupMenuItem!!.isVisible = !menuLoggedIn

        if (menuLoggedIn) {
            headerNameView!!.text = sharedPreferences!!.getString(PREF_NAME, null)
            headerEmailView!!.text = sharedPreferences!!.getString(PREF_EMAIL, null)
        } else {
            headerNameView!!.setText(R.string.no_account)
            headerEmailView!!.text = null
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == PREF_TOKEN) {
            setLoggedIn(sharedPreferences.getString(PREF_TOKEN, null) != null)
        }
    }

    private inner class ConnectedThread internal constructor(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: InputStream?
        private var mmBuffer: ByteArray? = null
        private var run = true

        init {
            var tmpIn: InputStream? = null
            var tmpOut: OutputStream? = null

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = mmSocket.inputStream
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "Error Input Stream", Toast.LENGTH_SHORT).show()
            }

            try {
                tmpOut = mmSocket.outputStream
            } catch (e: IOException) {
                Toast.makeText(applicationContext, "Error Output Stream", Toast.LENGTH_SHORT).show()
            }

            mmInStream = tmpIn
        }

        override fun run() {
            mmBuffer = ByteArray(1024)  // buffer store for the stream
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (run) {
                try {
                    // Read from the InputStream
                    if (mmInStream == null) break

                    numBytes = mmInStream.read(mmBuffer!!)
                    if (numBytes > 0) {
                        for (i in 0..numBytes - 1) {
                            bufferArray.add(mmBuffer!![i])
                        }
                        // если встречаем конец строки,
                        var sbprint = StringBuilder()
                        var i = 0
                        while (i < bufferArray.size) {
                            if (bufferArray[i] == 127.toByte()) {
                                if (sbprint.isNotEmpty()) {
                                    handler!!.obtainMessage(0, 0, 0, sbprint.toString()).sendToTarget()
                                }
                                sbprint = StringBuilder()
                                val j = 0
                                while (j <= i) {
                                    bufferArray.removeAt(j)
                                    i--
                                }
                            } else {
                                sbprint.append(bufferArray[i]).append(";")
                            }
                            i++
                        }
                    }
                } catch (e: Exception) {
                    try {
                        Thread.sleep(1000)
                    } catch (e1: InterruptedException) {
                        e1.printStackTrace()
                    }

                    findWand()
                    break
                }

            }

            try {
                mmSocket.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        /* Call this from the main activity to shutdown the connection */
        internal fun cancel() {
            run = false
        }
    }

    companion object {
        val BT_DEVICE_NAME = "SPP-CA"
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val REQUEST_READ_EXTERNAL_STORAGE_SAVE: Int = 123
        private val REQUEST_READ_EXTERNAL_STORAGE_SHARE: Int = 124
    }
}
