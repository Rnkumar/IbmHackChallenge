package com.codeworks.projects.collectingsocialdata;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneInput;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneOptions;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.Utterance;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SolutionActivity extends AppCompatActivity {

    ToneOptions toneOptions;
    ToneAnalyzer services;
    ProgressDialog progressDialog;
    TextView nameTextView,resultDataTextView,joy,anger,tentative,fear,sadness,analytical,confident;
    ArrayList<TextView> textViews;
    String tag="";
    int max = Integer.MIN_VALUE;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_solution);
        getSupportActionBar().setTitle(R.string.mood_analysis_title);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        nameTextView = findViewById(R.id.nametext);
        setName();
        textViews = new ArrayList<>();
        progressDialog = new ProgressDialog(this);
        joy = findViewById(R.id.one);
        joy.setTag("joy");
        anger = findViewById(R.id.two);
        anger.setTag("anger");
        tentative = findViewById(R.id.three);
        tentative.setTag("tentative");
        fear = findViewById(R.id.four);
        fear.setTag("fear");
        sadness = findViewById(R.id.five);
        sadness.setTag("sadness");
        analytical = findViewById(R.id.six);
        analytical.setTag("analytical");
        confident = findViewById(R.id.seven);
        confident.setTag("confident");

        textViews.add(joy);
        textViews.add(analytical);
        textViews.add(anger);
        textViews.add(sadness);
        textViews.add(confident);
        textViews.add(tentative);
        textViews.add(fear);

        services = new ToneAnalyzer(getString(R.string.tone_analyser_versiondate));
        services.setUsernameAndPassword(getString(R.string.tone_analyzer_username),
                getString(R.string.tone_analyzer_password));
      //  resultDataTextView = findViewById(R.id.resultdata);
        ArrayList<String> resultData = getIntent().getStringArrayListExtra("data");
        StringBuilder finalData= new StringBuilder();
        Log.e("Size","Size: "+resultData.size());
        for (int i=0;i<resultData.size();i++){
            finalData.append(resultData.get(i).replace("."," ")).append(".\n");
        }
        toneOptions = new ToneOptions.Builder()
                .text(finalData.toString())
                .sentences(false)
                .build();
        Task task=new Task();
        task.execute();
    }

    private void setName() {
        SharedPreferences preferences = getSharedPreferences(getString(R.string.sharedpreferencename), Context.MODE_PRIVATE);
        String fbName = preferences.getString("fbusername","");
        String twitterName = preferences.getString("twitterusername","");
        String name = "Hello ";
        if (!fbName.equals("")){
            name+=fbName+"!";
        }else if (!twitterName.equals("")){
            name+=twitterName+"!";
        }else{
            name+="Guest!";
        }
        nameTextView.setText(name);
    }

    public void getSuggestions(View view) {
        if (max==Integer.MIN_VALUE)
            tag = "joy";
        Intent intent = new Intent(this,SuggestionsActivity.class);
        intent.putExtra("tag",tag);
        startActivity(intent);
    }

    class Task extends AsyncTask<String,Void,String> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage("Analyzing Mood");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {
            ToneAnalysis tone = services.tone(toneOptions).execute();
            return tone.toString();
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            progressDialog.dismiss();
         //   resultDataTextView.setText(s);
            try {
                JSONObject object = new JSONObject(s);
                JSONObject document_tone = object.getJSONObject("document_tone");
                JSONArray tones = document_tone.getJSONArray("tones");
                for (int i=0;i<tones.length();i++){
                    JSONObject toneElement = tones.getJSONObject(i);
                    String tone_id = toneElement.getString("tone_id");
                    int score = (int)(toneElement.getDouble("score")*100);

                    for(int j=0;j<textViews.size();j++){
                        if (max<score) {
                            max = score;
                            tag = tone_id;
                        }
                        TextView item =  textViews.get(j);
                        if (textViews.get(j).getTag().equals(tone_id)){
                            String textData = tone_id.toUpperCase()+"\n"+score+"%";
                            item.setText(textData);
                        }
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.e("Final Result",s);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                super.onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
