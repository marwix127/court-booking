package com.marwix127.court_booking.common;

import org.hibernate.exception.ConstraintViolationException;

/**
 * Utilidad para saber que restriccion de base de datos ha fallado.
 *
 * Spring envuelve el error en DataIntegrityViolationException; dentro, Hibernate
 * deja una ConstraintViolationException con el nombre de la restriccion que
 * extrae del mensaje de Postgres. Esto permite distinguir "email duplicado" de
 * cualquier otra violacion y reaccionar solo a la que toca.
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
        }
        return false;
    }
}
