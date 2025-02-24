-- V12__Permitir_SaldoNegativo_en_pagos.sql
ALTER TABLE pagos DROP CONSTRAINT IF EXISTS pagos_saldo_restante_check;
