package nilenso.com.kulu_mobile2;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Locale;

import io.realm.RealmResults;

public class InvoiceListAdapter extends ArrayAdapter<ExpenseEntry> {
    public InvoiceListAdapter(Context ctx, int viewResourceId, RealmResults<ExpenseEntry> fs) {
        super(ctx, viewResourceId, fs);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        convertView = getConvertView(convertView, parent);
        ExpenseEntry currentItem = getItem(position);

        setRemarks(convertView, currentItem);
        setCreatedAtTime(convertView, currentItem);

        return convertView;
    }

    private View getConvertView(View convertView, ViewGroup parent) {
        if (!convertViewExists(convertView))
            convertView = inflateConvertView(parent);
        return convertView;
    }

    private View inflateConvertView(ViewGroup parent) {
        return LayoutInflater.from(getContext())
                .inflate(R.layout.invoices_list_item, parent, false);
    }

    private boolean convertViewExists(View convertView) {
        return convertView != null;
    }


    private void setCreatedAtTime(View convertView, ExpenseEntry currentItem) {
        TextView tv1 = (TextView) convertView.findViewById(R.id.invoice_file_timestamp);
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy, hh:mm aaa", Locale.ENGLISH);
        tv1.setText(sdf.format(currentItem.getCreatedAt()));
    }

    private void setRemarks(View convertView, ExpenseEntry currentItem) {
        TextView tv = (TextView) convertView.findViewById(R.id.invoice_file_name);

        String comments = currentItem.getComments();
        if (comments.isEmpty()) comments = "-";

        tv.setText(comments);
    }
}