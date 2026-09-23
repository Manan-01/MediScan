package com.example.mediscan;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class BackendClient {

    private static final String BASE_URL = "http://192.168.29.67:8080";

    public interface ParseCallback{
        void onResult(boolean success,String name,String expiry,String errorMessage);
    }

    public static boolean isNetworkAvailable(Context context){
        ConnectivityManager connectivityManager= (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager==null) return false;
        NetworkCapabilities capabilities=connectivityManager.getNetworkCapabilities(connectivityManager.getActiveNetwork());
        return capabilities !=null && (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)|| capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR));

    }

    public static  void parseMedicine(List<String> rawTexts,ParseCallback callback){
        try{
            JSONArray textArray=new JSONArray();
            for(String t: rawTexts) textArray.put(t);
            JSONObject requestBody=new JSONObject();
            requestBody.put("rawTexts",textArray);

            URL url=new URL(BASE_URL+"/api/parse-medicine");
            HttpURLConnection connection=(HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type","application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);
            try(OutputStream outputStream=connection.getOutputStream()){
                outputStream.write(requestBody.toString().getBytes(StandardCharsets.UTF_8));

            }

            int status=connection.getResponseCode();
            if(status!=200){
                callback.onResult(false,null,null,"Backend returned status "+status);
                return;
            }
            StringBuilder stringBuilder=new StringBuilder();
            try(BufferedReader reader=new BufferedReader(new InputStreamReader(connection.getInputStream()))){
                String line;
                while((line= reader.readLine()) !=null) stringBuilder.append(line);
            }

            JSONObject responseJson=new JSONObject(stringBuilder.toString());
            boolean success=responseJson.optBoolean("success",false);
            String name= responseJson.optString("name","");
             String expiry=responseJson.optString("expiry","");
            String errorMessage=responseJson.optString("errorMessage",null);
            callback.onResult(success,name,expiry,errorMessage);

        } catch (Exception e) {
            Log.e("BackendClient", "parseMedicine failed", e);
            callback.onResult(false, null, null, e.toString());
        }
    }
}
