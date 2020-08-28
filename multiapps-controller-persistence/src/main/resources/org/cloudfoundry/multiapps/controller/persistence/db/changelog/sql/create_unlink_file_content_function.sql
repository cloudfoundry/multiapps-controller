CREATE OR REPLACE FUNCTION UNLINK_FILE_CONTENT() RETURNS TRIGGER AS
$$
  BEGIN
    PERFORM lo_unlink(OLD.content);
    RETURN NULL;
  EXCEPTION
    WHEN undefined_object THEN RAISE NOTICE 'Referenced large object could not be deleted, because it does not exist!';
    RETURN NULL;
  END;
$$
LANGUAGE 'plpgsql' STABLE;
