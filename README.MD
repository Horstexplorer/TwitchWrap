# TwitchWrap
##### Easy Wrapper For Get Request To The Twitch API
>Version: 1.0.0_1

## Usage
Create a new instance (should be reused)
```java
TwitchWrap twitchWrap = new TwitchWrap(userId, userSecret);
                        new TwitchWrap(userId, userSecret, existingBearer);     
```
Send request
```java
JSONObject result = twitchWrap.request(String url);
```
Get the name of a game by its id (instance should be reused)
```java
GameCache gameCache = new GameCache(twitchWrap); // should be reused
String name = gameCache.getNameOf(id);
```