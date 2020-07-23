/*
 *     Copyright 2020 Horstexplorer @ https://www.netbeacon.de
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.netbeacon.twitchwrap;

import de.netbeacon.twitchwrap.auth.ApplicationBearerToken;
import de.netbeacon.twitchwrap.exceptions.WorkerException;
import de.netbeacon.twitchwrap.interceptor.RateLimitInterceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONObject;

/**
 * Provides the functionality to send GET requests to the twitch api
 */
public class TwitchWrap {

    private final OkHttpClient okHttpClient;
    private final ApplicationBearerToken applicationBearerToken;

    /**
     * Used to create a new instance of this class
     * @param userID twitch user id
     * @param userSecret twitch user secret
     */
    public TwitchWrap(String userID, String userSecret){
        okHttpClient = new OkHttpClient.Builder().addInterceptor(new RateLimitInterceptor()).build();
        this.applicationBearerToken = new ApplicationBearerToken(this, userID, userSecret, "");
    }

    /**
     * Used to create a new instance of this class
     * @param userID twitch user id
     * @param userSecret twitch user secret
     * @param knownToken an already existing token
     */
    public TwitchWrap(String userID, String userSecret, String knownToken){
        okHttpClient = new OkHttpClient.Builder().addInterceptor(new RateLimitInterceptor()).build();
        this.applicationBearerToken = new ApplicationBearerToken(this, userID, userSecret, knownToken);
    }

    /**
     * Used to get the OKHTTPClient
     * @return OKHTTPClient
     */
    public OkHttpClient getOKHTTPClient(){
        return okHttpClient;
    }

    /**
     * Can be used to execute a new request
     * @param url String with parameter
     * @return JSONObject
     */
    public JSONObject request(String url){
        Response response = null;
        try{
            if(!applicationBearerToken.isValid()){
                applicationBearerToken.update();
            }
            Request request = new Request.Builder()
                    .get()
                    .header("Client-ID", applicationBearerToken.getClientID())
                    .header("Authorization", "Bearer "+applicationBearerToken.getBearerToken())
                    .url(url)
                    .build();
            response = okHttpClient.newCall(request).execute();
            int code = response.code();
            if(code == 200){
                ResponseBody responseBody = response.body();
                if(responseBody == null){
                    throw new Exception("Unexpected Response: No Data Received");
                }
                return new JSONObject(new String(responseBody.bytes()));
            }else{
                throw new Exception("Unexpected Response Code: "+code);
            }
        }catch (Exception e){
            throw new WorkerException("Failed To Execute Request: "+e.getMessage());
        }finally {
            if(response != null){
                response.close();
            }
        }
    }
}
