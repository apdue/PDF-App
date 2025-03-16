package com.example.pdfjitpekc.dummy;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.example.pdfjitpekc.R;
import com.example.pdfjitpekc.screens.HomeActivity;
import com.example.pdfjitpekc.screens.MainActivity;
import com.example.pdfjitpekc.screens.SplashActivity;
import com.example.pdfjitpekc.util.AdsManager;
import com.example.pdfjitpekc.util.PrefManagerVideo;

public class FirstActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen_one);
        RelativeLayout rl = (RelativeLayout) findViewById(R.id.rlCustomAd);

        if (new PrefManagerVideo(this).getString(SplashActivity.dummy_one_screen).contains("ad")){
            rl.setVisibility(View.GONE);
            AdsManager.showAndLoadNativeAd(this, findViewById(R.id.nativeAd), 1);
            AdsManager.showAndLoadNativeAd(this, findViewById(R.id.nativeLayoutSmaller), 0);
        } else {
            rl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = new PrefManagerVideo(FirstActivity.this).getString(SplashActivity.webview_url);
                    CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
                    CustomTabsIntent customTabsIntent = builder.build();
                    customTabsIntent.launchUrl(FirstActivity.this, Uri.parse(url));
                }
            });
        }

        findViewById(R.id.btnNext).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity();
            }
        });

    }

    private void startActivity(){
        Intent intent;
        if (new PrefManagerVideo(FirstActivity.this).getString(SplashActivity.status_dummy_two_enabled).contains("true")) {
            intent = new Intent(FirstActivity.this, SecondActivity.class);
        } else if (new PrefManagerVideo(FirstActivity.this).getString(SplashActivity.status_dummy_three_enabled).contains("true")) {
            intent = new Intent(FirstActivity.this, ThirdActivity.class);
        } else if (new PrefManagerVideo(FirstActivity.this).getString(SplashActivity.status_dummy_four_enabled).contains("true")) {
            intent = new Intent(FirstActivity.this, FourthActivity.class);
        }  else if (new PrefManagerVideo(this).getString(SplashActivity.status_dummy_five_enabled).contains("true")) {
             intent = new Intent(this, FifthActivity.class);
         } else {
            intent = new Intent(FirstActivity.this, HomeActivity.class);
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
        finishAffinity();
    }
}