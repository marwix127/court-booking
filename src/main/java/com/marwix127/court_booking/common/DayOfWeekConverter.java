package com.marwix127.court_booking.common;

import java.time.DayOfWeek;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convierte DayOfWeek a smallint y viceversa usando la numeracion ISO-8601
 * (1 = lunes ... 7 = domingo), que es la que espera el CHECK
 * ck_opening_hours_dow definido en la migracion V1.
 *
 * Con autoApply = true se aplica solo a cualquier campo DayOfWeek de
 * cualquier entidad, sin necesidad de anotarlo con @Convert.
 */
@Converter(autoApply = true)
public class DayOfWeekConverter implements AttributeConverter<DayOfWeek, Short> {

    @Override
    public Short convertToDatabaseColumn(DayOfWeek attribute) {
        if (attribute == null) {
            return null;
        }
        return (short) attribute.getValue();
    }

    @Override
    public DayOfWeek convertToEntityAttribute(Short dbData) {
        if (dbData == null) {
            return null;
        }
        return DayOfWeek.of(dbData);
    }
}
