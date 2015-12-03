
package com.uberspot.a2048;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.webkit.WebSettings;
import android.webkit.WebSettings.RenderPriority;
import android.webkit.WebView;
import android.widget.Toast;
import android.util.Log;
import android.webkit.JavascriptInterface;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.Socket;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import android.os.Environment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends Activity {

    private WebView mWebView;
    private long mLastBackPress;
    private static final long mBackPressThreshold = 3500;
    private static final String IS_FULLSCREEN_PREF = "is_fullscreen_pref";
    private static boolean DEF_FULLSCREEN = true;
    private long mLastTouch;
    private static final long mTouchThreshold = 2000;
    private Toast pressBackToast;
    private String url ="http://localhost:2001/";
    private long fileNum;
    private FileWriter fw;
    private BufferedWriter bw;

    @SuppressLint({ "SetJavaScriptEnabled", "NewApi", "ShowToast" })
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Don't show an action bar or title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        fileNum = 0;
        // If on android 3.0+ activate hardware acceleration
        if (Build.VERSION.SDK_INT >= 11) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }

        // Apply previous setting about showing status bar or not
        applyFullScreen(isFullScreen());

        // Check if screen rotation is locked in settings
        boolean isOrientationEnabled = false;
        try {
            isOrientationEnabled = Settings.System.getInt(getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION) == 1;
        } catch (SettingNotFoundException e) { }

        // If rotation isn't locked and it's a LARGE screen then add orientation changes based on sensor
        int screenLayout = getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK;
        if (((screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE)
                || (screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE))
                    && isOrientationEnabled) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }

        setContentView(R.layout.activity_main);

        // Load webview with game
        mWebView = (WebView) findViewById(R.id.mainWebView);
        WebSettings settings = mWebView.getSettings();
        String packageName = getPackageName();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setRenderPriority(RenderPriority.HIGH);
        settings.setDatabasePath("/data/data/" + packageName + "/databases");
        mWebView.addJavascriptInterface(new JsOperation(this), "client");

        // If there is a previous instance restore it in the webview
        if (savedInstanceState != null) {
            mWebView.restoreState(savedInstanceState);
        } else {
            mWebView.loadUrl("file:///android_asset/2048/index.html");
        }

        Toast.makeText(getApplication(), R.string.toggle_fullscreen, Toast.LENGTH_SHORT).show();
        // Set fullscreen toggle on webview LongClick
        mWebView.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // Implement a long touch action by comparing
                // time between action up and action down
                long currentTime = System.currentTimeMillis();
                if ((event.getAction() == MotionEvent.ACTION_UP)
                        && (Math.abs(currentTime - mLastTouch) > mTouchThreshold)) {
                    boolean toggledFullScreen = !isFullScreen();
                    saveFullScreen(toggledFullScreen);
                    applyFullScreen(toggledFullScreen);
                } else if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    mLastTouch = currentTime;
                }
                // return so that the event isn't consumed but used
                // by the webview as well
                return false;
            }
        });

        pressBackToast = Toast.makeText(getApplicationContext(), R.string.press_back_again_to_exit,
                Toast.LENGTH_SHORT);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        mWebView.saveState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void saveFullScreen(boolean isFullScreen) {
        // save in preferences
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean(IS_FULLSCREEN_PREF, isFullScreen);
        editor.commit();
    }

    private boolean isFullScreen() {
        return PreferenceManager.getDefaultSharedPreferences(this).getBoolean(IS_FULLSCREEN_PREF,
                DEF_FULLSCREEN);
    }

    /**
     * Toggles the activitys fullscreen mode by setting the corresponding window flag
     * @param isFullScreen
     */
    private void applyFullScreen(boolean isFullScreen) {
        if (isFullScreen) {
            getWindow().clearFlags(LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onBackPressed() {
        long currentTime = System.currentTimeMillis();
        if (Math.abs(currentTime - mLastBackPress) > mBackPressThreshold) {
            pressBackToast.show();
            mLastBackPress = currentTime;
        } else {
            pressBackToast.cancel();
            super.onBackPressed();
        }
    }
    public static int printValue(int value){
        Log.e("2048",String.valueOf(value));
        return 0;
    }
    public static void sendToServer(int value){

    }

    class JsOperation{
        Activity mActivity;
        public JsOperation(Activity activity){
            mActivity = activity;
        }
        @JavascriptInterface
        public void printValue(int value){
            Log.e("2048", String.valueOf(value));




        }
        @JavascriptInterface
        public void createFile(){

            fileNum++;
            if(bw != null) try {
                bw.close();
                if(fw != null ) fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }


            File file  = new File(Environment.getExternalStorageDirectory(),"f.txt");
            Log.e("2048", "create file: "+file.getName());
            if(file.exists()){
                file.delete();
            }
            if(!file.exists()){
                try {
                    file.createNewFile();
                    fw = new FileWriter(Environment.getExternalStorageDirectory()+"/f.txt", true);
                    bw = new BufferedWriter(fw);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        @JavascriptInterface
        public void writeToFile(int value){
            Log.e("2048", "write "+value);
            try{

   /*
                //File file  = new File(Environment.getExternalStorageDirectory()+"/f.txt");
                File file  = new File(Environment.getExternalStorageDirectory(),"f.txt");


                if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){


                    FileOutputStream fos = new FileOutputStream(file);


                    String message = String.valueOf(value);


                    byte[] buffer = message.getBytes();


                    fos.write(buffer);


                    fos.close();


                }
                */
                bw.write(String.valueOf(value));
                bw.newLine();
                bw.flush();

            }catch(Exception ex){

            }
        }
        @JavascriptInterface
        public void sendToServer(int value){
            String hostIP = "127.0.0.1";
            int port = 2001;
            try {
                Socket socket = new Socket(hostIP, port);
                DataOutputStream out;
                out = new DataOutputStream(socket.getOutputStream());

                out.writeInt(value);
                out.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            /*
            RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());

            String jsonurl = url;
            JSONObject jsonObj = new JSONObject();

            try {

                jsonObj.put("value", value);


            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest sRequest = new JsonObjectRequest(Request.Method.POST, jsonurl, jsonObj, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    System.out.println(response);

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError arg0) {
                    System.out.println("sorry,Error");
                }
            });
            sRequest.setShouldCache(false);
            requestQueue.add(sRequest);
            */
        }
    }
}
