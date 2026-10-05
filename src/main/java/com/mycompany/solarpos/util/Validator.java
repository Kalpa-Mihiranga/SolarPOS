package com.mycompany.solarpos.util;

/** Central validation rules used by every form. */
public class Validator {

    private Validator() { }

    public static boolean isPhone(String s) {
        return s != null && s.matches("^(?:0|\\+94)\\d{9}$");
    }

    public static boolean isEmail(String s) {
        return s != null && s.matches("^[\\w.+-]+@[\\w-]+\\.[\\w.]+$");
    }

    public static void requireText(String field, String value) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(field + " is required.");
        }
    }

    public static double requirePositiveDouble(String field, String value) throws ValidationException {
        requireText(field, value);
        try {
            double d = Double.parseDouble(value.trim());
            if (d <= 0) throw new ValidationException(field + " must be greater than 0.");
            return d;
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a number.");
        }
    }

    public static int requirePositiveInt(String field, String value) throws ValidationException {
        requireText(field, value);
        try {
            int n = Integer.parseInt(value.trim());
            if (n <= 0) throw new ValidationException(field + " must be greater than 0.");
            return n;
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a whole number.");
        }
    }

    public static int requireNonNegativeInt(String field, String value) throws ValidationException {
        requireText(field, value);
        try {
            int n = Integer.parseInt(value.trim());
            if (n < 0) throw new ValidationException(field + " cannot be negative.");
            return n;
        } catch (NumberFormatException e) {
            throw new ValidationException(field + " must be a whole number.");
        }
    }

    public static void requirePhone(String value) throws ValidationException {
        if (!isPhone(value)) throw new ValidationException("Enter a valid phone number (e.g. 0771234567).");
    }

    public static void requireOptionalEmail(String value) throws ValidationException {
        if (value != null && !value.trim().isEmpty() && !isEmail(value.trim())) {
            throw new ValidationException("Enter a valid email address.");
        }
    }
    
    public static double requireNonNegativeDouble(String field, String value) throws ValidationException {
    requireText(field, value);
         try {
             double d = Double.parseDouble(value.trim());
                if (d < 0) throw new ValidationException(field + " cannot be negative.");
                return d;
            } catch (NumberFormatException e) {
                throw new ValidationException(field + " must be a number.");
            }
    }
}