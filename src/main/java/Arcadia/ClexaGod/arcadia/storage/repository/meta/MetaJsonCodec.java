package Arcadia.ClexaGod.arcadia.storage.repository.meta;

import Arcadia.ClexaGod.arcadia.storage.repository.json.JsonCodec;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class MetaJsonCodec implements JsonCodec<MetaRecord> {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    @Override
    public String encode(MetaRecord record) {
        return GSON.toJson(record);
    }

    @Override
    public MetaRecord decode(String json) {
        return GSON.fromJson(json, MetaRecord.class);
    }
}
