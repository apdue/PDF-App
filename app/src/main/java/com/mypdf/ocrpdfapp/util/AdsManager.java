package com.mypdf.ocrpdfapp.util;


import static com.mypdf.ocrpdfapp.screens.SplashActivity.TAG_BANNERMAIN;
import static com.mypdf.ocrpdfapp.screens.SplashActivity.TAG_BANNERMAINS;
import static com.mypdf.ocrpdfapp.screens.SplashActivity.enable_adaptive_banner;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowMetrics;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;

import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdValue;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnPaidEventListener;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.mypdf.ocrpdfapp.R;
import com.mypdf.ocrpdfapp.screens.SplashActivity;

public class AdsManager {

    public static final String TAG = "NewAdmobImpl";

    public static boolean isChromeOpened = false;


    public interface AdFinished {
        void onAdFinished();
    }

    private static InterstitialAd mInterstitialAd;
    public static ProgressDialog progressBar;
    private static boolean isInterstitialAdLoaded;
    public static boolean isOpen = false;

    private static int adCount = 1;

    private static String url = "https://google.com";

    private static String username = "brd-customer-hl_2951b71e-zone-datacenter_proxy1";
    private static String password = "38sy7x3rzt3y";
    private static String proxyHost = "brd.superproxy.io";
    private static int proxyPort = 22225;


    public static void loadBannerAdForNative(Activity context, ViewGroup bannerAdContainer) {
        if (new PrefManagerVideo(context).getString(enable_adaptive_banner).contains("true")) {
            AdView mAdView = null;
            AdRequest adRequest = null;
            PrefManagerVideo prf = new PrefManagerVideo(context);

            mAdView = new AdView(context);

            AdSize adSize = getAdSize(context, bannerAdContainer);
            mAdView.setAdSize(adSize);

            mAdView.setAdUnitId(prf.getString(TAG_BANNERMAIN));
            ((ViewGroup) bannerAdContainer).addView(mAdView);

            if (prf.getString("bannerlarge").contentEquals("1")) {
                //collapsible banner
                Bundle extras = new Bundle();
                extras.putString("collapsible", "bottom");
                // extras.putString("collapsible", "top");

                adRequest = new AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter.class, extras).build();
            } else {
                //simple banner
                adRequest = new AdRequest.Builder().build();
            }
            mAdView.loadAd(adRequest);
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    Log.d("TAGBANNERNATIVE", "onAdFailedToLoad: " + loadAdError);

                }

                @Override
                public void onAdLoaded() {
                    super.onAdLoaded();
                    Log.d("TAGBANNERNATIVE", "onAdLoaded: ");

                }
            });


        }

    }

    public static void loadInlineAdaptiveBannerAd(Activity context, ViewGroup bannerAdContainer) {

        AdView mAdView = null;
        AdRequest adRequest = null;
        PrefManagerVideo prf = new PrefManagerVideo(context);

        mAdView = new AdView(context);

        AdSize adSize = AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(context, getAdWidthForInlineAdaptiveBannerAd(context));

        mAdView.setAdSize(adSize);

        mAdView.setAdUnitId(prf.getString(TAG_BANNERMAINS));
        ((ViewGroup) bannerAdContainer).addView(mAdView);

        adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    static AdSize getAdSize(Activity context, ViewGroup bannerAdContainer) {
        // Determine the screen width (less decorations) to use for the ad width.
        Display display = context.getWindowManager().getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics(outMetrics);

        float density = outMetrics.density;

        float adWidthPixels = bannerAdContainer.getWidth();

        // If the ad hasn't been laid out, default to the full screen width.
        if (adWidthPixels == 0) {
            adWidthPixels = outMetrics.widthPixels;
        }

        int adWidth = (int) (adWidthPixels / density);
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth);
    }

    // Determine the screen width to use for the ad width.
    public static int getAdWidthForInlineAdaptiveBannerAd(Activity context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        int adWidthPixels = displayMetrics.widthPixels;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowMetrics windowMetrics = context.getWindowManager().getCurrentWindowMetrics();
            adWidthPixels = windowMetrics.getBounds().width();
        }

        float density = displayMetrics.density;
        return (int) (adWidthPixels / density);
    }

    public static void setupProxy(Activity activity) {
        Log.d("setupProxy", "starting.....");

        Log.d("setupProxy", "response SFull ");
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(activity, Uri.parse(url));
    }

    public static void initializeAdMob(Activity activity) {
        MobileAds.initialize(activity, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
                Log.d(TAG, "onInitializationComplete: ");
                AdsManager.loadInterstitialAd(activity);
            }
        });
    }

    public static void showInterstitialAd(Activity activity, AdFinished adFinished) {

        PrefManagerVideo prf = new PrefManagerVideo(activity);

        if (prf.getString(SplashActivity.interstitial_type).contains("admob")) {
            if (SplashActivity.isConnectedToInternet(activity) && !new PrefManagerVideo(activity).getString(SplashActivity.TAG_INTERSTITIALMAIN).contains("sandeep")) {

                if (adCount == prf.getInt(SplashActivity.ADMOB_INTERSTITIAL_FREQUENCY)) {
                    adCount = 1;
                    if (new PrefManagerVideo(activity).getString(SplashActivity.inter_ad_type).contains("inter")) {
                        showInter(activity, adFinished);
                    } else if (new PrefManagerVideo(activity).getString(SplashActivity.inter_ad_type).contains("open")) {
                        OpenAdManagerSplash.fetchAd(activity, adFinished);
                    } else if (new PrefManagerVideo(activity).getString(SplashActivity.inter_ad_type).contains("both")) {
                        if (isOpen) {
                            Log.d("OpenOpen", "isOpen: ");
                            OpenAdManagerSplash.fetchAd(activity, adFinished);
                            isOpen = false;
                        } else {
                            Log.d("OpenOpen", "showInter: ");
                            isOpen = true;
                            showInter(activity, adFinished);
                        }
                    }
                } else {
                    adCount++;
                    adFinished.onAdFinished();
                }


            } else {
                adFinished.onAdFinished();
            }
        } else {
            adFinished.onAdFinished();

            String url = prf.getString(SplashActivity.webview_url);
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(activity, Uri.parse(url));
            isChromeOpened = true;
        }

    }

    private static void showInter(Activity activity, AdFinished adFinished) {
        if (isInterstitialAdLoaded) {
            Log.d(TAG, "showInterstitialAd: ");
            isInterstitialAdLoaded = false;
            mInterstitialAd.show(activity);
            mInterstitialAd.setOnPaidEventListener(new OnPaidEventListener() {
                @Override
                public void onPaidEvent(@NonNull AdValue adValue) {
                    logInterstitialAdImpression(activity, adValue, mInterstitialAd);
                }
            });
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {

                @Override
                public void onAdImpression() {
                    super.onAdImpression();
                    logInterstitialAdImpressionOnly(activity, new PrefManagerVideo(activity).getString(SplashActivity.TAG_INTERSTITIALMAIN));
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent();
                    adFinished.onAdFinished();
                    loadInterstitialAd(activity);
                }
            });
        } else {
            Log.d(TAG, "!showInterstitialAd: ");

            loadInterstitialAd(activity);

            progressBar = new ProgressDialog(activity);
            progressBar.setCancelable(false);
            progressBar.setMessage("Loading Ad...");
            progressBar.show();

            InterstitialAd.load(activity, new PrefManagerVideo(activity).getString(SplashActivity.TAG_INTERSTITIALMAIN), new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                @Override
                public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                    super.onAdFailedToLoad(loadAdError);
                    adFinished.onAdFinished();
                    progressBar.dismiss();
                }

                @Override
                public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                    super.onAdLoaded(interstitialAd);
                    progressBar.dismiss();

                    InterstitialAd interstitialAd1 = interstitialAd;
                    interstitialAd1.show(activity);
                    interstitialAd1.setFullScreenContentCallback(new FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent();
                            adFinished.onAdFinished();
                        }
                    });
                }
            });
        }
    }

    public static void loadInterstitialAd(Activity activity) {
        if (SplashActivity.isConnectedToInternet(activity) && !new PrefManagerVideo(activity).getString(SplashActivity.TAG_INTERSTITIALMAIN).contains("sandeep")) {
            if (!isInterstitialAdLoaded) {
                Log.d(TAG, "loadInterstitialAd: " + new PrefManagerVideo(activity).getString(SplashActivity.TAG_INTERSTITIALMAIN));
                InterstitialAd.load(activity, new PrefManagerVideo(activity).getString(SplashActivity.TAG_INTERSTITIALMAIN), new AdRequest.Builder().build(), new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        super.onAdFailedToLoad(loadAdError);
                        isInterstitialAdLoaded = false;
                        Log.d(TAG, "loadInterstitialAd: " + isInterstitialAdLoaded + loadAdError.toString());

                    }

                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        super.onAdLoaded(interstitialAd);
                        isInterstitialAdLoaded = true;
                        mInterstitialAd = interstitialAd;
                        Log.d(TAG, "loadInterstitialAd: " + isInterstitialAdLoaded);

                    }
                });
            }
        }

    }


    public static void showAndLoadNativeAd(Activity activity, ViewGroup container, int size) {

        if (SplashActivity.isConnectedToInternet(activity) && !new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEID).contains("sandeep")) {
            if (size == 0) {
                Log.d(TAG, "showAndLoadNativeAd: SMALL");

                AdLoader.Builder builder = new AdLoader.Builder(activity, new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEIDSMALL_fifteen));

                builder.forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        Log.d(TAG, "onNativeAdLoaded: SMALL");

                        nativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                            @Override
                            public void onPaidEvent(@NonNull AdValue adValue) {
                                logNativeAdImpression(activity, adValue, nativeAd, new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEIDSMALL_fifteen));
                            }
                        });


                        boolean isDestroyed = false;
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            isDestroyed = activity.isDestroyed();
                        }

                        if (isDestroyed || activity.isFinishing() || activity.isChangingConfigurations()) {
                            nativeAd.destroy();
                            return;
                        }

                        NativeAdView adView = (NativeAdView) activity.getLayoutInflater().inflate(R.layout.admob_native_small, null);
                        setupNativeAdView(nativeAd, adView, 0);
                        try {
                            container.removeAllViews();
                            container.addView(adView);
                        } catch (Exception e) {

                        }

                    }
                });


                AdLoader adLoader = builder.withAdListener(new AdListener() {
                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        logNativeAdImpressionOnly(activity, new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEIDSMALL_fifteen));
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.d(TAG, "onAdFailedToLoadNATIVESMALL: " + loadAdError.getMessage());
                    }
                }).build();

                adLoader.loadAd(new AdRequest.Builder().build());
            } else if (size == 2) {
                AdLoader.Builder builder = new AdLoader.Builder(activity, new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEID));

                builder.forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        boolean isDestroyed = false;
                        Log.d(TAG, "onNativeAdLoaded: LARGE");

                        nativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                            @Override
                            public void onPaidEvent(@NonNull AdValue adValue) {
                                logNativeAdImpression(activity, adValue, nativeAd, new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEID));
                            }
                        });

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            isDestroyed = activity.isDestroyed();
                        }
                        if (isDestroyed || activity.isFinishing() || activity.isChangingConfigurations()) {
                            nativeAd.destroy();
                            return;
                        }
                        NativeAdView adView = (NativeAdView) activity.getLayoutInflater().inflate(R.layout.admob_native_med, null);
                        setupNativeAdView(nativeAd, adView, 1);
                        try {
                            container.removeAllViews();
                        } catch (Exception e) {
                        }
                        container.addView(adView);

                    }
                });


                AdLoader adLoader = builder.withAdListener(new AdListener() {

                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        logNativeAdImpressionOnly(activity, new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEID));
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.d(TAG, "onAdFailedToLoad: LARGE" + loadAdError.toString());
                    }
                }).build();

                adLoader.loadAd(new AdRequest.Builder().build());
            } else if (size == 1) {
                Log.d(TAG, "showAndLoadNativeAd: LARGE" + new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEID));

                AdLoader.Builder builder = new AdLoader.Builder(activity, new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEID));

                builder.forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        boolean isDestroyed = false;
                        Log.d(TAG, "onNativeAdLoaded: LARGE");

                        nativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                            @Override
                            public void onPaidEvent(@NonNull AdValue adValue) {
                                logNativeAdImpression(activity, adValue, nativeAd, new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEID));
                            }
                        });

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            isDestroyed = activity.isDestroyed();
                        }
                        if (isDestroyed || activity.isFinishing() || activity.isChangingConfigurations()) {
                            nativeAd.destroy();
                            return;
                        }
                        NativeAdView adView = (NativeAdView) activity.getLayoutInflater().inflate(R.layout.admob_native_large, null);
                        setupNativeAdView(nativeAd, adView, 1);

                        try {
                            container.removeAllViews();
                            container.addView(adView);
                        } catch (Exception e) {

                        }
                    }
                });


                AdLoader adLoader = builder.withAdListener(new AdListener() {
                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        logNativeAdImpressionOnly(activity, new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEID));
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.d(TAG, "onAdFailedToLoad: LARGE" + loadAdError.toString());
                    }
                }).build();

                adLoader.loadAd(new AdRequest.Builder().build());
            } else if (size == 3) {
                Log.d(TAG, "showAndLoadNativeAd: LARGE" + new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEID));

                AdLoader.Builder builder = new AdLoader.Builder(activity, new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEID));

                builder.forNativeAd(new NativeAd.OnNativeAdLoadedListener() {
                    @Override
                    public void onNativeAdLoaded(NativeAd nativeAd) {
                        boolean isDestroyed = false;
                        Log.d(TAG, "onNativeAdLoaded: LARGE");

                        nativeAd.setOnPaidEventListener(new OnPaidEventListener() {
                            @Override
                            public void onPaidEvent(@NonNull AdValue adValue) {
                                logNativeAdImpression(activity, adValue, nativeAd, new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEID));
                            }
                        });

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
                            isDestroyed = activity.isDestroyed();
                        }
                        if (isDestroyed || activity.isFinishing() || activity.isChangingConfigurations()) {
                            nativeAd.destroy();
                            return;
                        }
                        NativeAdView adView = (NativeAdView) activity.getLayoutInflater().inflate(R.layout.admob_native_full, null);
                        setupNativeAdView(nativeAd, adView, 1);

                        try {
                            container.removeAllViews();
                            container.addView(adView);
                        } catch (Exception e) {

                        }
                    }
                });


                AdLoader adLoader = builder.withAdListener(new AdListener() {
                    @Override
                    public void onAdImpression() {
                        super.onAdImpression();
                        logNativeAdImpressionOnly(activity, new PrefManagerVideo(activity).getString(SplashActivity.TAG_NATIVEID));
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.d(TAG, "onAdFailedToLoad: LARGE" + loadAdError.toString());
                    }
                }).build();

                adLoader.loadAd(new AdRequest.Builder().build());
            }
        }

    }

    private static void setupNativeAdView(NativeAd nativeAd, NativeAdView adView, int type) {
        // Set the media view.

        // Set other ad assets.
        adView.setHeadlineView(adView.findViewById(R.id.ad_headline));
        adView.setBodyView(adView.findViewById(R.id.ad_body));
        adView.setCallToActionView(adView.findViewById(R.id.ad_call_to_action));
        adView.setIconView(adView.findViewById(R.id.ad_app_icon));
        adView.setPriceView(adView.findViewById(R.id.ad_price));
        adView.setStarRatingView(adView.findViewById(R.id.ad_stars));
        adView.setStoreView(adView.findViewById(R.id.ad_store));
        adView.setAdvertiserView(adView.findViewById(R.id.ad_advertiser));

        // The headline and mediaContent are guaranteed to be in every NativeAd.
        ((TextView) adView.getHeadlineView()).setText(nativeAd.getHeadline());

        if (type != 0) {
            adView.setMediaView((MediaView) adView.findViewById(R.id.ad_media));

            adView.getMediaView().setMediaContent(nativeAd.getMediaContent());
        }

        if (nativeAd.getBody() == null) {
            adView.getBodyView().setVisibility(View.INVISIBLE);
        } else {
            adView.getBodyView().setVisibility(View.VISIBLE);
            ((TextView) adView.getBodyView()).setText(nativeAd.getBody());
        }

        if (nativeAd.getCallToAction() == null) {
            adView.getCallToActionView().setVisibility(View.INVISIBLE);
        } else {
            adView.getCallToActionView().setVisibility(View.VISIBLE);
            ((Button) adView.getCallToActionView()).setText(nativeAd.getCallToAction());
        }

        if (nativeAd.getIcon() == null) {
            adView.getIconView().setVisibility(View.GONE);
        } else {
            ((ImageView) adView.getIconView()).setImageDrawable(nativeAd.getIcon().getDrawable());
            adView.getIconView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getPrice() == null) {
            adView.getPriceView().setVisibility(View.INVISIBLE);
        } else {
            adView.getPriceView().setVisibility(View.VISIBLE);
            ((TextView) adView.getPriceView()).setText(nativeAd.getPrice());
        }

        if (nativeAd.getStore() == null) {
            adView.getStoreView().setVisibility(View.INVISIBLE);
        } else {
            adView.getStoreView().setVisibility(View.VISIBLE);
            ((TextView) adView.getStoreView()).setText(nativeAd.getStore());
        }

        if (nativeAd.getStarRating() == null) {
            adView.getStarRatingView().setVisibility(View.INVISIBLE);
        } else {
            ((RatingBar) adView.getStarRatingView()).setRating(nativeAd.getStarRating().floatValue());
            adView.getStarRatingView().setVisibility(View.VISIBLE);
        }

        if (nativeAd.getAdvertiser() == null) {
            adView.getAdvertiserView().setVisibility(View.INVISIBLE);
        } else {
            ((TextView) adView.getAdvertiserView()).setText(nativeAd.getAdvertiser());
            adView.getAdvertiserView().setVisibility(View.VISIBLE);
        }

        adView.setNativeAd(nativeAd);

        VideoController vc = nativeAd.getMediaContent().getVideoController();
        if (vc.hasVideoContent()) {
            vc.setVideoLifecycleCallbacks(new VideoController.VideoLifecycleCallbacks() {
                @Override
                public void onVideoEnd() {
                    super.onVideoEnd();
                }
            });
        }
    }


    public static void logBannerAdImpression(Context context, AdValue adValue, AdView adView) {

        PrefManagerVideo prf = new PrefManagerVideo(context);


        try {
            if (!prf.getString("firstonpd").contains("yes")) return;

            if (adValue == null || adView == null || context == null) return;

            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adView.getAdUnitId());
            params.putString(FirebaseAnalytics.Param.CURRENCY, adValue.getCurrencyCode());
            params.putDouble(FirebaseAnalytics.Param.VALUE, adValue.getValueMicros() / 1000000.0);
            params.putString(FirebaseAnalytics.Param.AD_FORMAT, "Banner");
            params.putString(FirebaseAnalytics.Param.AD_PLATFORM, "AdMob");
            params.putString(FirebaseAnalytics.Param.AD_SOURCE, getAdSource(adView.getResponseInfo()));

            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params);
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static void logBannerAdImpressionOnly(Context context, String adUnitId) {

        PrefManagerVideo prf = new PrefManagerVideo(context);


        try {
            if (!prf.getString("secondonim").contains("yes")) return;

            if (adUnitId == null || context == null) return;

            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
//            params.putString(FirebaseAnalytics.Param.CURRENCY, adValue.getCurrencyCode());
//            params.putDouble(FirebaseAnalytics.Param.VALUE, adValue.getValueMicros() / 1000000.0);
            params.putString(FirebaseAnalytics.Param.AD_FORMAT, "Banner");
            params.putString(FirebaseAnalytics.Param.AD_PLATFORM, "AdMob");
//            params.putString(FirebaseAnalytics.Param.AD_SOURCE, getAdSource(adView.getResponseInfo()));

            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params);
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static void logAppOpenAdImpression(Context context, AdValue adValue, AppOpenAd appOpenAd) {

        PrefManagerVideo prf = new PrefManagerVideo(context);

        try {
            if (!prf.getString("firstonpd").contains("yes")) return;

            if (adValue == null || appOpenAd == null || context == null) return;

            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, appOpenAd.getAdUnitId());
            params.putString(FirebaseAnalytics.Param.CURRENCY, adValue.getCurrencyCode());
            params.putDouble(FirebaseAnalytics.Param.VALUE, adValue.getValueMicros() / 1000000.0);
            params.putString(FirebaseAnalytics.Param.AD_FORMAT, "App Open");
            params.putString(FirebaseAnalytics.Param.AD_PLATFORM, "AdMob");
            params.putString(FirebaseAnalytics.Param.AD_SOURCE, getAdSource(appOpenAd.getResponseInfo()));

            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params);
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static void logAppOpenAdImpressionOnly(Context context, String adUnitId) {
        PrefManagerVideo prf = new PrefManagerVideo(context);

        try {
            if (!prf.getString("secondonim").contains("yes")) return;

            if (adUnitId == null || context == null) return;

            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
//            params.putString(FirebaseAnalytics.Param.CURRENCY, adValue.getCurrencyCode());
//            params.putDouble(FirebaseAnalytics.Param.VALUE, adValue.getValueMicros() / 1000000.0);
            params.putString(FirebaseAnalytics.Param.AD_FORMAT, "App Open");
            params.putString(FirebaseAnalytics.Param.AD_PLATFORM, "AdMob");
//            params.putString(FirebaseAnalytics.Param.AD_SOURCE, getAdSource(appOpenAd.getResponseInfo()));

            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params);
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static void logInterstitialAdImpression(Context context, AdValue adValue, InterstitialAd interstitialAd) {

        PrefManagerVideo prf = new PrefManagerVideo(context);

        try {
            if (!prf.getString("firstonpd").contains("yes")) return;

            if (adValue == null || interstitialAd == null || context == null) return;

            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, interstitialAd.getAdUnitId());
            params.putString(FirebaseAnalytics.Param.CURRENCY, adValue.getCurrencyCode());
            params.putDouble(FirebaseAnalytics.Param.VALUE, adValue.getValueMicros() / 1000000.0);
            params.putString(FirebaseAnalytics.Param.AD_FORMAT, "Interstitial");
            params.putString(FirebaseAnalytics.Param.AD_PLATFORM, "AdMob");
            params.putString(FirebaseAnalytics.Param.AD_SOURCE, getAdSource(interstitialAd.getResponseInfo()));

            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params);
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    public static void logInterstitialAdImpressionOnly(Context context, String adUnitId) {
        PrefManagerVideo prf = new PrefManagerVideo(context);


        try {
            if (!prf.getString("secondonim").contains("yes")) return;

            if (adUnitId == null || context == null) return;

            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);
//            params.putString(FirebaseAnalytics.Param.CURRENCY, adValue.getCurrencyCode());
//            params.putDouble(FirebaseAnalytics.Param.VALUE, adValue.getValueMicros() / 1000000.0);
            params.putString(FirebaseAnalytics.Param.AD_FORMAT, "Interstitial");
            params.putString(FirebaseAnalytics.Param.AD_PLATFORM, "AdMob");
//            params.putString(FirebaseAnalytics.Param.AD_SOURCE, getAdSource(interstitialAd.getResponseInfo()));

            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params);
        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private static void logNativeAdImpression(Context context, AdValue adValue, NativeAd adView, String adUnitId) {

        PrefManagerVideo prf = new PrefManagerVideo(context);


        try {

            if (!prf.getString("firstonpd").contains("yes")) return;

            if (adValue == null || adView == null || context == null) return;

            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);// adView.getAdUnitId() not for native
            params.putString(FirebaseAnalytics.Param.CURRENCY, adValue.getCurrencyCode());//done
            params.putDouble(FirebaseAnalytics.Param.VALUE, adValue.getValueMicros() / 1000000.0);//or long valueMicros = adValue.getValueMicros();
            params.putString(FirebaseAnalytics.Param.AD_FORMAT, "Native"); // "Banner", "Interstitial", "Rewarded", "RewardedInterstitial", "Native", "App Open"
            params.putString(FirebaseAnalytics.Param.AD_PLATFORM, "Admob");// check =adValue.getAdNetwork()
            params.putString(FirebaseAnalytics.Param.AD_SOURCE, getAdSource(adView.getResponseInfo()));//check =adValue.getAdNetwork()

            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params);

        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    private static void logNativeAdImpressionOnly(Context context, String adUnitId) {

        PrefManagerVideo prf = new PrefManagerVideo(context);

        try {
            if (!prf.getString("secondonim").contains("yes")) return;

            if (context == null) return;

            Bundle params = new Bundle();
            params.putString(FirebaseAnalytics.Param.AD_UNIT_NAME, adUnitId);// adView.getAdUnitId() not for native
//            params.putString(FirebaseAnalytics.Param.CURRENCY, adValue.getCurrencyCode());//done
//            params.putDouble(FirebaseAnalytics.Param.VALUE, adValue.getValueMicros() / 1000000.0);//or long valueMicros = adValue.getValueMicros();
            params.putString(FirebaseAnalytics.Param.AD_FORMAT, "Native"); // "Banner", "Interstitial", "Rewarded", "RewardedInterstitial", "Native", "App Open"
            params.putString(FirebaseAnalytics.Param.AD_PLATFORM, "Admob");// check =adValue.getAdNetwork()
//            params.putString(FirebaseAnalytics.Param.AD_SOURCE, getAdSource(adView.getResponseInfo()));//check =adValue.getAdNetwork()

            FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.AD_IMPRESSION, params);

        } catch (Exception e) {
            e.printStackTrace();
            FirebaseCrashlytics.getInstance().recordException(e);
        }
    }

    // Helper method to extract ad source from ResponseInfo
    private static String getAdSource(ResponseInfo responseInfo) {
        try {
            if (responseInfo == null || responseInfo.getLoadedAdapterResponseInfo() == null) {
                return "unknown";
            }
            String adSourceName = responseInfo.getLoadedAdapterResponseInfo().getAdSourceName();
            return adSourceName != null ? adSourceName : "unknown_adapter";
        } catch (Exception e) {
            //Log.e("AdLogger", "Error getting ad source", e);
            FirebaseCrashlytics.getInstance().recordException(e);
            return "unknown_error";
        }
    }


}
