package com.codeworks.projects.collectingsocialdata;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneInput;
import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterApiClient;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.core.services.StatusesService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;

public class MainScreenActivity extends AppCompatActivity {
    CallbackManager callbackManager;
    private static final String EMAIL = "email";
    LoginButton loginButton;
    TwitterLoginButton twitterLoginButton;
    Result<TwitterSession> result;
    TwitterCore twitterCore;
    ProgressDialog dialog;
    ArrayList<String> list;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TwitterConfig config = new TwitterConfig.Builder(this)
                .logger(new DefaultLogger(Log.ERROR))
                .twitterAuthConfig(new TwitterAuthConfig(getString(R.string.com_twitter_sdk_android_CONSUMER_KEY), getString(R.string.com_twitter_sdk_android_CONSUMER_SECRET)))
                .debug(true)
                .build();
        Twitter.initialize(config);
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);
        setContentView(R.layout.activity_main_screen);
        getSupportActionBar().hide();
        init();
    }

    void init(){
        dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage(getString(R.string.collect_data_progressdialog_text));
        list = new ArrayList<>();
        twitterCore = TwitterCore.getInstance();
        twitterLoginButton = findViewById(R.id.twitter_login_button);
        twitterLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                twitterCore.getSessionManager().clearActiveSession();
                twitterLoginButton.setText(R.string.twitter_login_alert_text);
            }
        });
        checkTwitterLoggedIn();
        twitterLoginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
//                if (result.response!=null)
//                Log.e("Result","Rslt: "+result.response.toString());
//                Log.e("Results","Result: "+result.data.toString());
                checkTwitterLoggedIn();
                String username = result.data.getUserName();
                setUsername(username,1);
                twitterLoginButton.setText(getString(R.string.logouttext));
                Toast.makeText(MainScreenActivity.this, "success", Toast.LENGTH_SHORT).show();
                MainScreenActivity.this.result = result;
            }

            @Override
            public void failure(TwitterException exception) {
                checkTwitterLoggedIn();
                Log.e("Exception in Login",exception.getMessage());
            }
        });
        callbackManager = CallbackManager.Factory.create();
        loginButton = findViewById(R.id.fb_login_button);
        loginButton.setReadPermissions(Arrays.asList(EMAIL));
        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                GraphRequest request = GraphRequest.newMeRequest(
                        loginResult.getAccessToken(),
                        new GraphRequest.GraphJSONObjectCallback() {

                            @Override
                            public void onCompleted(JSONObject object, GraphResponse response) {
                            //    Log.v("Main", response.toString());
                                try {
                                    setUsername(object.getString("name"),0);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                Bundle parameters = new Bundle();
                parameters.putString("fields", "id,name,email,gender, birthday");
                request.setParameters(parameters);
                request.executeAsync();
                Toast.makeText(MainScreenActivity.this, "Success", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancel() {
                // App code
                Toast.makeText(MainScreenActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(FacebookException exception) {
                // App code
                Toast.makeText(MainScreenActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }
        });
        if (!checkInternet()){
            Toast.makeText(this, R.string.check_internet_text, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkTwitterLoggedIn() {
        TwitterSession twitterSession = twitterCore.getSessionManager().getActiveSession();
        Result<TwitterSession> twitterSessionResult = new Result<>(twitterSession,null);
        this.result = twitterSessionResult;
        if (twitterSession!=null){
            TwitterAuthToken token = twitterSession.getAuthToken();
            if (token!=null)
            twitterLoginButton.setText(getString(R.string.logouttext));
            else{
                twitterLoginButton.setText(getString(R.string.twitter_login_alert_text));
                return false;
            }

            return true;
        }else{
            twitterLoginButton.setText(getString(R.string.twitter_login_alert_text));
            return false;
        }
    }

    private boolean checkFacebookLogIn(){
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        boolean isLoggedIn = accessToken != null && !accessToken.isExpired();
        return isLoggedIn;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (FacebookSdk.isFacebookRequestCode(requestCode))
            callbackManager.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        twitterLoginButton.onActivityResult(requestCode,resultCode,data);
    }

    public void updateData(){
        GraphRequest request = GraphRequest.newGraphPathRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/feed",
                new GraphRequest.Callback() {
                    @Override
                    public void onCompleted(GraphResponse response) {
                        try {
                            JSONArray arr = response.getJSONObject().getJSONArray("data");
                            for (int i=0;i<arr.length();i++) {
                                JSONObject object = (JSONObject) arr.get(i);
                                if (object.has("message")) {
                                  list.add(object.getString("message"));
                                }
                                if (object.has("description")){
                                    list.add(object.getString("description"));
                                }
                                if (object.has("story")){
                                    list.add(object.getString("story"));
                                }
                                if (object.has("name")){
                                    list.add(object.getString("name"));
                                }
                                if (object.has("caption")){
                                    list.add(object.getString("caption"));
                                }
                            }
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.dismiss();
                                    Intent intent = new Intent(MainScreenActivity.this,SolutionActivity.class);
                                    intent.putExtra("data",list);
                                    intent.putExtra("test",1);
                                    startActivity(intent);
                                }
                            });
                        } catch (JSONException e) {
                            Toast.makeText(MainScreenActivity.this, "Try Again", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                        //Log.e("Response", String.valueOf(response.getJSONObject()));
                    }
                });

        Bundle parameters = new Bundle();
        parameters.putString("fields", "message,story,description,caption,name,status_type");
        parameters.putString("limit", "30");
        request.setParameters(parameters);
        request.executeAsync();
    }

    public void provideTweets(final int i){
        TwitterApiClient client = twitterCore.getApiClient(result.data);
        StatusesService service = client.getStatusesService();

        Call<List<Tweet>> tweets = service.userTimeline(result.data.getUserId(),null,50,null,null,null,true,true,false);
        tweets.enqueue(new Callback<List<Tweet>>() {
            @Override
            public void success(Result<List<Tweet>> result) {
                //Toast.makeText(MainScreenActivity.this, "Got Tweets", Toast.LENGTH_SHORT).show();
                List<Tweet> tweetlist = result.data;
                for(int i=0;i<tweetlist.size();i++){
                    list.add(tweetlist.get(i).text);
                   // Log.e("Data",i+": "+tweetlist.get(i).text);
                }
                if (i==1)
                updateData();
                else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Intent intent = new Intent(MainScreenActivity.this,SolutionActivity.class);
                            intent.putExtra("data",list);
                            intent.putExtra("test",0);
                            startActivity(intent);
                        }
                    });
                }
            }

            @Override
            public void failure(TwitterException exception) {
                if (i==1)
                    updateData();
                else{
                    Toast.makeText(MainScreenActivity.this, "Try Again", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    public void setUsername(String username,int i) {
        SharedPreferences preferences=getSharedPreferences(getString(R.string.sharedpreferencename), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
       // Log.e("Username",username);
        if (i==0)
            editor.putString("fbusername",username);
        else
            editor.putString("twitterusername",username);
        editor.commit();
    }

    public boolean checkLoginsAndReturn(){
        boolean fb = checkFacebookLogIn();
        boolean twitter = checkTwitterLoggedIn();
        if (fb&&twitter){
            dialog.show();
            provideTweets(1);
            return true;
        }else if(fb){
            dialog.show();
            updateData();
            return true;
        }else if (twitter){
            dialog.show();
            provideTweets(0);
            return true;
        }

        return false;
    }

    public void proceedAnalysis(View view) {
        if (!checkLoginsAndReturn()){
            Toast.makeText(this, R.string.social_login_alerttext, Toast.LENGTH_SHORT).show();
        }
    }

    public boolean checkInternet(){
        ConnectivityManager manager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return info!=null&&info.isConnected();
    }

}
