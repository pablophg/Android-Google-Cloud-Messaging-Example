package net.pablophg.pushnotifications;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class MainActivity extends ActionBarActivity {

    /**
     * This project registers a new device on GCM servers, and then allows
     * you to make a request to a test server, that will then send you a message back
     * through Google Cloud Message API,
     * and then display a notification.
     */

    /**
     * PLEASE NOTE:
     *
     * The test server is hosted on a free heroku instance,
     * so if the server has been inactive for long, the first request may
     * take a few minutes to load
     */

    /**
     * YOU NEED TO CHANGE THIS VALUE WITH THE
     * PROJECT ID YOU GOT ON GOOGLE APIS CONSOLE
     */
    String SENDER_ID = "";

    /**
     * YOU NEED TO CHANGE THIS VALUE WITH THE
     * API SERVER KEY YOU GOT ON GOOGLE APIS CONSOLE
     *
     * NB: do not forget to allow any IP addresses for this test
     */
    String GOOGLE_API_KEY = "";

    /**
     *  Nothing else needs to be changed
     */
    // Test server
    private static final String WEB_SERVICE_URL = "https://pablophg-gcm-test.herokuapp.com/index.php";

    // Used for Shared Preferences
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "app_version";

    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;



    // Tag for logcat
    static final String TAG = "GCMDemo";

    TextView mDisplay;
    GoogleCloudMessaging gcm;
    Context context;

    Button mButtonRequest;

    String regid;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setTitle("GCM Test");

        setContentView(R.layout.activity_main);
        mDisplay = (TextView) findViewById(R.id.display);
        mButtonRequest = (Button) findViewById(R.id.button);
        mButtonRequest.setEnabled(false);

        context = getApplicationContext();

        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);
            if (regid.isEmpty()) {
                // If version changes, the device registers again.
                // You should (or could) take care of that on your server
                // to invalidate devices using older versions once
                // they register for the new version
                registerInBackground();
            }else{
                mButtonRequest.setEnabled(true);
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }

        // Request to the test web service, that will then send a GCM request
        // to google servers and then to this device (only)
        mButtonRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TestGCMRequest().execute();
                Toast.makeText(MainActivity.this, "Sending request...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // You need to do the Play Services APK check here too.
    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }else{
            mDisplay.setText("Device already registered, registration ID=" + registrationId);
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing registration ID is not guaranteed to work with
        // the new app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the registration ID in your app is up to you.
        return getSharedPreferences(MainActivity.class.getSimpleName(), Context.MODE_PRIVATE);
    }

    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private class RegisterOnGCMServers extends AsyncTask<Void, Integer, Boolean> {

        private String mResult;

        @Override
        protected Boolean doInBackground(Void... params) {
            String msg = "";
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(context);
                }
                regid = gcm.register(SENDER_ID);
                msg = "Device registered, registration ID=" + regid;

                // You should send the registration ID to your server over HTTP,
                // so it can use GCM/HTTP or CCS to send messages to your app.
                // The request to your server should be authenticated if your app
                // is using accounts.
                sendRegistrationIdToBackend();

                // For this demo: we don't need to send it because the device
                // will send upstream messages to a server that echo back the
                // message using the 'from' address in the message.

                // Persist the registration ID - no need to register again.
                storeRegistrationId(context, regid);
                mButtonRequest.setEnabled(true);
            } catch (IOException ex) {
                msg = "Error :" + ex.getMessage();
                // If there is an error, don't just keep trying to register.
                // Require the user to click a button again, or perform
                // exponential back-off.
                // TODO implementation in case of failure during registration
            }
            mResult = msg;
            return null;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            mDisplay.append(mResult + "\n");
        }
    }

    private class TestGCMRequest extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... params) {
            //this.url = params[0];
            try {
                URL webURL = new URL(WEB_SERVICE_URL+"?apikey="+GOOGLE_API_KEY+"&device="+regid);
                InputStream is = null;
                try {
                    HttpURLConnection httpUrlConnection = (HttpURLConnection) webURL.openConnection();
                    httpUrlConnection.setRequestMethod("GET");
                    httpUrlConnection.addRequestProperty("Accept","*/*");
                    httpUrlConnection.connect();
                    if(httpUrlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        Log.e("ASYNCTASK", "Failed");
                        return false;
                    }
                    // Obtenemos el input stream de la conexión y decodificamos
                    is = httpUrlConnection.getInputStream();
                    try{
                        Log.d("Results", is.toString());
                        return true;
                    }finally{
                        // Finalmente cerramos lo necesario
                        if(is!=null) is.close();
                        httpUrlConnection.disconnect();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (MalformedURLException e1) {
                e1.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            Toast.makeText(MainActivity.this, "Request sent!", Toast.LENGTH_LONG).show();
        }
    }

    private void registerInBackground() {
        new RegisterOnGCMServers().execute();
    }

    // Save the ID on our own server
    private void sendRegistrationIdToBackend() {
        /**
         * Here you should send the registration ID to your own server to save this device
         * and send messages to it later.
         */
        Log.e("REGISTRATION ID", regid);
    }

    // Saves the registration code on shared preferences for future launches
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }
}