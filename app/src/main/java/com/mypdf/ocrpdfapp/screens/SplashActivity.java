package com.mypdf.ocrpdfapp.screens;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.android.installreferrer.api.InstallReferrerClient;
import com.android.installreferrer.api.InstallReferrerStateListener;
import com.android.installreferrer.api.ReferrerDetails;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.mypdf.ocrpdfapp.PdfApplication;
import com.mypdf.ocrpdfapp.R;
import com.mypdf.ocrpdfapp.dummy.FifthActivity;
import com.mypdf.ocrpdfapp.dummy.FirstActivity;
import com.mypdf.ocrpdfapp.dummy.FourthActivity;
import com.mypdf.ocrpdfapp.dummy.SecondActivity;
import com.mypdf.ocrpdfapp.dummy.SixthActivity;
import com.mypdf.ocrpdfapp.dummy.ThirdActivity;
import com.mypdf.ocrpdfapp.util.AdsManager;
import com.mypdf.ocrpdfapp.util.JsonParser;
import com.mypdf.ocrpdfapp.util.OpenAdManager;
import com.mypdf.ocrpdfapp.util.OpenAdManagerSplash;
import com.mypdf.ocrpdfapp.util.PrefManagerVideo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class SplashActivity extends AppCompatActivity {

    private static final String url = "https://guide3.myappadmin.xyz/gb/" + "pdf_app.php";
    public static PrefManagerVideo prf;

    public static final String TAG_BANNERMAIN = "TAG_BANNERMAIN";
    public static final String TAG_BANNERMAINS = "TAG_BANNERMAINS";
    public static final String open_button = "open_button";
    public static final String sign_button = "sign_button";
    public static final String convert_to_images_button = "convert_to_images_button";
    public static final String extract_text_button = "extract_text_button";
    public static final String share_button = "share_button";
    public static final String print_button = "print_button";
    public static final String edit_button = "edit_button";
    public static final String compress_button = "compress_button";

    public static final String bannerlarge = "bannerlarge";

    public static final String pdf_editor_button = "pdf_editor_button";
    public static final String image_to_pdf_button = "image_to_pdf_button";
    public static final String pdf_to_image_button = "pdf_to_image_button";
    public static final String sign_pdf_button = "sign_pdf_button";

    public static final String interstitial_type = "interstitial_type";
    public static final String proxy_username = "proxy_username";
    public static final String proxy_password = "proxy_password";
    public static final String proxy_port = "proxy_port";
    public static final String proxy_host = "proxy_host";
    public static final String webview_url = "webview_url";

    public static final String firstonpd = "firstonpd";
    public static final String secondonim = "secondonim";

    public static final String status_dummy_two_back_enabled = "status_dummy_two_back_enabled";
    Map<String, String> params = new HashMap<>();
    public static final String inter_ad_type = "inter_ad_type";
    public static final String dummy_three_screen = "dummy_three_screen";
    public static final String status_dummy_three_enabled = "status_dummy_three_enabled";
    private String st1, st2, st3, st4, st5, st6;
    private static final String TAG_SUCCESS = "success";
    public static final String status_dummy_three_back_enabled = "status_dummy_three_back_enabled";
    public static final String TAG_APP_ID_AD_UNIT_ID = "app_id_ad_unit_id";
    public AppOpenAd appOpenAd;
    public static final String TAG_NATIVEIDSMALL_fifteen = "nativeid_small";
    public static final String enable_extract_feature = "enable_extract_feature";
    public static final String ADMOB_INTERSTITIAL_FREQUENCY = "ADMOB_INTERSTITIAL_FREQUENCY";
    public static final String TAG_NATIVEID = "nativeid";
    public static final String status_dummy_five_back_enabled = "status_dummy_five_back_enabled";
    public static final String TAG_OPENAPPID = "openappid";
    public static final String status_dummy_two_enabled = "status_dummy_two_enabled";
    public static final String dummy_two_screen = "dummy_two_screen";
    public static final String TAG_OPENAPP_ADS_ENABLED = "openapp_ads_enabled";
    public static final String status_dummy_one_enabled_fifteen = "status_dummy_one_enabled";
    public static final String status_dummy_one_back_enabled = "status_dummy_one_back_enabled";
    public static final String status_dummy_six_enabled = "status_dummy_six_enabled";
    public static final String status_dummy_six_back_enabled = "status_dummy_six_back_enabled";
    public static final String enable_adaptive_banner = "enable_adaptive_banner";
    public static final String status_dummy_five_enabled = "status_dummy_five_enabled";
    public static final String exit_screen = "exit_screen";
    public static final String TAG_INTERSTITIALSPLASH = "interstitialsplash";
    public static final String status_dummy_four_back_enabled = "status_dummy_four_back_enabled";
    public static final String dummy_one_screen = "dummy_one_screen";
    public static final String home_screen = "home_screen";
    private final Executor backgroundExecutor = Executors.newSingleThreadExecutor();
    public static final String TAG_PKG = "pkg";
    public static boolean isFirstStart;
    private static final String TAG = "TAGSPLASHH";
    private final JsonParser jsonParser = new JsonParser();
    public static final String dummy_four_screen = "dummy_four_screen";
    private int rds = 0;
    private static OpenAdManager openAdManagerFourteenVideo;
    private String sf1, sf2, sf3, sf4, sf5, sf6;
    private String rUrl = "sandeep";
    public static final String TAG_INTERSTITIALMAIN = "interstitialmain";
    private int success = 0;
    public static final String status_dummy_four_enabled = "status_dummy_four_enabled";

    public static boolean isConnectedToInternet(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }


    private static String username = "brd-customer-hl_2951b71e-zone-datacenter_proxy1";
    private static String password = "38sy7x3rzt3y";
    private static String proxyHost = "brd.superproxy.io";
    private static int proxyPort = 22225;

//    public static Response response;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        Log.d("TAGVER", "version: " + Build.VERSION.SDK_INT);
        AdsManager.initializeAdMob(this);
        prf = new PrefManagerVideo(this);
        initialization();

        RelativeLayout noInternetLayout = findViewById(R.id.noInternetLayout);
        ProgressBar progressBar = findViewById(R.id.progress_bar);

        if (isConnectedToInternet(this)) {
            proceed();
        } else {

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    progressBar.setVisibility(View.GONE);
                    noInternetLayout.setVisibility(View.VISIBLE);
                    Button btnRefresh = findViewById(R.id.btnRefresh);
                    btnRefresh.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            noInternetLayout.setVisibility(View.GONE);
                            progressBar.setVisibility(View.VISIBLE);

                            if (isConnectedToInternet(SplashActivity.this)) {
                                noInternetLayout.setVisibility(View.GONE);
                                progressBar.setVisibility(View.VISIBLE);

                                proceed();
                            } else {

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressBar.setVisibility(View.GONE);
                                        noInternetLayout.setVisibility(View.VISIBLE);
                                    }
                                }, 1500);

                            }
                        }
                    });
                }
            }, 1500);

        }


    }

    private void proceed() {


//        Thread thread = new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                try {
//                    Authenticator proxyAuthenticator = new Authenticator() {
//                        @Override public Request authenticate(Route route, Response response) throws IOException {
//                            String credential = Credentials.basic(username, password);
//                            return response.request().newBuilder()
//                                    .header("Proxy-Authorization", credential)
//                                    .build();
//                        }
//                    };
//
//                    OkHttpClient client = new OkHttpClient.Builder()
//                            .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)))
//                            .proxyAuthenticator(proxyAuthenticator)
//                            .build();
//
//                    // Create a request to make an HTTP GET call to the URL
//                    Request request = new Request.Builder()
//                            .url(url)
//                            .build();
//
//                    try {
//
//                        Log.d("setupProxy", "try ");
//
//                        response = client.newCall(request).execute();
//
//                        if (response.isSuccessful())
//                        {
//                            Log.d("setupProxy", "Success!");
//
//                        }
//
//
//                    } catch (IOException e) {
//                        // Display an error message if an exception occurs
//                        e.printStackTrace();
//                        Log.d("setupProxy", "catch "+e.getMessage());
//
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    Log.d("setupProxy", "catch1 "+e.getMessage());
//
//                }
//            }
//        });
//
//        thread.start();

        isFirstStart = prf.getBoolean("firstStart");
        if (isFirstStart) {
            System.out.println("Rajan_isFirstStart" + isFirstStart);
            prf.setBoolean("firstStart", false);
            prf.setString("rUrl", "notset");
        }

        if (prf.getString("rUrl").contains("notset")) {
            checkIns();
        } else {
            rUrl = prf.getString("rUrl");
            new BackgroundSplashTask().execute();
        }
    }

    private void initialization() {
        prf.setString("startclicktext", "Start");
        prf.setInt(ADMOB_INTERSTITIAL_FREQUENCY, 1);
        prf.setString(TAG_INTERSTITIALMAIN, "ca-app-pub-3940256099942544/103317371200");
        prf.setString("rd", "0");
        prf.setString(home_screen, "text");
        prf.setString(dummy_two_screen, "ad");

        prf.setString(status_dummy_three_back_enabled, "false");
        prf.setString(exit_screen, "exit_screen");
        prf.setString(status_dummy_three_enabled, "false");
        prf.setString(status_dummy_four_back_enabled, "false");
        prf.setString(TAG_NATIVEID, "c");
        prf.setString(TAG_OPENAPP_ADS_ENABLED, "no");
        prf.setString("skipfirstscreen", "1");
        prf.setString(TAG_NATIVEIDSMALL_fifteen, "ca-app-pub-3940256099942544/2247696110");
        prf.setString(enable_extract_feature, "false");
        prf.setString(status_dummy_one_back_enabled, "false");
        prf.setString(status_dummy_six_enabled, "false");
        prf.setString(status_dummy_six_back_enabled, "false");
        prf.setString(dummy_three_screen, "ad");
        prf.setString(TAG_APP_ID_AD_UNIT_ID, "ca-app-pub-3940256099942544~3347511713");
        prf.setString(TAG_OPENAPPID, "ca-app-pub-3940256099942544/341983529400");
        prf.setString(status_dummy_five_enabled, "false");
        prf.setString(enable_adaptive_banner, "false");
        prf.setString(dummy_four_screen, "ad");

        prf.setString(pdf_editor_button, "true");
        prf.setString(image_to_pdf_button, "true");
        prf.setString(pdf_to_image_button, "true");
        prf.setString(sign_pdf_button, "true");


        prf.setString(TAG_BANNERMAINS, "");
        prf.setString(TAG_BANNERMAIN, "");
        prf.setString(sign_button, "false");
        prf.setString(convert_to_images_button, "false");
        prf.setString(extract_text_button, "false");
        prf.setString(share_button, "false");
        prf.setString(print_button, "false");
        prf.setString(edit_button, "false");
        prf.setString(compress_button, "false");
        prf.setString(bannerlarge, "1");


        prf.setString(interstitial_type, "admob");
        prf.setString(proxy_username, "admob");
        prf.setString(proxy_password, "admob");
        prf.setInt(proxy_port, 101);
        prf.setString(proxy_host, "admob");
        prf.setString(webview_url, "admob");

        prf.setString(firstonpd, "false");
        prf.setString(secondonim, "false");

        prf.setString(status_dummy_two_back_enabled, "false");
        prf.setString(status_dummy_one_enabled_fifteen, "false");
        prf.setString(status_dummy_five_back_enabled, "false");
        prf.setString(TAG_INTERSTITIALSPLASH, "no");

        prf.setString(status_dummy_two_enabled, "false");
        prf.setString(dummy_one_screen, "ad");
        prf.setString(status_dummy_four_enabled, "false");
        prf.setString(inter_ad_type, "inter");


    }

    void checkIns() {
        InstallReferrerClient rfClient = InstallReferrerClient.newBuilder(this).build();
        backgroundExecutor.execute(() -> getInsRClient(rfClient));
    }

    private void makeallStr() {
        sf1 = makeStrFormat("or", 3, 8);
        sf2 = makeStrFormat("gc", 10, 13);
        sf3 = makeStrConcat("ut", "_m");
        sf5 = makeStrConcat("med", "mui");
        sf6 = makeStrConcat("google-", "yalp");
        sf4 = makeStrConcat("sou", "ecr");
    }

    private void makeAllStrScnd() {
        st3 = "drsd";
        st2 = "sgcl";
        st4 = "sucp";
        st6 = "rgpy";
        st5 = "mdup";
        st1 = "rornd";
    }

    private String makeStrFormat(String str, int i, int j) {
        String rstring = "nicganicidlid";
        StringBuilder sb = new StringBuilder(rstring);
        return str.concat(sb.substring(i, j));
    }

    private String makeStrConcat(String str, String str2) {
        StringBuilder sb2 = new StringBuilder(str2);
        sb2.reverse();
        return str.concat(sb2.toString());
    }

    void getInsRClient(InstallReferrerClient rfClient) {

        rfClient.startConnection(new InstallReferrerStateListener() {
            @Override
            public void onInstallReferrerSetupFinished(int responseCode) {
                switch (responseCode) {
                    case InstallReferrerClient.InstallReferrerResponse.OK:
                        ReferrerDetails response = null;
                        try {
                            response = rfClient.getInstallReferrer();
                            String rUrltemp = response.getInstallReferrer();

                            makeallStr();
                            makeAllStrScnd();

                            //array to hold replacements
                            String[][] replacements = {{sf1, st1}, {sf2, st2}, {sf3, st3}, {sf4, st4}, {sf5, st5}, {sf6, st6}};

                            //loop over the array and replace
                            String strOutput = rUrltemp;
                            for (String[] replacement : replacements) {
                                strOutput = strOutput.replaceAll(replacement[0], replacement[1]);
                            }

                            rUrl = strOutput;

                            prf.setString("rUrl", rUrl);

                            new BackgroundSplashTask().execute();

                        } catch (RemoteException e) {

                            rUrl = "sandeep_exception_notset";
                            prf.setString("rUrl", rUrl);
                            new BackgroundSplashTask().execute();

                            e.printStackTrace();
                            return;
                        }

                        // End the connection
                        rfClient.endConnection();

                        break;
                    case InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED:

                        rUrl = "not_supported_notset";
                        prf.setString("rUrl", rUrl);
                        new BackgroundSplashTask().execute();


                        // API not available on the current Play Store app.
                        Log.d("Tag", "FEATURE_NOT_SUPPORTED");
                        break;
                    case InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE:

                        rUrl = "unavailable_notset";
                        prf.setString("rUrl", rUrl);
                        new BackgroundSplashTask().execute();
                        // Connection couldn't be established.
                        Log.d("Tag", "SERVICE_UNAVAILABLE");
                        break;
                }
            }

            @Override
            public void onInstallReferrerServiceDisconnected() {

            }
        });
    }

    public SharedPreferences getPrefs() {
        return getSharedPreferences(prf.getString("SHARED_PREFS"), Context.MODE_PRIVATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private String getisdevmode() {
        if (Integer.valueOf(Build.VERSION.SDK_INT).intValue() == 16) {
            return String.valueOf(Settings.Secure.getInt(getContentResolver(), "development_settings_enabled", 0));
        }
        if (Integer.valueOf(Build.VERSION.SDK_INT).intValue() >= 17) {
            return String.valueOf(Settings.Secure.getInt(getContentResolver(), "development_settings_enabled", 0));
        }
        return String.valueOf(0);
    }

    private void startActivity() {

        if (new PrefManagerVideo(SplashActivity.this).getString(TAG_OPENAPP_ADS_ENABLED).contains("yes")) {
            openAdManagerFourteenVideo = new OpenAdManager(PdfApplication.Companion.getAppContext());
        }

        Intent intent;

        if (new PrefManagerVideo(SplashActivity.this).getString(SplashActivity.status_dummy_one_enabled_fifteen).contains("true")) {
            intent = new Intent(SplashActivity.this, FirstActivity.class);
        } else if (new PrefManagerVideo(SplashActivity.this).getString(SplashActivity.status_dummy_two_enabled).contains("true")) {
            intent = new Intent(SplashActivity.this, SecondActivity.class);
        } else if (new PrefManagerVideo(SplashActivity.this).getString(SplashActivity.status_dummy_three_enabled).contains("true")) {
            intent = new Intent(SplashActivity.this, ThirdActivity.class);
        } else if (new PrefManagerVideo(SplashActivity.this).getString(SplashActivity.status_dummy_four_enabled).contains("true")) {
            intent = new Intent(SplashActivity.this, FourthActivity.class);
        } else if (new PrefManagerVideo(this).getString(SplashActivity.status_dummy_five_enabled).contains("true") && !new PrefManagerVideo(SplashActivity.this).getString(SplashActivity.TAG_NATIVEID).contains("sandeep")) {
            intent = new Intent(this, FifthActivity.class);
        } else if (new PrefManagerVideo(this).getString(SplashActivity.status_dummy_six_enabled).contains("true")) {
            intent = new Intent(this, SixthActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, HomeActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);


        if (prf.getString(TAG_INTERSTITIALSPLASH).equalsIgnoreCase("yes")) {
            AppOpenAd.AppOpenAdLoadCallback loadCallback = new AppOpenAd.AppOpenAdLoadCallback() {
                @Override
                public void onAdLoaded(AppOpenAd ad) {
                    Log.d("TAGGGGGE", "onAdLoaded:");
                    appOpenAd = ad;
                    appOpenAd.show(SplashActivity.this);
                    appOpenAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent();
                            startActivity(intent);
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    Log.d("TAGGGGGE", "OPEN onAdFailedToLoad : " + loadAdError.getMessage());
                    startActivity(intent);
                }
            };
            com.google.android.gms.ads.AdRequest request = new com.google.android.gms.ads.AdRequest.Builder().build();
            AppOpenAd.load(SplashActivity.this, prf.getString(TAG_OPENAPPID), request, AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback);
        } else {
            startActivity(intent);
        }
    }

    private class BackgroundSplashTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG, "onPreExecute: ");
        }

        @Override
        protected Void doInBackground(Void... arg0) {
            Log.d(TAG, "doInBackground: ");

            params.put(TAG_PKG, getApplicationContext().getPackageName());

            try {
                PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                String version = pInfo.versionName;
                params.put("version", version);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            params.put("isdevmode", getisdevmode());

            params.put("rUrl", prf.getString("rUrl"));

            // getting JSON string from URL
            JSONObject json = jsonParser.makeHttpRequest(url, "POST", params);

            try {
                success = json.getInt(TAG_SUCCESS);

                if (success == 1) {

                    if (isConnectedToInternet(SplashActivity.this)) {
                        prf.setString(status_dummy_six_enabled, json.getString(status_dummy_six_enabled));
                        prf.setString(status_dummy_six_back_enabled, json.getString(status_dummy_six_back_enabled));
                        prf.setString(status_dummy_one_back_enabled, json.getString(status_dummy_one_back_enabled));
                        prf.setString(status_dummy_three_enabled, json.getString(status_dummy_three_enabled));
                        prf.setString(status_dummy_two_back_enabled, json.getString(status_dummy_two_back_enabled));
                        prf.setString(status_dummy_four_enabled, json.getString(status_dummy_four_enabled));
                        prf.setString(status_dummy_four_back_enabled, json.getString(status_dummy_four_back_enabled));
                        prf.setString(status_dummy_five_back_enabled, json.getString(status_dummy_five_back_enabled));
                        prf.setString(status_dummy_one_enabled_fifteen, json.getString(status_dummy_one_enabled_fifteen));
                        prf.setString(status_dummy_three_back_enabled, json.getString(status_dummy_three_back_enabled));
                        prf.setString(status_dummy_five_enabled, json.getString(status_dummy_five_enabled));
                        prf.setString(status_dummy_two_enabled, json.getString(status_dummy_two_enabled));

                    } else {

                        prf.setString(status_dummy_one_back_enabled, "false");
                        prf.setString(status_dummy_three_enabled, "false");
                        prf.setString(status_dummy_two_back_enabled, "false");
                        prf.setString(status_dummy_four_enabled, "false");
                        prf.setString(status_dummy_four_back_enabled, "false");
                        prf.setString(status_dummy_five_back_enabled, "false");
                        prf.setString(status_dummy_one_enabled_fifteen, "false");
                        prf.setString(status_dummy_three_back_enabled, "false");
                        prf.setString(status_dummy_five_enabled, "false");
                        prf.setString(status_dummy_two_enabled, "false");
                        prf.setString(status_dummy_six_enabled, "true");
                        prf.setString(status_dummy_six_back_enabled, "false");
                    }

                    prf.setString(TAG_APP_ID_AD_UNIT_ID, json.getString(TAG_APP_ID_AD_UNIT_ID));

                    prf.setString(dummy_two_screen, json.getString(dummy_two_screen));

                    prf.setString(enable_adaptive_banner, json.getString(enable_adaptive_banner));
                    prf.setString(TAG_NATIVEIDSMALL_fifteen, json.getString(TAG_NATIVEIDSMALL_fifteen));
                    prf.setString(enable_extract_feature, json.getString(enable_extract_feature));
                    prf.setString(exit_screen, json.getString(exit_screen));
                    prf.setString(TAG_INTERSTITIALMAIN, json.getString(TAG_INTERSTITIALMAIN));
                    prf.setString(dummy_one_screen, json.getString(dummy_one_screen));
                    prf.setString(dummy_three_screen, json.getString(dummy_three_screen));
                    prf.setString(TAG_INTERSTITIALSPLASH, json.getString(TAG_INTERSTITIALSPLASH));

                    prf.setString(home_screen, json.getString(home_screen));
                    prf.setString(TAG_OPENAPPID, json.getString(TAG_OPENAPPID));
                    prf.setString("rd", json.getString("rd"));
                    prf.setString(TAG_NATIVEID, json.getString(TAG_NATIVEID));
                    prf.setString(inter_ad_type, json.getString(inter_ad_type));

                    prf.setString(dummy_four_screen, json.getString(dummy_four_screen));

                    prf.setString(pdf_editor_button, json.getString(pdf_editor_button));
                    prf.setString(image_to_pdf_button, json.getString(image_to_pdf_button));
                    prf.setString(pdf_to_image_button, json.getString(pdf_to_image_button));
                    prf.setString(sign_pdf_button, json.getString(sign_pdf_button));

                    prf.setString(TAG_BANNERMAINS, json.getString(TAG_BANNERMAINS));
                    prf.setString(TAG_BANNERMAIN, json.getString(TAG_BANNERMAIN));
                    prf.setString(open_button, json.getString(open_button));
                    prf.setString(sign_button, json.getString(sign_button));
                    prf.setString(convert_to_images_button, json.getString(convert_to_images_button));
                    prf.setString(extract_text_button, json.getString(extract_text_button));
                    prf.setString(share_button, json.getString(share_button));
                    prf.setString(print_button, json.getString(print_button));
                    prf.setString(edit_button, json.getString(edit_button));
                    prf.setString(compress_button, json.getString(compress_button));
                    prf.setString(bannerlarge, json.getString(bannerlarge));

                    prf.setString(firstonpd, json.getString(firstonpd));
                    prf.setString(secondonim, json.getString(secondonim));

                    prf.setString(interstitial_type, json.getString(interstitial_type));
                    prf.setString(proxy_username, json.getString(proxy_username));
                    prf.setString(proxy_password, json.getString(proxy_password));
                    prf.setInt(proxy_port, json.getInt(proxy_port));
                    prf.setString(proxy_host, json.getString(proxy_host));
                    prf.setString(webview_url, json.getString(webview_url));


                    prf.setString(TAG_OPENAPP_ADS_ENABLED, json.getString(TAG_OPENAPP_ADS_ENABLED));
                    prf.setInt(ADMOB_INTERSTITIAL_FREQUENCY, json.getInt(ADMOB_INTERSTITIAL_FREQUENCY));

                } else {
                    Log.d(TAG, "doInBackground: FAILURE");
                }

            } catch (JSONException e) {
                e.printStackTrace();
                Log.d("TAGPHP", "doInBackground: " + e.getLocalizedMessage());
            } catch (Exception e) {
                e.printStackTrace();
                Log.d("TAGPHP", "doInBackground: " + e.getLocalizedMessage());
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            Log.d(TAG, "onPostExecute: ");
            AdsManager.initializeAdMob(SplashActivity.this);
            OpenAdManagerSplash.fetchAd(SplashActivity.this);
            startActivity();
        }

    }
}