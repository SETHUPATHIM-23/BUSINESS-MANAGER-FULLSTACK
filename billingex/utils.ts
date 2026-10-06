import { Invoice } from './types';

export const formatCurrency = (amount: number): string => {
  return new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2
  }).format(amount);
};

export const formatTime12Hour = (timeStr?: string): string => {
  if (!timeStr) return '';
  if (timeStr.includes('AM') || timeStr.includes('PM') || timeStr.includes('am') || timeStr.includes('pm')) {
    return timeStr;
  }
  const parts = timeStr.split(':');
  if (parts.length < 2) return timeStr;
  let hours = parseInt(parts[0], 10);
  if (isNaN(hours)) return timeStr;
  const ampm = hours >= 12 ? 'PM' : 'AM';
  hours = hours % 12;
  hours = hours ? hours : 12;
  const minutes = parts[1].slice(0, 2);
  return `${String(hours).padStart(2, '0')}:${minutes} ${ampm}`;
};

export const numberToWords = (num: number): string => {
  const a = ['', 'One ', 'Two ', 'Three ', 'Four ', 'Five ', 'Six ', 'Seven ', 'Eight ', 'Nine ', 'Ten ', 'Eleven ', 'Twelve ', 'Thirteen ', 'Fourteen ', 'Fifteen ', 'Sixteen ', 'Seventeen ', 'Eighteen ', 'Nineteen '];
  const b = ['', '', 'Twenty', 'Thirty', 'Forty', 'Fifty', 'Sixty', 'Seventy', 'Eighty', 'Ninety'];

  const inWords = (n: number): string => {
    if (n < 20) return a[n];
    if (n < 100) return b[Math.floor(n / 10)] + (n % 10 !== 0 ? ' ' + a[n % 10] : '');
    if (n < 1000) return a[Math.floor(n / 100)] + 'Hundred ' + (n % 100 !== 0 ? 'and ' + inWords(n % 100) : '');
    if (n < 100000) return inWords(Math.floor(n / 1000)) + 'Thousand ' + (n % 1000 !== 0 ? inWords(n % 1000) : '');
    if (n < 10000000) return inWords(Math.floor(n / 100000)) + 'Lakh ' + (n % 100000 !== 0 ? inWords(n % 100000) : '');
    return inWords(Math.floor(n / 10000000)) + 'Crore ' + (n % 10000000 !== 0 ? inWords(n % 10000000) : '');
  };

  const whole = Math.floor(num);
  const fraction = Math.round((num - whole) * 100);
  
  let str = inWords(whole) + 'Rupees ';
  if (fraction > 0) {
    str += 'and ' + inWords(fraction) + 'Paise ';
  }
  return str + 'Only';
};

export const getCSVContent = (invoices: Invoice[]): string => {
  const headers = [
    'Date',
    'Invoice No',
    'Customer Name',
    'Customer GSTIN',
    'Vehicle No',
    'Subtotal',
    'CGST',
    'SGST',
    'Round Off',
    'Grand Total'
  ];

  const rows = invoices.map(inv => [
    new Date(inv.invoiceDate).toLocaleDateString(),
    inv.invoiceNo,
    `"${inv.customerName}"`,
    inv.customerGSTIN || '',
    inv.vehicleNo || '',
    inv.subtotal.toFixed(2),
    inv.cgst.toFixed(2),
    inv.sgst.toFixed(2),
    inv.roundOff.toFixed(2),
    inv.grandTotal.toFixed(2)
  ]);

  return [
    headers.join(','),
    ...rows.map(r => r.join(','))
  ].join('\n');
};

export const exportInvoicesToCSV = (invoices: Invoice[]) => {
  if (invoices.length === 0) return;
  const csvContent = getCSVContent(invoices);
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.setAttribute('href', url);
  link.setAttribute('download', `Senthur_Chemical_Sales_Report_${new Date().toLocaleDateString()}.csv`);
  link.style.visibility = 'hidden';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

/**
 * File System Access API Helpers
 */
export async function getStructuredFolder(rootHandle: any, date: Date) {
  const year = date.getFullYear().toString();
  const month = date.toLocaleString('default', { month: 'long' });
  
  const yearHandle = await rootHandle.getDirectoryHandle(year, { create: true });
  const monthHandle = await yearHandle.getDirectoryHandle(month, { create: true });
  
  return monthHandle;
}

export async function saveFileToFolder(folderHandle: any, fileName: string, blob: Blob) {
  const fileHandle = await folderHandle.getFileHandle(fileName, { create: true });
  const writable = await fileHandle.createWritable();
  await writable.write(blob);
  await writable.close();
}
