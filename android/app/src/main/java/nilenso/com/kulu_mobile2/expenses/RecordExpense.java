package nilenso.com.kulu_mobile2.expenses;

import android.os.Bundle;

import nilenso.com.kulu_mobile2.R;


public class RecordExpense extends Record {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.activity_record_expense);
        super.onCreate(savedInstanceState);
    }
}
