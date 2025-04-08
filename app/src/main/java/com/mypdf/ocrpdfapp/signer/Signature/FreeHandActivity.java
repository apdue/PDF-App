package com.mypdf.ocrpdfapp.signer.Signature;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.SeekBar;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.mypdf.ocrpdfapp.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class FreeHandActivity extends AppCompatActivity {
    private boolean isFreeHandCreated = false;
    private SignatureView signatureView;
    private SeekBar inkWidth;
    private Menu menu = null;

    FloatingActionButton fab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_free_hand);
        ActionBar ab = getSupportActionBar();
        signatureView = findViewById(R.id.inkSignatureOverlayView);
        inkWidth = findViewById(R.id.seekBar);
        inkWidth.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                signatureView.setStrokeWidth(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        fab = findViewById(R.id.fabSign);
        fab.setEnabled(false);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveFreeHand();
                Intent data = new Intent();
                String text = "Result OK";
                data.setAction(text);
                setResult(RESULT_OK, data);
                finish();
            }
        });

        findViewById(R.id.action_clear).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                FreeHandActivity.this.clearSignature();
                FreeHandActivity.this.enableClear(false);
                FreeHandActivity.this.enableSave(false);
            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.freehandmenu, menu);
        this.menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.signature_save) {
            saveFreeHand();
            Intent data = new Intent();
            String text = "Result OK";
            data.setAction(text);
            setResult(RESULT_OK, data);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked


        if (view.getId() == R.id.radioBlack) {
            if (checked) {
                signatureView.setStrokeColor(ContextCompat.getColor(FreeHandActivity.this, R.color.inkblack));
            }
        } else if (view.getId() == R.id.radioRed) {
            if (checked)
                signatureView.setStrokeColor(ContextCompat.getColor(FreeHandActivity.this, R.color.inkred));
        } else if (view.getId() == R.id.radioBlue) {
            if (checked) {
                signatureView.setStrokeColor(ContextCompat.getColor(FreeHandActivity.this, R.color.inkblue));
            }
        } else if (view.getId() == R.id.radiogreen) {
            if (checked) {
                signatureView.setStrokeColor(ContextCompat.getColor(FreeHandActivity.this, R.color.inkgreen));
            }
        }
    }

    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);

    }

    public void clearSignature() {
        signatureView.clear();
        signatureView.setEditable(true);
    }

    public void enableClear(boolean z) {
        ImageButton button = findViewById(R.id.action_clear);
        button.setEnabled(z);
        if (z) {
            button.setAlpha(1.0f);
        } else {
            button.setAlpha(0.5f);
        }
    }

    public void enableSave(boolean z) {
        fab.setEnabled(z);
    }

    public void saveFreeHand() {
        SignatureView localSignatureView = findViewById(R.id.inkSignatureOverlayView);
        ArrayList localArrayList = localSignatureView.mInkList;
        if ((localArrayList != null) && (localArrayList.size() > 0)) {
            isFreeHandCreated = true;
        }
        SignatureUtils.saveSignature(getApplicationContext(), localSignatureView);
    }
}
