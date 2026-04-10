CREATE TABLE training_catalog_item
(
    catalog_item_id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    training_type   ENUM('FOOD_HYGIENE', 'ALLERGEN_HANDLING', 'TEMPERATURE_CONTROL', 'CLEANING_ROUTINES', 'RESPONSIBLE_ALCOHOL_SERVICE', 'AGE_VERIFICATION', 'OTHER') NOT NULL,
    display_name    VARCHAR(255) NOT NULL,
    description     TEXT NULL,
    sort_order      INT NOT NULL DEFAULT 0,
    is_active       TINYINT(1) NOT NULL DEFAULT 1,

    CONSTRAINT uq_training_catalog_type UNIQUE (training_type),
    INDEX ix_training_catalog_active_sort (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO training_catalog_item (training_type, display_name, description, sort_order, is_active)
VALUES
    ('FOOD_HYGIENE', 'Mat hygiene', 'Grunnleggende opplæring i trygg håndtering av mat og hygiene.', 10, 1),
    ('ALLERGEN_HANDLING', 'Allergen handtering', 'Rutiner for allergener, merking og trygg servering til gjester.', 20, 1),
    ('TEMPERATURE_CONTROL', 'Temperaturkontroll', 'Kontroll av temperaturkrav ved lagring, produksjon og servering.', 30, 1),
    ('CLEANING_ROUTINES', 'Rengjøringsrutiner', 'Dokumenterte rutiner for renhold, orden og forebygging av avvik.', 40, 1),
    ('RESPONSIBLE_ALCOHOL_SERVICE', 'Ansvarlig alkoholservering', 'Krav til ansvarlig servering, skjenkeregler og internkontroll.', 50, 1),
    ('AGE_VERIFICATION', 'Alderskontroll', 'Praktisk kontroll av legitimasjon og aldersgrenser ved salg og skjenking.', 60, 1),
    ('OTHER', 'Annet', 'Andre sertifikater eller interne kurs som virksomheten vil følge opp.', 70, 1);
