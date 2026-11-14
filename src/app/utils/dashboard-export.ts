import { orderStatusLabel } from '../models/order';
import {
  CategoryStat,
  DashOrder,
  DashboardModel,
  DishRow,
  PROFIT_MARKUP,
  RangePair,
  addDays,
  categoryStats,
  dateTimeLabel,
  compareSeries,
  dishRows,
  fmtDate,
  fmtMoney,
  pctChange,
  rangeSeries,
  topProducts,
  totalsFor,
} from './dashboard-metrics';
import { ChartImage, ReportCharts, renderCharts } from './dashboard-charts';

// =====================================================================
// Exportación del dashboard: CSV, Excel (.xlsx) y PDF.
// Excel y PDF llevan el logo de la empresa (public/images/logo-hero.svg).
// exceljs y jspdf se cargan solo al exportar, para no engordar la app.
// =====================================================================

export type ExportFormat = 'csv' | 'excel' | 'pdf';

// Colores de la marca
const CHARCOAL = '#2c2c2c';
const FOREST = '#556141';
const CREAM = '#f5f2eb';
const TERRA = '#b2571f';

// Logo claro (crema): se coloca siempre sobre una franja charcoal
const LOGO_URL = '/images/logo-hero.svg';

// Datos del reporte
export interface SummaryRow {
  label: string;
  current: number;
  previous: number;
  // null = el periodo anterior no tiene datos para comparar
  delta: number | null;
  kind: 'money' | 'int';
}

export interface ReportData {
  rangeLabel: string;
  periodText: string;
  previousLabel: string;
  generatedAt: Date;
  fileBase: string;
  summary: SummaryRow[];
  // Gráficas ya dibujadas; null si el usuario no las quiso (o el formato no las admite)
  charts: ChartImage[] | null;
  categories: CategoryStat[];
  dishes: DishRow[];
  orders: DashOrder[];
}

// Arma todo lo que se exporta a partir del modelo y del rango elegido.
export function buildReport(
  model: DashboardModel,
  pair: RangePair,
  now: Date,
  includeCharts = false,
): ReportData {
  const cur = totalsFor(model, pair.current);
  const prev = totalsFor(model, pair.previous);

  const row = (
    label: string,
    key: 'revenue' | 'orders' | 'units' | 'profit',
    kind: 'money' | 'int',
  ): SummaryRow => ({
    label,
    current: cur[key],
    previous: prev[key],
    delta: pctChange(cur[key], prev[key]),
    kind,
  });

  const lastDay = addDays(pair.current.to, -1);
  const singleDay = lastDay.getTime() < addDays(pair.current.from, 1).getTime();
  const periodText = singleDay
    ? fmtDate(pair.current.from)
    : `${fmtDate(pair.current.from)} - ${fmtDate(lastDay)}`;

  const pad = (n: number) => String(n).padStart(2, '0');
  const stamp = (d: Date) =>
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  const fileBase = singleDay
    ? `terragalega-reporte-${stamp(pair.current.from)}`
    : `terragalega-reporte-${stamp(pair.current.from)}_a_${stamp(lastDay)}`;

  const categories = categoryStats(model, pair.current);

  let charts: ChartImage[] | null = null;
  if (includeCharts) {
    const sales = rangeSeries(model, pair.current);
    const compare = compareSeries(model, pair);
    const data: ReportCharts = {
      sales,
      compare: {
        labels: compare.labels,
        current: compare.current,
        previous: compare.previous,
        currentName: pair.label,
        previousName: pair.previousLabel,
      },
      top: topProducts(model, pair.current, 'units')
        .slice(0, 8)
        .map((p) => ({ label: p.name, value: p.units })),
      categories: categories.map((c) => ({ label: c.name, value: c.revenue })),
    };
    charts = renderCharts(data, periodText);
  }

  return {
    rangeLabel: pair.label,
    periodText,
    previousLabel: pair.previousLabel,
    generatedAt: now,
    fileBase,
    summary: [
      row('Ventas', 'revenue', 'money'),
      row('Pedidos', 'orders', 'int'),
      row('Productos vendidos', 'units', 'int'),
      row(
        `Ganancia (${Math.round(PROFIT_MARKUP * 100)} % incluido)`,
        'profit',
        'money',
      ),
    ],
    charts,
    categories,
    dishes: dishRows(model, pair.current).sort(
      (a, b) => b.sales - a.sales || a.name.localeCompare(b.name),
    ),
    orders: model.orders
      .filter(
        (o) =>
          o.date.getTime() >= pair.current.from.getTime() &&
          o.date.getTime() < pair.current.to.getTime(),
      )
      .sort((a, b) => b.date.getTime() - a.date.getTime()),
  };
}

// Punto de entrada: genera y descarga el archivo del formato elegido.
export async function exportReport(
  format: ExportFormat,
  report: ReportData,
  previewWindow: Window | null = null,
): Promise<void> {
  if (format === 'csv') return exportCsv(report);
  if (format === 'excel') return exportExcel(report);

  return exportPdf(report, previewWindow);
}
// Utilidades
function download(blob: Blob, filename: string) {
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}

// "7 días (3 may 2026 - 9 may 2026)"; si el nombre ya es el periodo no se repite
function periodTitle(r: ReportData): string {
  return r.rangeLabel === r.periodText
    ? r.periodText
    : `${r.rangeLabel} (${r.periodText})`;
}

function pctText(delta: number | null): string {
  if (delta === null) return 'Nuevo';
  const sign = delta > 0 ? '+' : '';
  return `${sign}${(Math.round(delta * 10) / 10).toString().replace('.', ',')} %`;
}

// Total de pedidos que cuentan como venta (los cancelados no)
function countedTotals(orders: DashOrder[]) {
  return orders
    .filter((o) => o.counted)
    .reduce(
      (t, o) => ({
        total: t.total + o.total,
        units: t.units + o.units,
        profit: t.profit + o.profit,
      }),
      { total: 0, units: 0, profit: 0 },
    );
}

// Logo del proyecto convertido a PNG transparente. Los SVG del proyecto no
// traen ancho/alto, así que se los ponemos antes de dibujarlos en un canvas.
// Si algo falla devuelve null y el reporte se genera igual, sin logo.
async function loadLogo(
  widthPx = 900,
): Promise<{ dataUrl: string; ratio: number; w: number; h: number } | null> {
  try {
    const text = await (await fetch(LOGO_URL)).text();
    const open = text.match(/<svg[^>]*>/)?.[0];
    const viewBox = open?.match(/viewBox="([^"]+)"/)?.[1];
    if (!open || !viewBox) return null;

    const [, , vw, vh] = viewBox
      .trim()
      .split(/[\s,]+/)
      .map(Number);
    const h = Math.round((widthPx * vh) / vw);
    const fixed = open
      .replace(/\s(width|height|style)="[^"]*"/g, '')
      .replace('<svg', `<svg width="${widthPx}" height="${h}"`);
    const svg = text.replace(open, fixed);

    const url = URL.createObjectURL(
      new Blob([svg], { type: 'image/svg+xml;charset=utf-8' }),
    );
    const img = await new Promise<HTMLImageElement>((ok, fail) => {
      const el = new Image();
      el.onload = () => ok(el);
      el.onerror = () => fail(new Error('No se pudo leer el logo'));
      el.src = url;
    });

    const canvas = document.createElement('canvas');
    canvas.width = widthPx;
    canvas.height = h;
    canvas.getContext('2d')!.drawImage(img, 0, 0, widthPx, h);
    URL.revokeObjectURL(url);

    return {
      dataUrl: canvas.toDataURL('image/png'),
      ratio: vw / vh,
      w: widthPx,
      h,
    };
  } catch {
    return null;
  }
}
// CSV
// Separador ";" y BOM UTF-8: es lo que Excel en español abre bien con tildes y columnas.
function csvCell(value: string | number): string {
  const text = String(value);
  return /[;"\n\r]/.test(text) ? `"${text.replace(/"/g, '""')}"` : text;
}

function exportCsv(r: ReportData) {
  const lines: (string | number)[][] = [];
  const blank = () => lines.push([]);
  const money = (n: number) => Math.round(n);

  lines.push(['TERRA GALEGA - Reporte de ventas']);
  lines.push(['Periodo', periodTitle(r)]);
  lines.push(['Generado', dateTimeLabel(r.generatedAt)]);
  blank();

  lines.push(['RESUMEN']);
  lines.push([
    'Indicador',
    'Periodo actual',
    `Comparado con (${r.previousLabel})`,
    'Variación',
  ]);
  r.summary.forEach((s) =>
    lines.push([
      s.label,
      s.kind === 'money' ? money(s.current) : s.current,
      s.kind === 'money' ? money(s.previous) : s.previous,
      pctText(s.delta),
    ]),
  );
  blank();

  lines.push(['VENTAS POR CATEGORÍA']);
  lines.push(['Categoría', 'Unidades', 'Ventas', 'Ganancia']);
  r.categories.forEach((c) =>
    lines.push([c.name, c.units, money(c.revenue), money(c.profit)]),
  );
  blank();

  lines.push([
    `PLATOS Y GANANCIA (${Math.round(PROFIT_MARKUP * 100)} % incluido en el precio)`,
  ]);
  lines.push([
    'Plato',
    'Categoría',
    'Precio final',
    'Costo',
    'Ganancia por unidad',
    'Unidades',
    'Ventas',
    'Ganancia',
  ]);
  r.dishes.forEach((d) =>
    lines.push([
      d.name,
      d.category,
      money(d.price),
      money(d.cost),
      money(d.unitProfit),
      d.units,
      money(d.sales),
      money(d.profit),
    ]),
  );
  blank();

  lines.push(['PEDIDOS DEL PERIODO']);
  lines.push([
    'N.º',
    'Fecha',
    'Cliente',
    'Estado',
    'Platos',
    'Unidades',
    'Total',
    'Ganancia',
  ]);
  r.orders.forEach((o) =>
    lines.push([
      o.id,
      dateTimeLabel(o.date),
      o.clientName,
      orderStatusLabel(o.status),
      o.items,
      o.units,
      money(o.total),
      o.counted ? money(o.profit) : 0,
    ]),
  );

  const csv = lines.map((l) => l.map(csvCell).join(';')).join('\r\n');
  download(
    new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' }),
    `${r.fileBase}.csv`,
  );
}
// Excel
const MONEY_FMT = '"$"#,##0';
const INT_FMT = '#,##0';

async function exportExcel(r: ReportData) {
  const mod: any = await import('exceljs');
  const ExcelJS = mod.default ?? mod;
  const logo = await loadLogo();

  const wb = new ExcelJS.Workbook();
  wb.creator = 'Terra Galega';
  wb.created = r.generatedAt;
  const logoId = logo
    ? wb.addImage({ base64: logo.dataUrl, extension: 'png' })
    : null;

  // Hoja con franja charcoal arriba: logo a la izquierda y título a su lado
  const sheet = (name: string, title: string, widths: number[]) => {
    const ws = wb.addWorksheet(name, { views: [{ showGridLines: false }] });
    ws.columns = widths.map((width) => ({ width }));
    const last = widths.length;

    for (let row = 1; row <= 4; row++) {
      ws.getRow(row).height = 22;
      for (let col = 1; col <= last; col++) {
        ws.getCell(row, col).fill = {
          type: 'pattern',
          pattern: 'solid',
          fgColor: { argb: 'FF' + CHARCOAL.slice(1).toUpperCase() },
        };
      }
    }
    if (logo && logoId !== null) {
      const height = 78;
      ws.addImage(logoId, {
        tl: { col: 0.12, row: 0.25 },
        ext: { width: Math.round(height * logo.ratio), height },
      });
    }
    ws.mergeCells(2, 2, 2, last);
    ws.mergeCells(3, 2, 3, last);
    ws.getCell(2, 2).value = title;
    ws.getCell(2, 2).font = {
      name: 'Calibri',
      size: 16,
      bold: true,
      color: { argb: 'FFF5F2EB' },
    };
    ws.getCell(3, 2).value =
      `${periodTitle(r)} - generado el ${dateTimeLabel(r.generatedAt)}`;
    ws.getCell(3, 2).font = {
      name: 'Calibri',
      size: 10,
      color: { argb: 'FFC9C5BB' },
    };
    return ws;
  };

  // Encabezado de tabla verde con texto crema
  const header = (ws: any, rowNumber: number, labels: string[]) => {
    const row = ws.getRow(rowNumber);
    labels.forEach((text, i) => {
      const cell = row.getCell(i + 1);
      cell.value = text;
      cell.font = { bold: true, color: { argb: 'FFF5F2EB' } };
      cell.fill = {
        type: 'pattern',
        pattern: 'solid',
        fgColor: { argb: 'FF556141' },
      };
      cell.alignment = {
        vertical: 'middle',
        horizontal: i === 0 ? 'left' : 'center',
      };
    });
    row.height = 24;
  };

  // Fila de totales en negrita con fondo crema
  const totalRow = (ws: any, rowNumber: number, columns: number) => {
    const row = ws.getRow(rowNumber);
    for (let c = 1; c <= columns; c++) {
      const cell = row.getCell(c);
      cell.font = { bold: true };
      cell.fill = {
        type: 'pattern',
        pattern: 'solid',
        fgColor: { argb: 'FFEFECE4' },
      };
    }
  };

  // ---- Resumen
  const s1 = sheet('Resumen', 'Reporte de ventas', [34, 20, 22, 14]);
  header(s1, 6, [
    'Indicador',
    'Periodo actual',
    `Anterior (${r.previousLabel})`,
    'Variación',
  ]);
  r.summary.forEach((s, i) => {
    const row = s1.getRow(7 + i);
    row.getCell(1).value = s.label;
    row.getCell(2).value = s.current;
    row.getCell(3).value = s.previous;
    row.getCell(2).numFmt = row.getCell(3).numFmt =
      s.kind === 'money' ? MONEY_FMT : INT_FMT;
    // La variación va como fracción con formato % para que Excel la pueda usar
    if (s.delta === null) {
      row.getCell(4).value = 'Nuevo';
    } else {
      row.getCell(4).value = s.delta / 100;
      row.getCell(4).numFmt = '+0.0%;-0.0%;0.0%';
      row.getCell(4).font = {
        bold: true,
        color: { argb: s.delta >= 0 ? 'FF556141' : 'FF8A2222' },
      };
    }
    row.getCell(4).alignment = { horizontal: 'center' };
  });

  // ---- Gráficas (imágenes dibujadas con los mismos datos de las tablas)
  if (r.charts && r.charts.length) {
    const s0 = sheet('Gráficas', 'Gráficas del periodo', Array(12).fill(14));
    const px = 0.9; // escala de las imágenes dentro de la hoja
    const ROW_PX = 20;
    const place = (img: ChartImage, col: number, row: number) => {
      const id = wb.addImage({ base64: img.dataUrl, extension: 'png' });
      s0.addImage(id, {
        tl: { col, row },
        ext: { width: Math.round(img.w * px), height: Math.round(img.h * px) },
      });
      return Math.ceil((img.h * px) / ROW_PX) + 1;
    };
    const byKey = (k: ChartImage['key']) => r.charts!.find((c) => c.key === k)!;

    let row = 5;
    row += place(byKey('sales'), 0.12, row);
    row += place(byKey('compare'), 0.12, row);
    // Las dos gráficas pequeñas van lado a lado
    place(byKey('top'), 0.12, row);
    place(byKey('categories'), 5.4, row);
  }

  // ---- Platos
  const s2 = sheet(
    'Platos',
    'Platos y ganancia',
    [34, 18, 15, 15, 18, 11, 16, 16],
  );
  header(s2, 6, [
    'Plato',
    'Categoría',
    'Precio final',
    'Costo',
    `Ganancia ${Math.round(PROFIT_MARKUP * 100)} % (u.)`,
    'Unidades',
    'Ventas',
    'Ganancia',
  ]);
  r.dishes.forEach((d, i) => {
    const row = s2.getRow(7 + i);
    row.values = [
      d.name,
      d.category,
      d.price,
      d.cost,
      d.unitProfit,
      d.units,
      d.sales,
      d.profit,
    ];
    [3, 4, 5, 7, 8].forEach((c) => (row.getCell(c).numFmt = MONEY_FMT));
    row.getCell(6).numFmt = INT_FMT;
  });
  const dEnd = 6 + r.dishes.length;
  const dTotal = s2.getRow(dEnd + 1);
  dTotal.getCell(1).value = 'Total';
  // Fórmulas: si se edita una cifra en Excel, los totales se recalculan
  dTotal.getCell(6).value = { formula: `SUM(F7:F${dEnd})` };
  dTotal.getCell(7).value = { formula: `SUM(G7:G${dEnd})` };
  dTotal.getCell(8).value = { formula: `SUM(H7:H${dEnd})` };
  dTotal.getCell(6).numFmt = INT_FMT;
  dTotal.getCell(7).numFmt = dTotal.getCell(8).numFmt = MONEY_FMT;
  totalRow(s2, dEnd + 1, 8);
  s2.autoFilter = { from: { row: 6, column: 1 }, to: { row: dEnd, column: 8 } };
  s2.views = [{ showGridLines: false, state: 'frozen', ySplit: 6 }];

  // ---- Categorías
  const s3 = sheet('Categorías', 'Ventas por categoría', [34, 14, 18, 18]);
  header(s3, 6, ['Categoría', 'Unidades', 'Ventas', 'Ganancia']);
  r.categories.forEach((c, i) => {
    const row = s3.getRow(7 + i);
    row.values = [c.name, c.units, c.revenue, c.profit];
    row.getCell(2).numFmt = INT_FMT;
    row.getCell(3).numFmt = row.getCell(4).numFmt = MONEY_FMT;
  });

  // ---- Pedidos
  const s4 = sheet(
    'Pedidos',
    'Pedidos del periodo',
    [34, 18, 26, 14, 46, 11, 16, 16],
  );
  header(s4, 6, [
    'N.º de pedido',
    'Fecha',
    'Cliente',
    'Estado',
    'Platos',
    'Unidades',
    'Total',
    'Ganancia',
  ]);
  r.orders.forEach((o, i) => {
    const row = s4.getRow(7 + i);
    row.values = [
      o.id,
      o.date,
      o.clientName,
      orderStatusLabel(o.status),
      o.items,
      o.units,
      o.total,
      o.counted ? o.profit : 0,
    ];
    row.getCell(1).alignment = { horizontal: 'left' };
    row.getCell(2).numFmt = 'dd/mm/yyyy hh:mm';
    row.getCell(6).numFmt = INT_FMT;
    row.getCell(7).numFmt = row.getCell(8).numFmt = MONEY_FMT;
    if (!o.counted) row.font = { color: { argb: 'FF8A2222' }, italic: true };
  });
  const t = countedTotals(r.orders);
  const oTotal = s4.getRow(7 + r.orders.length);
  oTotal.getCell(1).value = 'Total (sin cancelados)';
  oTotal.getCell(6).value = t.units;
  oTotal.getCell(7).value = t.total;
  oTotal.getCell(8).value = t.profit;
  oTotal.getCell(6).numFmt = INT_FMT;
  oTotal.getCell(7).numFmt = oTotal.getCell(8).numFmt = MONEY_FMT;
  totalRow(s4, 7 + r.orders.length, 8);
  s4.views = [{ showGridLines: false, state: 'frozen', ySplit: 6 }];

  const buffer = await wb.xlsx.writeBuffer();
  download(
    new Blob([buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    }),
    `${r.fileBase}.xlsx`,
  );
}
// PDF
function rgb(hex: string): [number, number, number] {
  return [
    parseInt(hex.slice(1, 3), 16),
    parseInt(hex.slice(3, 5), 16),
    parseInt(hex.slice(5, 7), 16),
  ];
}

async function exportPdf(r: ReportData, previewWindow: Window | null) {
  if (!previewWindow || previewWindow.closed) {
    throw new Error(
      'La pestaña de vista previa no está disponible. Vuelve a intentarlo.',
    );
  }
  const { jsPDF } = await import('jspdf');
  const autoTable = (await import('jspdf-autotable')).default;
  const logo = await loadLogo();

  const doc = new jsPDF({ unit: 'mm', format: 'a4' });
  const pageW = doc.internal.pageSize.getWidth();
  const pageH = doc.internal.pageSize.getHeight();
  const margin = 12;

  // Franja superior charcoal con el logo y el título
  doc.setFillColor(...rgb(CHARCOAL));
  doc.rect(0, 0, pageW, 34, 'F');
  if (logo) {
    const h = 22;
    doc.addImage(logo.dataUrl, 'PNG', margin, 6, h * logo.ratio, h);
  }
  doc.setTextColor(...rgb(CREAM));
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(17);
  doc.text('Reporte de ventas', pageW - margin, 15, { align: 'right' });
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(9);
  doc.text(periodTitle(r), pageW - margin, 21, {
    align: 'right',
  });
  doc.text(`Generado el ${dateTimeLabel(r.generatedAt)}`, pageW - margin, 26, {
    align: 'right',
  });

  let y = 44;
  const lastY = () => (doc as any).lastAutoTable?.finalY ?? y;

  // Título de sección (salta de página si no cabe)
  const section = (title: string) => {
    if (y > pageH - 40) {
      doc.addPage();
      y = 20;
    }
    doc.setTextColor(...rgb(TERRA));
    doc.setFont('helvetica', 'bold');
    doc.setFontSize(12);
    doc.text(title, margin, y);
    y += 3;
  };

  const table = (
    head: string[],
    body: string[][],
    right: number[],
    foot?: string[],
  ) => {
    autoTable(doc, {
      startY: y,
      head: [head],
      body,
      foot: foot ? [foot] : undefined,
      margin: { left: margin, right: margin, top: 18 },
      styles: { fontSize: 8.5, cellPadding: 2, textColor: rgb(CHARCOAL) },
      headStyles: { fillColor: rgb(FOREST), textColor: rgb(CREAM) },
      footStyles: {
        fillColor: [239, 236, 228],
        textColor: rgb(CHARCOAL),
        fontStyle: 'bold',
      },
      alternateRowStyles: { fillColor: [250, 248, 243] },
      columnStyles: Object.fromEntries(
        right.map((c) => [c, { halign: 'right' as const }]),
      ),
    });
    y = lastY() + 10;
  };

  // Resumen
  section('Resumen');
  table(
    [
      'Indicador',
      'Periodo actual',
      `Anterior (${r.previousLabel})`,
      'Variación',
    ],
    r.summary.map((s) => [
      s.label,
      s.kind === 'money' ? fmtMoney(s.current) : String(s.current),
      s.kind === 'money' ? fmtMoney(s.previous) : String(s.previous),
      pctText(s.delta),
    ]),
    [1, 2, 3],
  );

  // Gráficas
  if (r.charts && r.charts.length) {
    const find = (k: ChartImage['key']) => r.charts!.find((c) => c.key === k)!;
    const fullW = pageW - margin * 2;
    const halfW = (fullW - 6) / 2;

    // Pone una imagen a todo el ancho, saltando de página si no cabe
    const full = (img: ChartImage) => {
      const h = (fullW * img.h) / img.w;
      if (y + h > pageH - 16) {
        doc.addPage();
        y = 20;
      }
      doc.addImage(img.dataUrl, 'PNG', margin, y, fullW, h);
      y += h + 6;
    };

    section('Gráficas');
    y += 2;
    full(find('sales'));
    full(find('compare'));

    // Las dos pequeñas, lado a lado
    const a = find('top');
    const b = find('categories');
    const ha = (halfW * a.h) / a.w;
    const hb = (halfW * b.h) / b.w;
    if (y + Math.max(ha, hb) > pageH - 16) {
      doc.addPage();
      y = 20;
    }
    doc.addImage(a.dataUrl, 'PNG', margin, y, halfW, ha);
    doc.addImage(b.dataUrl, 'PNG', margin + halfW + 6, y, halfW, hb);
    y += Math.max(ha, hb) + 10;
  }

  // Categorías
  section('Ventas por categoría');
  table(
    ['Categoría', 'Unidades', 'Ventas', 'Ganancia'],
    r.categories.map((c) => [
      c.name,
      String(c.units),
      fmtMoney(c.revenue),
      fmtMoney(c.profit),
    ]),
    [1, 2, 3],
  );

  // Platos
  section(
    `Platos y ganancia (${Math.round(PROFIT_MARKUP * 100)} % incluido en el precio)`,
  );
  const tUnits = r.dishes.reduce((s, d) => s + d.units, 0);
  const tSales = r.dishes.reduce((s, d) => s + d.sales, 0);
  const tProfit = r.dishes.reduce((s, d) => s + d.profit, 0);
  table(
    ['Plato', 'Precio', 'Costo', 'Gan. u.', 'Uds.', 'Ventas', 'Ganancia'],
    r.dishes.map((d) => [
      d.name,
      fmtMoney(d.price),
      fmtMoney(d.cost),
      fmtMoney(d.unitProfit),
      String(d.units),
      fmtMoney(d.sales),
      fmtMoney(d.profit),
    ]),
    [1, 2, 3, 4, 5, 6],
    ['Total', '', '', '', String(tUnits), fmtMoney(tSales), fmtMoney(tProfit)],
  );

  // Pedidos
  section('Pedidos del periodo');
  table(
    ['N.º', 'Fecha', 'Cliente', 'Estado', 'Uds.', 'Total'],
    r.orders.map((o) => [
      `#${o.id}`,
      dateTimeLabel(o.date),
      o.clientName,
      orderStatusLabel(o.status),
      String(o.units),
      fmtMoney(o.total),
    ]),
    [4, 5],
  );

  // Pie de página con numeración en todas las hojas
  const pages = doc.getNumberOfPages();
  for (let p = 1; p <= pages; p++) {
    doc.setPage(p);
    doc.setDrawColor(...rgb(FOREST));
    doc.line(margin, pageH - 12, pageW - margin, pageH - 12);
    doc.setTextColor(120, 120, 120);
    doc.setFont('helvetica', 'normal');
    doc.setFontSize(8);
    doc.text('Terra Galega', margin, pageH - 7);
    doc.text(`Página ${p} de ${pages}`, pageW - margin, pageH - 7, {
      align: 'right',
    });
  }

  if (previewWindow.closed) {
    throw new Error(
      'Se cerró la vista previa antes de terminar de generar el PDF.',
    );
  }

  doc.setProperties({
    title: `${r.fileBase}.pdf`,
  });

  // Genera el PDF en memoria sin descargarlo.
  const url = URL.createObjectURL(doc.output('blob'));

  try {
    // Abre el PDF en el visor del navegador.
    previewWindow.location.replace(url);
  } catch (error) {
    URL.revokeObjectURL(url);
    throw error;
  }

  // Conserva el PDF mientras la pestaña siga abierta,
  // para que el administrador pueda descargarlo cuando quiera.
  const cleanup = window.setInterval(() => {
    if (previewWindow.closed) {
      URL.revokeObjectURL(url);
      window.clearInterval(cleanup);
    }
  }, 1000);
}
