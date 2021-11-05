package com.linetec.socket_app;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MyActivity extends AppCompatActivity {

    private final String TAG = MyActivity.class.getSimpleName();

    private Socket socket;

    private static final int STATUS_INIT = 0;
    private static final int STATUS_SSID_CHECK = 1;
    private static final int STATUS_PASS_CHECK = 2;
    private static final int STATUS_OK = 3;

    private static final int SERVERPORT = 1001;
    private static final String SERVER_IP = "192.168.4.1";

    private static final String HEXES = "0123456789ABCDEF";

    private static final byte messageStart = 0x7E;
    private static final byte messageEnd = 0x7F;

    private int status = STATUS_SSID_CHECK;
    private int substate = 0;

    private ByteBuffer lastMessageSent = ByteBuffer.allocate(100);

    private ArrayList<String> listItems = new ArrayList<String>();
    private ArrayAdapter<String> adapter;

    WifiManager wifiManager;

    Handler updateConversationHandler;
    Thread clientThread;
    Thread commThread;

    private Button mSendButton;
    private Button mConnectButton;
    private Button mDisconnectButton;
    private Button mAPtoSTAButton;
    private Button mConfigAllButton;
    private Button mSendSSIDButton;
    private Button mSendPassButton;
    private Button mConnectToAPButton;
    private EditText mSendEditText;
    private EditText mSSIDEditText;
    private EditText mPassEditText;
    private ListView mReceiveListView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mSendButton = findViewById(R.id.send_button);
        mConnectButton = findViewById(R.id.connect_button);
        mDisconnectButton = findViewById(R.id.disconnect_button);
        mAPtoSTAButton = findViewById(R.id.ap_to_sta_button);
        mConfigAllButton = findViewById(R.id.cfg_all_button);
        mSendSSIDButton = findViewById(R.id.send_ssid_button);
        mSendPassButton = findViewById(R.id.send_pass_button);
        mConnectToAPButton = findViewById(R.id.connect_to_ap_button);
        mSendEditText = findViewById(R.id.send_to_socket_edittext);
        mSSIDEditText = findViewById(R.id.ssid_edittext);
        mPassEditText = findViewById(R.id.pass_edittext);
        mReceiveListView = findViewById(R.id.receive_list);

        wifiManager = (WifiManager) super.getSystemService(android.content.Context.WIFI_SERVICE);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
        mReceiveListView.setAdapter(adapter);

        updateConversationHandler = new Handler();

        mConnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clientThread = new Thread(new ClientThread());
                clientThread.start();
            }
        });

        mDisconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (socket != null) {
                        socket.close();
                    }
                    updateConversationHandler.post(new updateUIThread("Socket closed."));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        mConfigAllButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    status = STATUS_SSID_CHECK;
                    substate = 0;
                    sendSSID();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                /*Thread threadControl = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try  {
                            while(status!=STATUS_OK)
                            {
                                switch (status)
                                {
                                    case STATUS_SSID_CHECK:
                                        if (substate <= 0) {
                                            threadSSID.start();
                                            substate++;
                                        }
                                        break;

                                    case STATUS_PASS_CHECK:
                                        if (substate <= 0) {
                                            threadPASS.start();
                                            substate++;
                                        }
                                        break;
                                }
                            }

                            threadChangeMode.start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                try {
                    threadSSID.start();
                    threadSSID.join();
                    threadPASS.start();
                    threadPASS.join();
                    threadChangeMode.start();
                    threadChangeMode.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    Log.d(TAG, "THREAD: failed tasks.");
                }*/
            }
        });

        mSendSSIDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendSSID();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        mSendPassButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    sendPass();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        mAPtoSTAButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    changeMode();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /*try {
                    Thread thread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                OutputStream out = new DataOutputStream(socket.getOutputStream());

                                ByteBuffer charSendBuff = ByteBuffer.allocate(100);
                                charSendBuff.clear();
                                charSendBuff.put(mSendEditText.getText().toString().getBytes());
                                out.write(charSendBuff.array());
                                out.flush();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

                    thread.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }*/
            }
        });

        mConnectToAPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectToWifi("DrAromas", "123456789");
            }
        });

        // check GPS permission
        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // connect automatically to preferred network at start of activity
        mConnectToAPButton.callOnClick();
    }

    private void connectToWifi(final String networkSSID, final String networkPassword) {
        int netID = -1;
        String confSSID = String.format("\"%s\"", networkSSID);
        String confPassword = String.format("\"%s\"", networkPassword);

        netID = getExistingNetworkID(confSSID, netID);
        /*
         * If ssid not found in preconfigured list it will return -1
         * then add new wifi
         */
        if (netID == -1) {

            Log.d(TAG, "New wifi config added");

            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = confSSID;
            conf.preSharedKey = confPassword;
            netID = wifiManager.addNetwork(conf);

        }
        wifiManager.disconnect();
        wifiManager.enableNetwork(netID, true);
        wifiManager.reconnect();
    }

    private int getExistingNetworkID(String confSSID, int netID) {

        try {
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 101);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<WifiConfiguration> wifiConfigurationList = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration item : wifiConfigurationList){
        /*
          Find if the SSID is in the preconfigured list - if found get netID
         */
            if (item.SSID != null && item.SSID.equals(confSSID)){

                Log.d(TAG, "Pre-configured running");
                netID = item.networkId;
                break;
            }
        }
        return netID;
    }

    private void sendSSID() throws InterruptedException {
        Thread threadSSID = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    ByteBuffer byteSendBuff = ByteBuffer.allocate(100);
                    OutputStream out = new DataOutputStream(socket.getOutputStream());

                    // send SSID
                    byte[] pre = {0x00, 0x01, 0x00, 0x02, 0x02, 0x00, 0x00, 0x00, 0x00};
                    byte[] cmd = {0x00, 0x23};

                    byteSendBuff.clear();
                    byteSendBuff.put(messageStart);
                    byteSendBuff.put(pre);
                    byteSendBuff.put(cmd);
                    byteSendBuff.put(mSSIDEditText.getText().toString().getBytes());
                    byteSendBuff.put(getCRC(byteSendBuff.array(), pre.length + cmd.length + mSSIDEditText.getText().length()));
                    byteSendBuff.put(messageEnd);

                    lastMessageSent = byteSendBuff;

                    out.write(byteSendBuff.array());
                    out.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        threadSSID.start();
        threadSSID.join();
    }

    private void sendPass() throws InterruptedException {
        Thread threadPASS = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    ByteBuffer byteSendBuff = ByteBuffer.allocate(100);
                    OutputStream out = new DataOutputStream(socket.getOutputStream());

                    // send pass
                    byte[] pre = {0x00, 0x01, 0x00, 0x02, 0x02, 0x00, 0x00, 0x00, 0x00};
                    byte[] cmd = {0x00, 0x24};

                    byteSendBuff.clear();
                    byteSendBuff.put(messageStart);
                    byteSendBuff.put(pre);
                    byteSendBuff.put(cmd);
                    byteSendBuff.put(mPassEditText.getText().toString().getBytes());
                    byteSendBuff.put(getCRC(byteSendBuff.array(), pre.length + cmd.length + mPassEditText.getText().length()));
                    byteSendBuff.put(messageEnd);

                    lastMessageSent = byteSendBuff;

                    out.write(byteSendBuff.array());
                    out.flush();
                } catch (Exception ioException) {
                    ioException.printStackTrace();
                }
            }
        });

        threadPASS.start();
        threadPASS.join();
    }

    private void changeMode() throws InterruptedException {
        Thread threadChangeMode = new Thread(new Runnable() {
            @Override
            public void run() {
                try  {
                    ByteBuffer byteSendBuff = ByteBuffer.allocate(100);
                    OutputStream out = new DataOutputStream(socket.getOutputStream());

                    // change AP to STA
                    byte[] pre = {0x00, 0x01, 0x00, 0x02, 0x02, 0x00, 0x00, 0x00, 0x00};
                    byte[] cmd = {0x00, 0x28, 0x01};

                    byteSendBuff.clear();
                    byteSendBuff.put(messageStart);
                    byteSendBuff.put(pre);
                    byteSendBuff.put(cmd);
                    byteSendBuff.put(getCRC(byteSendBuff.array(), pre.length + cmd.length));
                    byteSendBuff.put(messageEnd);

                    lastMessageSent = byteSendBuff;

                    out.write(byteSendBuff.array());
                    out.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        threadChangeMode.start();
        threadChangeMode.join();
    }

    class ClientThread implements Runnable {

        @Override
        public void run() {
 
            try {
                InetAddress serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket(serverAddr, SERVERPORT);

                while(!socket.isConnected()) {

                }
                updateConversationHandler.post(new updateUIThread("Socket connected."));

                // create receive thread
                commThread = new Thread(new CommunicationThread(socket));
                commThread.start();

            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    class CommunicationThread implements Runnable {

        private InputStream inputStream;

        public CommunicationThread(Socket clientSocket) {
            try {
                inputStream = clientSocket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run() {

            while (!Thread.currentThread().isInterrupted() && Thread.currentThread().isAlive() && socket.isConnected())
            {
                try {
                    byte[] byte_buf = new byte[100];

                    int bytesRead = inputStream.read(byte_buf);

                    if(bytesRead < 0) {
                        socket.close();
                        inputStream.close();
                    } else if(bytesRead == 0) {
                        updateConversationHandler.post(new updateUIThread("No bytes read."));
                    } else {
                        Log.d("RESPONSE RECEIVED!: ", getHex(byte_buf));

                        updateConversationHandler.post(new updateUIThread(getHex(byte_buf)));

                        switch (status) {
                            case STATUS_SSID_CHECK:
                                if(Arrays.equals(byte_buf, lastMessageSent.array())) {
                                    updateConversationHandler.post(new updateUIThread("There is a match! SSID CHECK"));
                                    status = STATUS_PASS_CHECK;
                                    substate = 0;

                                    sendPass();
                                } else {
                                    updateConversationHandler.post(new updateUIThread("Not a match!"));
                                }
                                break;
                            case STATUS_PASS_CHECK:
                                if(Arrays.equals(byte_buf, lastMessageSent.array())) {
                                    updateConversationHandler.post(new updateUIThread("There is a match! PASS CHECK - ALL CHECK"));
                                    status = STATUS_OK;
                                    substate = 0;

                                    changeMode();
                                } else {
                                    updateConversationHandler.post(new updateUIThread("Not a match!"));
                                }
                                break;
                            default:
                                updateConversationHandler.post(new updateUIThread("I don't know what to do."));
                                break;
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    class updateUIThread implements Runnable {
        private String msg;

        public updateUIThread(String str) {
            SimpleDateFormat s = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
            this.msg = s.format(new Date()) + " || " + str;
        }

        @Override
        public void run() {
            listItems.add(msg);
            adapter.notifyDataSetChanged(); // next thing you have to do is check if your adapter has changed
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static byte[] getCRC(byte[] payload, int len) {
        byte[] calculatedCRC = new byte[2];

        char crc = 0; // 16 bits
        byte Aux; // 8 bits
        int index = 1;

        while(len > 0) {
            len--;
            Aux = (byte) ((crc ^ payload[index]) & 0x000000FF);
            index++;
            crc = (char) ((crc >> 8) ^ CRCUtils.CRCtbl[Byte.toUnsignedInt(Aux)]);
        }

        calculatedCRC[0] = (byte) ((crc >> 8) & 0xFF);
        calculatedCRC[1] = (byte) (crc & 0xFF);

        return calculatedCRC;
    }

    public static String getHex(byte [] raw) {
        if (raw == null) {
            return null;
        }

        final StringBuilder hex = new StringBuilder(2 * raw.length);
        for (final byte b : raw) {
            hex.append(HEXES.charAt((b & 0xF0) >> 4)).append(HEXES.charAt((b & 0x0F)));
            hex.append(' ');
        }
        return hex.toString();
    }
}


