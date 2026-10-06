import React, { useState, useCallback } from 'react';
import ReactDOM from 'react-dom/client';
import { api } from '../../utils/api';
import { toastEvents } from '../../utils/toast';
import { getStoredCompanyDetails } from '../billing/CompanySettings';
import { jsPDF } from 'jspdf';
import html2canvas from 'html2canvas';
import { Download, RefreshCw, FileText, AlertCircle } from 'lucide-react';

// ---------- constants ----------
const MONTHS = [
  'January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December',
];

const fmt = (v: number) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', minimumFractionDigits: 2 }).format(v || 0);

const fmtDate = (d: string) => {
  try { return new Date(d).toLocaleDateString('en-IN'); } catch { return d; }
};

// ---------- API mapping ----------
type MappedInvoice = ReturnType<typeof mapApiInvoice>;

const mapApiInvoice = (apiInv: any) => ({
  id: apiInv.id,
  invoiceNo: apiInv.invoiceNumber,
  invoiceDate: apiInv.invoiceDate,
  invoiceTime: apiInv.invoiceTime || '',
  customerName: apiInv.customerName || apiInv.customer?.name || '',
  customerAddress: apiInv.customerAddress || apiInv.customer?.address || '',
  customerGSTIN: apiInv.customerGSTIN || apiInv.customerTaxId || apiInv.customer?.taxId || '',
  customerState: apiInv.customerState || apiInv.customer?.state || 'Tamil Nadu',
  customerStateCode: apiInv.customerStateCode || apiInv.customer?.stateCode || '33',
  vehicleNo: apiInv.vehicleNo || '',
  billType: apiInv.billType || 'tax_exclusive',
  items: (apiInv.lines || []).map((l: any, idx: number) => ({
    sNo: idx + 1,
    productName: l.productName || l.product?.name || '',
    hsnCode: l.hsnCode || l.product?.hsnCode || '',
    dcNo: l.dcNo || '-',
    quantity: Number(l.quantity) || 0,
    rate: Number(l.unitPrice) || 0,
    amount: Number(l.lineTotal) || 0,
  })),
  subtotal: apiInv.subtotal || 0,
  cgst: apiInv.cgstAmount ?? 0,
  sgst: apiInv.sgstAmount ?? 0,
  igst: apiInv.igstAmount ?? 0,
  cgstRate: apiInv.cgstRate ?? 0,
  sgstRate: apiInv.sgstRate ?? 0,
  igstRate: apiInv.igstRate ?? 0,
  roundOff: apiInv.roundOff || 0,
  grandTotal: apiInv.grandTotal || 0,
  notes: apiInv.notes || '',
  taxLines: apiInv.taxLines || [],
});

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// HALF-PAGE TRANSPORTER COPY
// A4 landscape half = 560px wide Ã— 794px tall
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
interface HalfCopyProps {
  invoice: MappedInvoice;
  companyDetails: ReturnType<typeof getStoredCompanyDetails>;
}

const HalfTransporterCopy: React.FC<HalfCopyProps> = ({ invoice, companyDetails }) => {
  const isChallan = invoice.billType === 'delivery_challan';
  const dateStr = fmtDate(invoice.invoiceDate);
  const BLUE = '#1e3a8a';
  const LIGHT_BLUE = '#4a6fa5';
  const BORDER = '#374151';

  const taxRows: Array<[string, number]> =
    invoice.taxLines && invoice.taxLines.length > 0
      ? invoice.taxLines.map((tl: any) => [tl.name + (tl.rate ? ` (${tl.rate}%)` : ''), Number(tl.amount)] as [string, number])
      : [
          ...(invoice.cgst > 0 ? [[`CGST (${invoice.cgstRate}%)`, invoice.cgst] as [string, number]] : []),
          ...(invoice.sgst > 0 ? [[`SGST (${invoice.sgstRate}%)`, invoice.sgst] as [string, number]] : []),
          ...(invoice.igst > 0 ? [[`IGST (${invoice.igstRate}%)`, invoice.igst] as [string, number]] : []),
        ];

  const metaRows: [string, string][] = [
    ...(!isChallan ? [['Invoice No', invoice.invoiceNo] as [string, string]] : []),
    ['Date', dateStr],
    ['Vehicle No', invoice.vehicleNo || 'N/A'],
    ['State', `${invoice.customerState} (${invoice.customerStateCode})`],
  ];

  const visibleItems = invoice.items.slice(0, 10);
  const hiddenCount = invoice.items.length - visibleItems.length;

  return (
    <div style={{
      width: '560px', height: '794px', boxSizing: 'border-box',
      padding: '9px 11px 7px',
      backgroundColor: '#ffffff', color: '#000000',
      fontFamily: 'Arial, sans-serif', fontSize: '9px',
      display: 'flex', flexDirection: 'column', overflow: 'hidden',
    }}>
      {/* Watermark */}
      <div style={{
        textAlign: 'center', fontSize: '7px', fontWeight: 900,
        textTransform: 'uppercase', letterSpacing: '0.15em', color: '#777',
        borderBottom: '1.5px dashed #bbb', paddingBottom: '3px', marginBottom: '4px',
      }}>
        ORIGINAL FOR TRANSPORTER
      </div>

      {/* Title */}
      <div style={{
        textAlign: 'center', fontSize: '11px', fontWeight: 900,
        textTransform: 'uppercase', marginBottom: '4px', color: '#000',
      }}>
        {isChallan ? 'Delivery Challan' : 'Tax Invoice'}
      </div>

      {/* Company + Meta */}
      <div style={{ display: 'flex', justifyContent: 'space-between', gap: '7px', borderBottom: `2px solid ${BORDER}`, paddingBottom: '4px', marginBottom: '4px' }}>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: '13px', fontWeight: 900, color: BLUE, textTransform: 'uppercase', lineHeight: 1.1, marginBottom: '2px' }}>
            {companyDetails.name}
          </div>
          <div style={{ fontSize: '7.5px', color: '#333', whiteSpace: 'pre-line', lineHeight: 1.3, marginBottom: '2px' }}>
            {companyDetails.address}
          </div>
          <div style={{ fontSize: '7.5px', fontWeight: 700, color: '#444' }}>GSTIN: {companyDetails.gstin}</div>
          <div style={{ fontSize: '7.5px', fontWeight: 700, color: '#444' }}>Mobile: {companyDetails.mobile}</div>
        </div>
        <div style={{ border: `1px solid ${BORDER}`, fontSize: '7.5px', minWidth: '145px', flexShrink: 0, alignSelf: 'flex-start', overflow: 'hidden' }}>
          <div style={{ background: BLUE, color: '#fff', textAlign: 'center', fontWeight: 900, padding: '2px 4px', fontSize: '9px', textTransform: 'uppercase' }}>
            {isChallan ? 'Delivery Challan' : 'Tax Invoice'}
          </div>
          {metaRows.map(([k, v]) => (
            <div key={k} style={{ display: 'flex', borderTop: '0.5px solid #aaa' }}>
              <div style={{ width: '65px', padding: '1.5px 3px', fontWeight: 700, background: '#f5f5f5', borderRight: '0.5px solid #aaa', fontSize: '7px', flexShrink: 0, textTransform: 'uppercase' }}>{k}</div>
              <div style={{ padding: '1.5px 3px', fontWeight: 900, fontSize: '8px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', flex: 1 }}>{v}</div>
            </div>
          ))}
        </div>
      </div>

      {/* Bill To + Consignee */}
      <div style={{ display: 'flex', gap: '4px', marginBottom: '4px' }}>
        {['RECEIVER (BILL TO):', 'CONSIGNEE (SHIP TO):'].map(lbl => (
          <div key={lbl} style={{
            flex: 1, border: '1px solid #aaa', padding: '2.5px 5px',
            fontSize: '7.5px', lineHeight: 1.3, background: '#fafafa',
            minHeight: '52px', display: 'flex', flexDirection: 'column', justifyContent: 'space-between',
          }}>
            <div>
              <div style={{ fontWeight: 700, textTransform: 'uppercase', color: '#555', fontSize: '6.5px', borderBottom: '0.5px solid #ddd', paddingBottom: '1.5px', marginBottom: '2px' }}>{lbl}</div>
              <div style={{ fontWeight: 900, fontSize: '10px', textTransform: 'uppercase', lineHeight: 1.2, marginBottom: '2px' }}>{invoice.customerName}</div>
              {invoice.customerAddress && <div style={{ color: '#555', fontSize: '7px', lineHeight: 1.3 }}>{invoice.customerAddress}</div>}
            </div>
            {invoice.customerGSTIN && (
              <div style={{ fontWeight: 700, fontSize: '7px', borderTop: '0.5px solid #ddd', paddingTop: '1.5px', marginTop: '2px' }}>
                GSTIN: <span style={{ fontWeight: 900 }}>{invoice.customerGSTIN}</span>
              </div>
            )}
          </div>
        ))}
      </div>

      {/* Items table */}
      <div style={{ border: `2px solid ${BORDER}`, marginBottom: '3px', overflow: 'hidden', flex: 1, display: 'flex', flexDirection: 'column' }}>
        <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '7.5px', tableLayout: 'fixed' }}>
          <thead>
            <tr style={{ background: BLUE, color: '#fff' }}>
              {(['S.No', 'Description', 'DC No', 'HSN', 'Qty (Ltrs)', 'Rate', 'Amount'] as const).map((h, i) => (
                <th key={h} style={{
                  padding: '2px 2.5px',
                  textAlign: i >= 4 ? 'right' : i === 0 ? 'center' : 'left',
                  borderRight: i < 6 ? `0.5px solid ${LIGHT_BLUE}` : undefined,
                  fontWeight: 900, textTransform: 'uppercase', fontSize: '6.5px',
                  width: i === 0 ? '20px' : i === 2 ? '34px' : i === 3 ? '34px' : i === 4 ? '46px' : i === 5 ? '48px' : i === 6 ? '56px' : undefined,
                  whiteSpace: 'nowrap', overflow: 'hidden',
                }}>{h}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {visibleItems.map((item: MappedInvoice['items'][number], i: number) => (
              <tr key={i} style={{ background: i % 2 === 0 ? '#fafafa' : '#fff', borderBottom: '0.5px solid #ddd' }}>
                <td style={{ padding: '1.5px 2.5px', textAlign: 'center', borderRight: '0.5px solid #ddd', fontWeight: 900 }}>{item.sNo}</td>
                <td style={{ padding: '1.5px 2.5px', borderRight: '0.5px solid #ddd', fontWeight: 900, textTransform: 'uppercase', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{item.productName}</td>
                <td style={{ padding: '1.5px 2.5px', borderRight: '0.5px solid #ddd', textAlign: 'center', fontWeight: 700 }}>{item.dcNo || '-'}</td>
                <td style={{ padding: '1.5px 2.5px', borderRight: '0.5px solid #ddd', textAlign: 'center', fontWeight: 700 }}>{item.hsnCode || '-'}</td>
                <td style={{ padding: '1.5px 2.5px', borderRight: '0.5px solid #ddd', textAlign: 'right', fontFamily: 'monospace', fontWeight: 900 }}>{item.quantity}</td>
                <td style={{ padding: '1.5px 2.5px', borderRight: '0.5px solid #ddd', textAlign: 'right', fontFamily: 'monospace', fontWeight: 700 }}>{item.rate.toFixed(2)}</td>
                <td style={{ padding: '1.5px 2.5px', textAlign: 'right', fontFamily: 'monospace', fontWeight: 900 }}>{item.amount.toFixed(2)}</td>
              </tr>
            ))}
            {hiddenCount > 0 && (
              <tr>
                <td colSpan={7} style={{ padding: '1.5px 3px', color: '#888', fontStyle: 'italic', fontSize: '6.5px' }}>
                  â€¦ {hiddenCount} more item(s) â€” see original invoice
                </td>
              </tr>
            )}
            <tr style={{ height: '100%' }}>
              {[0,1,2,3,4,5,6].map(i => (
                <td key={i} style={{ borderRight: i < 6 ? '0.5px solid #ddd' : undefined }} />
              ))}
            </tr>
          </tbody>
        </table>
      </div>

      {/* Footer: bank + totals */}
      <div style={{ display: 'flex', border: `2px solid ${BORDER}`, marginBottom: '3px', overflow: 'hidden', flexShrink: 0 }}>
        {/* Bank details */}
        <div style={{ width: '52%', borderRight: `2px solid ${BORDER}`, display: 'flex', flexDirection: 'column' }}>
          <div style={{ borderBottom: `1px solid ${BORDER}`, padding: '2px 4px', background: '#fff' }}>
            <div style={{ fontSize: '6.5px', fontWeight: 700, textTransform: 'uppercase', color: '#666', marginBottom: '1px' }}>Total Amount in Words:</div>
            <div style={{ fontSize: '7px', fontWeight: 900, textTransform: 'uppercase', lineHeight: 1.3 }}>
              {fmt(invoice.grandTotal)} Only
            </div>
          </div>
          <div style={{ padding: '2px 4px', flex: 1 }}>
            <div style={{ fontSize: '7px', fontWeight: 900, textTransform: 'uppercase', marginBottom: '2px', borderBottom: '0.5px solid #ddd', paddingBottom: '1px' }}>Bank Details</div>
            <table style={{ width: '100%', borderCollapse: 'collapse', border: `1px solid ${BORDER}`, fontSize: '7px' }}>
              <tbody>
                {[
                  ['Bank', companyDetails.bankName],
                  ['Acc No', companyDetails.accountNo],
                  ['IFSC', companyDetails.ifscCode],
                  ['Branch', companyDetails.branch],
                ].map(([k, v]) => (
                  <tr key={k} style={{ borderBottom: '0.5px solid #ddd' }}>
                    <td style={{ padding: '1px 2.5px', background: '#f5f5f5', borderRight: '0.5px solid #ddd', fontWeight: 700, width: '36px', textTransform: 'uppercase', whiteSpace: 'nowrap' }}>{k}</td>
                    <td style={{ padding: '1px 2.5px', fontWeight: 900, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{v || 'â€”'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {invoice.notes && (
              <div style={{ fontSize: '6.5px', marginTop: '2px', fontStyle: 'italic', color: '#555', lineHeight: 1.3 }}>{invoice.notes}</div>
            )}
          </div>
        </div>
        {/* Tax summary */}
        <div style={{ width: '48%', fontSize: '7.5px' }}>
          <table style={{ width: '100%', borderCollapse: 'collapse', height: '100%' }}>
            <tbody>
              <tr style={{ borderBottom: '0.5px solid #ddd' }}>
                <td style={{ padding: '1.5px 3px', borderRight: '0.5px solid #ddd', fontWeight: 700, textTransform: 'uppercase', fontSize: '7px', color: '#555' }}>Subtotal</td>
                <td style={{ padding: '1.5px 3px', textAlign: 'right', fontFamily: 'monospace', fontWeight: 900 }}>{fmt(invoice.subtotal)}</td>
              </tr>
              {!isChallan && taxRows.map(([lbl, val], idx) => (
                <tr key={idx} style={{ borderBottom: '0.5px solid #ddd' }}>
                  <td style={{ padding: '1.5px 3px', borderRight: '0.5px solid #ddd', fontWeight: 700, textTransform: 'uppercase', fontSize: '7px', color: '#555' }}>{lbl}</td>
                  <td style={{ padding: '1.5px 3px', textAlign: 'right', fontFamily: 'monospace', fontWeight: 900 }}>{fmt(val)}</td>
                </tr>
              ))}
              {invoice.roundOff !== 0 && (
                <tr style={{ borderBottom: '0.5px solid #ddd' }}>
                  <td style={{ padding: '1.5px 3px', borderRight: '0.5px solid #ddd', fontWeight: 700, textTransform: 'uppercase', fontSize: '7px', color: '#555' }}>Round Off</td>
                  <td style={{ padding: '1.5px 3px', textAlign: 'right', fontFamily: 'monospace', fontWeight: 900 }}>{fmt(invoice.roundOff)}</td>
                </tr>
              )}
              <tr style={{ height: '100%' }}><td colSpan={2} /></tr>
              <tr style={{ background: BLUE, color: '#fff' }}>
                <td style={{ padding: '2.5px 3px', fontWeight: 900, textTransform: 'uppercase', fontSize: '8.5px', borderRight: `0.5px solid ${LIGHT_BLUE}` }}>TOTAL</td>
                <td style={{ padding: '2.5px 3px', textAlign: 'right', fontFamily: 'monospace', fontWeight: 900, fontSize: '10px' }}>{fmt(invoice.grandTotal)}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* Signatures */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end', flexShrink: 0, marginBottom: '2px' }}>
        <div style={{ textAlign: 'center' }}>
          <div style={{ borderTop: '1px solid #000', width: '105px', paddingTop: '2px', fontSize: '7px', fontWeight: 700, textTransform: 'uppercase' }}>
            Customer's Signature
          </div>
        </div>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: '7px', fontWeight: 700, textTransform: 'uppercase', marginBottom: '13px' }}>
            For {companyDetails.name}
          </div>
          <div style={{ borderTop: '1px solid #000', width: '115px', paddingTop: '2px', fontSize: '7px', fontWeight: 700, textTransform: 'uppercase' }}>
            Authorised Signatory
          </div>
        </div>
      </div>

      {/* Disclaimer */}
      <div style={{
        borderTop: '1px solid #000', paddingTop: '2px',
        fontSize: '6px', color: '#777', fontWeight: 700,
        textTransform: 'uppercase', display: 'flex', justifyContent: 'space-between',
        flexShrink: 0,
      }}>
        <span>This is a computer generated invoice. Subject to Erode Jurisdiction.</span>
        {invoice.invoiceTime && <span style={{ fontWeight: 900, color: '#444' }}>Time: {invoice.invoiceTime}</span>}
      </div>
    </div>
  );
};

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// A4 LANDSCAPE PAGE â€” one invoice, two identical copies side by side
// 1123px wide Ã— 794px tall (A4 landscape at 96dpi)
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
interface LandscapePageProps {
  invoice: MappedInvoice;
  companyDetails: ReturnType<typeof getStoredCompanyDetails>;
}

const LandscapeTransporterPage: React.FC<LandscapePageProps> = ({ invoice, companyDetails }) => (
  <div style={{
    width: '1123px', height: '794px',
    background: '#fff', display: 'flex', flexDirection: 'row',
    boxSizing: 'border-box', overflow: 'hidden',
  }}>
    {/* Left copy */}
    <div style={{ width: '560px', height: '794px', overflow: 'hidden', flexShrink: 0 }}>
      <HalfTransporterCopy invoice={invoice} companyDetails={companyDetails} />
    </div>

    {/* Dashed vertical divider */}
    <div style={{ width: '3px', height: '794px', borderLeft: '2px dashed #aaa', flexShrink: 0 }} />

    {/* Right copy (identical) */}
    <div style={{ width: '560px', height: '794px', overflow: 'hidden', flexShrink: 0 }}>
      <HalfTransporterCopy invoice={invoice} companyDetails={companyDetails} />
    </div>
  </div>
);

// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
// MAIN COMPONENT
// â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
export const MonthlyBillsDownload: React.FC = () => {
  const companyDetails = getStoredCompanyDetails();
  const now = new Date();
  const [selectedMonth, setSelectedMonth] = useState(now.getMonth());
  const [selectedYear, setSelectedYear] = useState(now.getFullYear());
  const [generating, setGenerating] = useState(false);
  const [progress, setProgress] = useState({ done: 0, total: 0, stage: '' });
  const years = Array.from({ length: 6 }, (_, i) => now.getFullYear() - i);

  const capturePage = useCallback(async (invoice: MappedInvoice): Promise<HTMLCanvasElement> => {
    return new Promise((resolve, reject) => {
      const mountEl = document.createElement('div');
      mountEl.style.cssText = 'position:fixed;left:-9999px;top:0;z-index:-1;pointer-events:none;';
      document.body.appendChild(mountEl);

      const root = ReactDOM.createRoot(mountEl);
      root.render(<LandscapeTransporterPage invoice={invoice} companyDetails={companyDetails} />);

      setTimeout(async () => {
        try {
          const canvas = await html2canvas(mountEl.firstChild as HTMLElement, {
            scale: 2, useCORS: true, logging: false,
            backgroundColor: '#ffffff',
            width: 1123, height: 794,
            windowWidth: 1123, windowHeight: 794,
          });
          root.unmount();
          document.body.removeChild(mountEl);
          resolve(canvas);
        } catch (err) {
          root.unmount();
          document.body.removeChild(mountEl);
          reject(err);
        }
      }, 130);
    });
  }, [companyDetails]);

  const handleDownload = async () => {
    setGenerating(true);
    setProgress({ done: 0, total: 0, stage: 'Fetching invoice listâ€¦' });

    try {
      const y = selectedYear;
      const m = selectedMonth + 1;
      const startDate = `${y}-${String(m).padStart(2, '0')}-01`;
      const lastDay = new Date(y, m, 0).getDate();
      const endDate = `${y}-${String(m).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}`;

      // Page through all billings
      const allBillings: any[] = [];
      let pg = 0, totalPages = 1;
      do {
        const res = await api.get('/api/billings', {
          params: { startDate, endDate, page: pg, size: 100, sort: 'invoiceDate,asc' },
        });
        const content = res.data.content ?? res.data ?? [];
        allBillings.push(...content);
        totalPages = res.data.totalPages ?? 1;
        pg++;
      } while (pg < totalPages);

      if (allBillings.length === 0) {
        toastEvents.error(`No invoices found for ${MONTHS[selectedMonth]} ${y}`);
        return;
      }

      setProgress({ done: 0, total: allBillings.length, stage: 'Loading invoice detailsâ€¦' });

      // Fetch full details
      const invoices: MappedInvoice[] = [];
      for (let i = 0; i < allBillings.length; i++) {
        try {
          const res = await api.get(`/api/billings/${allBillings[i].id}`);
          invoices.push(mapApiInvoice(res.data));
        } catch { /* skip */ }
        setProgress({ done: i + 1, total: allBillings.length, stage: `Loading invoice detailsâ€¦ (${i + 1}/${allBillings.length})` });
      }

      if (invoices.length === 0) {
        toastEvents.error('Could not load any invoice details.');
        return;
      }

      setProgress({ done: 0, total: invoices.length, stage: 'Rendering pagesâ€¦' });

      // A4 landscape: 297mm Ã— 210mm â€” one page per invoice
      const pdf = new jsPDF({ orientation: 'landscape', unit: 'mm', format: 'a4', compress: true });
      let isFirst = true;

      for (let i = 0; i < invoices.length; i++) {
        const canvas = await capturePage(invoices[i]);
        if (!isFirst) pdf.addPage('a4', 'landscape');

        const imgData = canvas.toDataURL('image/jpeg', 0.90);
        pdf.addImage(imgData, 'JPEG', 0, 0, 297, 210, undefined, 'FAST');

        pdf.setFontSize(5.5);
        pdf.setFont('helvetica', 'bold');
        pdf.setTextColor(160, 160, 160);
        pdf.text(
          `${MONTHS[selectedMonth].toUpperCase()} ${y} â€” TRANSPORTER COPIES â€” ${i + 1} of ${invoices.length}`,
          148.5, 2, { align: 'center' }
        );

        isFirst = false;
        setProgress({ done: i + 1, total: invoices.length, stage: `Rendering pagesâ€¦ (${i + 1}/${invoices.length})` });
      }

      const fileName = `Invoices_${MONTHS[selectedMonth]}_${y}_Transporter.pdf`;
      pdf.save(fileName);
      toastEvents.success(`âœ… Downloaded ${invoices.length} invoice(s) â€” ${fileName}`);
    } catch (err: any) {
      console.error(err);
      toastEvents.error('Failed to generate PDF. Check console for details.');
    } finally {
      setGenerating(false);
      setProgress({ done: 0, total: 0, stage: '' });
    }
  };

  const pct = progress.total > 0 ? Math.round((progress.done / progress.total) * 100) : 0;

  return (
    <div style={{ maxWidth: '700px', margin: '0 auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '0.8rem', marginBottom: '1.5rem' }}>
        <div style={{ width: '44px', height: '44px', borderRadius: '10px', background: 'rgba(59,130,246,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
          <FileText size={22} color="#3b82f6" />
        </div>
        <div>
          <h2 style={{ fontSize: '1.15rem', fontWeight: 800, color: 'var(--text-primary)', margin: 0, lineHeight: 1.2 }}>
            Monthly Bills â€” Transporter Copy PDF
          </h2>
          <p style={{ fontSize: '0.82rem', color: 'var(--text-secondary)', margin: 0, marginTop: '2px' }}>
            Download all bills as a single <strong>A4 Landscape</strong> PDF â€”{' '}
            <em>2 identical "Original for Transporter" copies side-by-side</em> per page.
          </p>
        </div>
      </div>

      <div style={{ background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: '14px', padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'flex-end' }}>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem', flex: '1 1 160px' }}>
            <label style={{ fontSize: '0.7rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-secondary)' }}>Month</label>
            <select className="form-select" value={selectedMonth} onChange={e => setSelectedMonth(Number(e.target.value))} disabled={generating} style={{ fontSize: '0.92rem', fontWeight: 600, padding: '0.55rem 0.8rem' }}>
              {MONTHS.map((m, i) => <option key={m} value={i}>{m}</option>)}
            </select>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.3rem', flex: '0 0 115px' }}>
            <label style={{ fontSize: '0.7rem', fontWeight: 700, textTransform: 'uppercase', letterSpacing: '0.06em', color: 'var(--text-secondary)' }}>Year</label>
            <select className="form-select" value={selectedYear} onChange={e => setSelectedYear(Number(e.target.value))} disabled={generating} style={{ fontSize: '0.92rem', fontWeight: 600, padding: '0.55rem 0.8rem' }}>
              {years.map(y => <option key={y} value={y}>{y}</option>)}
            </select>
          </div>

          <button
            onClick={handleDownload}
            disabled={generating}
            style={{
              display: 'inline-flex', alignItems: 'center', gap: '0.5rem',
              padding: '0.6rem 1.4rem', borderRadius: '9px',
              fontWeight: 700, fontSize: '0.88rem',
              background: generating ? 'rgba(255,255,255,0.06)' : '#3b82f6',
              color: generating ? 'var(--text-secondary)' : '#fff',
              border: generating ? '1px solid var(--border)' : 'none',
              cursor: generating ? 'not-allowed' : 'pointer',
              alignSelf: 'flex-end', minWidth: '210px', justifyContent: 'center',
            }}
          >
            {generating
              ? <><RefreshCw size={15} style={{ animation: 'mbd-spin 1s linear infinite' }} /> Generatingâ€¦</>
              : <><Download size={15} /> Download Transporter PDF</>}
          </button>
        </div>

        {generating && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '0.78rem', color: 'var(--text-secondary)' }}>
              <span>{progress.stage}</span>
              <span style={{ fontWeight: 700, fontFamily: 'monospace' }}>{pct}%</span>
            </div>
            <div style={{ height: '7px', background: 'var(--border)', borderRadius: '99px', overflow: 'hidden' }}>
              <div style={{ height: '100%', width: `${pct}%`, background: 'linear-gradient(90deg,#3b82f6,#6366f1)', borderRadius: '99px', transition: 'width 0.25s ease' }} />
            </div>
          </div>
        )}

        <div style={{ display: 'flex', gap: '0.65rem', padding: '0.85rem 1rem', background: 'rgba(245,158,11,0.07)', border: '1px solid rgba(245,158,11,0.28)', borderRadius: '9px', fontSize: '0.79rem', color: 'var(--text-secondary)' }}>
          <AlertCircle size={15} color="#f59e0b" style={{ flexShrink: 0, marginTop: '1px' }} />
          <div>
            <strong style={{ color: '#f59e0b' }}>What this generates:</strong>{' '}
            A4 <strong>landscape</strong> PDF â€” each page has <strong>one invoice printed twice side-by-side</strong>{' '}
            (left and right are identical "Original for Transporter" copies, separated by a dashed cut line).{' '}
            Full invoice layout: company header, Bill To / Consignee, item table (with DC No, HSN, Qty, Rate), tax summary, bank details, and signatures.
            One page per invoice. Large months may take a minute.
          </div>
        </div>
      </div>

      <style>{`@keyframes mbd-spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }`}</style>
    </div>
  );
};

export default MonthlyBillsDownload;

