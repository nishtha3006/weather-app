package com.example.nishtha.myapplication;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;

/**
 * Created by nishtha on 23/3/16.
 */
public class FetchWeatherTask extends AsyncTask<String,Void,String[]> {
    HttpURLConnection connection=null;
    BufferedReader reader=null;
    String jsonString;
    private String getReadableDateString(long time){
        SimpleDateFormat shortenedDateFormat = new SimpleDateFormat("EEE MMM dd");
        return shortenedDateFormat.format(time);
    }
    private String formatHighLows(double high, double low) {
        long roundedHigh = Math.round(high);
        long roundedLow = Math.round(low);
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(getActivity());
        String unitype=prefs.getString(getString(R.string.unit_key),getString(R.string.unit_metric));
        if(unitype.equals(getString(R.string.unit_imperial))){
            roundedHigh=Math.round(roundedHigh*1.8+32);
            roundedLow=Math.round(roundedLow*1.8+32);
        }
        String highLowStr = roundedHigh + "/" + roundedLow;
        return highLowStr;
    }
    long addLocation(String locationSetting, String cityName, double lat, double lon) {
        // Students: First, check if the location with this city name exists in the db
        // If it exists, return the current ID
        // Otherwise, insert it using the content resolver and the base URI
        return -1;
    }
    String[] convertContentValuesToUXFormat(Vector<ContentValues> cvv) {
        // return strings to keep UI functional for now
        String[] resultStrs = new String[cvv.size()];
        for ( int i = 0; i < cvv.size(); i++ ) {
            ContentValues weatherValues = cvv.elementAt(i);
            String highAndLow = formatHighLows(
                    weatherValues.getAsDouble(WeatherEntry.COLUMN_MAX_TEMP),
                    weatherValues.getAsDouble(WeatherEntry.COLUMN_MIN_TEMP));
            resultStrs[i] = getReadableDateString(
                    weatherValues.getAsLong(WeatherEntry.COLUMN_DATE)) +
                    " - " + weatherValues.getAsString(WeatherEntry.COLUMN_SHORT_DESC) +
                    " - " + highAndLow;
        }
        return resultStrs;
    }
    private String[] getWeatherDataFromJson(String forecastJsonStr, int numDays)
            throws JSONException {
        final String OWM_LIST = "list";
        final String OWM_WEATHER = "weather";
        final String OWM_TEMPERATURE = "temp";
        final String OWM_MAX = "max";
        final String OWM_MIN = "min";
        final String OWM_DESCRIPTION = "main";

        JSONObject forecastJson = new JSONObject(forecastJsonStr);
        JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
        Time dayTime = new Time();
        dayTime.setToNow();
        int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);
        dayTime = new Time();
        String[] resultStrs = new String[numDays];
        for(int i = 0; i < weatherArray.length(); i++) {
            String day;
            String description;
            String highAndLow;
            JSONObject dayForecast = weatherArray.getJSONObject(i);
            long dateTime;
            dateTime = dayTime.setJulianDay(julianStartDay+i);
            day = getReadableDateString(dateTime);
            JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
            description = weatherObject.getString(OWM_DESCRIPTION);
            JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
            double high = temperatureObject.getDouble(OWM_MAX);
            double low = temperatureObject.getDouble(OWM_MIN);
            highAndLow = formatHighLows(high, low);
            resultStrs[i] = day + " - " + description + " - " + highAndLow;
        }
        return resultStrs;
    }
    @Override
    protected String[] doInBackground(String... params) {
        String url="http://api.openweathermap.org/data/2.5/forecast/daily?q="+params[0]+"&mode=json&units=metric&cnt=7";
        String apiid="&APPID=a38d80a2cd2f57a066c2bf6b119d0104";
        int numofdays=7;
        try {
            URL baseurl = new URL(url.concat(apiid));
            connection=(HttpURLConnection)baseurl.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            InputStream inputStream=connection.getInputStream();
            if(inputStream==null){
                return null;
            }
            StringBuffer buffer=new StringBuffer();
            reader=new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while((line=reader.readLine())!=null){
                buffer.append(line + "\n");
            }
            if(buffer.length()==0){
                return null;
            }
            jsonString=buffer.toString();
        }catch (Exception e){
            Log.e("sunshine", " is not working");
        }finally {
            if(connection!=null){
                connection.disconnect();
            }
            if(reader!=null){
                try {
                    reader.close();
                }catch (Exception e){
                    Log.e("sunshine", "Error closing stream", e);
                }
            }
        }
        try{
            return getWeatherDataFromJson(jsonString,numofdays);
        }catch (Exception e){
            Log.e("sunshine",e.getMessage());
        }
        return null;
    }
    @Override
    protected void onPostExecute(String[] strings) {
        if(strings!=null){
            mForecastAdapter.clear();
            for(String s:strings){
                mForecastAdapter.add(s);
            }
        }
    }
}



