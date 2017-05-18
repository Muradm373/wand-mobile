package io_developers.sssemil.com.wand;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.GridView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.phatware.android.WritePadFlagManager;
import com.phatware.android.WritePadManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import io_developers.sssemil.com.wand.Account.ApiHelper;
import io_developers.sssemil.com.wand.Account.LoginActivity;
import io_developers.sssemil.com.wand.Account.SignupActivity;

import static io_developers.sssemil.com.wand.Account.ApiHelper.PREF_EMAIL;
import static io_developers.sssemil.com.wand.Account.ApiHelper.PREF_NAME;
import static io_developers.sssemil.com.wand.Account.ApiHelper.PREF_TOKEN;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String BT_DEVICE_NAME = "SPP-CA";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    RecognizerService mBoundService;
    InkView mInkView;
    TextView mRecognizedText;
    DeviceStatusView mDeviceStatusView;
    private Handler mHandler;
    private BluetoothSocket mBluetoothSocket;
    private ConnectedThread mConnectedThread;
    private ArrayList<Byte> mBufferArray = new ArrayList<>();
    private int mPrevX;
    private int mPrevY;
    private ServiceConnection mConnection;
    private boolean mRecoInit;

    private MenuItem mLogoutMenuItem;
    private MenuItem mSignupMenuItem;
    private MenuItem mLoginMenuItem;

    private SharedPreferences mSharedPreferences;
    private TextView mHeaderEmailView;
    private TextView mHeaderNameView;
    private SlidingUpPanelLayout mSlidingLayout;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mInkView = (InkView) findViewById(R.id.ink_view);
        mRecognizedText = (TextView) findViewById(R.id.recognized_text);
        mDeviceStatusView = (DeviceStatusView) findViewById(R.id.deviceState);

        mSlidingLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        mHeaderEmailView = (TextView) navigationView.getHeaderView(0).findViewById(R.id.userid);
        mHeaderNameView = (TextView) navigationView.getHeaderView(0).findViewById(R.id.username);

        Menu navMenu = navigationView.getMenu();

        mLogoutMenuItem = navMenu.findItem(R.id.nav_logout);

        mLoginMenuItem = navMenu.findItem(R.id.nav_login);
        mSignupMenuItem = navMenu.findItem(R.id.nav_sign_up);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        setLoggedIn(mSharedPreferences.getString(PREF_TOKEN, null) != null);

        // initialize ink inkView class
        String lName = WritePadManager.getLanguageName();
        WritePadManager.setLanguage(lName, this);

        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        defaultDisplay.getSize(size);
        int screenHeight = size.y;
        int textViewHeight = 15 * screenHeight / 100;


        int colorList[] = new int[]{
                getResources().getColor(R.color.green),
                getResources().getColor(R.color.light_green),
                getResources().getColor(R.color.lime),
                getResources().getColor(R.color.yellow),
                getResources().getColor(R.color.orange),
                getResources().getColor(R.color.red),
                getResources().getColor(R.color.pink),
                getResources().getColor(R.color.cyan),
                getResources().getColor(R.color.blue),
                getResources().getColor(R.color.purple),
                getResources().getColor(R.color.indigo),
                getResources().getColor(R.color.black)
        };


        GridView gridView = (GridView) findViewById(R.id.gridView);

        CustomAdapter ca = new CustomAdapter(this, colorList, new CustomAdapter.OnColorClick() {
            @Override
            public void onColorClick(int colorId) {
                mInkView.brushColor = colorId;
                RelativeLayout rl = (RelativeLayout) findViewById(R.id.layoutline);
                rl.setBackgroundColor(colorId);

            }
        });

        gridView.setAdapter(ca);

        mInkView.setRecognizedTextContainer(mRecognizedText);

        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                // This is called when the connection with the service has been
                // established, giving us the service object we can use to
                // interact with the service.  Because we have bound to a explicit
                // service that we know is running in our own process, we can
                // cast its IBinder to a concrete class and directly access it.
                mBoundService = ((RecognizerService.RecognizerBinder) service).getService();
                mBoundService.mHandler = mInkView.getHandler();
            }

            public void onServiceDisconnected(ComponentName className) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                // Because it is running in our same process, we should never
                // see this happen.
                mBoundService = null;
            }
        };

        bindService(new Intent(this, RecognizerService.class), mConnection, android.content.Context.BIND_AUTO_CREATE);

        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                String sbprint = (String) msg.obj;

                Log.d("BT1", sbprint);

                String[] spl = sbprint.split(";");
                if (spl.length == 4) {
                    spl = Arrays.copyOfRange(spl, 1, 4);
                }
                if (spl.length >= 2 || spl.length == 3) {
                    try {
                        int x = -Integer.valueOf(spl[0]);
                        int y = Integer.valueOf(spl[1]);

                        int state = 1;

                        if (spl.length >= 3) {
                            state = Integer.valueOf(spl[2]);
                        }

                        if (x != mPrevX || y != mPrevY) {
                            if (mInkView != null) {
                                /*if (state == 1) {
                                    mInkView.addLine(x, y, false);
                                } else if (state == 2) {
                                    //mInkView.touch_up();
                                    mInkView.addLine(x, y, true);
                                }*/

                                mInkView.onWandEvent(x, y, state == 1);
                            }
                            mPrevX = x;
                            mPrevY = y;
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        };

        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void registerReceivers() {
        if (mDeviceStatusView != null) {
            mDeviceStatusView.registerReceiver();
        }
    }

    private void unregisterReceivers() {
        if (mDeviceStatusView != null) {
            mDeviceStatusView.unregisterReceiver();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            if (!mSlidingLayout.getPanelState().equals(SlidingUpPanelLayout.PanelState.EXPANDED)) {
                mSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            } else {
                mSlidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch (id) {
            case R.id.nav_about:
                startActivity(new Intent(this, AboutActivity.class));
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                break;
            case R.id.nav_license:
                startActivity(new Intent(this, LicenseActivity.class));
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                break;
            case R.id.nav_login:
                startActivity(new Intent(this, LoginActivity.class));
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                break;
            case R.id.nav_sign_up:
                startActivity(new Intent(this, SignupActivity.class));
                overridePendingTransition(R.anim.push_left_in, R.anim.push_left_out);
                break;
            case R.id.nav_logout:
                ApiHelper.logOut(mSharedPreferences);
                break;
            default:
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onResume() {
        if (mInkView != null) {
            mInkView.cleanView(true);
        }

        WritePadFlagManager.initialize(this);

        super.onResume();

        new Thread(new Runnable() {
            @Override
            public void run() {
                findWand();
            }
        }).start();

        registerReceivers();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        unregisterReceivers();
    }

    @Override
    protected void onDestroy() {
        unbindService(mConnection);
        super.onDestroy();
        if (mRecoInit) {
            WritePadManager.recoFree();
        }
        mRecoInit = false;
    }

    private void findWand() {
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Device doesn't Support Bluetooth :(", Toast.LENGTH_SHORT).show();
                    mDeviceStatusView.setStatus(DeviceStatusView.ERROR);
                }
            });

            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableAdapter, 0);
        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        BluetoothDevice device = null;

        if (bondedDevices.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getString(R.string.pair_request), Toast.LENGTH_SHORT).show();
                    //mDeviceStatusView.setStatus(DeviceStatusView.DISCONNECTED);
                }
            });
        } else {
            for (BluetoothDevice iterator : bondedDevices) {
                Log.i("name", iterator.getName());
                if (iterator.getName().equals(BT_DEVICE_NAME)) {
                    device = iterator; //device is an object of type BluetoothDevice
                    break;
                }
            }
        }

        if (device != null) {
            /*runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //mDeviceStatusView.setStatus(DeviceStatusView.CONNECTING);
                }
            });*/
            try {
                mBluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDeviceStatusView.setStatus(DeviceStatusView.ERROR);
                    }
                });
                return;
            }

            try {
                mBluetoothSocket.connect();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                        mDeviceStatusView.setStatus(DeviceStatusView.CONNECTED);
                    }
                });
            } catch (IOException e) {
                try {
                    mBluetoothSocket.close();
                } catch (IOException e2) {
                    Log.e("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mDeviceStatusView.setStatus(DeviceStatusView.ERROR);
                        }
                    });
                    return;
                }
            }

            if (mConnectedThread != null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            mConnectedThread = new ConnectedThread(mBluetoothSocket);
            mConnectedThread.start();
        }
    }

    private void setLoggedIn(boolean menuLoggedIn) {
        mLogoutMenuItem.setVisible(menuLoggedIn);

        mLoginMenuItem.setVisible(!menuLoggedIn);
        mSignupMenuItem.setVisible(!menuLoggedIn);

        if (menuLoggedIn) {
            mHeaderNameView.setText(mSharedPreferences.getString(PREF_NAME, null));
            mHeaderEmailView.setText(mSharedPreferences.getString(PREF_EMAIL, null));
        } else {
            mHeaderNameView.setText(R.string.no_account);
            mHeaderEmailView.setText(null);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PREF_TOKEN)) {
            setLoggedIn(sharedPreferences.getString(PREF_TOKEN, null) != null);
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private byte[] mmBuffer;
        private boolean run = true;

        ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error Input Stream", Toast.LENGTH_SHORT).show();
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error Output Stream", Toast.LENGTH_SHORT).show();
            }

            mmInStream = tmpIn;
        }

        public void run() {
            mmBuffer = new byte[1024];  // buffer store for the stream
            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (run) {
                try {
                    // Read from the InputStream
                    if (mmInStream == null) break;

                    numBytes = mmInStream.read(mmBuffer);
                    if (numBytes > 0) {
                        for (int i = 0; i < numBytes; i++) {
                            mBufferArray.add(mmBuffer[i]);
                        }
                        // если встречаем конец строки,
                        StringBuilder sbprint = new StringBuilder();
                        for (int i = 0; i < mBufferArray.size(); i++) {
                            if (mBufferArray.get(i) == 127) {
                                if (sbprint.length() > 0) {
                                    mHandler.obtainMessage(0, 0, 0, sbprint.toString()).sendToTarget();
                                }
                                sbprint = new StringBuilder();
                                for (int j = 0; j <= i; ) {
                                    mBufferArray.remove(j);
                                    i--;
                                }
                            } else {
                                sbprint.append(mBufferArray.get(i)).append(";");
                            }
                        }
                    }
                } catch (Exception e) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    findWand();
                    break;
                }
            }

            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /* Call this from the main activity to shutdown the connection */
        void cancel() {
            run = false;
        }
    }
}
