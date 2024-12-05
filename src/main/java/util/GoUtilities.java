package util;

public class GoUtilities {

    public static String getType(String input) {
        if (input == null || input.isEmpty()) {
            return "String";  // Consideriamo vuoto o nullo come Stringa
        }

        // Tentiamo di convertire la stringa in un intero
        try {
            Integer.parseInt(input);
            return "Integer";
        } catch (NumberFormatException e) {
            // Se non è un intero, proviamo a convertirlo in float
            try {
                Float.parseFloat(input);
                return "Float";
            } catch (NumberFormatException ex) {
                // Se non è né un intero né un float, la lasciamo come stringa
                return "String";
            }
        }
    }

    public static String getTGoType(String input) {
        if (input == null || input.isEmpty()) {
            return "string";  // Consideriamo vuoto o nullo come Stringa
        }

        // Tentiamo di convertire la stringa in un intero
        try {
            Integer.parseInt(input);
            return "int";
        } catch (NumberFormatException e) {
            // Se non è un intero, proviamo a convertirlo in float
            try {
                Float.parseFloat(input);
                return "float64";
            } catch (NumberFormatException ex) {
                // Se non è un float, proviamo a convertirlo in booleano
                if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase("false") || input.equals("1") || input.equals("0")) {
                    return "bool";
                }
                // Se non è né un intero, né un float, né un booleano, la lasciamo come stringa
                return "string";
            }
        }
    }
}
