package nilenso.com.kulu_mobile2.expenses;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.mobsandgeeks.saripaar.Validator;

import java.text.ParseException;

import nilenso.com.kulu_mobile2.R;
import nilenso.com.kulu_mobile2.validations.AddExpenseActivity;

public class RecordNoProofExpense extends Record {
    private Validator validator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_record_no_proof_expense);
        super.onCreate(savedInstanceState);
        validator = new Validator(this);
        validator.setValidationListener(new AddExpenseActivity(this));
    }

    @Override
    public void saveExpense(View view) throws ParseException {
        Log.i(LOG_TAG, "Saving the expense...");
        validator.validate();
   }
}
