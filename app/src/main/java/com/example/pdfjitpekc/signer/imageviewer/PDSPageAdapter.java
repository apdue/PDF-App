package com.example.pdfjitpekc.signer.imageviewer;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.example.pdfjitpekc.signer.Document.PDSFragment;
import com.example.pdfjitpekc.signer.PDF.PDSPDFDocument;

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
