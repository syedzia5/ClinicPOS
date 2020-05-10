package com.zcs.clinicpos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.net.http.*; //added this import statement
import android.webkit.SslErrorHandler;
import android.net.http.SslError;
import android.view.*;
import 	android.webkit.*;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.*;
import androidx.core.content.ContextCompat;


import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.*;

public class LoginActivity extends AppCompatActivity {

    private static Context appContext;
    //Permission variables
    static boolean ASWP_JSCRIPT    = SmartWebView.ASWP_JSCRIPT;
    static boolean ASWP_FUPLOAD    = SmartWebView.ASWP_FUPLOAD;
    static boolean ASWP_CAMUPLOAD  = SmartWebView.ASWP_CAMUPLOAD;
    static boolean ASWP_ONLYCAM		= SmartWebView.ASWP_ONLYCAM;
    static boolean ASWP_MULFILE    = SmartWebView.ASWP_MULFILE;
    static boolean ASWP_LOCATION   = SmartWebView.ASWP_LOCATION;
    static boolean ASWP_RATINGS    = SmartWebView.ASWP_RATINGS;
    static boolean ASWP_PULLFRESH	= SmartWebView.ASWP_PULLFRESH;
    static boolean ASWP_PBAR       = SmartWebView.ASWP_PBAR;
    static boolean ASWP_ZOOM       = SmartWebView.ASWP_ZOOM;
    static boolean ASWP_SFORM      = SmartWebView.ASWP_SFORM;
    static boolean ASWP_OFFLINE		= SmartWebView.ASWP_OFFLINE;
    static boolean ASWP_EXTURL		= SmartWebView.ASWP_EXTURL;

    //Security variables
    static boolean ASWP_CERT_VERIFICATION = SmartWebView.ASWP_CERT_VERIFICATION;

    //Configuration variables
    private static String ASWV_URL      = SmartWebView.ASWV_URL;
    private String CURR_URL				 = ASWV_URL;
    private static String ASWV_F_TYPE   = SmartWebView.ASWV_F_TYPE;

    public static String ASWV_HOST		= aswm_host(ASWV_URL);

    private WebView webView;

    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private static final String TAG = LoginActivity.class.getSimpleName();
    private WebSettings webSettings;
    private ValueCallback<Uri[]> mUploadMessage;
    private String mCameraPhotoPath = null;
    private long size = 0;
    private final static int loc_perm = 1;
    private final static int file_perm = 2;
    private SecureRandom random = new SecureRandom();

    NotificationManager asw_notification;
    Notification asw_notification_new;
    // Storage Permissions variables
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mUploadMessage == null) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        try {
            String file_path = mCameraPhotoPath.replace("file:","");
            File file = new File(file_path);
            size = file.length();

        }catch (Exception e){
            Log.e("Error!", "Error while opening image file" + e.getLocalizedMessage());
        }

        if (data != null || mCameraPhotoPath != null) {
            Integer count = 0; //fix fby https://github.com/nnian
            ClipData images = null;
            try {
                images = data.getClipData();
            }catch (Exception e) {
                Log.e("Error!", e.getLocalizedMessage());
            }

            if (images == null && data != null && data.getDataString() != null) {
                count = data.getDataString().length();
            } else if (images != null) {
                count = images.getItemCount();
            }
            Uri[] results = new Uri[count];
            // Check that the response is a good one
            if (resultCode == Activity.RESULT_OK) {
                if (size != 0) {
                    // If there is not data, then we may have taken a photo
                    if (mCameraPhotoPath != null) {
                        results = new Uri[]{Uri.parse(mCameraPhotoPath)};
                    }
                } else if (data.getClipData() == null) {
                    results = new Uri[]{Uri.parse(data.getDataString())};
                } else {

                    for (int i = 0; i < images.getItemCount(); i++) {
                        results[i] = images.getItemAt(i).getUri();
                    }
                }
            }

            mUploadMessage.onReceiveValue(results);
            mUploadMessage = null;
        }
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have read or write permission

        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int cameraPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED || cameraPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        appContext = getApplicationContext();

        webView = (WebView)findViewById(R.id.webview);

        /*
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }
        });
        */
        //Getting GPS location of device if given permission
        if(ASWP_LOCATION && !check_permission(1)){
            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, loc_perm);
        }
        get_location();

        verifyStoragePermissions(this);

        WebSettings  ws = webView.getSettings();

        /*
        ws.setJavaScriptEnabled(true);
        ws.setGeolocationEnabled(true);
        ws.setJavaScriptCanOpenWindowsAutomatically(true);
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        ws.setSaveFormData(true);
        ws.setSupportZoom(true);
        ws.setGeolocationEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowFileAccessFromFileURLs(true);
        ws.setAllowUniversalAccessFromFileURLs(true);
        ws.setUseWideViewPort(true);
        ws.setDomStorageEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setPluginState(WebSettings.PluginState.ON);
        ws.setBuiltInZoomControls(true);
        */
        webSettings = webView.getSettings();
        webSettings.setAppCacheEnabled(true);
        webSettings.setCacheMode(webSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAppCacheEnabled(true);
        webSettings.setLoadsImagesAutomatically(true);
        webSettings.setGeolocationEnabled(true);
        webView.setVerticalScrollBarEnabled(true);
        //webView.setHorizontalScrollBarEnabled(true);
        //webSettings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.addJavascriptInterface(new WebAppDBInterface(this), "AndroidDB");

        webView.setWebViewClient(new PQClient());
        webView.setWebChromeClient(new PQChromeClient());
        //if SDK version is greater of 19 then activate hardware acceleration otherwise activate software acceleration
        if (Build.VERSION.SDK_INT >= 19) {
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else if (Build.VERSION.SDK_INT >= 11 && Build.VERSION.SDK_INT < 19) {
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(false);

        String url =  null;
        if (DetectConnection.isInternetAvailable(this)) {
            //webVieif (w.getSettings().setJavaScriptEnabled(true);
            //webView.loadUrl("https://"+CIMCAppGlobals.getServerIPAddress()+":8443/VRTE-CIMC-PROTO-1/cimcmiblogin.html");
            url = "https://" + CIMCAppGlobals.getServerIPAddress() + ":8443/VRTE-CIMC-PROTO-1/CIMCMobileLoginPage";
        }
        else {
            //url = "file:///android_asset/CIMCLogin.html";
            DatabaseHelper dh = new DatabaseHelper(appContext);
            /*
            if (dh) {

             */
                UserData ud = dh.getUserData();
                if (ud == null) {

                    url = "file:///android_asset/startupmessage.html?dspmsg=You are not authorized to use this app";
                } else if (ud.roleid() == 7) {

                    url = "file:///android_asset/CIMCMblHmPg.html";
                } else {

                    url = "file:///android_asset/startupmessage.html?dspmsg=Mobile network not available.";
                }
            /*
            }
            else {
                url = "file:///android_asset/startupmessage.html?dspmsg=SQLLite database not available";
            }

             */
        }
        Toast.makeText(appContext, url, Toast.LENGTH_LONG).show();
        webView.loadUrl(url);//("https://"+CIMCAppGlobals.getServerIPAddress()+":8443/VRTE-CIMC-PROTO-1/CIMCMobileLoginPage");
        webView.setHorizontalScrollBarEnabled(false);

        /*
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                callback.invoke(origin, true, false);
            }
            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                //request.grant(request.getResources());
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.getResources());
                }
            }
        });
        webView.setWebViewClient(new WebViewClient () {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // When user clicks a hyperlink, load in the existing WebView
                view.loadUrl(url);
                return true;
            }
        });
        */
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        return imageFile;
    }
    //Checking if particular permission is given or not

    public class PQChromeClient extends WebChromeClient {

        // For Android 5.0+
        public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePath, WebChromeClient.FileChooserParams fileChooserParams) {
            // Double check that we don't have any existing callbacks
            if (mUploadMessage != null) {
                mUploadMessage.onReceiveValue(null);
            }
            mUploadMessage = filePath;
            Log.e("FileCooserParams => ", filePath.toString());

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e(TAG, "Unable to create Image File", ex);
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                } else {
                    takePictureIntent = null;
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            contentSelectionIntent.setType("image/*");

            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[2];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);
            startActivityForResult(Intent.createChooser(chooserIntent, "Select images"), 1);

            return true;

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        else
        {
            finish();
        }
        return super.onKeyDown(keyCode, event);
    }

    public class PQClient extends WebViewClient {
        ProgressDialog progressDialog;


        // overload the geoLocations permissions prompt to always allow instantly as app permission was granted previously
        public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
            if(Build.VERSION.SDK_INT < 23 || check_permission(loc_perm)){
                // location permissions were granted previously so auto-approve
                callback.invoke(origin, true, false);
            } else {
                // location permissions not granted so request them
                ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, loc_perm);
            }
        }
        @Override
        public void onReceivedSslError (WebView view, SslErrorHandler handler, SslError error) {
            handler.proceed();
        }

        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            // If url contains mailto link then open Mail Intent
            if (url.contains("mailto:")) {

                // Could be cleverer and use a regex
                //Open links in new browser
                view.getContext().startActivity(
                        new Intent(Intent.ACTION_VIEW, Uri.parse(url)));

                // Here we can open new activity

                return true;

            } else {

                // Stay within this webview and load url
                view.loadUrl(url);
                return true;
            }
        }

        /*
        //Show loader on url load
        public void onPageStarted(WebView view, String url, Bitmap favicon) {

            // Then show progress  Dialog
            // in standard case YourActivity.this
            if (progressDialog == null) {
                progressDialog = new ProgressDialog(LoginActivity.this);
                progressDialog.setMessage("Loading...");
                progressDialog.hide();
            }
        }

        // Called when all page resources loaded
        public void onPageFinished(WebView view, String url) {
            webView.loadUrl("javascript:(function(){ " +
                    "document.getElementById('android-app').style.display='none';})()");

            try {
                // Close progressDialog
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                    progressDialog = null;
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
        */
    }
    public String random_id() {
        return new BigInteger(130, random).toString(32);
    }

    void aswm_view(String url, Boolean tab) {
        if (tab) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        } else {
            if(url.contains("?")){ // check to see whether the url already has query parameters and handle appropriately.
                url += "&";
            } else {
                url += "?";
            }
            url += "rid="+random_id();
            webView.loadUrl(url);
        }
    }

    //Actions based on shouldOverrideUrlLoading
    public boolean url_actions(WebView view, String url){
        boolean a = true;
        //Show toast error if not connected to the network
        if (!ASWP_OFFLINE && !DetectConnection.isInternetAvailable(LoginActivity.this)) {
            Toast.makeText(getApplicationContext(), getString(R.string.check_connection), Toast.LENGTH_SHORT).show();

            //Use this in a hyperlink to redirect back to default URL :: href="refresh:android"
        } else if (url.startsWith("refresh:")) {
            pull_fresh();

            //Use this in a hyperlink to launch default phone dialer for specific number :: href="tel:+919876543210"
        } else if (url.startsWith("tel:")) {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse(url));
            startActivity(intent);

            //Use this to open your apps page on google play store app :: href="rate:android"
        } else if (url.startsWith("rate:")) {
            final String app_package = getPackageName(); //requesting app package name from Context or Activity object
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + app_package)));
            } catch (ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + app_package)));
            }

            //Sharing content from your webview to external apps :: href="share:URL" and remember to place the URL you want to share after share:___
        } else if (url.startsWith("share:")) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, view.getTitle());
            intent.putExtra(Intent.EXTRA_TEXT, view.getTitle()+"\nVisit: "+(Uri.parse(url).toString()).replace("share:",""));
            startActivity(Intent.createChooser(intent, getString(R.string.share_w_friends)));

            //Use this in a hyperlink to exit your app :: href="exit:android"
        } else if (url.startsWith("exit:")) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            //Getting location for offline files
        } else if (url.startsWith("offloc:")) {
            String offloc = ASWV_URL+"?loc="+get_location();
            aswm_view(offloc,false);
            Log.d("OFFLINE LOC REQ",offloc);

            //Opening external URLs in android default web browser
        } else if (ASWP_EXTURL && !aswm_host(url).equals(ASWV_HOST)) {
            aswm_view(url,true);
        } else {
            a = false;
        }
        return a;
    }
    //Getting host name
    public static String aswm_host(String url){
        if (url == null || url.length() == 0) {
            return "";
        }
        int dslash = url.indexOf("//");
        if (dslash == -1) {
            dslash = 0;
        } else {
            dslash += 2;
        }
        int end = url.indexOf('/', dslash);
        end = end >= 0 ? end : url.length();
        int port = url.indexOf(':', dslash);
        end = (port > 0 && port < end) ? port : end;
        Log.w("URL Host: ",url.substring(dslash, end));
        return url.substring(dslash, end);
    }

    //Reloading current page
    public void pull_fresh(){
        aswm_view(CURR_URL,false);
    }


    public void get_info(){
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setCookie(ASWV_URL, "DEVICE=android");
        cookieManager.setCookie(ASWV_URL, "DEV_API=" + Build.VERSION.SDK_INT);
    }

    //Checking permission for storage and camera for writing and uploading images
    public void get_file(){
        String[] perms = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA};

        //Checking for storage permission to write images for upload
        if (ASWP_FUPLOAD && ASWP_CAMUPLOAD && !check_permission(2) && !check_permission(3)) {
            ActivityCompat.requestPermissions(LoginActivity.this, perms, file_perm);

            //Checking for WRITE_EXTERNAL_STORAGE permission
        } else if (ASWP_FUPLOAD && !check_permission(2)) {
            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, file_perm);

            //Checking for CAMERA permissions
        } else if (ASWP_CAMUPLOAD && !check_permission(3)) {
            ActivityCompat.requestPermissions(LoginActivity.this, new String[]{Manifest.permission.CAMERA}, file_perm);
        }
    }
    //Using cookies to update user locations
    public String get_location(){
        String newloc = "0,0";
        //Checking for location permissions
        if (ASWP_LOCATION && (Build.VERSION.SDK_INT < 23 || check_permission(1))) {
            GPSTrack gps;
            gps = new GPSTrack(LoginActivity.this);
            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();
            if (gps.canGetLocation()) {
                if (latitude != 0 || longitude != 0) {
                    if(!ASWP_OFFLINE) {
                        CookieManager cookieManager = CookieManager.getInstance();
                        cookieManager.setAcceptCookie(true);
                        cookieManager.setCookie(ASWV_URL, "lat=" + latitude);
                        cookieManager.setCookie(ASWV_URL, "long=" + longitude);
                    }
                    //Log.w("New Updated Location:", latitude + "," + longitude);  //enable to test dummy latitude and longitude
                    newloc = latitude+","+longitude;
                } else {
                    Log.w("New Updated Location:", "NULL");
                }
            } else {
                show_notification(1, 1);
                Log.w("New Updated Location:", "FAIL");
            }
        }
        return newloc;
    }

    //Checking if particular permission is given or not
    public boolean check_permission(int permission){
        switch(permission){
            case 1:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

            case 2:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

            case 3:
                return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        }
        return false;
    }

    //Creating image file for upload
    private File create_image() throws IOException {
        @SuppressLint("SimpleDateFormat")
        String file_name    = new SimpleDateFormat("yyyy_mm_ss").format(new Date());
        String new_name     = "file_"+file_name+"_";
        File sd_directory   = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(new_name, ".jpg", sd_directory);
    }

    //Launching app rating dialoge [developed by github.com/hotchemi]
    public void get_rating() {
        if (DetectConnection.isInternetAvailable(LoginActivity.this)) {
            AppRate.with(this)
                    .setStoreType(StoreType.GOOGLEPLAY)     //default is Google Play, other option is Amazon App Store
                    .setInstallDays(SmartWebView.ASWR_DAYS)
                    .setLaunchTimes(SmartWebView.ASWR_TIMES)
                    .setRemindInterval(SmartWebView.ASWR_INTERVAL)
                    .setTitle(R.string.rate_dialog_title)
                    .setMessage(R.string.rate_dialog_message)
                    .setTextLater(R.string.rate_dialog_cancel)
                    .setTextNever(R.string.rate_dialog_no)
                    .setTextRateNow(R.string.rate_dialog_ok)
                    .monitor();
            AppRate.showRateDialogIfMeetsConditions(this);
        }
        //for more customizations, look for AppRate and DialogManager
    }

    //Creating custom notifications with IDs
    public void show_notification(int type, int id) {
        long when = System.currentTimeMillis();
        asw_notification = (NotificationManager) LoginActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent i = new Intent();
        if (type == 1) {
            i.setClass(LoginActivity.this, LoginActivity.class);
        } else if (type == 2) {
            i.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        } else {
            i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            i.addCategory(Intent.CATEGORY_DEFAULT);
            i.setData(Uri.parse("package:" + LoginActivity.this.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        }
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(LoginActivity.this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(LoginActivity.this, "");
        switch(type){
            case 1:
                builder.setTicker(getString(R.string.app_name));
                builder.setContentTitle(getString(R.string.loc_fail));
                builder.setContentText(getString(R.string.loc_fail_text));
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.loc_fail_more)));
                builder.setVibrate(new long[]{350,350,350,350,350});
                builder.setSmallIcon(R.mipmap.ic_launcher);
                break;

            case 2:
                builder.setTicker(getString(R.string.app_name));
                builder.setContentTitle(getString(R.string.loc_perm));
                builder.setContentText(getString(R.string.loc_perm_text));
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.loc_perm_more)));
                builder.setVibrate(new long[]{350, 700, 350, 700, 350});
                builder.setSound(alarmSound);
                builder.setSmallIcon(R.mipmap.ic_launcher);
                break;
        }
        builder.setOngoing(false);
        builder.setAutoCancel(true);
        builder.setContentIntent(pendingIntent);
        builder.setWhen(when);
        builder.setContentIntent(pendingIntent);
        asw_notification_new = builder.build();
        asw_notification.notify(id, asw_notification_new);
    }

    public static Context getAppContext() {
        return appContext;
    }

    public class WebAppInterface {
        Context mContext;

        /**
         * Instantiate the interface and set the context
         */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /**
         * Show a toast from the web page
         */
        @JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void showToastLong(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_LONG).show();
        }

        @JavascriptInterface
        public void Alert(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_LONG).show();
            new AlertDialog.Builder(mContext).setTitle("Alert").setMessage(toast).setNeutralButton("Close", null).show();
        }

        @JavascriptInterface
        public String GetLocation() {
            return get_location();
        }

        @JavascriptInterface
        public boolean isInternetAvailable() {

            return DetectConnection.isInternetAvailable(mContext); //(LoginActivity.this);
        }
        @JavascriptInterface
        public void Exit() {

            moveTaskToBack(true);
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }
    public class WebAppDBInterface {

        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppDBInterface(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void saveDataLocal(String data) {

            DatabaseHelper dh = new DatabaseHelper(mContext); //(LoginActivity.getAppContext());
            dh.addData(data);
        }

        @JavascriptInterface
        public boolean dataExistsForUpload() {

            DatabaseHelper dh = new DatabaseHelper(mContext); //(LoginActivity.getAppContext());
            return dh.getData().getCount() > 0;
        }

        @JavascriptInterface
        public void syncProjectData(String xmldata) {

            //Toast.makeText(mContext, "in syncProjectData", Toast.LENGTH_SHORT).show();

            DatabaseHelper dh = new DatabaseHelper(mContext); //(LoginActivity.getAppContext());
            dh.syncProjectData(xmldata);
        }

        @JavascriptInterface
        public String getPrjData() {

            //Toast.makeText(mContext, "in getPrjData", Toast.LENGTH_SHORT).show();

            DatabaseHelper dh = new DatabaseHelper(mContext); //(LoginActivity.getAppContext());

            Cursor c = dh.getPrjData();

            int numprj = c.getCount();
            //Toast.makeText(mContext, "in getPrjData: Num of Projects"+Integer.toString(numprj), Toast.LENGTH_SHORT).show();

            String s = "";

            if (numprj > 0) {
                if (c.moveToFirst()) {
                    s += "<PRJMBLDATA>";
                    do {
                        s += "<PRJMBLDATAREC>";
                            s += "<PRJID>"+c.getString(c.getColumnIndex("PrjID"))+"</PRJID>";
                            s += "<PRJNAME>"+c.getString(c.getColumnIndex("Name"))+"</PRJNAME>";
                            s += "<PRJDEVNAME>"+c.getString(c.getColumnIndex("UserName"))+"</PRJDEVNAME>";
                            s += "<PRJADDR1>"+c.getString(c.getColumnIndex("address1"))+"</PRJADDR1>";
                            String sv = c.getString(c.getColumnIndex("address2"));
                            boolean isn = (sv == null) || (sv.length() <= 0);
                            s += "<PRJADDR2>"+((isn)?"":sv)+"</PRJADDR2>";
                            sv = c.getString(c.getColumnIndex("address3"));
                            isn = (sv == null) || (sv.length() <= 0);
                            s += "<PRJADDR3>"+((isn)?"":sv)+"</PRJADDR3>";
                            s += "<PRJCITY>"+c.getString(c.getColumnIndex("DistName"))+"</PRJCITY>";
                            s += "<PRJZIP>"+c.getString(c.getColumnIndex("zipcode"))+"</PRJZIP>";
                            s += "<PRJSTATE>"+c.getString(c.getColumnIndex("StateName"))+"</PRJSTATE>";
                        s += "</PRJMBLDATAREC>";
                    } while (c.moveToNext());
                    s += "</PRJMBLDATA>";
                }
                dh.close();
           }

            return s;
        }

        @JavascriptInterface
        public void uploadData(String cookie) {

            DatabaseHelper dh = new DatabaseHelper(mContext); //(LoginActivity.getAppContext());
            dh.uploadData(cookie);
        }

        @JavascriptInterface
        public void setUserData(String udata) {

            DatabaseHelper dh = new DatabaseHelper(mContext); //(LoginActivity.getAppContext());

            try {
                dh.setUserData(udata);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
