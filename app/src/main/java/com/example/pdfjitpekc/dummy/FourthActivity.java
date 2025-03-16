package com.example.pdfjitpekc.dummy;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.example.pdfjitpekc.R;
import com.example.pdfjitpekc.screens.HomeActivity;
import com.example.pdfjitpekc.screens.MainActivity;
import com.example.pdfjitpekc.screens.SplashActivity;
import com.example.pdfjitpekc.util.AdsManager;
import com.example.pdfjitpekc.util.PrefManagerVideo;

public class FourthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_four);

        if (new PrefManagerVideo(this).getString(SplashActivity.dummy_four_screen).contains("ad")) {
            AdsManager.showAndLoadNativeAd(this, findViewById(R.id.nativeAd), 1);
        }

        AdsManager.showAndLoadNativeAd(this, findViewById(R.id.nativeLayoutSmaller), 0);

    }

    public void CLICK(View view) {
        startActivity();
    }

    private void startActivity() {
        Intent intent;

        if (new PrefManagerVideo(this).getString(SplashActivity.status_dummy_five_enabled).contains("true")) {
            intent = new Intent(this, FifthActivity.class);
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
        if (new PrefManagerVideo(this).getString(SplashActivity.status_dummy_three_back_enabled).contains("true")) {
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