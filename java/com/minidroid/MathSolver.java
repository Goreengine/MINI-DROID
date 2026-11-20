package com.minidroid;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class MathSolver {

    private static final Map<String, Double> NUMS = new HashMap<String, Double>();
    private static final Set<String> FILLER = new HashSet<String>();
    private static final Random RND = new Random();

    static {
        String[] n = {"cero", "uno", "un", "dos", "tres", "cuatro", "cinco", "seis", "siete", "ocho",
                "nueve", "diez", "once", "doce", "trece", "catorce", "quince", "dieciseis",
                "diecisiete", "dieciocho", "diecinueve", "veinte", "veintiuno", "veintidos",
                "veintitres", "veinticuatro", "veinticinco", "veintiseis", "veintisiete",
                "veintiocho", "veintinueve", "treinta", "cuarenta", "cincuenta", "sesenta",
                "setenta", "ochenta", "noventa", "cien"};
        double[] v = {0, 1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 40, 50, 60, 70, 80, 90, 100};
        for (int i = 0; i < n.length; i++) { NUMS.put(n[i], Double.valueOf(v[i])); }
        String[] f = {"cuanto", "cuantos", "es", "son", "sera", "seria", "da", "dime", "calcula",
                "calcular", "resuelve", "resolver", "resultado", "igual", "me", "puedes", "podrias",
                "la", "el", "de", "mini", "droid", "hola", "hey", "oye", "sabes", "cual", "que",
                "suma", "operacion"};
        for (String w : f) { FILLER.add(w); }
    }

    /** Devuelve la respuesta con el resultado, o null si el texto no es una cuenta. */
    public static String solve(String input) {
        String s = Normalizer.normalize(input.toLowerCase(Locale.ENGLISH), Normalizer.Form.NFD);
        s = s.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        s = " " + s + " ";
        s = s.replaceAll("(\\d),(\\d)", "$1.$2");
        s = s.replace("\u00f7", " / ").replace("\u00d7", " * ");
        s = s.replaceAll("(?<=\\d)\\s*x\\s*(?=\\d)", " * ");
        s = s.replace(" dividido entre ", " / ").replace(" dividido por ", " / ")
                .replace(" dividido ", " / ")
                .replace(" multiplicado por ", " * ").replace(" multiplicado ", " * ")
                .replace(" por ", " * ").replace(" veces ", " * ")
                .replace(" mas ", " + ").replace(" menos ", " - ").replace(" entre ", " / ");

        List<Object> toks = new ArrayList<Object>();
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(s.charAt(i + 1)))) {
                int j = i;
                while (j < n && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) { j++; }
                try {
                    toks.add(Double.valueOf(s.substring(i, j)));
                } catch (NumberFormatException e) {
                    return null;
                }
                i = j;
            } else if (c == '+' || c == '-' || c == '*' || c == '/') {
                toks.add(Character.valueOf(c));
                i++;
            } else if (Character.isLetter(c)) {
                int j = i;
                while (j < n && Character.isLetter(s.charAt(j))) { j++; }
                String w = s.substring(i, j);
                if (NUMS.containsKey(w)) {
                    toks.add(NUMS.get(w));
                } else if (w.equals("y")) {
                    toks.add(Character.valueOf('+'));
                } else if (!FILLER.contains(w)) {
                    return null;
                }
                i = j;
            } else {
                i++;
            }
        }

        List<Double> nums = new ArrayList<Double>();
        List<Character> ops = new ArrayList<Character>();
        boolean expectNum = true;
        boolean neg = false;
        for (Object o : toks) {
            if (o instanceof Double) {
                if (!expectNum) { return null; }
                double v = ((Double) o).doubleValue();
                if (neg) { v = -v; }
                nums.add(Double.valueOf(v));
                neg = false;
                expectNum = false;
            } else {
                char op = ((Character) o).charValue();
                if (expectNum) {
                    if (op == '-') { neg = !neg; }
                    else if (op != '+') { return null; }
                } else {
                    ops.add(Character.valueOf(op));
                    expectNum = true;
                }
            }
        }
        if (expectNum || nums.size() < 2) { return null; }

        List<Double> n2 = new ArrayList<Double>();
        List<Character> o2 = new ArrayList<Character>();
        double cur = nums.get(0).doubleValue();
        for (int k = 0; k < ops.size(); k++) {
            char op = ops.get(k).charValue();
            double nx = nums.get(k + 1).doubleValue();
            if (op == '*') {
                cur = cur * nx;
            } else if (op == '/') {
                if (nx == 0) { return "No se puede dividir entre cero, ni yo puedo con eso. Prueba otra cuenta."; }
                cur = cur / nx;
            } else {
                n2.add(Double.valueOf(cur));
                o2.add(Character.valueOf(op));
                cur = nx;
            }
        }
        n2.add(Double.valueOf(cur));
        double res = n2.get(0).doubleValue();
        for (int k = 0; k < o2.size(); k++) {
            double nx = n2.get(k + 1).doubleValue();
            res = (o2.get(k).charValue() == '+') ? res + nx : res - nx;
        }
        if (Double.isNaN(res) || Double.isInfinite(res) || Math.abs(res) > 1e15) {
            return "Ese resultado es demasiado grande para mí.";
        }
        String r = fmt(res);
        String[] p = {
            "Eso da " + r + ".",
            "El resultado es " + r + ".",
            "Son " + r + ". ¿Hacemos otra cuenta?",
            r + ". Las cuentas me salen con calma."
        };
        return p[RND.nextInt(p.length)];
    }

    static String fmt(double d) {
        if (Math.abs(d - Math.rint(d)) < 1e-9) {
            return Long.toString((long) Math.rint(d));
        }
        String s = String.format(Locale.US, "%.4f", d);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s.replace('.', ',');
    }
}
