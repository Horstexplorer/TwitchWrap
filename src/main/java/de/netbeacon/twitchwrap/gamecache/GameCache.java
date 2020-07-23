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

package de.netbeacon.twitchwrap.gamecache;

import de.netbeacon.twitchwrap.TwitchWrap;
import de.netbeacon.twitchwrap.exceptions.GameCacheException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class can be used to keep track of id -> name conversion for twitch game ids
 */
public class GameCache {

    private final TwitchWrap twitchWrap;
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    private final Logger logger = LoggerFactory.getLogger(GameCache.class);

    /**
     * Creates a new instance of this class
     * @param twitchWrap providing the okhttp client & login data
     */
    public GameCache(TwitchWrap twitchWrap){
        this.twitchWrap = twitchWrap;
    }

    /**
     * Can be used to get the name of a game by its id
     * <br>
     * This will request the data if it hasnt been cached already
     * @param id gameId
     * @return String
     */
    public String getNameOf(String id){
        if(cache.containsKey(id)){
            return cache.get(id);
        }else{
            try{
                JSONObject jsonObject = twitchWrap.request("https://api.twitch.tv/helix/games?id="+id);
                String name = jsonObject.getJSONArray("data").getJSONObject(0).getString("name");
                cache.put(id, name);
                return name;
            }catch (Exception e){
                logger.warn("Something Went Wrong Requesting The Name Matching The Game ID "+id, e);
                return "Unknown Game";
            }
        }
    }

    /**
     * Can be used to update the names of all games stored in the cache
     */
    public void updateAll(){
        try{
            // update all game ids
            List<String> gameids = new ArrayList<>();
            int processed = 0;
            for(Map.Entry<String, String> entry : cache.entrySet()){
                // check if the size would be over 100 (then we have already processed them)
                if(gameids.size()+1 > 100){
                    gameids.clear();
                }
                // add game id
                gameids.add(entry.getKey());
                processed++;
                // process if we have 100 game ids or if we have no more game ids
                if(gameids.size() == 100 || processed == cache.size()){
                    // build string
                    StringBuilder request = new StringBuilder();
                    Iterator<?> iterator = gameids.iterator();
                    while(iterator.hasNext()){
                        request.append(iterator.next()); // add value
                        if(iterator.hasNext()){
                            request.append("&id=");  // add delimiter
                        }
                    }
                    //parse json to new hashmap
                    String finalRequest = request.toString();
                    new Thread(() -> {
                        JSONObject json =  twitchWrap.request("https://api.twitch.tv/helix/games?id="+ finalRequest);
                        for(int n = 0; n < json.getJSONArray("data").length(); n++){
                            String id = json.getJSONArray("data").getJSONObject(n).getString("id");
                            String game = json.getJSONArray("data").getJSONObject(n).getString("name");
                            cache.put(id, game);
                        }
                    }).start();
                }
            }
        }catch (Exception e){
            logger.warn("An Error Occurred While Updating The GameCache", e);
        }
    }

    /**
     * Can be used to load the cache from a file
     * @param file File
     */
    public void loadFromFile(File file){
        try{
            if(!file.exists()){file.createNewFile();}
            else {
                String content = new String(Files.readAllBytes(file.toPath()));
                if(!content.isEmpty()){
                    JSONObject jsonObject = new JSONObject(content);
                    JSONArray jsonArray = jsonObject.getJSONArray("games");
                    for(int i = 0; i < jsonArray.length(); i++){
                        try{
                            JSONObject object = jsonArray.getJSONObject(i);
                            cache.put(object.getString("id"), object.getString("name"));
                        }catch (Exception e){
                            logger.warn("Something Went Wrong While Processing Entry "+i+" From JSON", e);
                        }
                    }
                }
            }
        }catch (Exception e){
            logger.error("Failed To Load Game Cache From File");
            throw new GameCacheException("Failed To Load Game Cache From File");
        }
        logger.debug("Loaded Game Cache From File");
    }

    /**
     * Can be used to store the cache to a file
     * @param file File
     */
    public void saveToFile(File file) {
        try{
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            JSONArray jsonArray = new JSONArray();
            for(Map.Entry<String, String> entry : cache.entrySet()){
                jsonArray.put(new JSONObject().put("id", entry.getKey()).put("name", entry.getValue()));
            }
            bufferedWriter.write(new JSONObject().put("games", jsonArray).toString());
            bufferedWriter.flush();
            bufferedWriter.close();
        }catch (IOException e){
            logger.error("Failed To Store Game Cache To File");
            throw new GameCacheException("Failed To Store Game Cache To File");
        }
        logger.debug("Stored Game Cache To File");
    }
}
