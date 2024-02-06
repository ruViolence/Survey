-- Survey SQLite Schema

CREATE TABLE IF NOT EXISTS `results`
(
    `id`          INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE,
    `survey_key`  TEXT NOT NULL,
    `player_uuid` TEXT NOT NULL,
    `data`        JSON NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS `results_survey_player` ON `results` (`survey_key`, `player_uuid`);

CREATE TABLE IF NOT EXISTS `opt_out`
(
    `id`          INTEGER PRIMARY KEY AUTOINCREMENT UNIQUE,
    `survey_key`  TEXT    NOT NULL,
    `player_uuid` TEXT    NOT NULL,
    `since`       INTEGER NOT NULL
);
CREATE UNIQUE INDEX IF NOT EXISTS `opt_out_survey_player` ON `opt_out` (`survey_key`, `player_uuid`);