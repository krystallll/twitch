package com.krystal.jupiter.service;
import java.util.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krystal.jupiter.entity.db.Item;
import com.krystal.jupiter.entity.db.ItemType;
import com.krystal.jupiter.entity.response.Game;
import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Service
public class GameService {

    private static final String TOKEN = "Bearer 3qtj59jicupwfzp3g6rbhlu2hgefwf";
    private static final String CLIENT_ID = "5pfs6cinlbygycegwwdepd6ih045v9";
    private static final String TOP_GAME_URL = "https://api.twitch.tv/helix/games/top?first=%s";
    private static final String GAME_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/games?name=%s";
    private static final int DEFAULT_GAME_LIMIT = 20;

    private static final String STREAM_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/streams?game_id=%s&first=%s";
    private static final String VIDEO_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/videos?game_id=%s&first=%s";
    private static final String CLIP_SEARCH_URL_TEMPLATE = "https://api.twitch.tv/helix/clips?game_id=%s&first=%s";
    private static final String TWITCH_BASE_URL = "https://www.twitch.tv/";
    private static final int DEFAULT_SEARCH_LIMIT = 20;


    private String buildGameURL(String url, String gameName, int limit) {
        if(gameName.equals("")){
            return String.format(url, limit);
        }else{
            try{
                gameName = URLEncoder.encode(gameName, "UTF-8");
            }catch (UnsupportedEncodingException ex){
                ex.printStackTrace();
            }
            return String.format(url, gameName);

        }
    }

    private String buildSearchURL (String url, String gameId, int limit){
        try{
            gameId = URLEncoder.encode(gameId,"UTF-8");
        }catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
        return String.format(url, gameId, limit);
    }

    private String searchTwitch(String url) throws TwitchException {
        CloseableHttpClient httpclient = HttpClients.createDefault();

        // Define the response handler to parse and return HTTP response body returned from Twitch
        ResponseHandler<String> responseHandler = response -> {
            int responseCode = response.getStatusLine().getStatusCode();
            if (responseCode != 200) {
                System.out.println("Response status: " + response.getStatusLine().getReasonPhrase());
                throw new TwitchException("Failed to get result from Twitch API");
            }
            HttpEntity entity = response.getEntity();
            if (entity == null) {
                throw new TwitchException("Failed to get result from Twitch API");
            }
            JSONObject obj = new JSONObject(EntityUtils.toString(entity));
            return obj.getJSONArray("data").toString();
        };

        try {
            // Define the HTTP request, TOKEN and CLIENT_ID are used for user authentication on Twitch backend
            HttpGet request = new HttpGet(url);
            request.setHeader("Authorization", TOKEN);
            request.setHeader("Client-Id", CLIENT_ID);
            return httpclient.execute(request, responseHandler);
        } catch (IOException e) {
            e.printStackTrace();
            throw new TwitchException("Failed to get result from Twitch API");
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    private List<Game> getGameList(String data){
        ObjectMapper mapper = new ObjectMapper();
        try {
            return Arrays.asList(mapper.readValue(data, Game[].class));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            throw new  RuntimeException("Failed to parse game data from Twitch API");
        }
    }

    // Integrate search() and getGameList() together, returns the top x popular games from Twitch.
    public List<Game> topGames(int limit) {
        if (limit <= 0) {
            limit = DEFAULT_GAME_LIMIT;
        }
        String url = buildGameURL(TOP_GAME_URL,"", limit);
        String data = searchTwitch(url);

        return getGameList(data);
    }

    // Integrate search() and getGameList() together, returns the dedicated game based on the game name.
    public Game searchGame(String gameName) {
        String url = buildGameURL(GAME_SEARCH_URL_TEMPLATE, gameName,0);
        String data = searchTwitch(url);
        List<Game> gameList = getGameList(data);

        if (gameList.size() != 0) {
            return gameList.get(0);
        }
        return null;
    }

    private List<Item> getItemList(String data) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return Arrays.asList(mapper.readValue(data, Item[].class));
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
            throw new TwitchException("Failed to parse item data from Twitch API");
        }
    }

    private List<Item> searchStreams(String gameId, int limit)  {
        String url = buildSearchURL(STREAM_SEARCH_URL_TEMPLATE,gameId,limit);
        String data = searchTwitch(url);
        List<Item> streams = getItemList(data);

        for(Item item : streams){
            item.setUrl(TWITCH_BASE_URL + item.getBroadcasterName());
            item.setType(ItemType.STREAM);
      }
        return streams;
    }

    private List<Item> searchClips(String gameId, int limit)  {
        String url = buildSearchURL(CLIP_SEARCH_URL_TEMPLATE,gameId,limit);
        String data = searchTwitch(url);
        List<Item> clips = getItemList(data);
        for (Item item : clips) {
            item.setType(ItemType.CLIP);
        }
        return clips;
    }

    private List<Item> searchVideos(String gameId, int limit)  {
        String url = buildSearchURL(VIDEO_SEARCH_URL_TEMPLATE,gameId,limit);
        String data = searchTwitch(url);
        List<Item> videos = getItemList(data);
        for (Item item : videos) {
            item.setGameId(gameId);
            item.setType(ItemType.VIDEO);
        }
        return videos;
    }

    private List<Item> searchByType(String gameId, ItemType type, int limit){
        List<Item> items = new ArrayList<>();
        switch (type){
            case STREAM:
                items = searchStreams(gameId,limit);
                break;
            case VIDEO:
                items = searchVideos(gameId,limit);
                break;
            case CLIP:
                items = searchClips(gameId,limit);
                break;
        }
        return items;
    }


    public Map<String, List<Item>> searchItems(String gameId){
        Map<String, List<Item>> itemMap = new HashMap<>();
        for(ItemType type : ItemType.values()){
            itemMap.put(type.name(), searchByType(gameId, type, DEFAULT_SEARCH_LIMIT));
        }
        return itemMap;
    }






}






