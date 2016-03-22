package nilenso.com.kulu_mobile2.validations;
import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.EditText;

import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;

import java.text.ParseException;
import java.util.List;

import nilenso.com.kulu_mobile2.expenses.RecordNoProofExpense;

public class AddExpenseActivity implements Validator.ValidationListener {
    private Context mContext;

    public AddExpenseActivity(Context context) {
        mContext = context;
    }

    @Override

    public void onValidationSucceeded() {
        try {
            ((RecordNoProofExpense) mContext).createExpenseEntry();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        ((Activity) mContext).finish();
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        for (ValidationError error : errors) {
            View failedView = error.getView();
            String errorMessage = error.getFailedRules().get(0).getMessage(mContext);

            if (failedView instanceof EditText) {
                ((EditText) failedView).setError(errorMessage);
            }
        }
    }
}
