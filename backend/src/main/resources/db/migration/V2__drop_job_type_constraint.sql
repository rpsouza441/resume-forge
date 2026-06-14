-- Flyway Migration V2: Remove job_type check constraint
-- Job type agora e livre — qualquer valor aceito (input livre, nao dropdown).
-- Mantemos o constraint de status (que e controlado por enum no codigo).

ALTER TABLE job_applications DROP CONSTRAINT IF EXISTS chk_job_applications_job_type;
