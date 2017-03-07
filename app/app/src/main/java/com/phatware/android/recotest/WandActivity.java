package com.phatware.android.recotest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.*;
import android.widget.*;
import com.phatware.android.CustomAdapter;
import com.phatware.android.WritePadFlagManager;
import com.phatware.android.WritePadManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static com.phatware.android.RecoInterface.WritePadAPI.TAG;


public class WandActivity extends Activity {

    private static float storedX = 0;
    private static float storedY = 0;

    private static final int SENSOR_DELAY = 33;//ms

    private ConnectedThread mConnectedThread;
    Handler mHandler;
    private ArrayList<Byte> sb = new ArrayList<>();
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int CLEAR_MENU_ID = Menu.FIRST + 1;
    private static final int SETTINGS_MENU_ID = Menu.FIRST + 2;
    public boolean state = false;
    public BluetoothDevice device;
    public BluetoothSocket socket;
    public RecognizerService mBoundService;
    EditText textView;
    private boolean mRecoInit;
    private InkView inkView;
    private Integer brushColor;
    private String textFilePath = "/sdcard/Documents";
    private String picFilePath = "/sdcard/Pictures";
    private boolean flag = false;
    private BluetoothAdapter bluetoothAdapter;
    private OutputStream outStream;
    private InputStream inStream;
    private boolean visib = true;
    private ServiceConnection mConnection;

    private long mPrevTouch = 0;
    private boolean mPrevWasDown = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ArrayList<Integer> colorList = new ArrayList<>();
        {
            colorList.add(getResources().getColor(R.color.green));
            colorList.add(getResources().getColor(R.color.light_green));
            colorList.add(getResources().getColor(R.color.lime));
            colorList.add(getResources().getColor(R.color.yellow));
            colorList.add(getResources().getColor(R.color.orange));
            colorList.add(getResources().getColor(R.color.red));

            colorList.add(getResources().getColor(R.color.pink));
            colorList.add(getResources().getColor(R.color.cyan));
            colorList.add(getResources().getColor(R.color.blue));
            colorList.add(getResources().getColor(R.color.purple));
            colorList.add(getResources().getColor(R.color.indigo));
            colorList.add(getResources().getColor(R.color.black));
        }


        GridView gridView = (GridView) findViewById(R.id.gridView);

        CustomAdapter ca = new CustomAdapter(this, colorList);
        gridView.setAdapter(ca);


        String lName = WritePadManager.getLanguageName();
        WritePadManager.setLanguage(lName, this);

        // initialize ink inkView class
        inkView = (InkView) findViewById(R.id.ink_view);
        textView = (EditText) findViewById(R.id.textView);


        Display defaultDisplay = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        defaultDisplay.getSize(size);
        int screenHeight = size.y;
        int textViewHeight = 15 * screenHeight / 100;
        final LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, textViewHeight);

        inkView.setRecognizedTextContainer(textView);

        mConnection = new ServiceConnection() {
            public void onServiceConnected(ComponentName className, IBinder service) {
                // This is called when the connection with the service has been
                // established, giving us the service object we can use to
                // interact with the service.  Because we have bound to a explicit
                // service that we know is running in our own process, we can
                // cast its IBinder to a concrete class and directly access it.
                mBoundService = ((RecognizerService.RecognizerBinder) service).getService();
                mBoundService.mHandler = inkView.getHandler();
            }

            public void onServiceDisconnected(ComponentName className) {
                // This is called when the connection with the service has been
                // unexpectedly disconnected -- that is, its process crashed.
                // Because it is running in our same process, we should never
                // see this happen.
                mBoundService = null;
            }
        };

        bindService(new Intent(WandActivity.this,
                RecognizerService.class), mConnection, Context.BIND_AUTO_CREATE);

        mHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                String sbprint = (String) msg.obj;

                Log.d("BT1", sbprint);

                String[] spl = sbprint.split(";");
                if (spl.length == 2) {
                    try {
                        int x = -Integer.valueOf(spl[0]);
                        int y = Integer.valueOf(spl[1]);

                        if(x!=1 && y!=1) {
                            storedX -= (float) (x / 10.0);
                            storedY -= (float) (y / 10.0);

                            if (inkView != null) {
                                inkView.addLine(storedX, storedY);
                            }
                        }
                        /*long downTime;
                        long now = System.currentTimeMillis();

                        if (mPrevTouch == 0) mPrevTouch = now;

                        downTime = now - mPrevTouch;

                        int action = MotionEvent.ACTION_MOVE;

                        //TODO work on this part
                        if (downTime > SENSOR_DELAY * 2) {
                            if(mPrevWasDown) {
                                action = MotionEvent.ACTION_UP;
                            } else {
                                action = MotionEvent.ACTION_DOWN;
                            }

                            mPrevWasDown = !mPrevWasDown;
                        }

                        inkView.onTouchEvent(MotionEvent.obtain(downTime, now, action, x / 1f, y / 1f, 0));

                        mPrevTouch = now;*/
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        };
    }

    private void errorExit(String title, String message) {
        Toast.makeText(getBaseContext(), title + " - " + message, Toast.LENGTH_LONG).show();
        finish();
    }

    public void findWand(View v) {
        BluetoothDevice device = null;

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Device doesn't Support Bluetooth :(", Toast.LENGTH_SHORT).show();
                }
            });
        }
        if (!bluetoothAdapter.isEnabled()) {

            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            startActivityForResult(enableAdapter, 0);

        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        if (bondedDevices.isEmpty()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Please Pair the Device first", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            for (BluetoothDevice iterator : bondedDevices) {

                Log.i("name", iterator.getName());
                if (iterator.getName().equals("SPP-CA")) {
                    device = iterator; //device is an object of type BluetoothDevice
                    break;
                }
            }
        }

        if (device != null) {
            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                errorExit("Fatal Error", "In onResume() and socket create failed: " + e.getMessage() + ".");
            }

            try {
                socket.connect();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                try {
                    socket.close();
                } catch (IOException e2) {
                    errorExit("Fatal Error", "In onResume() and unable to close socket during connection failure" + e2.getMessage() + ".");
                }
            }

            if(mConnectedThread!=null) {
                mConnectedThread.cancel();
                mConnectedThread = null;
            }

            mConnectedThread = new ConnectedThread(socket);
            mConnectedThread.start();
        }
    }

    @Override
    public void onResume() {
        if (inkView != null) {
            inkView.cleanView(true);
        }

        WritePadFlagManager.initialize(this);

        super.onResume();

        findWand(null);
    }

    public void setTextFilePath(View v) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        Uri startDir = Uri.fromFile(new File("/sdcard"));
        intent.setType("folder/*");
        startActivityForResult(intent, 105);
    }

    public void setPictureFilePath(View v) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        Uri startDir = Uri.fromFile(new File("/sdcard"));
        intent.setType("file/*");
        startActivityForResult(intent, 106);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 105) {
            if (resultCode == RESULT_OK) {
                textFilePath = data.getDataString();
                TextView textFileView = (TextView) findViewById(R.id.textPathTextView);
                textFileView.setText(textFilePath);
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
        if (requestCode == 106) {
            if (resultCode == RESULT_OK) {
                picFilePath = data.getDataString();
                TextView picFileView = (TextView) findViewById(R.id.picturePathTextView);
                picFileView.setText(picFilePath);
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    public void setBrushColor(int brushCol) {
        brushColor = brushCol;
        inkView.brushColor = brushCol;
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.layoutline);
        RelativeLayout rl2 = (RelativeLayout) findViewById(R.id.headerLayout);
        rl.setBackgroundColor(brushCol);
        rl2.setBackgroundColor(brushCol);
    }

    public void swapToText(View v) {
        ImageView inkText = (ImageView) findViewById(R.id.imageView);
        ImageView textText = (ImageView) findViewById(R.id.imageView2);
        textView = (EditText) findViewById(R.id.textView);

        if (visib) {
            inkView.setVisibility(View.INVISIBLE);
            inkText.setVisibility(View.INVISIBLE);
            textText.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            visib = false;
        } else {
            inkView.setVisibility(View.VISIBLE);
            inkText.setVisibility(View.VISIBLE);
            textText.setVisibility(View.INVISIBLE);
            textView.setVisibility(View.INVISIBLE);
            visib = true;
        }
    }

    public void shareImage(View v) {
        Intent share = new Intent();
        share.setAction(Intent.ACTION_SEND);
        share.putExtra(Intent.EXTRA_TEXT, "Wand: \n" + textView.getText());
        share.setType("text/plain");

        startActivity(share);
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

    public void onSettingsClick(View v) {
        SlidingUpPanelLayout slidingLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        if (!state) {
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            state = true;
        } else {
            state = false;
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
    }

    public void getContentText(View v) {

        //btn1.setText(wordArray.);
    }


    public interface OnInkViewListener {
        void cleanView(boolean emptyAll);

        Handler getHandler();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private byte[] mmBuffer;
        private boolean run = true;

        public ConnectedThread(BluetoothSocket socket) {
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
                    if(mmInStream==null) break;

                    numBytes = mmInStream.read(mmBuffer);
                    if(numBytes>0) {
                        for (int i = 0; i < numBytes; i++) {
                            sb.add(mmBuffer[i]);
                        }
                        // если встречаем конец строки,
                        String sbprint = "";
                        for(int i = 0; i<sb.size();i++){
                            if(sb.get(i)==127) {
                                if(sbprint.length()>0) {
                                    mHandler.obtainMessage(0, 0, 0, sbprint).sendToTarget();
                                }
                                sbprint = "";
                                for(int j = 0; j<=i;){
                                    sb.remove(j);
                                    i--;
                                }
                            } else {
                                sbprint+= sb.get(i)+";";
                            }
                        }

                        /*int endOfLineIndex = -1;

                        for(int i = 0;i<sb.size();i++){
                            if(sb.get(i)==126) {
                                endOfLineIndex = i;
                                break;
                            }
                        }

                        if (endOfLineIndex >= 0) {                                           // если встречаем конец строки,
                            String sbprint = "";

                            for (int i = 0; i < endOfLineIndex; ) {
                                sbprint+= sb.get(i);
                                sb.remove(i);
                                endOfLineIndex--;
                            }

                            if(sbprint.length()>0) {
                                mHandler.obtainMessage(0, 0, 0, sbprint).sendToTarget();
                            }
                        }*/
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    findWand(null);
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
        public void cancel() {
            run = false;
        }
    }
}
