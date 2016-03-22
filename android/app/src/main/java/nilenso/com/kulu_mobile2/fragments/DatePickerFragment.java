package nilenso.com.kulu_mobile2.fragments;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.widget.DatePicker;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import nilenso.com.kulu_mobile2.R;

public class DatePickerFragment extends DialogFragment
        implements DatePickerDialog.OnDateSetListener {

    private static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the current date as the default date in the picker
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), this, year, month, day);

        // Create a new instance of DatePickerDialog and return it
        datePickerDialog.getDatePicker().setMaxDate(new Date().getTime());
        return datePickerDialog;
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        Calendar cal = new GregorianCalendar(year, month, day);
        DATE_FORMATTER.setTimeZone(cal.getTimeZone());
        TextView datePicker = (TextView) getActivity().findViewById(R.id.datePicker);
        datePicker.setText(DATE_FORMATTER.format(cal.getTime()));
    }
}
