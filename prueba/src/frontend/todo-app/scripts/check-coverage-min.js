#!/usr/bin/env node
/** Impone una cobertura global mínima del 30 % para el frontend. */
const fs = require('fs');
const path = require('path');

const MIN_PERCENT = 30;
const summaryPath = path.join(__dirname, '..', 'coverage', 'coverage-summary.json');

if (!fs.existsSync(summaryPath)) {
  console.error('[check-coverage-min] coverage-summary.json no encontrado.');
  process.exit(1);
}

const total = JSON.parse(fs.readFileSync(summaryPath, 'utf8')).total;
if (!total) {
  console.error('[check-coverage-min] El reporte no contiene el total global.');
  process.exit(1);
}

const metrics = ['statements', 'branches', 'functions', 'lines'];
let failed = false;
console.log(`[check-coverage-min] Umbral mínimo global: ${MIN_PERCENT}%`);
for (const metric of metrics) {
  const percentage = total[metric].pct;
  const ok = percentage >= MIN_PERCENT;
  console.log(`  ${ok ? 'OK' : 'X'} ${metric.padEnd(10)} ${percentage.toFixed(2)}%`);
  if (!ok) failed = true;
}

if (failed) {
  console.error(`\n[check-coverage-min] FAIL — alguna métrica está por debajo de ${MIN_PERCENT}%.`);
  process.exit(1);
}
console.log(`\n[check-coverage-min] OK — todas las métricas son >= ${MIN_PERCENT}%.`);
