package com.minidroid;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class Brain {

    private static class Block {
        List<String> triggers = new ArrayList<String>();
        List<String> replies = new ArrayList<String>();
    }

    private final List<Block> blocks = new ArrayList<Block>();
    private final Map<String, Block> special = new HashMap<String, Block>();
    private final Random rnd = new Random();
    private final SharedPreferences prefs;
    private String last = "";

    public Brain(Context ctx) {
        prefs = ctx.getSharedPreferences("minidroid", Context.MODE_PRIVATE);
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(ctx.getAssets().open("frases.txt"), "UTF-8"));
            Block cur = null;
            String line;
            while ((line = r.readLine()) != null) {
                line = line.replace("\uFEFF", "").trim();
                if (line.length() == 0) { cur = null; continue; }
                if (line.startsWith("//")) { continue; }
                if (line.startsWith(">")) {
                    if (cur != null) { cur.replies.add(line.substring(1).trim()); }
                } else if (line.startsWith("#")) {
                    cur = new Block();
                    special.put(line, cur);
                } else {
                    cur = new Block();
                    for (String t : line.split("\\|")) {
                        String n = norm(t);
                        if (n.length() > 0) { cur.triggers.add(n); }
                    }
                    blocks.add(cur);
                }
            }
        } catch (Exception e) {
            // si falla, usara frases por defecto
        } finally {
            try { if (r != null) { r.close(); } } catch (Exception e) { }
        }
    }

    static String norm(String s) {
        String t = Normalizer.normalize(s.toLowerCase(Locale.ENGLISH), Normalizer.Form.NFD);
        t = t.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        t = t.replaceAll("[^a-z0-9* ]", " ");
        return t.replaceAll(" +", " ").trim();
    }

    public String greeting() {
        return pickFrom(special.get("#inicio"), "Hola, soy Mini Droid. ¿Cómo estás?");
    }

    public String reply(String input) {
        String t = norm(input);
        if (t.length() == 0) {
            return pickFrom(special.get("#nose"), "Te escucho.");
        }

        String name = prefs.getString("nombre", "");
        if (t.contains("como me llamo") || t.contains("cual es mi nombre") || t.contains("sabes mi nombre")) {
            if (name.length() > 0) { return "Te llamas " + name + ". Lo tengo bien guardado."; }
            return "Todavía no me lo has dicho. Puedes escribir: me llamo, y tu nombre.";
        }

        String learned = learnName(input);
        if (learned != null) {
            prefs.edit().putString("nombre", learned).commit();
            String[] ok = {
                "Mucho gusto, " + learned + ". Qué bonito nombre.",
                "Encantado, " + learned + ". Aquí me tienes para charlar con calma.",
                "Qué gusto conocerte, " + learned + "."
            };
            return ok[rnd.nextInt(ok.length)];
        }

        String padded = " " + t + " ";
        Block best = null;
        int bestScore = 0;
        for (Block b : blocks) {
            int score = 0;
            for (String tr : b.triggers) {
                boolean hit;
                if (tr.endsWith("*")) {
                    String core = tr.substring(0, tr.length() - 1).trim();
                    hit = core.length() > 0 && padded.contains(" " + core);
                } else {
                    hit = padded.contains(" " + tr + " ");
                }
                if (hit) { score += tr.split(" ").length; }
            }
            if (score > bestScore) { best = b; bestScore = score; }
        }
        if (best == null) {
            return pickFrom(special.get("#nose"), "Cuéntame más, te escucho.");
        }
        return pickFrom(best, "Te escucho.");
    }

    private String learnName(String input) {
        String low = input.toLowerCase(Locale.ENGLISH);
        String[] keys = { "me llamo ", "mi nombre es " };
        for (String k : keys) {
            int i = low.indexOf(k);
            if (i >= 0 && i + k.length() <= input.length()) {
                String rest = input.substring(i + k.length()).trim();
                int end = 0;
                while (end < rest.length() && Character.isLetter(rest.charAt(end))) { end++; }
                if (end > 0) {
                    return rest.substring(0, 1).toUpperCase() + rest.substring(1, end).toLowerCase();
                }
            }
        }
        return null;
    }

    private String pickFrom(Block b, String fallback) {
        if (b == null || b.replies.isEmpty()) { return fallback; }
        String name = prefs.getString("nombre", "");
        if (name.length() == 0) { name = "amigo"; }
        String s = b.replies.get(rnd.nextInt(b.replies.size()));
        if (b.replies.size() > 1) {
            int tries = 0;
            while (s.equals(last) && tries < 6) {
                s = b.replies.get(rnd.nextInt(b.replies.size()));
                tries++;
            }
        }
        last = s;
        return s.replace("{nombre}", name);
    }
}
