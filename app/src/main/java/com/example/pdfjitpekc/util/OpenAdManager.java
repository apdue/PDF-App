package com.example.pdfjitpekc.util;

import static androidx.lifecycle.Lifecycle.Event.ON_START;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import com.example.pdfjitpekc.PdfApplication;
import com.example.pdfjitpekc.screens.SplashActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.appopen.AppOpenAd;

import java.util.Date;

public class OpenAdManager implements LifecycleObserver, Application.ActivityLifecycleCallbacks {

    public static PrefManagerVideo prf;
    private static final String LOG_TAG = "AppOpenManager";
    public static AppOpenAd appOpenAd = null;
    private AppOpenAd.AppOpenAdLoadCallback loadCallback;
    private final PdfApplication application;
    private Activity currentActivity;
    private static boolean isShowingAd = false;
    private long loadTime = 0;

    public OpenAdManager(PdfApplication application) {
        this.application = application;
        prf = new PrefManagerVideo(application);
        this.application.registerActivityLifecycleCallbacks(this);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        Log.d("TAGOPEN", "AppOpenManagerFonts: ");

        fetchAd();
    }

    @OnLifecycleEvent(ON_START)
    public void onStart() {
            showAdIfAvailable();
            Log.d("TAGOPEN", "onStart: ");
    }

    public void fetchAd() {
        // Have unused ad, no need to fetch another.
        if (isAdAvailable()) {
            return;
        }

        loadCallback =
                new AppOpenAd.AppOpenAdLoadCallback() {

                    @Override
                    public void onAdLoaded(AppOpenAd ad) {
                        OpenAdManager.this.appOpenAd = ad;
                        OpenAdManager.this.loadTime = (new Date()).getTime();
                        Log.d("TAGOPEN", "onAdLoaded: ");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        // Handle the error.
                        Log.d("TAGOPEN", "onAdFailedToLoad: "+loadAdError.getMessage());

                    }

                };

        if (SplashActivity.isConnectedToInternet(application)
                && !new PrefManagerVideo(application).getString(SplashActivity.TAG_OPENAPPID).contains("sandeep")) {
            AdRequest request = getAdRequest();
            AppOpenAd.load(
                    application, prf.getString(SplashActivity.TAG_OPENAPPID), request,
                    AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback);
        }

    }

    /** Creates and returns ad request. */
    private AdRequest getAdRequest() {
        return new AdRequest.Builder().build();
    }

    /** Utility method to check if ad was loaded more than n hours ago. */
    private boolean wasLoadTimeLessThanNHoursAgo(long numHours) {
        long dateDifference = (new Date()).getTime() - this.loadTime;
        long numMilliSecondsPerHour = 3600000;
        return (dateDifference < (numMilliSecondsPerHour * numHours));
    }

    /** Utility method that checks if ad exists and can be shown. */
    public boolean isAdAvailable() {
        return appOpenAd != null && wasLoadTimeLessThanNHoursAgo(4);
    }

    /** Shows the ad if one isn't already showing. */
    public void showAdIfAvailable() {
        // Only show ad if there is not already an app open ad currently showing
        // and an ad is available.
        if (!isShowingAd && isAdAvailable()) {
            System.out.println("Rajan_openapp_Will show ad.");

            FullScreenContentCallback fullScreenContentCallback =
                    new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            // Set the reference to null so isAdAvailable() returns false.
                            OpenAdManager.this.appOpenAd = null;
                            isShowingAd = false;
                            fetchAd();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(AdError adError) {}

                        @Override
                        public void onAdShowedFullScreenContent() {
                            isShowingAd = true;
                        }
                    };

            appOpenAd.setFullScreenContentCallback(fullScreenContentCallback);
            appOpenAd.show(currentActivity);

        } else {
            Log.d(LOG_TAG, "Can not show ad.");
            fetchAd();
        }
    }

    /** ActivityLifecycleCallback methods */
    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(Activity activity) {
        currentActivity = activity;
    }

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivityPaused(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {}

    @Override
    public void onActivityDestroyed(Activity activity) {
        currentActivity = null;
    }

}

