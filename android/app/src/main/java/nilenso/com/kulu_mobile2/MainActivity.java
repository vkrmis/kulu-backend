package nilenso.com.kulu_mobile2;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SyncStatusObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.exceptions.RealmMigrationNeededException;
import nilenso.com.kulu_mobile2.accounts.GenericAccountService;
import nilenso.com.kulu_mobile2.expenses.RecordExpense;
import nilenso.com.kulu_mobile2.expenses.RecordNoProofExpense;


public class MainActivity extends ActionBarActivity {
    private InvoiceListAdapter invoiceListAdapter;
    private String LOG_TAG = "MainActivity";
    public static final String INVOICE_LOCATION = "invoiceLocationFromCamera";
    public static final String CURRENT_PHOTO_PATH = "currentPhotoPath";
    public static final String DEFAULT_PHOTO_PATH = "";
    public static final String SYNC_STATUS = "syncStatus";

    private TextView syncMessage;
    private static Animation animationFadeIn;

    public static final String AUTHORITY = "nilenso.com.kulu_mobile2.sync.basicsyncadapter";

    public static final long SECONDS_PER_MINUTE = 60L;
    public static final long SYNC_INTERVAL_IN_MINUTES = 30L;
    public static final long SYNC_INTERVAL =
            SYNC_INTERVAL_IN_MINUTES *
                    SECONDS_PER_MINUTE;

    // An account type, in the form of a domain name
    Account mAccount;
    private String[] syncStates = {"Turn off Auto Upload", "Turn on Auto Upload"};

    public final static RealmChangeListener syncListener = new RealmChangeListener() {
        @Override
        public void onChange() {
            ContentResolver.requestSync(
                    GenericAccountService.GetAccount(),
                    MainActivity.AUTHORITY,
                    Bundle.EMPTY);
        }
    };

    private Object statusListener;
    private SyncStatusObserver syncObserverHandle = new SyncStatusObserver() {
        @Override
        public void onStatusChanged(int which) {
            MainActivity.this.runOnUiThread(new Runnable() {
                public void run() {
                    hideOrShowSyncMessage();
                }
            });
        }
    };

    private void hideOrShowSyncMessage() {
        if (isSyncable()) {
            syncMessage.setVisibility(View.GONE);
        } else {
            syncMessage.setVisibility(View.VISIBLE);
        }
    }

    private void updateView() {
        setContentView(R.layout.activity_main);
        ListView invoiceList = (ListView) findViewById(R.id.listView);
        TextView empty = (TextView) findViewById(R.id.empty);
        syncMessage = (TextView) findViewById(R.id.syncmessage);
        hideOrShowSyncMessage();
        invoiceList.setEmptyView(empty);

        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE |
                ContentResolver.SYNC_OBSERVER_TYPE_PENDING | ContentResolver.SYNC_OBSERVER_TYPE_SETTINGS;
        statusListener = ContentResolver.addStatusChangeListener(mask, syncObserverHandle);

        FloatingActionButton cameraExpense = (FloatingActionButton) findViewById(R.id.camera_expense);
        cameraExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });

        FloatingActionButton pettyExpense = (FloatingActionButton) findViewById(R.id.petty_expense);
        pettyExpense.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addExpenseWithoutProof();
            }
        });

        invoiceList.invalidate();
        RealmResults<ExpenseEntry> expenses;

        try {
            Realm realm = Realm.getInstance(this);
            expenses = realm.where(ExpenseEntry.class)
                    .equalTo("deleted", false)
                    .equalTo("email", currentUserEmail())
                    .findAllSorted("createdAt", RealmResults.SORT_ORDER_DESCENDING);

        } catch (RealmMigrationNeededException ex) {
            Realm.deleteRealmFile(this);
            Realm realm = Realm.getInstance(this);
            expenses = realm.where(ExpenseEntry.class).findAll();
        }

        invoiceListAdapter = new InvoiceListAdapter(this, R.layout.invoices_list_item, expenses);
        invoiceList.setAdapter(invoiceListAdapter);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccount = CreateSyncAccount(this);
        updateView();

        Realm.getInstance(this).addChangeListener(syncListener);
        IntentFilter f = new IntentFilter(SyncAdapter.UPLOAD_FINISHED_ACTION);
        registerReceiver(uploadFinishedReceiver, f);
    }

    private Account CreateSyncAccount(Context context) {
        Account newAccount = GenericAccountService.GetAccount();
        AccountManager accountManager =
                (AccountManager) context.getSystemService(
                        ACCOUNT_SERVICE);

        accountManager.addAccountExplicitly(newAccount, null, null);
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);

        if (sharedPref.getInt(SYNC_STATUS, 1) > 0) {
            ContentResolver.setIsSyncable(newAccount, AUTHORITY, 1);
            ContentResolver.setSyncAutomatically(newAccount, AUTHORITY, true);
            ContentResolver.addPeriodicSync(newAccount, AUTHORITY, Bundle.EMPTY, SYNC_INTERVAL);
            ContentResolver.requestSync(newAccount, AUTHORITY, Bundle.EMPTY);
        }

        return GenericAccountService.GetAccount();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateView();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (statusListener != null) {
            ContentResolver.removeStatusChangeListener(statusListener);
            statusListener = null;
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(uploadFinishedReceiver);
        Realm.getInstance(this).removeAllChangeListeners();
        super.onDestroy();
    }

    private void addExpense(Uri invoiceURI) {
        Intent recordExpense = new Intent(this, RecordExpense.class);
        recordExpense.putExtra(INVOICE_LOCATION, invoiceURI.getPath());
        startActivity(recordExpense);
    }

    private void addExpenseWithoutProof() {
        Intent recordExpense = new Intent(this, RecordNoProofExpense.class);
        startActivity(recordExpense);
    }

    public BroadcastReceiver uploadFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(final Context context, Intent intent) {
            Bundle extra = intent.getExtras();
            String expenseEntry = extra.getString(SyncService.FILEUPLOADED_EXTRA);
            deleteUploadedExpense(expenseEntry);
            Toast.makeText(context,
                    "Expense uploaded", Toast.LENGTH_SHORT).show();

            invoiceListAdapter.notifyDataSetChanged();
        }
    };

    private Uri mCurrentPhotoPath = null;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            setCurrentPhotoPath();
            if (resultCode == RESULT_OK) {
                addExpense(mCurrentPhotoPath);

                Toast.makeText(getApplicationContext(),
                        "New image added", Toast.LENGTH_SHORT).show();

            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(getApplicationContext(),
                        "No new image was added", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "Failed to capture image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setCurrentPhotoPath() {
        if (currentPhotoPathExists()) return;
        mCurrentPhotoPath = getCurrentPhotoPath();
    }

    private boolean currentPhotoPathExists() {
        return mCurrentPhotoPath != null;
    }

    public void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;

            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Log.e(LOG_TAG, "Problem in saving the file" + ex.getMessage());
            }

            // continue only if the file was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, 1);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH).format(new Date());
        String imageFileName = "IMG_" + timeStamp + UUID.randomUUID().toString();
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);

        Log.i(LOG_TAG, "Creating image file...");
        File mediaFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        mCurrentPhotoPath = Uri.fromFile(mediaFile);
        saveCurrentPhotoPath();
        Log.e(LOG_TAG, "FILE path " + mCurrentPhotoPath);
        return mediaFile;
    }

    private void saveCurrentPhotoPath() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(CURRENT_PHOTO_PATH, mCurrentPhotoPath.getPath());
        editor.apply();
    }

    private Uri getCurrentPhotoPath() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        String currentPhotoPath = sharedPref.getString(CURRENT_PHOTO_PATH, DEFAULT_PHOTO_PATH);
        return Uri.parse(currentPhotoPath);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.removeItem(Menu.FIRST);

        if (isSyncable()) {
            menu.add(0, Menu.FIRST, Menu.NONE, syncStates[0]);
        } else {
            menu.add(0, Menu.FIRST, Menu.NONE, syncStates[1]);
        }

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.sign_out) {
            startSignOutActivity();
            return true;
        }

        if (id == Menu.FIRST) {
            int state = sync(!isSyncable()); // toggle sync
            item.setTitle(syncStates[state]);
            return true;
        }

        return true;
    }

    private boolean isSyncable() {
        return ContentResolver.getIsSyncable(mAccount, AUTHORITY) > 0;
    }

    private void startSignOutActivity() {
        Intent intent = new Intent(this, SplashScreen.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(SplashScreen.SIGN_OUT, "true");
        sync(false);
        startActivity(intent);
    }

    private int sync(boolean state) {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();

        if (state) {
            try {
                ContentResolver.setIsSyncable(mAccount, AUTHORITY, 1);
                ContentResolver.requestSync(mAccount, AUTHORITY, Bundle.EMPTY);
                ContentResolver.addPeriodicSync(mAccount, AUTHORITY, Bundle.EMPTY, SYNC_INTERVAL);
                ContentResolver.setSyncAutomatically(mAccount, AUTHORITY, true);

                editor.putInt(SYNC_STATUS, 1);
                editor.apply();
                return 1;
            } catch (Exception e) {
                Log.w(LOG_TAG, "Removing AutoSync [FAILED] " + e.toString());
                return 0;
            }
        } else {
            ContentResolver.setIsSyncable(mAccount, AUTHORITY, 0);
            ContentResolver.cancelSync(mAccount, AUTHORITY);
            ContentResolver.removePeriodicSync(mAccount, AUTHORITY, Bundle.EMPTY);

            editor.putInt(SYNC_STATUS, 0);
            editor.apply();

            return 0;
        }
    }

    public void onBackPressed() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        startActivity(intent);
    }

    private String currentUserEmail() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString(SplashScreen.ACCOUNT_NAME, "");
    }

    private void deleteUploadedExpense(String expenseEntry) {
        Realm realm = Realm.getInstance(this);
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
}
