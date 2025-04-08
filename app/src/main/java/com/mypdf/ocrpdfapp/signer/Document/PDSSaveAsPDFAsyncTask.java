package com.mypdf.ocrpdfapp.signer.Document;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.security.BouncyCastleDigest;
import com.itextpdf.text.pdf.security.DigestAlgorithms;
import com.itextpdf.text.pdf.security.ExternalDigest;
import com.itextpdf.text.pdf.security.ExternalSignature;
import com.itextpdf.text.pdf.security.MakeSignature;
import com.itextpdf.text.pdf.security.PrivateKeySignature;
import com.mypdf.ocrpdfapp.signer.DigitalSignatureActivity;
import com.mypdf.ocrpdfapp.signer.PDF.PDSPDFDocument;
import com.mypdf.ocrpdfapp.signer.PDF.PDSPDFPage;
import com.mypdf.ocrpdfapp.signer.PDSModel.PDSElement;
import com.mypdf.ocrpdfapp.signer.utils.ViewUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.io.IOException;

public class PDSSaveAsPDFAsyncTask extends AsyncTask<Void, Void, Boolean> {

    private String mfileName;
    private DigitalSignatureActivity mCtx;
    private String errorMessage;
    private File savedFile;

    public PDSSaveAsPDFAsyncTask(DigitalSignatureActivity context, String str) {
        this.mCtx = context;
        this.mfileName = str;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mCtx.savingProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public Boolean doInBackground(Void... voidArr) {
        if (Looper.myLooper() == null) {
            Looper.prepare();
        }

        PDSPDFDocument document = mCtx.getDocument();
        if (document == null || document.stream == null) {
            errorMessage = "Invalid document or document stream";
            return false;
        }

        // Save in public Documents directory
        File pdfDirectory = new File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "PDFApp/Signed"
        );
        if (!pdfDirectory.exists() && !pdfDirectory.mkdirs()) {
            errorMessage = "Could not create directory";
            return false;
        }

        savedFile = new File(pdfDirectory, mfileName);
        if (savedFile.exists() && !savedFile.delete()) {
            errorMessage = "Could not delete existing file";
            return false;
        }

        PdfReader reader = null;
        PdfStamper signer = null;
        FileOutputStream os = null;
        InputStream stream = null;

        try {
            stream = document.stream;
            os = new FileOutputStream(savedFile);
            reader = new PdfReader(stream);
            
            // Process each page
            for (int i = 0; i < document.getNumPages(); i++) {
                Rectangle mediabox = reader.getPageSize(i + 1);
                PDSPDFPage page = document.getPage(i);
                
                for (int j = 0; j < page.getNumElements(); j++) {
                    PDSElement element = page.getElement(j);
                    RectF bounds = element.getRect();
                    Bitmap elementBitmap = null;

                    try {
                        if (element.getType() == PDSElement.PDSElementType.PDSElementTypeSignature) {
                            elementBitmap = createSignatureBitmap(element);
                        } else {
                            elementBitmap = element.getBitmap();
                        }

                        if (elementBitmap == null) continue;

                        byte[] imageBytes = bitmapToByteArray(elementBitmap);
                        Image sigimage = Image.getInstance(imageBytes);

                        if (isDigitalSignatureAvailable()) {
                            signer = processDigitalSignature(reader, os, signer, sigimage, mediabox, bounds, i, j);
                        } else {
                            signer = processRegularImage(reader, os, signer, sigimage, mediabox, bounds, i);
                        }
                    } finally {
                        if (elementBitmap != null) {
                            elementBitmap.recycle();
                        }
                    }
                }
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            errorMessage = "Error processing PDF: " + e.getMessage();
            return false;
        } finally {
            closeResources(reader, signer, os, stream);
        }
    }

    private Bitmap createSignatureBitmap(PDSElement element) {
        PDSElementViewer viewer = element.mElementViewer;
        View dummy = viewer.getElementView();
        View view = ViewUtils.createSignatureView(mCtx, element, viewer.mPageViewer.getToViewCoordinatesMatrix());
        Bitmap bitmap = Bitmap.createBitmap(dummy.getWidth(), dummy.getHeight(), Bitmap.Config.ARGB_8888);
        view.draw(new Canvas(bitmap));
        return bitmap;
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

    private boolean isDigitalSignatureAvailable() {
        return mCtx.alises != null && mCtx.keyStore != null && mCtx.mdigitalIDPassword != null;
    }

    private PdfStamper processDigitalSignature(PdfReader reader, FileOutputStream os, PdfStamper signer,
                                             Image sigimage, Rectangle mediabox, RectF bounds, int pageIndex, int elementIndex) throws Exception {
        if (signer == null) {
            signer = PdfStamper.createSignature(reader, os, '\0');
        }

        KeyStore ks = mCtx.keyStore;
        String alias = mCtx.alises;
        PrivateKey pk = (PrivateKey) ks.getKey(alias, mCtx.mdigitalIDPassword.toCharArray());
        Certificate[] chain = ks.getCertificateChain(alias);

        PdfSignatureAppearance appearance = signer.getSignatureAppearance();
        float top = mediabox.getHeight() - (bounds.top + bounds.height());
        appearance.setVisibleSignature(new Rectangle(bounds.left, top, bounds.left + bounds.width(), top + bounds.height()),
                pageIndex + 1, "sig" + elementIndex);
        appearance.setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);
        appearance.setSignatureGraphic(sigimage);

        ExternalDigest digest = new BouncyCastleDigest();
        ExternalSignature signature = new PrivateKeySignature(pk, DigestAlgorithms.SHA256, null);
        MakeSignature.signDetached(appearance, digest, signature, chain, null, null, null, 0, MakeSignature.CryptoStandard.CADES);

        return signer;
    }

    private PdfStamper processRegularImage(PdfReader reader, FileOutputStream os, PdfStamper signer,
                                         Image sigimage, Rectangle mediabox, RectF bounds, int pageIndex) throws Exception {
        if (signer == null) {
            signer = new PdfStamper(reader, os, '\0');
        }

        PdfContentByte contentByte = signer.getOverContent(pageIndex + 1);
        sigimage.setAlignment(Image.ALIGN_UNDEFINED);
        sigimage.scaleToFit(bounds.width(), bounds.height());
        sigimage.setAbsolutePosition(bounds.left - (sigimage.getScaledWidth() - bounds.width()) / 2,
                mediabox.getHeight() - (bounds.top + bounds.height()));
        contentByte.addImage(sigimage);

        return signer;
    }

    private void closeResources(PdfReader reader, PdfStamper signer, FileOutputStream os, InputStream stream) {
        try {
            if (signer != null) signer.close();
            if (reader != null) reader.close();
            if (os != null) os.close();
            if (stream != null) stream.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onPostExecute(Boolean result) {
        mCtx.runPostExecution();
        if (!result) {
            String message = errorMessage != null ? errorMessage : "Something went wrong while signing PDF document, Please try again";
            Toast.makeText(mCtx, message, Toast.LENGTH_LONG).show();
        } else {
            // Make the file visible in gallery and file managers
            if (savedFile != null) {
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(savedFile);
                mediaScanIntent.setData(contentUri);
                mCtx.sendBroadcast(mediaScanIntent);
            }
            Toast.makeText(mCtx, "PDF document saved successfully", Toast.LENGTH_LONG).show();
        }
    }
}

