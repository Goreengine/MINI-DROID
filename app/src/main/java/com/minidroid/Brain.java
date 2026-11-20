package com.minidroid;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final Map<String, List<Block>> roles = new LinkedHashMap<String, List<Block>>();
    private final Map<String, Map<String, Block>> roleSpecial = new HashMap<String, Map<String, Block>>();
    private final Map<String, String> aliases = new HashMap<String, String>();
    private final Random rnd = new Random();
    private final SharedPreferences prefs;
    private String last = "";
    private String role = null;
    private boolean awaiting = false;
    private CustomRole custom = null;

    public Brain(Context ctx) {
        prefs = ctx.getSharedPreferences("minidroid", Context.MODE_PRIVATE);
        load(ctx, "frases.txt");
        load(ctx, "roles.txt");
    }

    private void load(Context ctx, String file) {
        BufferedReader r = null;
        try {
            r = new BufferedReader(new InputStreamReader(ctx.getAssets().open(file), "UTF-8"));
            Block cur = null;
            String curRole = null;
            String line;
            while ((line = r.readLine()) != null) {
                line = line.replace("\uFEFF", "").trim();
                if (line.length() == 0) { cur = null; continue; }
                if (line.startsWith("//")) { continue; }
                if (line.startsWith("@")) {
                    String[] parts = line.substring(1).split("\\|");
                    curRole = norm(parts[0]);
                    if (!roles.containsKey(curRole)) {
                        roles.put(curRole, new ArrayList<Block>());
                        roleSpecial.put(curRole, new HashMap<String, Block>());
                    }
                    for (int k = 1; k < parts.length; k++) {
                        String a = norm(parts[k]);
                        if (a.length() > 0) { aliases.put(a, curRole); }
                    }
                    cur = null;
                    continue;
                }
                if (line.startsWith(">")) {
                    if (cur != null) { cur.replies.add(line.substring(1).trim()); }
                } else if (line.startsWith("#")) {
                    cur = new Block();
                    Map<String, Block> target = (curRole == null) ? special : roleSpecial.get(curRole);
                    target.put(line, cur);
                } else {
                    cur = new Block();
                    for (String t : line.split("\\|")) {
                        String n = norm(t);
                        if (n.length() > 0) { cur.triggers.add(n); }
                    }
                    List<Block> target = (curRole == null) ? blocks : roles.get(curRole);
                    target.add(cur);
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

        String math = MathSolver.solve(input);
        if (math != null) { return math; }

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

        // crear un personaje propio desde el chat
        CustomRole created = CustomRole.tryCreate(input);
        if (created != null) {
            custom = created;
            role = "@custom";
            awaiting = false;
            return custom.intro(displayName());
        }
        if (t.contains("cambiar de personaje") || t.contains("cambiar personaje") || t.contains("otro personaje")) {
            role = null;
            awaiting = true;
            return menu();
        }

        // salir del rol
        if (role != null || awaiting) {
            if (t.contains("salir del rol") || t.contains("terminar el rol") || t.contains("fin del rol")
                    || t.contains("dejar el rol") || t.contains("ya no quiero jugar") || t.equals("salir")) {
                role = null;
                awaiting = false;
                return "Listo, {nombre}, salimos del rol y volvemos a platicar normal. Fue muy divertido."
                        .replace("{nombre}", displayName());
            }
        }

        // eligiendo personaje
        if (awaiting) {
            String r = findRole(t);
            if (r != null) { return enterRole(r); }
            if (t.equals("no") || t.contains("cancelar") || t.contains("nada")) {
                awaiting = false;
                return "Está bien, seguimos platicando normal.";
            }
            return menu();
        }

        // dentro de un rol
        if (role != null) {
            if (role.equals("@custom") && custom != null) {
                return custom.reply(input, displayName());
            }
            Block b = bestBlock(roles.get(role), t);
            if (b == null) {
                return pickFrom(roleSpecial.get(role).get("#nose"), "Cuéntame más.");
            }
            return pickFrom(b, "Sigamos.");
        }

        // empezar un rol
        if (wantsRole(t)) {
            String r = findRole(t);
            if (r != null) { return enterRole(r); }
            awaiting = true;
            return menu();
        }

        Block best = bestBlock(blocks, t);
        if (best == null) {
            return pickFrom(special.get("#nose"), "Cuéntame más, te escucho.");
        }
        return pickFrom(best, "Te escucho.");
    }

    private boolean wantsRole(String t) {
        String p = " " + t + " ";
        return p.contains(" rol ") || p.contains(" roleplay ") || p.contains(" rolplay ")
                || p.contains(" juguemos a ser ") || p.contains(" jugar a ser ")
                || p.contains(" finge que eres ") || p.contains(" actua como ")
                || p.contains(" hagamos un rol ") || p.contains(" juego de rol ");
    }

    private String findRole(String t) {
        String p = " " + t + " ";
        for (String key : roles.keySet()) {
            if (p.contains(" " + key + " ")) { return key; }
        }
        for (Map.Entry<String, String> e : aliases.entrySet()) {
            if (p.contains(" " + e.getKey() + " ")) { return e.getValue(); }
        }
        return null;
    }

    private String enterRole(String r) {
        role = r;
        awaiting = false;
        return pickFrom(roleSpecial.get(r).get("#inicio"), "Empezamos el rol.");
    }

    private String menu() {
        StringBuilder sb = new StringBuilder();
        for (String k : roles.keySet()) {
            if (sb.length() > 0) { sb.append(", "); }
            sb.append(k);
        }
        return "Con gusto. ¿Qué rol quieres? Tengo: " + sb.toString()
                + ". Elige uno y empezamos. O crea el tuyo escribiendo, por ejemplo: finge que eres un dragón amable llamado Ignis."
                + " Para salir escribe: salir del rol.";
    }

    private Block bestBlock(List<Block> list, String t) {
        String padded = " " + t + " ";
        Block best = null;
        int bestScore = 0;
        for (Block b : list) {
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
        return best;
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

    private String displayName() {
        String name = prefs.getString("nombre", "");
        return name.length() == 0 ? "amigo" : name;
    }

    private String pickFrom(Block b, String fallback) {
        if (b == null || b.replies.isEmpty()) { return fallback; }
        String s = b.replies.get(rnd.nextInt(b.replies.size()));
        if (b.replies.size() > 1) {
            int tries = 0;
            while (s.equals(last) && tries < 6) {
                s = b.replies.get(rnd.nextInt(b.replies.size()));
                tries++;
            }
        }
        last = s;
        return s.replace("{nombre}", displayName());
    }
}
