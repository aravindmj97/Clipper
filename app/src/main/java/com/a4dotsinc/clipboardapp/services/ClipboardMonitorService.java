package com.a4dotsinc.clipboardapp.services;

import android.app.Notification;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Environment;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.a4dotsinc.clipboardapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Date;

import static com.a4dotsinc.clipboardapp.App.CHANNEL_ID;

/**
 * Monitors the {@link ClipboardManager} for changes and logs the text to a file.
 */
public class ClipboardMonitorService extends Service {
    private static final String TAG = "ClipboardManager";
    private static final String FILENAME = "clipboard-history.txt";
    FirebaseDatabase database;
    DatabaseReference reference;

    private File mHistoryFile;
    private ExecutorService mThreadPool = Executors.newSingleThreadExecutor();
    private ClipboardManager mClipboardManager;

    @Override
    public void onCreate() {
        super.onCreate();

        // TODO: Show an ongoing notification when this service is running.
        database = FirebaseDatabase.getInstance();
        reference = database.getReference("/text");
        mHistoryFile = new File(getExternalFilesDir(null), FILENAME);
        mClipboardManager =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        String input = intent.getStringExtra("text");


        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ClipBoard Service")
                .setContentText(input)
                .setSmallIcon(R.drawable.ic_content_copy_black)
                .build();

        startForeground(1, notification);

        //do heavy work on a background thread
        //stopSelf();
        mClipboardManager.addPrimaryClipChangedListener(
                mOnPrimaryClipChangedListener);

        return START_REDELIVER_INTENT;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private ClipboardManager.OnPrimaryClipChangedListener mOnPrimaryClipChangedListener =
            new ClipboardManager.OnPrimaryClipChangedListener() {
                @Override
                public void onPrimaryClipChanged() {
                    Log.d(TAG, "onPrimaryClipChanged");
                    ClipData clip = mClipboardManager.getPrimaryClip();
                    mThreadPool.execute(new WriteHistoryRunnable(
                            clip.getItemAt(0).getText()));
                }
            };

    private class WriteHistoryRunnable implements Runnable {
        private final Date mNow;
        private final CharSequence mTextToWrite;

        public WriteHistoryRunnable(CharSequence text) {
            mNow = new Date(System.currentTimeMillis());
            mTextToWrite = text;
        }

        @Override
        public void run() {
            if (TextUtils.isEmpty(mTextToWrite)) {
                // Don't write empty text to the file
                return;
            }

            if (isExternalStorageWritable()) {
                reference.push().child("text").setValue(mTextToWrite).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        try {
                            //ContextCompat.startForegroundService(getApplicationContext(), new Intent(getApplicationContext(), ClipboardMonitorService.class).putExtra("text", mTextToWrite));
                            Log.i("FirebaseSave", "Success");
                            Log.i(TAG, "Writing new clip to history:");
                            Log.i(TAG, mTextToWrite.toString());
                            BufferedWriter writer =
                                    new BufferedWriter(new FileWriter(mHistoryFile, true));
                            writer.write(String.format("[%s]:\n%s:\n", mNow.toString(),android.os.Build.MODEL));
                            writer.write(mTextToWrite.toString());
                            writer.newLine();
                            writer.close();
                        } catch (IOException e) {
                            Log.w(TAG, String.format("Failed to open file %s for writing!",
                                    mHistoryFile.getAbsoluteFile()));
                        }
                    }
                });
            } else {
                Log.w(TAG, "External storage is not writable!");
            }
        }
    }
}