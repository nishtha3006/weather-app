package com.example.nishtha.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    ArrayAdapter mForecastAdapter;
    public MainActivityFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecast, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id=item.getItemId();
        if(id==R.id.refresh){
            updateWeather();
            return true;
        }
        if(id==R.id.settings){
            Intent i=new Intent(getActivity(),SettingsActivity.class);
            startActivity(i);
        }
        return super.onOptionsItemSelected(item);
    }
    public void updateWeather(){
        FetchWeatherTask fetchWeatherTask=new FetchWeatherTask();
        SharedPreferences preferences= PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location=preferences.getString(getString(R.string.pref1_key),getString(R.string.pref1_default));
        fetchWeatherTask.execute(location);
    }

    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        List<String> weekForecast = new ArrayList<String>();
        mForecastAdapter =
                new ArrayAdapter<String>(
                        getActivity(),
                        R.layout.itemview,
                        R.id.list_item,
                        weekForecast);
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);
        ListView listView = (ListView) rootView.findViewById(R.id.list);
        listView.setAdapter(mForecastAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String message=mForecastAdapter.getItem(position).toString();
                Intent i=new Intent(getActivity(),Details.class);
                i.putExtra(Intent.EXTRA_TEXT,message);
                startActivity(i);
            }
        });
        return rootView;
    }
    public class FetchWeatherTask extends AsyncTask<String,Void,String[]>{
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
            SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitype=prefs.getString(getString(R.string.unit_key),getString(R.string.unit_metric));
            if(unitype.equals(getString(R.string.unit_imperial))){
                roundedHigh=Math.round(roundedHigh*1.8+32);
                roundedLow=Math.round(roundedLow*1.8+32);
            }
            String highLowStr = roundedHigh + "/" + roundedLow;
            return highLowStr;
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
                Log.e("sunshine"," is not working");
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

}
