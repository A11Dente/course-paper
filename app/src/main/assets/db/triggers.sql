CREATE TRIGGER IF NOT EXISTS trg_fighter_age_ins BEFORE INSERT ON Fighter
WHEN NEW.birthDate > date('now', '-16 years')
BEGIN
  SELECT RAISE(ABORT, 'Спортсмену должно быть не менее 16 лет');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_fighter_age_upd BEFORE UPDATE OF birthDate ON Fighter
WHEN NEW.birthDate > date('now', '-16 years')
BEGIN
  SELECT RAISE(ABORT, 'Спортсмену должно быть не менее 16 лет');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_fighter_coach_club_ins BEFORE INSERT ON Fighter
WHEN (SELECT clubId FROM Coach WHERE id = NEW.coachId) <> NEW.clubId
BEGIN
  SELECT RAISE(ABORT, 'Тренер должен работать в том же клубе, что и спортсмен');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_fighter_coach_club_upd BEFORE UPDATE OF clubId, coachId ON Fighter
WHEN (SELECT clubId FROM Coach WHERE id = NEW.coachId) <> NEW.clubId
BEGIN
  SELECT RAISE(ABORT, 'Тренер должен работать в том же клубе, что и спортсмен');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_coach_club_upd BEFORE UPDATE OF clubId ON Coach
WHEN EXISTS (SELECT 1 FROM Fighter WHERE coachId = NEW.id AND clubId <> NEW.clubId)
BEGIN
  SELECT RAISE(ABORT, 'У тренера есть спортсмены из другого клуба');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_fight_date_ins BEFORE INSERT ON Fight
WHEN NEW.date <> (SELECT date FROM Tournament WHERE id = NEW.tournamentId)
BEGIN
  SELECT RAISE(ABORT, 'Дата поединка должна совпадать с датой турнира');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_fight_date_upd BEFORE UPDATE OF tournamentId, date ON Fight
WHEN NEW.date <> (SELECT date FROM Tournament WHERE id = NEW.tournamentId)
BEGIN
  SELECT RAISE(ABORT, 'Дата поединка должна совпадать с датой турнира');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_tournament_date_sync AFTER UPDATE OF date ON Tournament
BEGIN
  UPDATE Fight SET date = NEW.date WHERE tournamentId = NEW.id;
END
#####
CREATE TRIGGER IF NOT EXISTS trg_result_unique_ins BEFORE INSERT ON Result
WHEN EXISTS (SELECT 1 FROM Result WHERE fightId = NEW.fightId)
BEGIN
  SELECT RAISE(ABORT, 'Для этого поединка результат уже внесён');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_result_unique_upd BEFORE UPDATE OF fightId ON Result
WHEN EXISTS (SELECT 1 FROM Result WHERE fightId = NEW.fightId AND id <> NEW.id)
BEGIN
  SELECT RAISE(ABORT, 'Для этого поединка результат уже внесён');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_result_weight_ins BEFORE INSERT ON Result
WHEN (SELECT weightCategory FROM Fighter WHERE id = NEW.fighterId) <> (SELECT weightCategory FROM Fight WHERE id = NEW.fightId)
BEGIN
  SELECT RAISE(ABORT, 'Весовая категория спортсмена не совпадает с категорией поединка');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_result_weight_upd BEFORE UPDATE OF fightId, fighterId ON Result
WHEN (SELECT weightCategory FROM Fighter WHERE id = NEW.fighterId) <> (SELECT weightCategory FROM Fight WHERE id = NEW.fightId)
BEGIN
  SELECT RAISE(ABORT, 'Весовая категория спортсмена не совпадает с категорией поединка');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_result_round_ins BEFORE INSERT ON Result
WHEN NEW.round < 1 OR NEW.round > 5
BEGIN
  SELECT RAISE(ABORT, 'Номер раунда должен быть от 1 до 5');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_result_round_upd BEFORE UPDATE OF round ON Result
WHEN NEW.round < 1 OR NEW.round > 5
BEGIN
  SELECT RAISE(ABORT, 'Номер раунда должен быть от 1 до 5');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_fighter_audit_ins AFTER INSERT ON Fighter
BEGIN
  INSERT INTO AuditLog (tableName, operation, recordId, details, createdAt)
  VALUES ('Fighter', 'INSERT', NEW.id, NEW.surname || ' ' || NEW.name, datetime('now', 'localtime'));
END
#####
CREATE TRIGGER IF NOT EXISTS trg_fighter_audit_upd AFTER UPDATE ON Fighter
BEGIN
  INSERT INTO AuditLog (tableName, operation, recordId, details, createdAt)
  VALUES ('Fighter', 'UPDATE', NEW.id, NEW.surname || ' ' || NEW.name, datetime('now', 'localtime'));
END
#####
CREATE TRIGGER IF NOT EXISTS trg_fighter_audit_del AFTER DELETE ON Fighter
BEGIN
  INSERT INTO AuditLog (tableName, operation, recordId, details, createdAt)
  VALUES ('Fighter', 'DELETE', OLD.id, OLD.surname || ' ' || OLD.name, datetime('now', 'localtime'));
END
#####
CREATE TRIGGER IF NOT EXISTS trg_result_audit_ins AFTER INSERT ON Result
BEGIN
  INSERT INTO AuditLog (tableName, operation, recordId, details, createdAt)
  VALUES ('Result', 'INSERT', NEW.id, 'Поединок №' || NEW.fightId || ', ' || NEW.victoryMethod, datetime('now', 'localtime'));
END
#####
CREATE TRIGGER IF NOT EXISTS trg_result_audit_del AFTER DELETE ON Result
BEGIN
  INSERT INTO AuditLog (tableName, operation, recordId, details, createdAt)
  VALUES ('Result', 'DELETE', OLD.id, 'Поединок №' || OLD.fightId || ', ' || OLD.victoryMethod, datetime('now', 'localtime'));
END
#####
CREATE TRIGGER IF NOT EXISTS trg_fight_distinct_ins BEFORE INSERT ON Fight
WHEN NEW.fighter1Id IS NOT NULL AND NEW.fighter1Id = NEW.fighter2Id
BEGIN
  SELECT RAISE(ABORT, 'Нужно выбрать двух разных спортсменов');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_fight_distinct_upd BEFORE UPDATE OF fighter1Id, fighter2Id ON Fight
WHEN NEW.fighter1Id IS NOT NULL AND NEW.fighter1Id = NEW.fighter2Id
BEGIN
  SELECT RAISE(ABORT, 'Нужно выбрать двух разных спортсменов');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_fight_fighters_weight_ins BEFORE INSERT ON Fight
WHEN (NEW.fighter1Id IS NOT NULL AND (SELECT weightCategory FROM Fighter WHERE id = NEW.fighter1Id) <> NEW.weightCategory)
  OR (NEW.fighter2Id IS NOT NULL AND (SELECT weightCategory FROM Fighter WHERE id = NEW.fighter2Id) <> NEW.weightCategory)
BEGIN
  SELECT RAISE(ABORT, 'Участники должны быть из весовой категории поединка');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_fight_fighters_weight_upd BEFORE UPDATE OF fighter1Id, fighter2Id, weightCategory ON Fight
WHEN (NEW.fighter1Id IS NOT NULL AND (SELECT weightCategory FROM Fighter WHERE id = NEW.fighter1Id) <> NEW.weightCategory)
  OR (NEW.fighter2Id IS NOT NULL AND (SELECT weightCategory FROM Fighter WHERE id = NEW.fighter2Id) <> NEW.weightCategory)
BEGIN
  SELECT RAISE(ABORT, 'Участники должны быть из весовой категории поединка');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_fight_result_guard BEFORE UPDATE OF fighter1Id, fighter2Id ON Fight
WHEN EXISTS (SELECT 1 FROM Result WHERE fightId = NEW.id AND fighterId IS NOT NEW.fighter1Id AND fighterId IS NOT NEW.fighter2Id)
BEGIN
  SELECT RAISE(ABORT, 'Нельзя убрать победителя из участников: сначала удалите результат');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_result_participant_ins BEFORE INSERT ON Result
WHEN NOT EXISTS (SELECT 1 FROM Fight WHERE id = NEW.fightId AND fighter1Id IS NOT NULL AND fighter2Id IS NOT NULL
                 AND NEW.fighterId IN (fighter1Id, fighter2Id))
BEGIN
  SELECT RAISE(ABORT, 'Победитель должен быть одним из двух участников поединка');
END
#####
CREATE TRIGGER IF NOT EXISTS trg_result_participant_upd BEFORE UPDATE OF fightId, fighterId ON Result
WHEN NOT EXISTS (SELECT 1 FROM Fight WHERE id = NEW.fightId AND fighter1Id IS NOT NULL AND fighter2Id IS NOT NULL
                 AND NEW.fighterId IN (fighter1Id, fighter2Id))
BEGIN
  SELECT RAISE(ABORT, 'Победитель должен быть одним из двух участников поединка');
END
