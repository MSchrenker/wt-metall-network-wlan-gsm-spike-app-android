package eu.esys.networkbindtwosocketsspike;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.List;

import static java.lang.Thread.sleep;

// Test app, to find a way to work with a internetless wifi network and GSM/Internet via another network
// Starting point for research here: Most promising way is at the end. Requires Android Lollipop or Marshmallow (5.x bzw 6.x)
// https://android-developers.googleblog.com/2016/07/connecting-your-app-to-wi-fi-device.html
// Samples for NetworkRequest Builder setups:
// https://www.programcreek.com/java-api-examples/index.php?api=android.net.NetworkRequest
// Das aktuelle Problem ist wie ich an das Network-Handle/Id für das Network ohne Internet (wt-metall-node1) komme und
// das Handle für das "aktuelle" Mobile Network
// https://developer.android.com/reference/android/net/Network#openConnection(java.net.URL)
// Ich vermute dass das wirklich erst ab Android 28 und/oder höher geht (8.1)
public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button socket1 = findViewById(R.id.button_bind_to_camera_socket);
        socket1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                ListenToTCPCameraSocket listenToTCPCameraSocketTask = new ListenToTCPCameraSocket();
//                listenToTCPCameraSocketTask.execute("wt-metall-node1", "123456789");

                new OpenConnectionHttpToCamera().execute("wt-metall-node1", "123456789");
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        List<WifiConfiguration> wifiNetworks = wifiManager.getConfiguredNetworks();
        if (wifiNetworks != null && !wifiNetworks.isEmpty()) {
            for (WifiConfiguration knownConfiguration : wifiNetworks) {
                Log.v(TAG, knownConfiguration.toString());
//                if (knownConfiguration.SSID != null && knownConfiguration.SSID.equals("\"" + networkSSID + "\"")) {
//                    Log.e(TAG, "Call WifiManager.disconnect in connectToLastKnownWifi()");
//                    if (getConnectedNetwork(appContext) != null) {
//                        wifiManager.disconnect();
//                    }
//                    wifiManager.enableNetwork(knownConfiguration.networkId, true);
//                    ssidCurrentlyTryingToConnectTo = networkSSID;
//                    wifiManager.reconnect();
//                    break;
//                }
            }
        } else {
            Log.e(TAG, "WLAN ist aus. Keine Provider gefunden. Connect to Last Known Wifi nicht möglich. ToDo: Infodialog an den User scheint sinnvoll zu sein.");
        }
        Log.v(TAG, "[Leave] connectToLastKnownWifi");
    }


    class OpenConnectionHttpToCamera extends AsyncTask<String, Integer, Long> {

        @Override
        protected Long doInBackground(String... strings) {

            WifiConfiguration wtMetalNetworkConfiguration = null;

            WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            List<WifiConfiguration> wifiNetworks = wifiManager.getConfiguredNetworks();
            if (wifiNetworks != null && !wifiNetworks.isEmpty()) {
                for (WifiConfiguration knownConfiguration : wifiNetworks) {
                    Log.v(TAG, knownConfiguration.toString());

                    String networkSSID = "wt-metall-node1";
                    if (knownConfiguration.SSID != null && knownConfiguration.SSID.equals("\"" + networkSSID + "\"")) {
                        Log.v(TAG, "wt-metall-node1 Network found!");
                        wtMetalNetworkConfiguration = knownConfiguration;
                        break;
                    }
                }
            }

            // Now trying to get the wt-metall Network instance somehow, so that we can do something with the network (like network.bindSocket or network.openConnection)

            // create a "template" specifing a network we like to use
            NetworkRequest.Builder networkRequestBuilder = new NetworkRequest.Builder();
//            networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
            networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR);
//            networkRequestBuilder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI_AWARE);
            // available ab Android 28:
            // networkRequestBuilder.addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_ROAMING);

            NetworkRequest wtMetallNetWorkRequest = networkRequestBuilder.build();

            // register our network request and a callback in case such a network is available:
            ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
            connectivityManager.requestNetwork(wtMetallNetWorkRequest, new MyNetworkCallback());

            // now waiting for callback to fire...
            int count = 0;
            while (count < Long.MAX_VALUE) {
                try {
                    sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                count++;
            }
            return 0L;
        }
    }


    class MyNetworkCallback extends ConnectivityManager.NetworkCallback {
        private final String TAG = MyNetworkCallback.class.getSimpleName();

        @Override
        public void onAvailable(Network network) {
            super.onAvailable(network);
            Log.v(TAG, "onAvailable" + network.toString());
        }

        @Override
        public void onLosing(Network network, int maxMsToLive) {
            super.onLosing(network, maxMsToLive);
            Log.v(TAG, "onLosing");
        }

        @Override
        public void onLost(Network network) {
            super.onLost(network);
            Log.v(TAG, "onLost");
        }

        @Override
        public void onUnavailable() {
            super.onUnavailable();
            Log.v(TAG, "onUnavailable");
        }

        @Override
        public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
            super.onCapabilitiesChanged(network, networkCapabilities);
            Log.v(TAG, "onCapabilitiesChanged");
        }

        @Override
        public void onLinkPropertiesChanged(Network network, LinkProperties linkProperties) {
            super.onLinkPropertiesChanged(network, linkProperties);
            Log.v(TAG, "onLinkPropertiesChanged");
        }
    }


    // Does not work: Connection refused
    class ListenToTCPCameraSocket extends AsyncTask<String, Integer, Long> {

        @Override
        protected Long doInBackground(String... strings) {
            workOnSocket("wt-metall-node1", "123456789");
            return 0L;
        }

        private void workOnSocket(String domainAdress, String wifiPw) {
            Socket socket = null;

            try {
                Log.e(TAG, "1");
                // InetAddress ip = InetAddress.getByName(domainAdress);
                // InetAddress ip = InetAddress.getByName("192.168.0.5");

                byte[] ipInByte = {(byte) 192, (byte) 168, (byte) 0, (byte) 5};
                InetAddress ip = InetAddress.getByAddress(ipInByte);
                Log.e(TAG, "2");
                socket = new Socket(ip, 8081, true);
                Log.e(TAG, "3");

                DataInputStream dis = new
                        DataInputStream(socket.getInputStream());
                System.out.println(dis.readLine());
                Log.e(TAG, "10");

            } catch (UnknownHostException e) {
                Log.e(TAG, "" + e.toString());
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }

            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "close" + e.toString());
            }
            Log.e(TAG, "100");
        }
    }

}

/*
URL url = null;
            try {
                url = new URL("http", "wt-metall-node1", 8081, "");
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            if (url != null) {
                try {
                    Log.v(TAG, "meep 1");
                    URLConnection urlConnection = url.openConnection();
                    Log.v(TAG, urlConnection.getContentType());
                    Log.v(TAG, "meep 2");
                } catch (IOException e) {
                    Log.v(TAG, "meep Exception");
                    e.printStackTrace();
                }
            }
 */