-- V1: Initial placeholder migration
-- Full schema is in V2__full_schema.sql
-- This migration is kept for compatibility

-- Drop app_user if exists from old schema (we use 'users' table now)
DROP TABLE IF EXISTS app_user;
