SELECT table_name,
       (xpath('/row/c/text()', query_to_xml(
           format('SELECT count(*) AS c FROM public.%I', table_name), false, true, '')))[1]::text::bigint AS row_count
FROM information_schema.tables
WHERE table_schema = 'public'
  AND table_type = 'BASE TABLE'
  AND table_name <> 'flyway_schema_history'
ORDER BY table_name;
