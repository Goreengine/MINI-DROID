package com.minidroid;

import java.util.Locale;
import java.util.Random;

/** Personaje de roleplay creado por el usuario desde el chat. */
public class CustomRole {

    private static final String[] MARKERS = {
        "ahora eres ", "finge que eres ", "actua como ", "actúa como ",
        "quiero que seas ", "interpreta a ", "personaje: ", "personaje:"
    };
    private static final String[] NAME_KEYS = { " llamado ", " llamada ", " se llama ", " de nombre " };

    private final Random rnd = new Random();
    private final String name;
    private final String shown;
    private final String tone;
    private String scene = "";

    private CustomRole(String name, String shown, String tone) {
        this.name = name;
        this.shown = shown;
        this.tone = tone;
    }

    private static String firstWord(String s) {
        s = s.trim();
        int e = 0;
        while (e < s.length() && Character.isLetter(s.charAt(e))) { e++; }
        return s.substring(0, e);
    }

    private static String cutEnd(String s) {
        s = s.trim();
        while (s.length() > 0 && ".!,;".indexOf(s.charAt(s.length() - 1)) >= 0) {
            s = s.substring(0, s.length() - 1).trim();
        }
        return s;
    }

    /** Devuelve un personaje si el texto lo define ("finge que eres un dragón..."), o null. */
    public static CustomRole tryCreate(String input) {
        if (input.indexOf('?') >= 0) { return null; }
        String low = input.toLowerCase(Locale.ENGLISH);
        int best = -1;
        String bestMarker = null;
        for (String m : MARKERS) {
            int i = low.indexOf(m);
            if (i >= 0 && (best < 0 || i < best)) { best = i; bestMarker = m; }
        }
        if (bestMarker == null) { return null; }
        String rest = cutEnd(input.substring(best + bestMarker.length()));
        if (rest.length() < 3) { return null; }
        String lowRest = rest.toLowerCase(Locale.ENGLISH);

        String name = null;
        String shown = rest;
        for (String k : NAME_KEYS) {
            int j = lowRest.indexOf(k);
            if (j >= 0) {
                String w = firstWord(rest.substring(j + k.length()));
                if (w.length() > 0) {
                    name = w;
                    String joined = rest.substring(0, j) + rest.substring(j + k.length() + w.length());
                    shown = joined.trim();
                    break;
                }
            }
        }
        if (name == null) {
            String r = rest;
            String[] arts = { "un ", "una ", "el ", "la ", "mi " };
            for (String a : arts) {
                if (lowRest.startsWith(a)) { r = rest.substring(a.length()); break; }
            }
            name = firstWord(r);
        }
        if (name.length() == 0) { name = "Personaje"; }
        name = name.substring(0, 1).toUpperCase() + name.substring(1);
        if (shown.length() == 0) { shown = rest; }

        String n = Brain.norm(rest);
        String tone = "calm";
        if (n.contains("grun") || n.contains("malhumor") || n.contains("serio") || n.contains("seria")) {
            tone = "grumpy";
        } else if (n.contains("timid") || n.contains("vergonz")) {
            tone = "shy";
        } else if (n.contains("alegr") || n.contains("bromist") || n.contains("gracios") || n.contains("divertid")) {
            tone = "playful";
        } else if (n.contains("misterios") || n.contains("oscur") || n.contains("enigm")) {
            tone = "mysterious";
        }
        return new CustomRole(name, shown, tone);
    }

    public String intro(String user) {
        return "*" + name + " aparece ante ti.* Soy " + name + ". ¿Qué haremos hoy, " + user + "? "
                + "Puedes decirme dónde estamos escribiendo: estamos en, y el lugar. "
                + "Cuando quieras volver a platicar normal, escribe: salir del rol.";
    }

    private String pick(String... o) {
        return o[rnd.nextInt(o.length)];
    }

    private String opener() {
        if (tone.equals("grumpy")) {
            return pick("*" + name + " gruñe por lo bajo.*", "*" + name + " cruza los brazos.*");
        } else if (tone.equals("shy")) {
            return pick("*" + name + " baja un poco la mirada.*", "*" + name + " se encoge de hombros con timidez.*");
        } else if (tone.equals("playful")) {
            return pick("*" + name + " se ríe.*", "*" + name + " da una pequeña voltereta.*");
        } else if (tone.equals("mysterious")) {
            return pick("*" + name + " te observa entre las sombras.*", "*Una brisa extraña envuelve a " + name + ".*");
        }
        return pick("*" + name + " sonríe con calma.*", "*" + name + " te escucha con atención.*");
    }

    private static boolean any(String p, String... keys) {
        for (String k : keys) {
            if (k.endsWith("*")) {
                String core = k.substring(0, k.length() - 1);
                if (p.contains(" " + core)) { return true; }
            } else if (p.contains(" " + k + " ")) {
                return true;
            }
        }
        return false;
    }

    public String reply(String input, String user) {
        String t = Brain.norm(input);
        String p = " " + t + " ";
        String low = input.toLowerCase(Locale.ENGLISH);

        int si = low.indexOf("estamos en ");
        if (si >= 0) {
            String s = cutEnd(input.substring(si + 11));
            if (s.length() > 0) {
                scene = s;
                return opener() + " Miras a tu alrededor: estás en " + s
                        + ". Todo se siente muy real. ¿Qué haces primero?";
            }
        }

        String core;
        if (any(p, "hola", "buenas", "hey")) {
            core = pick("Me alegra verte, " + user + ". ¿Qué aventura toca hoy?",
                    "Bienvenido, " + user + ". ¿Por dónde empezamos?");
        } else if (any(p, "quien eres", "como te llamas", "tu nombre")) {
            core = "Soy " + name + ", " + shown + ". Y tú eres " + user + ", ¿verdad?";
        } else if (any(p, "donde estamos", "donde estoy")) {
            if (scene.length() > 0) {
                core = "Estamos en " + scene + ". ¿Quieres explorarlo?";
            } else {
                core = "Estamos en un lugar que todavía no tiene nombre. Dime tú dónde: escribe estamos en, y el lugar.";
            }
        } else if (any(p, "gracias")) {
            core = pick("De nada, " + user + ". Me gusta vivir esta historia contigo.",
                    "Es un placer, " + user + ". ¿Seguimos?");
        } else if (any(p, "adios", "chao", "me voy", "nos vemos")) {
            core = "Hasta pronto, " + user + ". Si quieres volver a platicar normal, escribe: salir del rol.";
        } else if (any(p, "miedo", "asustad*")) {
            core = pick("Tranquilo, " + user + ". Estoy aquí contigo, no pasará nada que no podamos manejar.",
                    "Respira hondo. Vamos juntos, paso a paso.");
        } else if (any(p, "triste*", "mal", "llorar", "cansad*", "agotad*")) {
            core = pick("Me importa cómo te sientes, " + user + ". Tomemos un respiro antes de seguir.",
                    "Está bien parar un momento. Aquí no hay prisa.");
        } else if (any(p, "feliz", "contento", "contenta", "alegre", "genial")) {
            core = pick("¡Qué bueno! Se nota en el ambiente. Aprovechemos esa energía.",
                    "Me alegra mucho, " + user + ". ¿Qué hacemos con este buen momento?");
        } else if (input.indexOf('*') >= 0) {
            core = pick("Noto tu movimiento y me quedo atento. El lugar parece reaccionar a lo que haces.",
                    "Hacer eso cambia algo en el ambiente. ¿Qué esperas encontrar?",
                    "Buena jugada, " + user + ". Algo se mueve cerca. ¿Lo sigues?");
        } else if (any(p, "pelear", "luchar", "atacar")) {
            core = pick("Piénsalo bien, " + user + ". A veces una buena idea vale más que un golpe. ¿Qué plan tienes?",
                    "Te preparas, pero primero observas al rival. ¿Qué haces?");
        } else if (any(p, "vamos", "explorar*", "caminar", "abrir", "mirar", "buscar", "correr",
                "saltar", "avanzar", "entrar", "subir", "bajar")) {
            core = pick("Avanzas con cuidado. A lo lejos hay una luz tenue y un sonido que no reconoces. ¿Te acercas?",
                    "Encuentras algo curioso en el camino: una puerta pequeña medio escondida. ¿La abres?",
                    "Das unos pasos y el suelo cruje. Hay dos caminos delante de ti. ¿Cuál tomas?");
        } else if (input.indexOf('?') >= 0) {
            core = pick("Buena pregunta... déjame pensar. ¿Tú qué crees, " + user + "?",
                    "Hmm, tal vez la respuesta esté cerca. ¿Lo averiguamos juntos?");
        } else {
            core = echo(input, user);
        }
        return opener() + " " + core;
    }

    private String echo(String input, String user) {
        String s = cutEnd(input);
        if (s.length() < 2 || s.length() > 70) { return narrative(); }
        s = s.substring(0, 1).toLowerCase(Locale.ENGLISH) + s.substring(1);
        s = swap(s);
        return pick("Así que " + s + "... interesante. ¿Y qué piensas hacer ahora?",
                "Dices que " + s + ". Cuéntame más, " + user + ".",
                "Mmm, " + s + ". Eso cambia las cosas. ¿Qué sigue?");
    }

    private static String swap(String s) {
        String[] from = { "yo", "mi", "mis", "me", "soy", "estoy", "quiero", "tengo", "voy",
                "puedo", "necesito", "siento", "creo", "veo" };
        String[] to = { "tú", "tu", "tus", "te", "eres", "estás", "quieres", "tienes", "vas",
                "puedes", "necesitas", "sientes", "crees", "ves" };
        String[] w = s.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String x : w) {
            String c = x.toLowerCase(Locale.ENGLISH);
            String rep = x;
            for (int k = 0; k < from.length; k++) {
                if (c.equals(from[k])) { rep = to[k]; break; }
            }
            if (sb.length() > 0) { sb.append(' '); }
            sb.append(rep);
        }
        return sb.toString();
    }

    private String narrative() {
        if (scene.length() > 0) {
            return pick("En " + scene + ", el aire cambia un instante y algo se mueve a lo lejos. ¿Lo investigamos?",
                    "Todo en " + scene + " parece esperar tu siguiente paso. ¿Qué haces?");
        }
        return pick("Algo se mueve a lo lejos... ¿lo investigamos?",
                "El silencio dura un momento y luego se oye un ruido extraño. ¿Qué haces?",
                "Hay mucho por descubrir aquí. ¿Hacia dónde vamos?");
    }
}
