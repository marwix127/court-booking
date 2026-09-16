package com.marwix127.court_booking.common;

import java.sql.SQLException;

import org.hibernate.exception.ConstraintViolationException;

/**
 * Utilidad para saber que restriccion de base de datos ha fallado.
 *
 * Hay que mirar por dos vias porque Hibernate no las trata igual:
 *
 *  - UNIQUE / FK / NOT NULL (SQLState 23505, 23503, 23502): Hibernate las
 *    envuelve en ConstraintViolationException y expone el nombre.
 *  - EXCLUDE (SQLState 23P01): NO las envuelve. La cadena de causas llega
 *    directa al SQLException del driver, y el nombre solo esta en el mensaje.
 *
 * Por eso la segunda via busca el nombre entrecomillado en el texto del error,
 * tal y como lo reporta Postgres:
 *   ERROR: conflicting key value violates exclusion constraint "booking_no_overlap"
 */
public final class ConstraintViolations {

    private ConstraintViolations() {
    }

    public static boolean isViolationOf(Throwable ex, String constraintName) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve
                    && constraintName.equalsIgnoreCase(cve.getConstraintName())) {
                return true;
            }
            if (t instanceof SQLException && mentions(t.getMessage(), constraintName)) {
                return true;
            }
        }
        return false;
    }

    private static boolean mentions(String message, String constraintName) {
        return message != null
                && message.toLowerCase().contains("\"" + constraintName.toLowerCase() + "\"");
    }
}
