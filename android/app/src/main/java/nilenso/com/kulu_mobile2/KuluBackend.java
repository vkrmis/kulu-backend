package nilenso.com.kulu_mobile2;


import com.google.gson.Gson;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class KuluBackend {
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private OkHttpClient client;
    private final String idKey;
    private final String requestKey;
    private final String remarksKey;
    private final String expenseTypeKey;
    private final String dateKey;
    private final String merchantNameKey;
    private final String amountKey;
    private final String currencyKey;
    private final String invoice;
    private final String emailKey;
    private final String organizationNameKey;
    private String organizationName;

    public KuluBackend(String orgName) {
        organizationName = orgName;
        idKey = "id";
        requestKey = "storage_key";
        remarksKey = "remarks";
        expenseTypeKey = "expense_type";
        dateKey = "date";
        merchantNameKey = "name";
        amountKey = "amount";
        currencyKey = "currency";
        invoice = "invoice";
        emailKey = "email";
        organizationNameKey = "organization_name";
        client = new OkHttpClient();
    }

    private String create(String url, String s3Location, ExpenseEntry expense, HashMap<String, String> userInfo, String token) throws IOException {
        Map<String, Object> requestMap = new HashMap<String, Object>();
        Map<String, Object> subRequestMap = new HashMap<String, Object>();
        subRequestMap.put(idKey, expense.getId());

        if (s3Location != null && !s3Location.isEmpty()) {
            subRequestMap.put(requestKey, FileUtils.getLastPartOfFile(s3Location));
        }

        subRequestMap.put(remarksKey, expense.getComments());
        subRequestMap.put(expenseTypeKey, expense.getExpenseType());
        subRequestMap.put(dateKey, getDate(expense));
        subRequestMap.put(emailKey, userInfo.get(SplashScreen.ACCOUNT_NAME));
        subRequestMap.put(merchantNameKey , expense.getMerchantName());
        subRequestMap.put(amountKey , expense.getAmount());
        subRequestMap.put(currencyKey , expense.getCurrency());
        subRequestMap.put(emailKey, userInfo.get(SplashScreen.ACCOUNT_NAME));

        requestMap.put(invoice, subRequestMap);
        requestMap.put(organizationNameKey, organizationName);

        return makeRequest(url, requestMap, token);
    }

    public String createInvoice(String url, String s3Location, ExpenseEntry expense, HashMap<String, String> userInfo, String token) throws IOException {
        return create(url, s3Location, expense, userInfo, token);
    }

    public String createNoProofInvoice(String url, ExpenseEntry expense, HashMap<String, String> userInfo, String token) throws IOException {
        return create(url, null, expense, userInfo, token);
    }

    private String getDate(ExpenseEntry expense) {
        return new DateTime(expense.getCreatedAt()).toString("yyyy-MM-dd");
    }

    private String makeRequest(String url, Map<String, Object> requestMap, String token) throws IOException {
        Gson gson = new Gson();
        String json = gson.toJson(requestMap);

        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .header("X-Auth-Token", token)
                .url(url)
                .post(body)
                .build();
        Response response = client.newCall(request).execute();

        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
        return response.body().string();
    }
}
