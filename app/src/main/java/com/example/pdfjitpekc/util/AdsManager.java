package com.example.pdfjitpekc.util;


import android.app.Activity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;

import com.example.pdfjitpekc.R;
import com.example.pdfjitpekc.screens.SplashActivity;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.VideoController;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAdView;
public class AdsManager {

    public static final String TAG = "NewAdmobImpl";


    public static boolean isChromeOpened  = false;


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

    public static void setupProxy(Activity activity) {
        Log.d("setupProxy", "starting.....");
//        if (response.isSuccessful()) {
            Log.d("setupProxy", "response SFull ");
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(activity, Uri.parse(url));
//        } else {
//            Toast.makeText(activity, "Failed to open URL. Please check your proxy settings and try again.", Toast.LENGTH_LONG).show();
//        }

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
            mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
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
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.d(TAG, "onAdFailedToLoad: LARGE" + loadAdError.toString());
                        container.setVisibility(View.GONE);
                    }
                }).build();

                adLoader.loadAd(new AdRequest.Builder().build());
            }
        } else {
            container.setVisibility(View.GONE);
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

}
