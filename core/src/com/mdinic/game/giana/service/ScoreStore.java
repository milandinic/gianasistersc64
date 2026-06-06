package com.mdinic.game.giana.service;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/** Pure helpers to (de)serialize cached score lists and the submit outbox. */
public final class ScoreStore {

    private ScoreStore() {
    }

    public static String scoresToJson(List<Score> scores) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < scores.size(); i++) {
            Score s = scores.get(i);
            if (i > 0) {
                sb.append(',');
            }
            sb.append("{\"name\":").append(quote(s.getName())).append(",\"score\":").append(s.getScore())
                    .append(",\"level\":").append(s.getLevel()).append('}');
        }
        return sb.append(']').toString();
    }

    public static List<Score> scoresFromJson(String json) {
        List<Score> out = new ArrayList<Score>();
        if (json == null || json.trim().isEmpty()) {
            return out;
        }
        JsonValue root = new JsonReader().parse(json);
        if (root == null) {
            return out;
        }
        for (JsonValue v = root.child; v != null; v = v.next) {
            out.add(new Score(v.getString("name", ""), v.getInt("score", 0), v.getInt("level", 0)));
        }
        return out;
    }

    private static String quote(String raw) {
        String s = raw == null ? "" : raw;
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' || c == '\\') {
                sb.append('\\').append(c);
            } else if (c == '\n') {
                sb.append("\\n");
            } else if (c == '\r') {
                sb.append("\\r");
            } else if (c == '\t') {
                sb.append("\\t");
            } else {
                sb.append(c);
            }
        }
        return sb.append('"').toString();
    }
}
