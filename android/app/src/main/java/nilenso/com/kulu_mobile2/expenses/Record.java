package nilenso.com.kulu_mobile2.expenses;

import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.mobsandgeeks.saripaar.annotation.NotEmpty;

import java.io.File;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import io.realm.Realm;
import nilenso.com.kulu_mobile2.ExpenseEntry;
import nilenso.com.kulu_mobile2.MainActivity;
import nilenso.com.kulu_mobile2.R;
import nilenso.com.kulu_mobile2.SplashScreen;
import nilenso.com.kulu_mobile2.StringUtils;
import nilenso.com.kulu_mobile2.User;
import nilenso.com.kulu_mobile2.fragments.DatePickerFragment;


public class Record extends FragmentActivity {
    protected String LOG_TAG = "RecordExpenseActivity";
    protected String invoiceLocation = null;

    @NotEmpty
    private EditText amount;

    @NotEmpty
    private EditText merchantName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        invoiceLocation = getIntent().getStringExtra(MainActivity.INVOICE_LOCATION);
        merchantName = (EditText) findViewById(R.id.merchantName);
        amount = (EditText) findViewById(R.id.amount);

        RadioButton companyExpenseRadio = (RadioButton) findViewById(R.id.company);
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        companyExpenseRadio.setText("Paid by " +
                StringUtils.capitalizeFirstLetter(sharedPref.getString(SplashScreen.TEAM_NAME, "")));

        setupDatePicker();
        setupCurrencyPicker();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        return id == R.id.action_settings || super.onOptionsItemSelected(item);
    }

    public void saveExpense(View view) throws ParseException {
        Log.i(LOG_TAG, "Saving the expense...");
        createExpenseEntry();
        finish();
    }

    public void createExpenseEntry() throws ParseException {
        Realm realm = Realm.getInstance(this);
        realm.beginTransaction();

        ExpenseEntry expenseEntry = realm.createObject(ExpenseEntry.class);

        expenseEntry.setId(UUID.randomUUID().toString());
        expenseEntry.setComments(getRemarks());
        expenseEntry.setExpenseType(getExpenseType());
        expenseEntry.setCurrency(getCurrency());
        expenseEntry.setAmount(getAmount());
        expenseEntry.setMerchantName(getMerchantName());
        expenseEntry.setExpenseDate(getExpenseDate());
        expenseEntry.setCreatedAt(new Date());

        if (invoiceLocation != null && !invoiceLocation.isEmpty()) {
            expenseEntry.setInvoice(getFileNameFromUri(invoiceLocation));
            expenseEntry.setInvoicePath(invoiceLocation);
        }

        createUserIfMissing(realm);

        expenseEntry.setEmail(currentUserEmail());

        realm.commitTransaction();
        Log.i(LOG_TAG, "Expense has been saved.");
    }

    protected void setupDatePicker() {
        SimpleDateFormat formatter = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);
        ((TextView) findViewById(R.id.datePicker)).setText(formatter.format(new Date()));
        findViewById(R.id.datePicker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new DatePickerFragment();
                newFragment.show(getFragmentManager(), "datePicker");
            }
        });
    }

    protected String getExpenseType() {
        return getCheckedRadioButtonValue();
    }

    protected String getCheckedRadioButtonValue() {
        RadioGroup expenseType = (RadioGroup) findViewById(R.id.expense_type);
        switch (expenseType.getCheckedRadioButtonId()) {
            case R.id.company:
                return "Company";
            case R.id.reimbursement:
                return "Reimbursement";
            default:
                return "";
        }
    }

    protected void createUserIfMissing(Realm realm) {
        User user = realm.where(User.class).equalTo("email", currentUserEmail()).findFirst();
        if (isNull(user)) {
            user = realm.createObject(User.class);
            user.setCurrentUserInfo(currentUserEmail());
        }
    }

    protected boolean isNull(User user) {
        return user == null;
    }

    protected String getFileNameFromUri(String invoiceLocation) {
        return new File(invoiceLocation).getName();
    }

    protected String currentUserEmail() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        return sharedPref.getString(SplashScreen.ACCOUNT_NAME, "");
    }

    protected String getMerchantName() {
        return merchantName.getText().toString();
    }

    protected void setupCurrencyPicker() {
        Spinner spinner = (Spinner) findViewById(R.id.currency);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.currency_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }

    protected String getCurrency() {
        return ((Spinner) findViewById(R.id.currency)).getSelectedItem().toString();
    }

    protected float getAmount() {
        String value = amount.getText().toString();
        if (value.isEmpty()) return (float) 0;
        return (new BigDecimal(value)).floatValue();
    }

    protected Date getExpenseDate() throws ParseException {
        TextView datePicker = (TextView) findViewById(R.id.datePicker);
        SimpleDateFormat formatter = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);
        return formatter.parse(datePicker.getText().toString());
    }

    protected String getRemarks() {
        return ((EditText) findViewById(R.id.editComments)).getText().toString();
    }
}
