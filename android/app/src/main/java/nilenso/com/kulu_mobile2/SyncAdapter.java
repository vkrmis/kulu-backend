package nilenso.com.kulu_mobile2;

import android.accounts.Account;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.readystatesoftware.simpl3r.UploadIterruptedException;
import com.readystatesoftware.simpl3r.Uploader;
import com.readystatesoftware.simpl3r.Uploader.UploadProgressListener;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import io.realm.Realm;
import io.realm.RealmResults;

class SyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String TAG = "SyncAdapter";
    public static final String S3KEY_EXTRA = "com.kulu_mobile.s3key";
    public static final String S3LOCATION_EXTRA = "com.kulu_mobile.s3location";
    public static final String FILEUPLOADED_EXTRA = "com.kulu_mobile.filetoremove";

    public static final String UPLOAD_STATE_CHANGED_ACTION = "com.kulu_mobile.UPLOAD_STATE_CHANGED_ACTION";
    public static final String UPLOAD_FINISHED_ACTION = "com.kulu_mobile.UPLOAD_FINISHED_ACTION";

    public static final String PERCENT_EXTRA = "com.kulu_mobile.percent";
    public static final String MSG_EXTRA = "com.kulu_mobile.msg";
    private static final int NOTIFY_ID_UPLOAD = 4815;

    /**
     * Content resolver, for performing database operations.
     * Content resolver, for performing database operations.
     */
    private AmazonS3Client s3Client;
    private NotificationManager nm;
    private SharedPreferences sharedPref;


    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        s3Client = new AmazonS3Client(
                new BasicAWSCredentials(getContext().getString(R.string.kulu_s3_access_key_id),
                        context.getString(R.string.kulu_s3_secret_access_key)));
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        s3Client = new AmazonS3Client(
                new BasicAWSCredentials(getContext().getString(R.string.kulu_s3_access_key_id),
                        getContext().getString(R.string.kulu_s3_secret_access_key)));
        nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
        Log.i(TAG, "Beginning network synchronization");
        Realm realm = Realm.getInstance(getContext());
        realm.refresh();
        RealmResults<ExpenseEntry> expenseEntries = realm.where(ExpenseEntry.class).equalTo("deleted", false).findAll();

        for (int i = 0; i < expenseEntries.size(); i++) {
            ExpenseEntry expense = expenseEntries.get(i);
            String filePath = expense.getInvoicePath();

            if (filePath.isEmpty()) {
                try {
                    String msg = "Uploading " + expense.getId();
                    Notification notification = buildNotification(msg, 0);
                    nm.notify(NOTIFY_ID_UPLOAD, notification);
                    uploadNoProofInvoice(expense);
                    broadcastState(100, msg);
                    nm.notify(NOTIFY_ID_UPLOAD, buildNotification("Upload finished", 100));
                    deleteUploadedExpense(expense.getId());
                    broadcastFinished(expense.getId());
                } catch (IOException e) {
                    e.printStackTrace();
                    broadcastState(-1, "Upload couldn't be finished as connection to Kulu Backend failed");
                    nm.notify(NOTIFY_ID_UPLOAD, buildNotification("Upload couldn't be finished as the connection to the backend failed.", -1));
                }
            } else {

                File fileToUpload = new File(filePath);
                final String s3ObjectKey = FileUtils.getLastPartOfFile(filePath);
                String s3BucketName = getContext().getString(R.string.kulu_s3_tmp_bucket);
                final String msg = "Uploading " + s3ObjectKey + "...";

                // create a new uploader for this file
                Log.e(TAG, "Bucket " + s3BucketName + " " + s3ObjectKey + " " + fileToUpload);
                Uploader uploader = new Uploader(getContext(), s3Client, s3BucketName, s3ObjectKey, fileToUpload, "image/jpeg");
                uploader.setProgressListener(new UploadProgressListener() {
                    @Override
                    public void progressChanged(ProgressEvent progressEvent,
                                                long bytesUploaded, int percentUploaded) {
                        Notification notification = buildNotification(msg, percentUploaded);
                        nm.notify(NOTIFY_ID_UPLOAD, notification);
                        broadcastState(s3ObjectKey, percentUploaded, msg);
                    }
                });


                String s3Location = null;

                try {
                    s3Location = uploader.start();
                } catch (UploadIterruptedException uie) {
                    Log.e(TAG, "Upload not happened");
                    broadcastState(s3ObjectKey, -1, "Upload was interrupted");
                } catch (Exception e) {
                    e.printStackTrace();
                    broadcastState(s3ObjectKey, -1, "Upload was interrupted");
                }

                if (s3Location != null && !s3Location.isEmpty()) {
                    try {
                        String fileName = fileToUpload.getName();
                        Log.e(TAG, s3Location + " " + fileName);
                        uploadInvoice(s3Location, expense);
                        broadcastState(s3ObjectKey, -1, "File successfully uploaded to " + s3Location);
                        nm.notify(NOTIFY_ID_UPLOAD, buildNotification("Upload finished", 100));
                        deleteUploadedExpense(expense.getId());
                        broadcastFinished(s3Location, expense.getId());
                    } catch (IOException e) {
                        broadcastState(s3ObjectKey, -1, "Upload couldn't be finished as connection to Kulu Backend failed");
                        nm.notify(NOTIFY_ID_UPLOAD + 1, buildNotification("Upload couldn't be finished as the connection to the backend failed."));
                    }
                } else {
                    nm.notify(NOTIFY_ID_UPLOAD + 1, buildNotification("There was a network error. But your expense is safe. Kulu will retry soon."));
                }

            }
        }
        nm.cancel(NOTIFY_ID_UPLOAD);
    }

    private KuluBackend upload() throws IOException {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        return new KuluBackend(sharedPref.getString(SplashScreen.TEAM_NAME, ""));
    }

    private void uploadInvoice(String s3Location, ExpenseEntry result) throws IOException {
        upload().createInvoice(getContext().getString(R.string.kulu_backend_service_url),
                s3Location,
                result,
                getUserInfo(getContext(),
                        result),
                sharedPref.getString(SplashScreen.TOKEN, ""));
    }

    private void uploadNoProofInvoice(ExpenseEntry result) throws IOException {
        upload().createNoProofInvoice(getContext().getString(R.string.kulu_backend_service_url),
                result,
                getUserInfo(getContext(),
                        result),
                sharedPref.getString(SplashScreen.TOKEN, ""));
    }

    private void broadcastFinished(String s3Location, String fileUploaded) {
        Bundle b = new Bundle();
        b.putString(S3LOCATION_EXTRA, s3Location);
        b.putString(FILEUPLOADED_EXTRA, fileUploaded);
        broadcast(UPLOAD_FINISHED_ACTION, b);
    }

    private void broadcastFinished(String fileUploaded) {
        Bundle b = new Bundle();
        b.putString(FILEUPLOADED_EXTRA, fileUploaded);
        broadcast(UPLOAD_FINISHED_ACTION, b);
    }

    private void broadcast(String intentContent, Bundle b) {
        Intent intent = new Intent(intentContent);
        intent.putExtras(b);
        getContext().sendBroadcast(intent);
    }

    private Notification buildNotification(String msg, int progress) {
        Notification.Builder builder = new Notification.Builder(getContext());
        builder.setWhen(System.currentTimeMillis());
        builder.setTicker(msg);
        builder.setContentTitle(getContext().getString(R.string.app_name));
        builder.setAutoCancel(true);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setOngoing(true);
        builder.setProgress(100, progress, false);
        builder.setStyle(new Notification.BigTextStyle().bigText(msg));
        Intent notificationIntent = new Intent(getContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent =
                PendingIntent.getActivity(getContext(), 0, notificationIntent, 0);
        builder.setContentIntent(contentIntent);

        return builder.build();
    }

    private Notification buildNotification(String msg) {
        Notification.Builder builder = new Notification.Builder(getContext());
        builder.setWhen(System.currentTimeMillis());
        builder.setTicker(msg);
        builder.setContentTitle(getContext().getString(R.string.app_name));
        builder.setAutoCancel(true);
        builder.setSmallIcon(R.drawable.ic_launcher);
        builder.setOngoing(false);

        builder.setStyle(new Notification.BigTextStyle().bigText(msg));
        Intent notificationIntent = new Intent(getContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent contentIntent =
                PendingIntent.getActivity(getContext(), 0, notificationIntent, 0);
        builder.setContentIntent(contentIntent);

        return builder.build();
    }

    private void broadcastState(String s3key, int percent, String msg) {
        Bundle b = new Bundle();
        b.putString(S3KEY_EXTRA, s3key);
        b.putInt(PERCENT_EXTRA, percent);
        b.putString(MSG_EXTRA, msg);
        broadcast(UPLOAD_STATE_CHANGED_ACTION, b);
    }

    private void broadcastState(int percent, String msg) {
        Bundle b = new Bundle();
        b.putInt(PERCENT_EXTRA, percent);
        b.putString(MSG_EXTRA, msg);
        broadcast(UPLOAD_STATE_CHANGED_ACTION, b);
    }

    private void deleteUploadedExpense(String expenseEntry) {
        Realm realm = Realm.getInstance(getContext());
        realm.refresh();
        realm.beginTransaction();
        realm.removeAllChangeListeners();
        ExpenseEntry expense = realm.where(ExpenseEntry.class)
                .equalTo("id", expenseEntry)
                .findFirst();
        if (expense != null) expense.setDeleted(true);
        realm.addChangeListener(MainActivity.syncListener);
        realm.commitTransaction();
    }

    public HashMap<String, String> getUserInfo(Context context, ExpenseEntry expense) {
        HashMap<String, String> userInfo = new HashMap<String, String>();
        Realm realm = Realm.getInstance(context);
        User user = realm.where(User.class).equalTo("email", expense.getEmail()).findFirst();
        if (user == null) return userInfo;
        Log.e(TAG, user.getEmail());
        Log.e(TAG, user.getDisplayName());
        userInfo.put(SplashScreen.ACCOUNT_NAME, user.getEmail());
        return userInfo;
    }
}

