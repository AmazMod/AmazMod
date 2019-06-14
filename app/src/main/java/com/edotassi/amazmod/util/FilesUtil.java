package com.edotassi.amazmod.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.AsyncTask;
import android.os.Bundle;

import com.pixplicity.easyprefs.library.Prefs;

import org.tinylog.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import amazmod.com.transport.Constants;

public class FilesUtil {

    public final static String APP_ICON = "app_icon";
    public final static String APP_LABEL = "app_label";
    public final static String APP_PKG = "app_pkg";

    public static boolean inputStreamToFile(InputStream in, String saveDir, String file) {
        Logger.debug(Constants.TAG, "FilesUtil inputStreamToFile saveDir: " + saveDir + " file: " + file);

        long length = 0;
        try {
            FileOutputStream out = new FileOutputStream(saveDir + File.separator + file);
            byte[] buffer = new byte[1024];
            int c;
            while ((c = in.read(buffer)) != -1) {
                out.write(buffer, 0, c);
                length+=c;
            }
            in.close();
            out.close();
        } catch (Exception e) {
            Logger.error(Constants.TAG, "FilesUtil inputStreamToFile exception: " + e.toString());
            return false;
        } finally {
            Logger.debug(Constants.TAG, "FilesUtil inputStreamToFile length: " + length);
        }
        return true;

    }

    public static class urlToFile extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... strings) {

            String url = strings[0];
            String saveDir = strings[1];
            String file = strings[2];
            Logger.debug(Constants.TAG, "FilesUtil urlToFile url: " + url + " saveDir: " + saveDir + " file: " + file);

            BufferedInputStream in = null;
            long length = 0;
            try {
                in = new BufferedInputStream(new URL(url).openStream());
                FileOutputStream out = new FileOutputStream(saveDir + File.separator + file);
                byte[] buffer = new byte[1024];
                int c;
                while ((c = in.read(buffer)) != -1) {
                    out.write(buffer, 0, c);
                    length += c;
                }
                out.close();
                in.close();
            } catch (Exception e) {
                Logger.error(Constants.TAG, "FilesUtil urlToFile exception: " + e.toString());
                return false;
            } finally {
                Logger.debug(Constants.TAG, "FilesUtil urlToFile length: " + length);
            }
            return true;
        }
    }

    public static void unzip(String zipFile, String targetDirectory) throws IOException {

        Logger.debug(Constants.TAG, "FilesUtil unzip file: " + zipFile + " targetDir: " + targetDirectory);

        try (ZipInputStream zis = new ZipInputStream(
                new BufferedInputStream(new FileInputStream(new File(zipFile))))) {
            ZipEntry ze;
            int count;
            byte[] buffer = new byte[8192];
            while ((ze = zis.getNextEntry()) != null) {
                File file = new File(new File(targetDirectory), ze.getName());
                File dir = ze.isDirectory() ? file : file.getParentFile();
                if (!dir.isDirectory() && !dir.mkdirs())
                    throw new FileNotFoundException("Failed to ensure directory: " +
                            dir.getAbsolutePath());
                if (ze.isDirectory())
                    continue;
                try (FileOutputStream fout = new FileOutputStream(file)) {
                    while ((count = zis.read(buffer)) != -1)
                        fout.write(buffer, 0, count);
                }
            }
        }
    }

    public static Bundle getApkInfo(Context context, String file) {
        PackageManager pm = context.getPackageManager();
        Bundle bundle = new Bundle();

        try {
            PackageInfo pi = pm.getPackageArchiveInfo(file, 0);

            pi.applicationInfo.sourceDir = file;
            pi.applicationInfo.publicSourceDir = file;


            Bitmap icon = drawableToBitmap(pi.applicationInfo.loadIcon(pm));
            bundle.putParcelable(APP_ICON, icon);
            bundle.putString(APP_LABEL, pi.applicationInfo.loadLabel(pm).toString());
            bundle.putString(APP_PKG, pi.applicationInfo.packageName);
        } catch (NullPointerException ex) {
            Logger.error(Constants.TAG, "FilesUtil getApkInfo NullPointerException: ", ex.toString());
            bundle = null;
        }

        return bundle;
    }

    public static Bitmap drawableToBitmap (Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if(drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static String getTagValueFromXML(String tagName, File file) {

        Logger.debug(Constants.TAG, "FilesUtil getTagValueFromXML file: " + file + " tagName: " + tagName);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(file);
            Element element = document.getDocumentElement();

            NodeList list = element.getElementsByTagName(tagName);
            if (list != null && list.getLength() > 0) {
                NodeList subList = list.item(0).getChildNodes();

                if (subList != null && subList.getLength() > 0) {
                    return subList.item(0).getNodeValue();
                }
            }
        } catch (ParserConfigurationException e) {
            Logger.error(Constants.TAG, "FilesUtil getTagValueFromXML ParserConfigurationException: " + e.toString());
            return null;
        } catch (SAXException e) {
            Logger.error(Constants.TAG, "FilesUtil getTagValueFromXML SAXException: " + e.toString());
            return null;
        } catch (IOException e) {
            Logger.error(Constants.TAG, "FilesUtil getTagValueFromXML IOException: " + e.toString());
            return null;
        }

        return null;
    }

    public static Drawable getRotateDrawable(final Drawable d, final float angle) {
        final Drawable[] arD = { d };
        return new LayerDrawable(arD) {
            @Override
            public void draw(final Canvas canvas) {
                canvas.save();
                canvas.rotate(angle, (float) d.getBounds().width() / 2, (float) d.getBounds().height() / 2);
                super.draw(canvas);
                canvas.restore();
            }
        };
    }

    /**
     * @param imageFile The file.
     * @param bm The Bitmap you want to save.
     * @param format Bitmap.CompressFormat can be PNG,JPEG or WEBP.
     * @param quality quality goes from 1 to 100. (Percentage).
     * @return true if the Bitmap was saved successfully, false otherwise.
     */
    public static boolean saveBitmapToFile(File imageFile, Bitmap bm, Bitmap.CompressFormat format, int quality) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);
            bm.compress(format,quality,fos);
            fos.close();
            return true;
        }catch (IOException e) {
            Logger.error(e.getMessage());
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return false;
    }
    public static boolean isVerge(){
        String model = Prefs.getString(Constants.PREF_HUAMI_MODEL, "-");
        boolean isVerge = Arrays.asList(Constants.BUILD_VERGE_MODELS).contains(model);
        Logger.debug("DeviceUtil isVerge: checking if model " + model + " is an Amazfit Verge: " + isVerge);
        return isVerge;
    }
}
