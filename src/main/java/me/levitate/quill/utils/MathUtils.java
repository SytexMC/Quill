package me.levitate.quill.utils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.regex.Pattern;

public class MathUtils {
    private static final Random RANDOM = new Random();
    private static final Pattern EVAL_PATTERN = Pattern.compile("[0-9+\\-*/().\\s]+");

    /**
     * Gets a random number between min and max (inclusive)
     */
    public static int randomInt(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        return RANDOM.nextInt(max - min + 1) + min;
    }

    /**
     * Gets a random double between min and max
     */
    public static double randomDouble(double min, double max) {
        if (min >= max) {
            throw new IllegalArgumentException("max must be greater than min");
        }
        return min + (max - min) * RANDOM.nextDouble();
    }

    /**
     * Evaluates a mathematical expression safely
     */
    public static OptionalDouble evaluateExpression(String expression) {
        if (!EVAL_PATTERN.matcher(expression).matches()) {
            return OptionalDouble.empty();
        }

        try {
            ScriptEngineManager mgr = new ScriptEngineManager();
            ScriptEngine engine = mgr.getEngineByName("JavaScript");
            Object result = engine.eval(expression);
            
            if (result instanceof Number) {
                return OptionalDouble.of(((Number) result).doubleValue());
            }
            
            return OptionalDouble.empty();
        } catch (ScriptException ex) {
            return OptionalDouble.empty();
        }
    }

    /**
     * Clamps a value between min and max
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Formats a number with commas
     */
    public static String formatNumber(double number) {
        return String.format("%,.2f", number);
    }
}