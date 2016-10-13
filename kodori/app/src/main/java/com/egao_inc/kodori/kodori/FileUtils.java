package com.egao_inc.kodori.kodori;

/**
 * Created by kanait on 2016/10/13.
 */
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utils for saving files
 */
public final class FileUtils {
    private FileUtils() {
    }

    /**
     * Save binary data to public image dirr
     * 画像保存
     *
     * @param content data to save
     * @return newly created file
     * @throws IOException
     */
    public static File saveImage(byte[] content, String extension) throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir, timeStamp + "." + extension);
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(image);
            out.write(content);
            out.close();
        } finally {
            if (out != null) {
                out.close();
            }
        }
        return image;
    }
}
