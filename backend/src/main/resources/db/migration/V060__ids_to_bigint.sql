-- V060__ids_to_bigint_safe.sql
-- Migracion segura int4 → int8 para PK/FK, excluyendo flyway_schema_history

-- 1) Catalogamos FKs que referencian PKs INTEGER (1 columna) y EXCLUIMOS flyway_schema_history
CREATE TEMP TABLE _fk_meta AS
WITH fk AS (
  SELECT
    con.oid,
    con.conname,
    nsf.nspname  AS fk_schema,
    rf.relname   AS fk_table,
    af.attname   AS fk_column,
    nsp.nspname  AS pk_schema,
    rp.relname   AS pk_table,
    ap.attname   AS pk_column,
    con.confmatchtype,
    con.confupdtype,
    con.confdeltype,
    con.condeferrable,
    con.condeferred,
    af.atttypid  AS fk_typid,
    ap.atttypid  AS pk_typid,
    array_length(con.conkey,1)  AS fk_cols,
    array_length(con.confkey,1) AS pk_cols
  FROM pg_constraint con
  JOIN pg_class rf        ON rf.oid = con.conrelid
  JOIN pg_namespace nsf   ON nsf.oid = rf.relnamespace
  JOIN pg_class rp        ON rp.oid = con.confrelid
  JOIN pg_namespace nsp   ON nsp.oid = rp.relnamespace
  JOIN pg_attribute af    ON af.attrelid = con.conrelid  AND af.attnum = con.conkey[1]
  JOIN pg_attribute ap    ON ap.attrelid = con.confrelid AND ap.attnum = con.confkey[1]
  WHERE con.contype = 'f'
)
SELECT
    oid, conname, fk_schema, fk_table, fk_column,
    pk_schema, pk_table, pk_column,
    confmatchtype, confupdtype, confdeltype,
    condeferrable, condeferred,
    fk_typid, pk_typid
FROM fk
WHERE fk_cols = 1 AND pk_cols = 1
  AND pk_typid = 'int4'::regtype
  AND NOT (pk_schema = 'public' AND pk_table = 'flyway_schema_history')
  AND NOT (fk_schema = 'public' AND fk_table = 'flyway_schema_history');

-- 2) Dropeamos esas FKs (luego las recreamos)
DO $$
DECLARE r record;
BEGIN
FOR r IN SELECT * FROM _fk_meta LOOP
    EXECUTE format('ALTER TABLE %I.%I DROP CONSTRAINT %I;',
                   r.fk_schema, r.fk_table, r.conname);
END LOOP;
END $$;

-- 3) Subimos a BIGINT las columnas FK que aun sean INTEGER
DO $$
DECLARE r record;
BEGIN
FOR r IN
SELECT DISTINCT fk_schema, fk_table, fk_column
FROM _fk_meta
WHERE fk_typid = 'int4'::regtype
  LOOP
    EXECUTE format(
      'ALTER TABLE %I.%I ALTER COLUMN %I TYPE BIGINT USING %I::bigint;',
      r.fk_schema, r.fk_table, r.fk_column, r.fk_column
    );
END LOOP;
END $$;

-- 4) Subimos TODAS las PK INTEGER → BIGINT (excluimos flyway_schema_history)
DO $$
DECLARE r record;
BEGIN
FOR r IN
SELECT
    kcu.table_schema AS schema_name,
    kcu.table_name   AS table_name,
    kcu.column_name  AS column_name
FROM information_schema.table_constraints tc
         JOIN information_schema.key_column_usage kcu
              ON tc.constraint_name = kcu.constraint_name
                  AND tc.table_schema   = kcu.table_schema
         JOIN information_schema.columns c
              ON c.table_schema = kcu.table_schema
                  AND c.table_name   = kcu.table_name
                  AND c.column_name  = kcu.column_name
WHERE tc.constraint_type = 'PRIMARY KEY'
  AND c.data_type = 'integer'
  AND kcu.table_schema NOT IN ('pg_catalog','information_schema')
  AND NOT (kcu.table_schema = 'public' AND kcu.table_name = 'flyway_schema_history')
    LOOP
    EXECUTE format(
      'ALTER TABLE %I.%I ALTER COLUMN %I TYPE BIGINT USING %I::bigint;',
      r.schema_name, r.table_name, r.column_name, r.column_name
    );
END LOOP;
END $$;

-- 5) Recreamos las FKs con los mismos nombres y reglas, NOT VALID + VALIDATE
DO $$
DECLARE
r record;
  v_match text;
  v_onupd text;
  v_ondel text;
  v_def   text;
BEGIN
FOR r IN SELECT * FROM _fk_meta LOOP
                           -- match
                           v_match := CASE r.confmatchtype
                 WHEN 'f' THEN ' MATCH FULL'
                 WHEN 'p' THEN ' MATCH PARTIAL'
                 ELSE ''  -- simple (default)
END;
    -- on update
    v_onupd := CASE r.confupdtype
                 WHEN 'c' THEN ' ON UPDATE CASCADE'
                 WHEN 'n' THEN ' ON UPDATE SET NULL'
                 WHEN 'd' THEN ' ON UPDATE SET DEFAULT'
                 WHEN 'r' THEN ' ON UPDATE RESTRICT'
                 ELSE ''  -- NO ACTION
END;
    -- on delete
    v_ondel := CASE r.confdeltype
                 WHEN 'c' THEN ' ON DELETE CASCADE'
                 WHEN 'n' THEN ' ON DELETE SET NULL'
                 WHEN 'd' THEN ' ON DELETE SET DEFAULT'
                 WHEN 'r' THEN ' ON DELETE RESTRICT'
                 ELSE ''  -- NO ACTION
END;
    -- deferrable
    v_def := CASE
               WHEN r.condeferrable AND r.condeferred THEN ' DEFERRABLE INITIALLY DEFERRED'
               WHEN r.condeferrable THEN ' DEFERRABLE'
               ELSE ''
END;

EXECUTE format(
        'ALTER TABLE %I.%I ADD CONSTRAINT %I FOREIGN KEY (%I) '||
        'REFERENCES %I.%I(%I)%s%s%s%s NOT VALID;',
        r.fk_schema, r.fk_table, r.conname, r.fk_column,
        r.pk_schema, r.pk_table, r.pk_column,
        v_match, v_onupd, v_ondel, v_def
        );

EXECUTE format(
        'ALTER TABLE %I.%I VALIDATE CONSTRAINT %I;',
        r.fk_schema, r.fk_table, r.conname
        );
END LOOP;
END $$;

-- 6) Verificacion final (excluye flyway_schema_history)
DO $$
DECLARE v_cnt int;
BEGIN
SELECT COUNT(*) INTO v_cnt
FROM (
         WITH pks AS (
             SELECT tc.table_schema, tc.table_name, kcu.column_name, c.data_type AS pk_type
             FROM information_schema.table_constraints tc
                      JOIN information_schema.key_column_usage kcu
                           ON tc.constraint_name = kcu.constraint_name
                               AND tc.table_schema   = kcu.table_schema
                      JOIN information_schema.columns c
                           ON c.table_schema = kcu.table_schema
                               AND c.table_name   = kcu.table_name
                               AND c.column_name  = kcu.column_name
             WHERE tc.constraint_type = 'PRIMARY KEY'
               AND NOT (tc.table_schema='public' AND tc.table_name='flyway_schema_history')
         ),
              fks AS (
                  SELECT
                      tc.table_schema  AS fk_schema,
                      tc.table_name    AS fk_table,
                      kcu.column_name  AS fk_column,
                      c.data_type      AS fk_type,
                      ccu.table_schema AS pk_schema,
                      ccu.table_name   AS pk_table,
                      ccu.column_name  AS pk_column
                  FROM information_schema.table_constraints tc
                           JOIN information_schema.key_column_usage kcu
                                ON tc.constraint_name = kcu.constraint_name
                                    AND tc.table_schema   = kcu.table_schema
                           JOIN information_schema.constraint_column_usage ccu
                                ON ccu.constraint_name = tc.constraint_name
                                    AND ccu.table_schema   = tc.table_schema
                           JOIN information_schema.columns c
                                ON c.table_schema = kcu.table_schema
                                    AND c.table_name   = kcu.table_name
                                    AND c.column_name  = kcu.column_name
                  WHERE tc.constraint_type = 'FOREIGN KEY'
                    AND NOT (tc.table_schema='public' AND tc.table_name='flyway_schema_history')
              )
         SELECT 1
         FROM fks
                  JOIN pks
                       ON pks.table_schema = fks.pk_schema
                           AND pks.table_name   = fks.pk_table
                           AND pks.column_name  = fks.pk_column
         WHERE fks.fk_schema NOT IN ('pg_catalog','information_schema')
           AND pks.pk_type <> fks.fk_type
     ) z;

IF v_cnt > 0 THEN
    RAISE EXCEPTION 'Quedaron FKs con tipo distinto a su PK (=%). Revisar.', v_cnt;
END IF;
END $$;

-- Fin
