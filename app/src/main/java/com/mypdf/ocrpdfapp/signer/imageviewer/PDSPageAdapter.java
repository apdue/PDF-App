package com.mypdf.ocrpdfapp.signer.imageviewer;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.mypdf.ocrpdfapp.signer.Document.PDSFragment;
import com.mypdf.ocrpdfapp.signer.PDF.PDSPDFDocument;

public class PDSPageAdapter extends FragmentStatePagerAdapter {

    private PDSPDFDocument mDocument;

    public PDSPageAdapter(FragmentManager fragmentManager, PDSPDFDocument fASDocument) {
        super(fragmentManager);
        this.mDocument = fASDocument;
    }

    public int getCount() {
        return this.mDocument.getNumPages();
    }

    public Fragment getItem(int i) {
        return PDSFragment.newInstance(i);
    }

}
