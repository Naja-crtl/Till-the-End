package com.example.weddingapp.LandingPage.Budget;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class HuggingFaceClassifierHelper {

    public interface ClassificationCallback {
        void onSuccess(String category);
        void onFailure(Exception e);
    }

    private static final String MODEL = "valhalla/distilbart-mnli-12-6";
    private static final String API_URL = "https://api-inference.huggingface.co/models/" + MODEL;
    private static final String HF_API_KEY = {HF_CLASSIFIER};

    private static final MediaType JSON = MediaType.get("application/json; charset=UTF-8");

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .callTimeout(25, TimeUnit.SECONDS)
            .build();

    public void classifyExpense(String text, ClassificationCallback callback) {
        try {
            JSONObject payload = new JSONObject();
            payload.put("inputs", text);

            // Your wedding-budget categories
            JSONArray labels = new JSONArray()
                    .put("Venue Rental")
                    .put("Catering & Beverages")
                    .put("Wedding Attire & Accessories")
                    .put("Photography & Videography")
                    .put("Floral & Decorations")
                    .put("Entertainment & Music")
                    .put("Invitations & Stationery")
                    .put("Cake & Desserts")
                    .put("Transportation & Logistics")
                    .put("Beauty & Grooming")
                    .put("Officiant & Legal Fees")
                    .put("Rentals & Equipment")
                    .put("Wedding Planner / Coordination")
                    .put("Favors & Gifts")
                    .put("Accommodation & Lodging")
                    .put("Miscellaneous / Other");

            JSONObject params = new JSONObject();
            params.put("candidate_labels", labels);
            params.put("multi_label", false);
            params.put("hypothesis_template", "This expense is {}.");
            payload.put("parameters", params);

            RequestBody body = RequestBody.create(payload.toString(), JSON);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .post(body)
                    .header("Content-Type", "application/json; charset=UTF-8")
                    .header("Authorization", "Bearer " + HF_API_KEY)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException e) {
                    callback.onFailure(e);
                }
                @Override public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        callback.onFailure(new Exception(
                                "HF API error " + response.code() + ": " + response.body().string()
                        ));
                        return;
                    }
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        String bestLabel = json.getJSONArray("labels").getString(0);
                        callback.onSuccess(bestLabel);
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                }
            });

        } catch (Exception e) {
            callback.onFailure(e);
        }
    }
}
