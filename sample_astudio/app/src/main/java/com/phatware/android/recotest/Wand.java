
package com.phatware.android.recotest;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Display;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.graphics.Point;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.phatware.android.CustomAdapter;
import com.phatware.android.WritePadFlagManager;
import com.phatware.android.WritePadManager;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;
import com.phatware.android.recotest.InkView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class Wand extends Activity {
    private boolean mRecoInit;
    public boolean state = false;
    private InkView inkView;
    EditText textView;
    private Integer brushColor;
    private String textFilePath = "/sdcard/Documents";
    private String picFilePath = "/sdcard/Pictures";
    public BluetoothDevice device;
    private boolean flag = false;
    public BluetoothSocket socket;
    private BluetoothAdapter bluetoothAdapter;
    private OutputStream outStream;
    private InputStream inStream;
    private boolean visib = true;
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public RecognizerService mBoundService;

    private ServiceConnection mConnection;

    @Override
    protected void onResume() {
        if (inkView != null) {
            inkView.cleanView(true);
        }

        WritePadFlagManager.initialize(this);
        super.onResume();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ArrayList<Integer> colorList = new ArrayList<Integer>();
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

        bindService(new Intent(Wand.this,
                RecognizerService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    public void setTextFilePath(View v){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        Uri startDir = Uri.fromFile(new File("/sdcard"));
        intent.setType("folder/*");
        startActivityForResult(intent, 105);
    }

    public void setPictureFilePath(View v){
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_GET_CONTENT);
        Uri startDir = Uri.fromFile(new File("/sdcard"));
        intent.setType("file/*");
        startActivityForResult(intent, 106);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == 105){
            if(resultCode == RESULT_OK) {
                textFilePath = data.getDataString();
                TextView textFileView = (TextView) findViewById(R.id.textPathTextView);
                textFileView.setText(textFilePath);
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
        if(requestCode == 106){
            if(resultCode == RESULT_OK) {
                picFilePath = data.getDataString();
                TextView picFileView = (TextView) findViewById(R.id.picturePathTextView);
                picFileView.setText(picFilePath);
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    public void setBrushColor(int brushCol){
        brushColor = brushCol;
        inkView.brushColor = brushCol;
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.layoutline);
        RelativeLayout rl2 = (RelativeLayout) findViewById(R.id.headerLayout);
        rl.setBackgroundColor(brushCol);
        rl2.setBackgroundColor(brushCol);
    }

    public void swapToText(View v){
        ImageView inkText = (ImageView) findViewById(R.id.imageView);
        ImageView textText = (ImageView) findViewById(R.id.imageView2);
        textView = (EditText) findViewById(R.id.textView);

        if(visib == true) {
            inkView.setVisibility(View.INVISIBLE);
            inkText.setVisibility(View.INVISIBLE);
            textText.setVisibility(View.VISIBLE);
            textView.setVisibility(View.VISIBLE);
            visib = false;
        }else{
            inkView.setVisibility(View.VISIBLE);
            inkText.setVisibility(View.VISIBLE);
            textText.setVisibility(View.INVISIBLE);
            textView.setVisibility(View.INVISIBLE);
            visib = true;
        }
    }


    public void shareImage(View v){

        Intent share = new Intent();
        share.setAction(Intent.ACTION_SEND);
        share.putExtra(Intent.EXTRA_TEXT, "Wand: \n"+textView.getText());
        share.setType("text/plain");

        startActivity(share);
    }

    public void findWand(View v){
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {

            Toast.makeText(getApplicationContext(),"Device doesnt Support Bluetooth :(",Toast.LENGTH_SHORT).show();
        }
        if(!bluetoothAdapter.isEnabled())

        {

            Intent enableAdapter = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            startActivityForResult(enableAdapter, 0);

        }

        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();

        if(bondedDevices.isEmpty()) {

            Toast.makeText(getApplicationContext(),"Please Pair the Device first",Toast.LENGTH_SHORT).show();

        } else {

            for (BluetoothDevice iterator : bondedDevices) {

                if(iterator.getName().equals("HC_05")) //Replace with iterator.getName() if comparing Device names.

                {

                    device = iterator; //device is an object of type BluetoothDevice
                    flag = true;
                    break;

                } }
        }

        if(flag == true){
            try {
                socket = device.createRfcommSocketToServiceRecord(MY_UUID);
            }catch (IOException e){
                Toast.makeText(this, "Socket error", Toast.LENGTH_LONG).show();
            }
            try {
                socket.connect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


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

        if (state == false) {
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            state = true;
        } else {
            state = false;
            slidingLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        }
    }

    public void getContentText(View v){

        //btn1.setText(wordArray.);
    }
    private static final int CLEAR_MENU_ID = Menu.FIRST + 1;
    private static final int SETTINGS_MENU_ID = Menu.FIRST + 2;



    public interface OnInkViewListener {
        void cleanView(boolean emptyAll);
        Handler getHandler();

    }
}
