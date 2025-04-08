package com.mypdf.ocrpdfapp.signer.Document;

import android.app.Activity;
import android.content.Context;
//import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.viewpager.widget.ViewPager;

import com.mypdf.ocrpdfapp.signer.DigitalSignatureActivity;

import fr.castorflex.android.verticalviewpager.VerticalViewPager;

public class PDSViewPager extends VerticalViewPager {
    private Context mActivityContext = null;
    private boolean mDownReceieved = true;

    public PDSViewPager(Activity context) {
        super(context);
        this.mActivityContext = context;
        init();
    }

    public PDSViewPager(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.mActivityContext = context;
        init();
    }


    private void init() {
        setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                View focusedChild = PDSViewPager.this.getFocusedChild();
                if (focusedChild != null) {
                    PDSPageViewer pDSPageViewer = (PDSPageViewer) ((ViewGroup) focusedChild).getChildAt(0);
                    if (pDSPageViewer != null) {
                        pDSPageViewer.resetScale();
                    }
                }
                if (PDSViewPager.this.mActivityContext != null) {
                    ((DigitalSignatureActivity) PDSViewPager.this.mActivityContext).updatePageNumber(position + 1);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }
        });
    }

    public boolean onInterceptTouchEvent(MotionEvent motionEvent) {
        if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
            this.mDownReceieved = true;
        }
        if (motionEvent.getPointerCount() <= 1 && this.mDownReceieved) {
            return super.onInterceptTouchEvent(motionEvent);
        }
        this.mDownReceieved = false;
        return false;

    }
}
