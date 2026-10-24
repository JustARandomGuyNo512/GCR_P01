package com.sheridan.gcr;

import com.google.gson.JsonObject;

public interface IJsonSync {
    void writeToJson(JsonObject jsonObject);
    void loadFromJson(JsonObject jsonObject);
}
