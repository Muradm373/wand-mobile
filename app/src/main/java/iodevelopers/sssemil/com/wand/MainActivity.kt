package iodevelopers.sssemil.com.wand

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.*
import android.content.pm.PackageManager.FEATURE_BLUETOOTH_LE
import android.hardware.SensorAdditionalInfo
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.annotation.RequiresApi
import android.support.design.widget.NavigationView
import android.support.v4.app.FragmentManager
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.GridView
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import com.fastaccess.permission.base.PermissionHelper
import com.phatware.android.WritePadFlagManager
import com.phatware.android.WritePadManager
import com.sothree.slidinguppanel.SlidingUpPanelLayout
import iodevelopers.sssemil.com.wand.Account.ApiHelper
import iodevelopers.sssemil.com.wand.Account.LoginActivity
import iodevelopers.sssemil.com.wand.Account.SignupActivity
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
        SharedPreferences.OnSharedPreferenceChangeListener, DialogInterface.OnClickListener, SesitivityDialog.SensListener {


    private var sens = 1.5f;
    private var bluetoothAdapter: BluetoothAdapter? = null
    private val REQUEST_ENABLE_BT = 1
    private var scanHandler: Handler? = null
    private var leScanner: BluetoothLeScanner? = null
    private var settings: ScanSettings? = null
    private var filters: List<ScanFilter>? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var bluetoothGattCharacteristic: BluetoothGattCharacteristic? = null



    internal var boundService: RecognizerService? = null
    private var handler: android.os.Handler? = null

    private var connectedThread: MainActivity.ConnectedThread? = null
    private val bufferArray = ArrayList<Byte>()
    private var prevX: Byte = 0
    private var prevY: Byte = 0
    private var connection: android.content.ServiceConnection? = null
    private var recoInit: Boolean = false

    private var logoutMenuItem: MenuItem? = null
    private var signupMenuItem: MenuItem? = null
    private var loginMenuItem: MenuItem? = null

    private var sharedPreferences: SharedPreferences? = null
    private var headerEmailView: android.widget.TextView? = null
    private var headerNameView: android.widget.TextView? = null

    private var filter: android.content.IntentFilter? = null
    private var registeredReceiver = false

    private var currentColorId: Int = 0

    private var invertXY = false
    private var revertX = false
    private var revertY = false

    private var scanResultDevices: ArrayList<BluetoothDevice> = ArrayList()

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            Log.i("onConnectionStateChange", "Status: " + status)
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i("gattCallback", "STATE_CONNECTED")
                    gatt.discoverServices()
                    setStatus(DeviceStatusView.CONNECTED)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    setStatus(DeviceStatusView.DISCONNECTED)
                    Log.e("gattCallback", "STATE_DISCONNECTED")
                }
                else -> Log.e("gattCallback", "STATE_OTHER")
            }

        }



        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val services = gatt.services
            Log.i("onServicesDiscovered", services.toString())
            services.filter { it.uuid == MainActivity.Companion.BLE_UUID }
                    .forEach {
                        gatt.readCharacteristic(it.characteristics[0])
                    }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt,
                                          characteristic: BluetoothGattCharacteristic, status: Int) {
            Log.i("onCharacteristicRead", characteristic.toString())
            bluetoothGattCharacteristic = characteristic
            bluetoothGatt?.setCharacteristicNotification(characteristic, true);

            if (characteristic.descriptors.size > 0) {
                val descriptor: BluetoothGattDescriptor = characteristic.getDescriptor(MainActivity.Companion.BLE_UUID)

                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                bluetoothGatt?.writeDescriptor(descriptor)
                gatt.disconnect()
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt,
                                             characteristic: BluetoothGattCharacteristic) {
            Log.i("onCharacteristicChanged", characteristic.toString())

            handler!!.obtainMessage(0, 0, 0, characteristic.value).sendToTarget()
        }
    }


    override fun sensListener(num: Float) {
        Log.d("Sense", "Done");
        sens =  num;
        Log.d("Sense", ""+ sens);
    }

    private fun getScanCallback(): ScanCallback? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (scanCallbackVar === null) {
                scanCallbackVar = object : ScanCallback() {
                    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
                    override fun onScanResult(callbackType: Int, result: ScanResult) {
                        Log.i("callbackType", callbackType.toString())
                        Log.i("result", result.toString())

                        //val btDevice: BluetoothDevice = result.device
                        //connectToDevice(btDevice)
                        //onBatchScanResults(arrayListOf(result).toList())
                        if (!scanResultDevices.contains(result.device)) {
                            scanResultDevices.add(result.device)
                            scanAdapter?.add(result.device.toString())
                        }
                    }

                    override fun onBatchScanResults(results: List<ScanResult>) {
                        for (sr in results) {
                            Log.i("ScanResult - Results", sr.toString())
                        }

                        //this@MainActivity.scanResultDevices = results

                        //TODO show alert with list here & hide loading indicator

                        /*val adapter: ArrayAdapter<String> = ArrayAdapter(this@MainActivity,
                                android.R.layout.select_dialog_singlechoice);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            for (sr in results) {
                                adapter.add(sr.toString())
                            }
                        }

                        val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
                        builder.setAdapter(adapter, this@MainActivity)
                        builder.create().show()*/
                    }

                    override fun onScanFailed(errorCode: Int) {
                        Log.e("Scan Failed", "Error Code: " + errorCode)
                    }
                }
            }
        }

        return scanCallbackVar
    }

    private var scanCallbackVar: ScanCallback? = null

    private val leScanCallback = BluetoothAdapter.LeScanCallback { device, rssi, scanRecord ->
        runOnUiThread {
            Log.i("onLeScan", device.toString())
            //connectToDevice(device)
            if (!scanResultDevices.contains(device)) {
                scanResultDevices.add(device)
                scanAdapter?.add(device.toString())
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
            val action = intent.action
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

            if (BluetoothDevice.ACTION_ACL_CONNECTED == action) {
                //Device is now connected
                if (device != null && device.name == BT_DEVICE_NAME) {
                    setStatus(DeviceStatusView.Companion.CONNECTED)
                }
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED == action) {
                //Device has disconnected
                if (device != null && device.name == BT_DEVICE_NAME) {
                    setStatus(DeviceStatusView.Companion.DISCONNECTED)
                    //startScan()
                }
            }
        }
    }

    private var scanAdapter: ArrayAdapter<String>? = null

    @android.annotation.SuppressLint("HandlerLeak")
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        filter = android.content.IntentFilter()
        filter!!.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter!!.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        filter!!.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)

        //setStatus(DeviceStatusView.Companion.DISCONNECTED)

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = android.support.v7.app.ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()

        val navigationView = findViewById<NavigationView>(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        headerEmailView = navigationView.getHeaderView(0).findViewById<TextView>(R.id.userid)
        headerNameView = navigationView.getHeaderView(0).findViewById<TextView>(R.id.username)

        val navMenu = navigationView.menu

        logoutMenuItem = navMenu.findItem(R.id.nav_logout)

        loginMenuItem = navMenu.findItem(R.id.nav_login)
        signupMenuItem = navMenu.findItem(R.id.nav_sign_up)

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPreferences!!.registerOnSharedPreferenceChangeListener(this)

        invertXY = sharedPreferences!!.getBoolean(PREF_INVERT_XY, false)
        revertX = sharedPreferences!!.getBoolean(PREF_REVERT_X, false)
        revertY = sharedPreferences!!.getBoolean(PREF_REVERT_Y, false)

        setLoggedIn(sharedPreferences!!.getString(ApiHelper.Companion.PREF_TOKEN, null) != null)

        // initialize ink inkView class
        val lName = WritePadManager.getLanguageName()
        WritePadManager.setLanguage(lName, this)

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

        setColor(colorList.last())

        val gridView = findViewById<GridView>(R.id.gridView)

        val ca = CustomAdapter(this, colorList, CustomAdapter.OnColorClick { colorId ->
            setColor(colorId)
            sliding_layout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
        })

        gridView.adapter = ca

        ink_view!!.setRecognizedTextContainer(recognized_text)

        connection = object : android.content.ServiceConnection {
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

        bindService(android.content.Intent(this, RecognizerService::class.java), connection, android.content.Context.BIND_AUTO_CREATE)

        ink_view!!.onWandEvent(500f,
                500f, false)

        handler = object : android.os.Handler() {
            override fun handleMessage(msg: android.os.Message) {
                val bytes = msg.obj as ByteArray

                Log.d("BT1", bytes.toString())

                if (bytes.size >= 3) {
                    try {
                        if (true || bytes[0] != prevX || bytes[1] != prevY) {
                            if (ink_view != null) {
                                /*if (state == 1) {
                                    ink_view.addLine(x, y, false);
                                } else if (state == 2) {
                                    //ink_view.touch_up();
                                    ink_view.addLine(x, y, true);
                                }*/

                                ink_view!!.onWandEvent(
                                        3f * (
                                                if (invertXY) bytes[0].toFloat()
                                                else bytes[1].toFloat()) * (if (revertX) -1 else 1),
                                         3f* (
                                                if (invertXY) bytes[1].toFloat()
                                                else bytes[0].toFloat()) * (if (revertY) -1 else 1),
                                        bytes[2] != 1.toByte())
                            }
                            prevX = bytes[0]
                            prevY = bytes[1]
                        }
                    } catch (ignored: NumberFormatException) {
                    }

                }
            }
        }

        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }

        scanHandler = android.os.Handler()
        if (!packageManager.hasSystemFeature(FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(applicationContext, getString(R.string.no_bt_support_error),
                    LENGTH_SHORT).show()
            setStatus(DeviceStatusView.Companion.ERROR)
        }
        val bluetoothManager = getSystemService(android.content.Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        scanAdapter = ArrayAdapter(this@MainActivity,
                android.R.layout.select_dialog_singlechoice);
    }

    private fun setColor(colorId: Int) {
        currentColorId = colorId
        ink_view.brushColor = colorId
        layoutline.setBackgroundColor(colorId)
    }

    private fun registerReceivers() {
        if (!registeredReceiver) {
            registerReceiver(receiver, filter)
            registeredReceiver = true
        }
    }

    private fun unregisterReceivers() {
        if (registeredReceiver) {
            unregisterReceiver(receiver)
            registeredReceiver = false
        }
    }

    override fun onBackPressed() {
        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        if (drawer.isDrawerOpen(android.support.v4.view.GravityCompat.START)) {
            drawer.closeDrawer(android.support.v4.view.GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        when (id) {
            R.id.action_settings -> {
                if (sliding_layout.panelState != SlidingUpPanelLayout.PanelState.EXPANDED) {
                    sliding_layout.panelState = SlidingUpPanelLayout.PanelState.EXPANDED
                } else {
                    sliding_layout.panelState = SlidingUpPanelLayout.PanelState.COLLAPSED
                }
                return true
            }
            R.id.action_scan -> {
                //TODO show loading indicator

                val builder: AlertDialog.Builder = AlertDialog.Builder(this@MainActivity)
                builder.setAdapter(scanAdapter, this@MainActivity)
                builder.create().show()

                startScan()

                return true
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onClick(p0: DialogInterface?, p1: Int) {
        connectToDevice(scanResultDevices[p1])
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        grantResults.forEach {
            if (it == android.content.pm.PackageManager.PERMISSION_DENIED) {
                android.support.v7.app.AlertDialog.Builder(this)
                        .setMessage(getString(R.string.permission_denied))
                        .setNeutralButton(android.R.string
                                .ok, null)
                        .show()
                return
            }
        }

        when (requestCode) {
            MainActivity.Companion.REQUEST_READ_EXTERNAL_STORAGE_SAVE -> {
                saveDrawings()
            }
            MainActivity.Companion.REQUEST_READ_EXTERNAL_STORAGE_SHARE -> {
                shareDrawings()
            }
            else -> {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults)
            }
        }
    }

    private fun shareDrawings() {
        val postfix = saveDrawings(MainActivity.Companion.REQUEST_READ_EXTERNAL_STORAGE_SHARE)

        val path: File = File(String.format("%s/Wand/wand_%s.jpg",
                Environment.getExternalStorageDirectory().path, postfix))
        val mime: android.webkit.MimeTypeMap = android.webkit.MimeTypeMap.getSingleton()
        val ext = path.name.substring(path.name.lastIndexOf(".") + 1)
        val sharingIntent = android.content.Intent(android.content.Intent.ACTION_SEND)
        sharingIntent.type = mime.getMimeTypeFromExtension(ext)
        sharingIntent.putExtra(android.content.Intent.EXTRA_STREAM, android.net.Uri.fromFile(path))
        sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, recognized_text.text);
        startActivity(android.content.Intent.createChooser(sharingIntent, getString(R.string.share_using)))
    }

    private fun saveDrawings(): String? {
        return saveDrawings(MainActivity.Companion.REQUEST_READ_EXTERNAL_STORAGE_SAVE)
    }

    private fun saveDrawings(requestCode: Int): String? {
        if (!PermissionHelper.isPermissionGranted(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), requestCode)
            }
            return null
        } else {
            val c: Calendar = Calendar.getInstance()

            val postfix: String = SimpleDateFormat("dd-MMM-yyyy hh:mm:ss aa").format(c.time) +
                    java.util.Random().nextInt()

            val startPath: File = File(String.format("%s/Wand/", Environment
                    .getExternalStorageDirectory().path))

            if (!startPath.exists()) {
                startPath.mkdirs()
            }
            val b: android.graphics.Bitmap = ink_view.getBitmap()

            val allpixels: IntArray = kotlin.IntArray(b.width * b.height)

            b.getPixels(allpixels, 0, b.width, 0, 0, b.width, b.height)

            for (i in 0..(allpixels.size - 1)) {
                if (allpixels[i] == android.graphics.Color.TRANSPARENT) {
                    allpixels[i] = android.graphics.Color.WHITE
                }
            }

            b.setPixels(allpixels, 0, b.width, 0, 0, b.width,
                    b.height)

            //b.eraseColor(Color.TRANSPARENT)
            b.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100,
                    FileOutputStream(String.format("%s/wand_%s.jpg", startPath.path, postfix)))

            val out = PrintWriter(String.format("%s/wand_%s.txt", startPath.path,
                    postfix))
            out.print(recognized_text.text)
            out.flush()

            Toast.makeText(applicationContext, getString(R.string.saved_to) + startPath.path +
                    "/wand_" + postfix, Toast.LENGTH_LONG).show()

            return postfix
        }
    }

    override fun onNavigationItemSelected(item: android.view.MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val id = item.itemId

        when (id) {
            R.id.nav_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out)
            }
            R.id.nav_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
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
            R.id.action_save -> {
                saveDrawings()
                return true
            }
            R.id.action_share -> {
                shareDrawings()
                return true
            }
            R.id.nav_sign_up -> {
                startActivity(Intent(this, SignupActivity::class.java))
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out)
            }
            R.id.nav_sensitivity->{
                val pop = SesitivityDialog()
                val fm = this@MainActivity.fragmentManager

                pop.show(fm, "Choose sensitivity")
            }
        }

        val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    public override fun onResume() {
        if (ink_view != null) {
            ink_view!!.cleanView(true)
        }

        WritePadFlagManager.initialize(this)

        super.onResume()

        //startScan()

        //Thread(Runnable { findWand() }).start()

        registerReceivers()
    }

    private fun startScan() {
        Log.i("ble", "startScan")
        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                leScanner = bluetoothAdapter!!.bluetoothLeScanner
                settings = ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build()
                filters = ArrayList<ScanFilter>()
            }
            scanLeDevice(true)
        }
    }

    public override fun onPause() {
        super.onPause()
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }

        unregisterReceivers()

        if (bluetoothAdapter != null && bluetoothAdapter!!.isEnabled) {
            scanLeDevice(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(connection)

        if (bluetoothGatt == null) {
            return
        }
        bluetoothGatt!!.close()
        bluetoothGatt = null

        if (recoInit) {
            WritePadManager.recoFree()
        }
        recoInit = false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_CANCELED) {
                //Bluetooth not enabled.
                finish()
                return
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun scanLeDevice(enable: Boolean) {
        if (enable) {
            scanHandler!!.postDelayed({
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                    bluetoothAdapter!!.stopLeScan(leScanCallback)
                } else {
                    leScanner?.stopScan(getScanCallback())
                }
            }, MainActivity.Companion.SCAN_PERIOD)
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                bluetoothAdapter!!.startLeScan(leScanCallback)
            } else {
                leScanner?.startScan(filters, settings, getScanCallback())
            }
        } else {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
                bluetoothAdapter!!.stopLeScan(leScanCallback)
            } else {
                leScanner?.stopScan(getScanCallback())
            }
        }
    }


    fun connectToDevice(device: BluetoothDevice) {
        if (bluetoothGatt == null) {
            bluetoothGatt = device.connectGatt(this, false, gattCallback)
            scanLeDevice(false)// will stop after first device detection
        }
    }

    private fun findWand() {
        if (connectedThread != null) {
            connectedThread!!.cancel()
            connectedThread = null
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter == null) {
            runOnUiThread {
                Toast.makeText(applicationContext, getString(R.string.no_bt_support_error), Toast.LENGTH_SHORT).show()
                setStatus(DeviceStatusView.Companion.ERROR)
            }

            return
        } else if (!(bluetoothAdapter as BluetoothAdapter).isEnabled) {
            val enableAdapter = android.content.Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableAdapter, 0)
        } else {
            val bondedDevices = (bluetoothAdapter as BluetoothAdapter).bondedDevices

            var device: BluetoothDevice? = null

            if (bondedDevices == null || bondedDevices.isEmpty()) {
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
                try {
                    val bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)

                    try {
                        bluetoothSocket!!.connect()
                        runOnUiThread {
                            //Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                            setStatus(DeviceStatusView.CONNECTED.toInt())
                        }
                    } catch (e: IOException) {
                        try {
                            bluetoothSocket!!.close()
                        } catch (e2: IOException) {
                            Log.e("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.message + ".")
                            runOnUiThread { setStatus(DeviceStatusView.ERROR) }
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

                    runOnUiThread { setStatus(DeviceStatusView.ERROR) }
                    return
                }
            }
        }
    }

    fun setStatus(status: Int) {
        runOnUiThread({
            when (status) {
                DeviceStatusView.Companion.DISCONNECTED -> {
                    supportActionBar?.setSubtitle(R.string.disconnected)
                    //iconView!!.setBackgroundResource(R.drawable.ic_disconnected)
                }
                DeviceStatusView.Companion.CONNECTING -> {
                    supportActionBar?.setSubtitle(R.string.connecting)
                    //iconView!!.setBackgroundResource(R.drawable.ic_connecting)
                }
                DeviceStatusView.Companion.CONNECTED -> {
                    supportActionBar?.setSubtitle(R.string.connected)
                    //iconView!!.setBackgroundResource(R.drawable.ic_connected)
                }
                DeviceStatusView.Companion.ERROR -> {
                    supportActionBar?.setSubtitle(R.string.error)
                    //iconView!!.setBackgroundResource(R.drawable.ic_disconnected)
                }
            }
        })
    }


    private fun setLoggedIn(menuLoggedIn: Boolean) {
        logoutMenuItem!!.isVisible = menuLoggedIn

        loginMenuItem!!.isVisible = !menuLoggedIn
        signupMenuItem!!.isVisible = !menuLoggedIn

        if (menuLoggedIn) {
            headerNameView!!.text = sharedPreferences!!.getString(ApiHelper.Companion.PREF_NAME, null)
            headerEmailView!!.text = sharedPreferences!!.getString(ApiHelper.Companion.PREF_EMAIL, null)
        } else {
            headerNameView!!.setText(R.string.no_account)
            headerEmailView!!.text = null
        }
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        when (key) {
            ApiHelper.PREF_TOKEN -> {
                setLoggedIn(sharedPreferences.getString(ApiHelper.Companion.PREF_TOKEN, null) != null)
            }
            PREF_INVERT_XY -> {
                invertXY = sharedPreferences.getBoolean(key, false)
            }
            PREF_REVERT_X -> {
                revertX = sharedPreferences.getBoolean(key, false)
            }
            PREF_REVERT_Y -> {
                revertY = sharedPreferences.getBoolean(key, false)
            }
        }
    }

    private inner class ConnectedThread internal constructor(private val mmSocket: BluetoothSocket) : Thread() {
        private val mmInStream: java.io.InputStream?
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

                        var bytes: ByteArray = byteArrayOf()

                        var i = 0
                        var j = 0
                        while (i < bufferArray.size) {
                            if (bufferArray[i] == 127.toByte()) {
                                if (bytes.size >= 3) {
                                    handler!!.obtainMessage(0, 0, 0, bytes).sendToTarget()
                                }
                                bytes = byteArrayOf()
                                j = 0
                                while (i >= 0) {
                                    bufferArray.removeAt(0)
                                    i--
                                }
                            } else {
                                bytes[j] = bufferArray[i]
                                j++
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
        public var sens_t = 3f
        private val SCAN_PERIOD: Long = 10000
        val BT_DEVICE_NAME = "MLT-BT05"
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        private val BLE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
        private val REQUEST_READ_EXTERNAL_STORAGE_SAVE: Int = 123
        private val REQUEST_READ_EXTERNAL_STORAGE_SHARE: Int = 124

        val PREF_INVERT_XY: String = "invert_x_y"
        val PREF_REVERT_X: String = "revert_x"
        val PREF_REVERT_Y: String = "revert_y"

    }
}