package dan.dublinbustime;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;


import org.json.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;


public class MainActivity extends WearableActivity {

    private EditText searchText;

    private ListView lv;

    ArrayAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchText = findViewById(R.id.searchText);
        lv=findViewById(R.id.lv);

        searchText.setOnEditorActionListener(editorActionListener);

        adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1) {
            @SuppressLint("ResourceAsColor")
            @NonNull
            @Override
            public View getView(int position, View convertView, @NonNull ViewGroup parent) {
                // Cast the current view as a TextView
                TextView tv = (TextView) super.getView(position,convertView,parent);
                //set to text of the listview to center
                tv.setGravity(Gravity.CENTER_HORIZONTAL);
                tv.setTextColor(R.color.black);
                tv.setTextSize(14);
                return tv;
            }

        };
        lv.setAdapter(adapter);

        // Enables Always-on
        setAmbientEnabled();
    }



    private TextView.OnEditorActionListener editorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            callSyncTask();
            return false;
        }
    };


    private void callSyncTask(){
        new AsyncTask<String, Integer, ArrayList<String>>() {

            @Override
            protected  void onPreExecute() {
            }

            @Override
            protected ArrayList<String> doInBackground(String[] objects) {

                ArrayList<String> results = new ArrayList<>();

                String searchString = searchText.getText().toString();

                String uri = buildSearchURI(searchString);
                if(!uri.equals("UNDETERMINED") && !uri.equals("MALFORMED")) {
                    try {

                        BufferedReader br = createBufferedReader(uri);

                        String output;
                        Log.i("Output from Server ....", "");
                        while ((output = br.readLine()) != null) {
                            Log.i(output, "");
                            String json = output;
                            JSONObject obj = new JSONObject(json);
                            JSONArray busArray = obj.getJSONArray("results");

                            results = buildResult(busArray);

                            return results;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    results.add(uri);
                    return results;
                }

                return null;
            }

            @Override
            protected void onPostExecute(ArrayList<String> results) {
                    lv.setVisibility(View.VISIBLE);
                    adapter.clear();
                    if(results.isEmpty()) {
                        int h = Calendar.getInstance().getTime().getHours();

                        if(h >= 22 || h <= 6) {
                            adapter.add("No Results, buses may have finished for the night");
                        } else {
                            adapter.add("No Results");
                        }
                    } else {
                        adapter.addAll(results); //which will call notifydatasetchanged
                    }
            }
        }.execute();
    }


    private BufferedReader createBufferedReader(String uri) {
        try {
            URL url = new URL(uri);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            connection.setRequestMethod("GET");

            connection.setRequestProperty("Accept", "application/json");
            Log.i("xml : ", connection.getInputStream().toString());

            return new BufferedReader(new InputStreamReader(
                    (connection.getInputStream())));
        }catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String buildSearchURI(String searchText) {
        String searchURI = "https://data.smartdublin.ie/cgi-bin/rtpi/";
        searchURI += "realtimebusinformation?stopid=" + searchText;
        return searchURI;
    }

    private ArrayList<String> buildResult(JSONArray busArray) {

        ArrayList<String> results = new ArrayList<>();
        for (int i = 0; i < busArray.length(); i++) {
            try {
                String route = busArray.getJSONObject(i).getString("route");
                String duetime = busArray.getJSONObject(i).getString("duetime");
                if (!duetime.equals("Due")) {
                    duetime += " mins";
                }
                Log.i(results.toString(), "");
                results.add(route + " : " + duetime);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return results;
    }
}
