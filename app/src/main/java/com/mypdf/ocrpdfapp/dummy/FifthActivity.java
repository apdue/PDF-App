package com.mypdf.ocrpdfapp.dummy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.mypdf.ocrpdfapp.R;
import com.mypdf.ocrpdfapp.screens.HomeActivity;
import com.mypdf.ocrpdfapp.screens.SplashActivity;
import com.mypdf.ocrpdfapp.util.AdsManager;
import com.mypdf.ocrpdfapp.util.PrefManagerVideo;

public class FifthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_five);

        if (new PrefManagerVideo(this).getString(SplashActivity.dummy_one_screen).contains("ad")) {
            AdsManager.showAndLoadNativeAd(this, findViewById(R.id.nativeAd), 3);
        }

        onClicks();
    }

    private void onClicks() {
        findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity();
            }
        });
    }

    private void startActivity() {
        Intent intent;

        if (new PrefManagerVideo(this).getString(SplashActivity.status_dummy_six_enabled).contains("true")) {
            intent = new Intent(this, SixthActivity.class);
        } else {
            intent = new Intent(this, HomeActivity.class);
        }
        AdsManager.showInterstitialAd(this, new AdsManager.AdFinished() {

            @Override
            public void onAdFinished() {
                startActivity(intent);
            }
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent;
        if (new PrefManagerVideo(this).getString(SplashActivity.status_dummy_four_back_enabled).contains("true")) {
            intent = new Intent(this, FourthActivity.class);
            AdsManager.showInterstitialAd(this, new AdsManager.AdFinished() {
                @Override
                public void onAdFinished() {
                    startActivity(intent);
                }
            });
        } else if (new PrefManagerVideo(this).getString(SplashActivity.status_dummy_three_back_enabled).contains("true")) {
            intent = new Intent(this, ThirdActivity.class);

            AdsManager.showInterstitialAd(this, new AdsManager.AdFinished() {
                @Override
                public void onAdFinished() {
                    startActivity(intent);
                }
            });
        } else if (new PrefManagerVideo(this).getString(SplashActivity.status_dummy_two_back_enabled).contains("true")) {
            intent = new Intent(this, SecondActivity.class);

            AdsManager.showInterstitialAd(this, new AdsManager.AdFinished() {
                @Override
                public void onAdFinished() {
                    startActivity(intent);
                }
            });
        } else if (new PrefManagerVideo(this).getString(SplashActivity.status_dummy_one_back_enabled).contains("true")) {
            intent = new Intent(this, FirstActivity.class);
            AdsManager.showInterstitialAd(this, new AdsManager.AdFinished() {
                @Override
                public void onAdFinished() {
                    startActivity(intent);
                }
            });
        } else {
            finishAffinity();
        }

    }

}