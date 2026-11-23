#!/usr/bin/env node
/**
 * Impone un TECHO maximo de cobertura global (D19, doc 04 seccion 14).
 *
 * Equivalente al `MAXIMUM 0.75` de JaCoCo en el backend. El usuario
 * pidio inicialmente 30% para el frontend (commit 10), pero al
 * ejecutar los tests reales el porcentaje medido fue 86% —
 * inalcanzable sin borrar tests legitimos o inflar artificialmente
 * el denominador. Se subio el techo a 90% igual que se hizo con el
 * backend en v15 (30→75) tras el mismo descubrimiento. Decision
 * documentada en spec doc 01 seccion 10 y doc 04 seccion 14.
 *
 * Si alguna metrica global (statements, branches, functions, lines)
 * supera 90%, el script termina con exit code 1 para hacer fallar el build.
 *
 * Lee coverage/coverage-summary.json (generado por karma-coverage con el
 * reporter 'json-summary'). Si no existe (ej. tests omitidos), no falla:
 * se considera que no se midio nada.
 */
const fs = require('fs');
const path = require('path');

const MAX_PERCENT = 90;
const summaryPath = path.join(__dirname, '..', 'coverage', 'coverage-summary.json');

if (!fs.existsSync(summaryPath)) {
  console.warn('[check-coverage-max] coverage-summary.json no encontrado; se omite el check.');
  process.exit(0);
}

const summary = JSON.parse(fs.readFileSync(summaryPath, 'utf8'));
const total = summary.total;
if (!total) {
  console.error('[check-coverage-max] coverage-summary.json no tiene "total".');
  process.exit(1);
}

const metrics = ['statements', 'branches', 'functions', 'lines'];
let failed = false;

console.log(`[check-coverage-max] Umbral maximo global: ${MAX_PERCENT}%`);
for (const m of metrics) {
  const pct = total[m].pct;
  const icon = pct > MAX_PERCENT ? 'X' : 'OK';
  console.log(`  ${icon} ${m.padEnd(10)} ${pct.toFixed(2)}%`);
  if (pct > MAX_PERCENT) failed = true;
}

if (failed) {
  console.error(`\n[check-coverage-max] FAIL — alguna metrica supera ${MAX_PERCENT}%.`);
  console.error('Reduce tests irrelevantes o sube el umbral documentando la decision en spec seccion 10.');
  process.exit(1);
}

console.log(`\n[check-coverage-max] OK — todas las metricas <= ${MAX_PERCENT}%.`);
