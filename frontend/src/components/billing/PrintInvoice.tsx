import { useRef, useState, useImperativeHandle, forwardRef, memo } from 'react';
import { type CompanyDetails } from './CompanySettings';
import type { BillType, InvoiceItem } from './types';
import { formatCurrency, formatTime12Hour, exportInvoicesToCSV, getStructuredFolder, saveFileToFolder, getCSVContent, numberToWords } from './utils';
import { jsPDF } from 'jspdf';
import html2canvas from 'html2canvas';
import { Printer, Share2, Download, FileText } from 'lucide-react';
import { toastEvents } from '../../utils/toast';

export type { InvoiceItem };

export interface InvoiceData {
  id?: number;
  invoiceNo: string;
  invoiceDate: string;
  invoiceTime?: string;
  customerId?: number;
  customerName: string;
  customerAddress?: string;
  customerGSTIN?: string;
  customerState?: string;
  customerStateCode?: string;
  vehicleNo?: string;
  billType: BillType;
  items: InvoiceItem[];
  subtotal: number;
  taxLines?: Array<{ name: string; rate: number; amount: number }>;
  cgst: number;
  sgst: number;
  igst: number;
  roundOff: number;
  grandTotal: number;
  totalInWords: string;
  notes: string;
  gstPercentage?: number;
  cgstRate?: number;
  sgstRate?: number;
  igstRate?: number;
  taxLabel?: string;
  address?: string;
  customer?: any;
  gstin?: string;
  customerTaxId?: string;
  taxId?: string;
}

interface Props {
  invoice: InvoiceData;
  storageHandle?: any;
  companyDetails: CompanyDetails;
}

export interface PrintInvoiceHandle {
  printToSystem: () => Promise<void>;
}

interface PageConfig {
  items: InvoiceItem[];
  includeFooter: boolean;
  previousPageTotalQty: number;
  previousPageTotalAmount: number;
}

// ---------------------------------------------------------
// ACCURATE A4 PORTRAIT PAGINATION ENGINE (794x1123px)
// Table extends down to footer, footer anchored at page bottom
// ---------------------------------------------------------
const PAGE_HEIGHT = 1081; // Usable inner height inside page padding
const FULL_HEADER_HEIGHT = 360; // Title banner + Company details + Address boxes
const CONT_HEADER_HEIGHT = 65;  // Continuation header
const TABLE_HEADER_HEIGHT = 40; // Table <thead> + double border
const BROUGHT_FORWARD_ROW_HEIGHT = 36; // B/F row height
const FOOTER_HEIGHT = 300; // Summary block + Signatures + Disclaimer
const CONTINUATION_BANNER_HEIGHT = 30; // Continuation banner height
const SAFETY_MARGIN = 15; // Buffer to prevent overflow

export const PrintInvoice = memo(forwardRef<PrintInvoiceHandle, Props>(({ invoice, storageHandle, companyDetails }, ref) => {
  const invoiceRef = useRef<HTMLDivElement>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [saveStatus, setSaveStatus] = useState<string>('');

  const calculateInvoicePages = (items: InvoiceItem[] = []): PageConfig[] => {
    if (!items || items.length === 0) {
      return [{ items: [], includeFooter: true, previousPageTotalQty: 0, previousPageTotalAmount: 0 }];
    }

    const getItemHeight = (item: InvoiceItem): number => {
      const chars = item.productName ? item.productName.length : 0;
      if (chars > 75) return 68;
      if (chars > 30) return 52;
      return 36; // py-2 padding + line height + border
    };

    const pages: PageConfig[] = [];
    let remaining = [...items];
    let pageNum = 1;
    let runningTotalQty = 0;
    let runningTotalAmount = 0;

    while (remaining.length > 0) {
      const isFirst = pageNum === 1;
      const headerH = isFirst ? FULL_HEADER_HEIGHT : CONT_HEADER_HEIGHT;
      const bfRowH = isFirst ? 0 : BROUGHT_FORWARD_ROW_HEIGHT;

      const maxHWithFooter = PAGE_HEIGHT - headerH - TABLE_HEADER_HEIGHT - bfRowH - FOOTER_HEIGHT - SAFETY_MARGIN;
      const maxHWithoutFooter = PAGE_HEIGHT - headerH - TABLE_HEADER_HEIGHT - bfRowH - CONTINUATION_BANNER_HEIGHT - SAFETY_MARGIN;

      const prevQty = runningTotalQty;
      const prevAmount = runningTotalAmount;

      const totalRemainingH = remaining.reduce((sum, item) => sum + getItemHeight(item), 0);

      const pageItems: InvoiceItem[] = [];
      let includeFooter = false;

      if (totalRemainingH <= maxHWithFooter) {
        // All remaining items and footer fit on this page!
        while (remaining.length > 0) {
          const item = remaining.shift()!;
          pageItems.push(item);
          runningTotalQty += (Number(item.quantity) || 0);
          runningTotalAmount += (Number(item.amount) || 0);
        }
        includeFooter = true;
      } else {
        // Remaining items + footer do not fit together.
        // Fill this page up to maxHWithoutFooter, but ensure remaining has items for the next page.
        let currentTableH = 0;

        while (remaining.length > 0) {
          const h = getItemHeight(remaining[0]);
          if (currentTableH + h <= maxHWithoutFooter && remaining.length > 1) {
            const item = remaining.shift()!;
            pageItems.push(item);
            currentTableH += h;
            runningTotalQty += (Number(item.quantity) || 0);
            runningTotalAmount += (Number(item.amount) || 0);
          } else if (remaining.length === 1 && currentTableH + h <= maxHWithFooter) {
            const item = remaining.shift()!;
            pageItems.push(item);
            currentTableH += h;
            runningTotalQty += (Number(item.quantity) || 0);
            runningTotalAmount += (Number(item.amount) || 0);
            includeFooter = true;
          } else {
            break;
          }
        }
      }

      pages.push({
        items: pageItems,
        includeFooter,
        previousPageTotalQty: prevQty,
        previousPageTotalAmount: prevAmount
      });

      pageNum++;
    }

    return pages;
  };

  const pages = calculateInvoicePages(invoice.items);

  const generateDualPagePDF = async (autoPrint: boolean = false): Promise<{ blob: Blob; url: string }> => {
    if (!invoiceRef.current) throw new Error("Ref not found");
    const element = invoiceRef.current;
    
    const scaleFactor = 2; 
    const pageNodes = element.querySelectorAll('.invoice-page');
    const pageCanvases: HTMLCanvasElement[] = [];

    for (let i = 0; i < pageNodes.length; i++) {
      const pageNode = pageNodes[i] as HTMLElement;
      const canvas = await html2canvas(pageNode, {
        scale: scaleFactor, 
        useCORS: true,
        logging: false,
        backgroundColor: '#ffffff',
        width: 794,
        height: 1123,
        windowWidth: 794,
        windowHeight: 1123
      });
      pageCanvases.push(canvas);
    }

    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4',
      compress: true
    });

    if (autoPrint) pdf.autoPrint();

    const pagesPerCopy = pageCanvases.length;

    const renderCopyPages = (copyLabel: string, isFirstCopy: boolean) => {
      for (let p = 0; p < pagesPerCopy; p++) {
        if (!isFirstCopy || p > 0) pdf.addPage('a4', 'portrait');
        
        const canvasData = pageCanvases[p].toDataURL('image/jpeg', 0.95);
        pdf.addImage(canvasData, 'JPEG', 0, 0, 210, 297, undefined, 'FAST');
        
        pdf.setFontSize(8);
        pdf.setFont('helvetica', 'bold');
        pdf.setTextColor(30, 30, 30);
        const pageText = pagesPerCopy > 1 
          ? `(${copyLabel} - PAGE ${p + 1} OF ${pagesPerCopy})`
          : `(${copyLabel})`;
        pdf.text(pageText, 200, 7, { align: 'right' });
      }
    };

    renderCopyPages('ORIGINAL FOR RECIPIENT', true);
    renderCopyPages('DUPLICATE FOR TRANSPORTER', false);

    const blob = pdf.output('blob');
    const url = URL.createObjectURL(blob);
    return { blob, url };
  };

  const handlePrintNative = async () => {
    if (isProcessing) return;
    setIsProcessing(true);
    try {
      const { url } = await generateDualPagePDF(false);
      const printWindow = window.open(url, '_blank');
      if (!printWindow) {
        const { url: autoPrintUrl } = await generateDualPagePDF(true);
        window.open(autoPrintUrl, '_blank');
        return;
      }
      printWindow.onload = () => {
        setTimeout(() => { try { printWindow.print(); } catch {} }, 500);
      };
    } catch (err) {
      console.error(err);
      alert('Failed to prepare print. Please try again.');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleDownloadAndSave = async () => {
    if (isProcessing) return;
    setIsProcessing(true);
    setSaveStatus('Generating...');
    try {
      const { blob, url } = await generateDualPagePDF(false);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${invoice.invoiceNo}_Set.pdf`;
      link.click();
      
      if (storageHandle) {
        const folder = await getStructuredFolder(storageHandle, new Date(invoice.invoiceDate));
        await saveFileToFolder(folder, `${invoice.invoiceNo}_Set.pdf`, blob);
        const csvContent = getCSVContent([invoice]);
        const csvBlob = new Blob([csvContent], { type: 'text/csv' });
        await saveFileToFolder(folder, `${invoice.invoiceNo}_Report.csv`, csvBlob);
        setSaveStatus('Saved ✅');
        setTimeout(() => setSaveStatus(''), 3000);
      }
      setTimeout(() => URL.revokeObjectURL(url), 100);
    } catch (error) {
      console.error(error);
      alert('Failed to save.');
    } finally {
      setIsProcessing(false);
    }
  };

  const [isSharing, setIsSharing] = useState(false);

  const handleShareInvoice = async () => {
    if (isProcessing || isSharing) return;
    setIsSharing(true);
    try {
      const { blob } = await generateDualPagePDF(false);
      const fileName = `Invoice_${invoice.invoiceNo}.pdf`;
      const file = new File([blob], fileName, { type: 'application/pdf' });
      const shareText = `Invoice #${invoice.invoiceNo} for ${invoice.customerName} - Total: ${formatCurrency(invoice.grandTotal)}`;

      if (navigator.share && navigator.canShare && navigator.canShare({ files: [file] })) {
        await navigator.share({
          title: `Invoice #${invoice.invoiceNo}`,
          text: shareText,
          files: [file]
        });
        toastEvents.success('Invoice shared successfully!');
      } else if (navigator.share) {
        await navigator.share({
          title: `Invoice #${invoice.invoiceNo}`,
          text: shareText,
          url: window.location.href
        });
        toastEvents.success('Invoice link shared successfully!');
      } else {
        // Fallback: Copy summary link / text and open WhatsApp or download PDF
        const encodedText = encodeURIComponent(`${shareText}\n${window.location.href}`);
        window.open(`https://api.whatsapp.com/send?text=${encodedText}`, '_blank');
        toastEvents.info('Opened WhatsApp share option.');
      }
    } catch (err: any) {
      if (err?.name !== 'AbortError') {
        console.error('Share notice:', err);
        toastEvents.info('Share canceled or not supported natively.');
      }
    } finally {
      setIsSharing(false);
    }
  };

  useImperativeHandle(ref, () => ({ printToSystem: handlePrintNative }));

  const isChallan = invoice.billType === 'delivery_challan';
  const cgstPct = invoice.cgstRate || 0;
  const sgstPct = invoice.sgstRate || 0;
  const igstPct = invoice.igstRate || 0;

  const computedGrandTotal = Number((invoice.subtotal + invoice.cgst + invoice.sgst + invoice.igst + invoice.roundOff).toFixed(2));
  const displayTotalInWords = numberToWords(computedGrandTotal);

  return (
    <div className="flex flex-col items-center w-full overflow-x-auto py-4">
      {/* Single Consolidated Toolbar */}
      <div className="flex flex-col items-center gap-2 mb-6 no-print">
        <div className="flex flex-wrap items-center justify-center gap-3">
          <button
            onClick={handlePrintNative}
            disabled={isProcessing}
            className={`${isProcessing ? 'bg-gray-400 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-700 active:scale-95'} text-white px-6 py-2.5 rounded-lg transition-all font-bold flex items-center gap-2 shadow-md uppercase text-xs tracking-wider`}
          >
            <Printer size={16} />
            <span>{isProcessing ? 'Preparing...' : 'Print Invoice'}</span>
          </button>

          <button
            onClick={handleShareInvoice}
            disabled={isProcessing || isSharing}
            className={`${(isProcessing || isSharing) ? 'bg-gray-400 cursor-not-allowed' : 'bg-emerald-600 hover:bg-emerald-700 active:scale-95'} text-white px-6 py-2.5 rounded-lg transition-all font-bold flex items-center gap-2 shadow-md uppercase text-xs tracking-wider`}
          >
            <Share2 size={16} />
            <span>{isSharing ? 'Sharing...' : 'Share Invoice'}</span>
          </button>

          <button 
            onClick={handleDownloadAndSave} 
            disabled={isProcessing} 
            className={`${isProcessing ? 'bg-gray-400 cursor-not-allowed' : 'bg-slate-700 hover:bg-slate-800 active:scale-95'} text-white px-6 py-2.5 rounded-lg transition-all font-bold flex items-center gap-2 shadow-md uppercase text-xs tracking-wider`}
          >
            <Download size={16} />
            <span>Download PDF</span>
          </button>

          <button 
            onClick={() => exportInvoicesToCSV([invoice])} 
            className="bg-slate-100 hover:bg-slate-200 text-slate-800 border border-slate-300 px-5 py-2.5 rounded-lg transition-all font-bold flex items-center gap-2 shadow-sm active:scale-95 uppercase text-xs tracking-wider"
          >
            <FileText size={16} />
            <span>CSV</span>
          </button>
        </div>
        {saveStatus && <div className="text-xs font-black text-indigo-600 uppercase tracking-widest animate-bounce">{saveStatus}</div>}
      </div>

      {/* Pages Container - Explicit Portrait A4 Dimensions */}
      <div 
        ref={invoiceRef} 
        className="invoice-container flex flex-col items-center" 
        style={{ boxSizing: 'border-box', backgroundColor: '#f8fafc', fontFamily: 'Arial, sans-serif' }}
        data-theme="light"
      >
        {pages.map((page, pageIdx) => {
          const isFirstPage = pageIdx === 0;
          const isLastPage = pageIdx === pages.length - 1;
          const chunk = page.items;

          let startItemIdx = 0;
          for (let i = 0; i < pageIdx; i++) {
            startItemIdx += pages[i].items.length;
          }

          return (
            <div
              key={pageIdx}
              className="invoice-page border border-black p-5 flex flex-col justify-between relative bg-white text-black shrink-0"
              style={{
                boxSizing: 'border-box',
                width: '794px',
                minWidth: '794px',
                maxWidth: '794px',
                height: '1123px',
                minHeight: '1123px',
                maxHeight: '1123px',
                overflow: 'hidden',
                marginBottom: isLastPage ? '0' : '24px',
                boxShadow: '0 4px 15px rgba(0,0,0,0.08)',
                backgroundColor: '#ffffff',
                color: '#000000'
              }}
              data-theme="light"
            >
              {/* --- HEADER (Page 1 vs Continuation) --- */}
              {isFirstPage ? (
                <>
                  <div className="text-center mb-2 shrink-0">
                    <h1 className="text-2xl font-bold uppercase inline-block px-4 py-1 text-black">
                      {isChallan ? 'Delivery Challan' : 'Tax Invoice'}
                    </h1>
                  </div>

                  <div className="flex justify-between mb-2 border-b-2 border-gray-800 pb-2 shrink-0">
                    <div className="w-3/5">
                      <h2 className="text-2xl font-black text-blue-900 mb-1 leading-none">{companyDetails.name}</h2>
                      <p className="leading-tight whitespace-pre-line mb-4 font-bold text-gray-900 text-[14px]">{companyDetails.address}</p>
                      <div className="font-bold text-gray-900 text-[14px]">
                        <p><span className="font-bold uppercase">GSTIN:</span> {companyDetails.gstin}</p>
                        <p><span className="font-bold uppercase">Mobile:</span> {companyDetails.mobile}</p>
                      </div>
                    </div>
                    <div className="w-2/5 flex flex-col items-end">
                      <div className="border border-gray-800 w-full max-w-[220px] overflow-hidden rounded-sm bg-white">
                        {([
                          ...(isChallan ? [] : [['Invoice No', invoice.invoiceNo] as [string, string]]),
                          ['Date', new Date(invoice.invoiceDate).toLocaleDateString('en-IN')],
                          ['State', invoice.customerState || 'Tamil Nadu'],
                          ['State Code', invoice.customerStateCode || '33'],
                          ['Vehicle No', invoice.vehicleNo || 'N/A']
                        ] as [string, string][]).map(([label, value]) => (
                          <div key={label} className="flex border-b border-gray-800 last:border-0">
                            <div className="w-[95px] border-r border-gray-800 p-1.5 text-[11px] font-bold uppercase flex items-center bg-gray-50 text-black shrink-0">{label}</div>
                            <div className="flex-1 p-1.5 text-[13px] font-black text-gray-900 flex items-center uppercase overflow-hidden truncate">{value}</div>
                          </div>
                        ))}
                      </div>
                    </div>
                  </div>

                  <div className="mb-2.5 flex gap-2.5 shrink-0">
                    {[ { label: 'RECEIVER (BILL TO):', content: invoice }, { label: 'CONSIGNEE (SHIP TO):', content: invoice } ].map((box, i) => {
                      const custAddr = box.content.customerAddress || box.content.address || box.content.customer?.address || '';
                      const custGstin = box.content.customerGSTIN || box.content.gstin || box.content.customerTaxId || box.content.taxId || box.content.customer?.taxId || box.content.customer?.gstin || 'N/A';
                      return (
                        <div key={i} className="w-1/2 p-3 border border-gray-400 rounded-sm bg-white min-h-[140px] flex flex-col justify-between">
                          <div>
                            <h3 className="font-extrabold border-b border-gray-200 mb-1 pb-0.5 text-[10.5px] uppercase tracking-wide text-gray-600">{box.label}</h3>
                            <p className="font-black text-[15px] mb-1 uppercase text-gray-900 leading-tight break-words">{box.content.customerName}</p>
                            <p className="text-[11.5px] whitespace-pre-line leading-normal font-bold text-gray-900 uppercase mb-1.5">{custAddr}</p>
                          </div>
                          <div className="text-[12px] font-bold uppercase text-gray-900 pt-1 border-t border-gray-200 mt-auto">
                            <p>GSTIN: <span className="font-black text-black">{custGstin}</span></p>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </>
              ) : (
                <div className="flex justify-between items-center mb-3 border-b-2 border-gray-800 pb-2 shrink-0">
                  <div>
                    <h2 className="text-xl font-black text-blue-900 leading-none">{companyDetails.name}</h2>
                    <p className="text-[11px] font-bold text-gray-700 uppercase mt-0.5">{isChallan ? 'Delivery Challan' : 'Tax Invoice'} (Continuation)</p>
                  </div>
                  <div className="text-right">
                    {!isChallan && <p className="text-[13px] font-black text-gray-900 uppercase">Invoice No: {invoice.invoiceNo}</p>}
                    <p className="text-[11px] font-bold text-gray-700 uppercase">Date: {new Date(invoice.invoiceDate).toLocaleDateString('en-IN')}</p>
                    <p className="text-[11px] font-black text-blue-800 uppercase mt-0.5">Page {pageIdx + 1} of {pages.length}</p>
                  </div>
                </div>
              )}

              {/* --- MAIN ITEM TABLE (Extends to fill remaining vertical height) --- */}
              <div className="border-2 border-gray-800 mb-2 bg-white w-full overflow-hidden flex-1 flex flex-col justify-between">
                <table className="w-full border-collapse text-sm table-fixed h-full">
                  <thead className="bg-white shrink-0">
                    <tr className="border-b-[4px] border-double border-gray-800 h-10">
                      <th style={{ width: '5%' }} className="border-r border-gray-800 p-1 text-center font-black uppercase text-[11px] text-black">S.No</th>
                      <th style={{ width: '32%' }} className="border-r border-gray-800 p-1 text-center font-black uppercase text-[11px] text-black">Description of Goods</th>
                      <th style={{ width: '8%' }} className="border-r border-gray-800 p-1 text-center font-black uppercase text-[11px] text-black">DC NO</th>
                      <th style={{ width: '10%' }} className="border-r border-gray-800 p-1 text-center font-black uppercase text-[11px] text-black">HSN</th>
                      <th style={{ width: '12%' }} className="border-r border-gray-800 p-1 text-center font-black uppercase text-[11px] text-black">Qty (Ltrs)</th>
                      <th style={{ width: '15%' }} className="border-r border-gray-800 p-1 text-center font-black uppercase text-[11px] text-black">Rate</th>
                      <th style={{ width: '18%' }} className="p-1 text-center font-black uppercase text-[11px] text-black">Amount</th>
                    </tr>
                  </thead>
                  <tbody className="bg-white flex-1">
                    {/* --- BROUGHT FORWARD (B/F) ROW AT TOP OF CONTINUATION PAGES --- */}
                    {!isFirstPage && (
                      <tr className="border-b-2 border-gray-800 bg-blue-50 font-black">
                        <td style={{ width: '5%' }} className="border-r border-gray-800 py-2.5 px-1 text-center text-[12px] italic text-blue-900">—</td>
                        <td style={{ width: '32%' }} className="border-r border-gray-800 py-2.5 px-2 text-left text-[12px] uppercase font-black text-blue-900 tracking-wide">
                          BROUGHT FORWARD (B/F FROM PAGE {pageIdx})
                        </td>
                        <td style={{ width: '8%' }} className="border-r border-gray-800 py-2.5 px-1 text-center text-[12px] italic text-blue-900">—</td>
                        <td style={{ width: '10%' }} className="border-r border-gray-800 py-2.5 px-1 text-center text-[12px] italic text-blue-900">—</td>
                        <td style={{ width: '12%' }} className="border-r border-gray-800 py-2.5 px-1 text-center text-[12.5px] font-black text-blue-900">
                          {Number.isInteger(page.previousPageTotalQty) ? page.previousPageTotalQty : page.previousPageTotalQty.toFixed(2)}
                        </td>
                        <td style={{ width: '15%' }} className="border-r border-gray-800 py-2.5 px-1 text-center text-[12px] italic text-blue-900">—</td>
                        <td style={{ width: '18%' }} className="py-2.5 px-2 text-right pr-3 text-[12.5px] font-black text-blue-900">
                          {formatCurrency(page.previousPageTotalAmount)}
                        </td>
                      </tr>
                    )}

                    {/* --- ITEM ROWS --- */}
                    {chunk.map((item, idx) => (
                      <tr key={idx} className="border-b border-gray-300">
                        <td style={{ width: '5%' }} className="border-r border-gray-800 py-2.5 px-1 text-center font-black text-[12px] text-black">{startItemIdx + idx + 1}</td>
                        <td style={{ width: '32%' }} className="border-r border-gray-800 py-2.5 px-2 text-left font-black text-[11.5px] uppercase whitespace-normal break-words leading-snug text-black">
                          {item.productName}
                        </td>
                        <td style={{ width: '8%' }} className="border-r border-gray-800 py-2.5 px-1 text-center font-black text-[11.5px] text-black">{item.dcNo || '-'}</td>
                        <td style={{ width: '10%' }} className="border-r border-gray-800 py-2.5 px-1 text-center font-black text-[11.5px] text-black">{item.hsnCode}</td>
                        <td style={{ width: '12%' }} className="border-r border-gray-800 py-2.5 px-1 text-center font-black text-[12px] text-black">
                          {Number.isInteger(item.quantity) ? item.quantity : item.quantity.toFixed(2)}
                        </td>
                        <td style={{ width: '15%' }} className="border-r border-gray-800 py-2.5 px-1 text-center font-black text-[11.5px] text-black">{item.rate.toFixed(2)}</td>
                        <td style={{ width: '18%' }} className="py-2.5 px-2 text-right pr-3 font-black font-mono text-[12.5px] whitespace-nowrap overflow-visible text-black">{formatCurrency(item.amount)}</td>
                      </tr>
                    ))}

                    {/* --- EMPTY FILLER ROW TO EXTEND GRID LINES DOWN TO FOOTER --- */}
                    <tr className="h-full">
                      <td style={{ width: '5%' }} className="border-r border-gray-800"></td>
                      <td style={{ width: '32%' }} className="border-r border-gray-800"></td>
                      <td style={{ width: '8%' }} className="border-r border-gray-800"></td>
                      <td style={{ width: '10%' }} className="border-r border-gray-800"></td>
                      <td style={{ width: '12%' }} className="border-r border-gray-800"></td>
                      <td style={{ width: '15%' }} className="border-r border-gray-800"></td>
                      <td style={{ width: '18%' }}></td>
                    </tr>
                  </tbody>
                </table>
              </div>

              {/* --- FOOTER (ANCHORED AT BOTTOM OF PAGE WITH mt-auto) --- */}
              {page.includeFooter ? (
                <div className="shrink-0 mt-auto w-full">
                  <div className="flex border-2 border-gray-800 rounded-sm overflow-hidden mb-2">
                    <div className="w-[52%] flex flex-col border-r-2 border-gray-800">
                      <div className="p-2.5 border-b-2 border-gray-800 bg-white">
                        <p className="text-[10.5px] font-bold uppercase mb-0.5 text-gray-600">Total Amount in Words:</p>
                        <p className="text-[12px] font-black leading-tight uppercase tracking-wide text-gray-900">{displayTotalInWords}</p>
                      </div>
                      <div className="flex flex-grow bg-white min-h-[100px]">
                        <div className="w-3/5 p-2.5 border-r-2 border-gray-800 flex flex-col">
                          <h4 className="text-[10.5px] font-black uppercase text-black mb-1.5 border-b border-gray-300 pb-1">Bank Details</h4>
                          <table className="w-full border-collapse border border-gray-800 text-[10.5px] table-fixed">
                            <tbody>
                              <tr className="border-b border-gray-200">
                                <td className="w-24 p-1 bg-gray-50 border-r border-gray-200 font-bold uppercase text-black">Bank Name</td>
                                <td className="p-1 uppercase font-black truncate text-black">{companyDetails.bankName}</td>
                              </tr>
                              <tr className="border-b border-gray-200">
                                <td className="p-1 bg-gray-50 border-r border-gray-200 font-bold uppercase text-black">Acc No</td>
                                <td className="p-1 uppercase font-black truncate text-black">{companyDetails.accountNo}</td>
                              </tr>
                              <tr className="border-b border-gray-200">
                                <td className="p-1 bg-gray-50 border-r border-gray-200 font-bold uppercase text-black">IFSC Code</td>
                                <td className="p-1 uppercase font-black truncate text-black">{companyDetails.ifscCode}</td>
                              </tr>
                              <tr>
                                <td className="p-1 bg-gray-50 border-r border-gray-200 font-bold uppercase text-black">Branch</td>
                                <td className="p-1 uppercase font-black truncate text-black">{companyDetails.branch}</td>
                              </tr>
                            </tbody>
                          </table>
                        </div>
                        <div className="w-2/5 p-2.5 flex flex-col justify-between">
                          <div>
                            <p className="text-[10px] font-bold text-gray-600 uppercase mb-1">Declaration:</p>
                            <p className="text-[10.5px] leading-tight font-bold italic text-gray-800 uppercase">{invoice.notes}</p>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div className="w-[48%] bg-white flex flex-col justify-between">
                      <table className="w-full h-full border-collapse">
                        <tbody className="text-[11.5px] font-bold">
                          <tr className="border-b border-gray-300">
                            <td className="px-2.5 py-1.5 border-r border-gray-300 uppercase whitespace-nowrap text-[11px] text-gray-800 w-[55%]">Subtotal</td>
                            <td className="px-2.5 py-1.5 text-right font-black font-mono whitespace-nowrap text-[13px] text-black w-[45%] pr-3">{formatCurrency(invoice.subtotal)}</td>
                          </tr>
                          {!isChallan && (
                            <>
                              {(invoice.taxLines && invoice.taxLines.length > 0) ? (
                                invoice.taxLines.map((tax, idx) => (
                                  <tr key={idx} className="border-b border-gray-300">
                                    <td className="px-2.5 py-1.5 border-r border-gray-300 uppercase whitespace-nowrap text-[11px] text-gray-800 w-[55%]">
                                      {tax.name} ({tax.rate}%)
                                    </td>
                                    <td className="px-2.5 py-1.5 text-right font-black font-mono whitespace-nowrap text-[13px] text-black w-[45%] pr-3">
                                      {formatCurrency(tax.amount)}
                                    </td>
                                  </tr>
                                ))
                              ) : (
                                <>
                                  {cgstPct > 0 && (
                                    <tr className="border-b border-gray-300">
                                      <td className="px-2.5 py-1.5 border-r border-gray-300 uppercase whitespace-nowrap text-[11px] text-gray-800 w-[55%]">CGST ({cgstPct}%)</td>
                                      <td className="px-2.5 py-1.5 text-right font-black font-mono whitespace-nowrap text-[13px] text-black w-[45%] pr-3">{formatCurrency(invoice.cgst)}</td>
                                    </tr>
                                  )}
                                  {sgstPct > 0 && (
                                    <tr className="border-b border-gray-300">
                                      <td className="px-2.5 py-1.5 border-r border-gray-300 uppercase whitespace-nowrap text-[11px] text-gray-800 w-[55%]">SGST ({sgstPct}%)</td>
                                      <td className="px-2.5 py-1.5 text-right font-black font-mono whitespace-nowrap text-[13px] text-black w-[45%] pr-3">{formatCurrency(invoice.sgst)}</td>
                                    </tr>
                                  )}
                                  {igstPct > 0 && (
                                    <tr className="border-b border-gray-300">
                                      <td className="px-2.5 py-1.5 border-r border-gray-300 uppercase whitespace-nowrap text-[11px] text-gray-800 w-[55%]">IGST ({igstPct}%)</td>
                                      <td className="px-2.5 py-1.5 text-right font-black font-mono whitespace-nowrap text-[13px] text-black w-[45%] pr-3">{formatCurrency(invoice.igst)}</td>
                                    </tr>
                                  )}
                                </>
                              )}
                            </>
                          )}
                          <tr className="border-b border-gray-300">
                            <td className="px-2.5 py-1.5 border-r border-gray-300 uppercase whitespace-nowrap text-[11px] text-gray-800 w-[55%]">Round Off</td>
                            <td className="px-2.5 py-1.5 text-right font-black font-mono whitespace-nowrap text-[13px] text-black w-[45%] pr-3">{formatCurrency(invoice.roundOff)}</td>
                          </tr>
                          <tr className="bg-gray-50">
                            <td className="px-2.5 py-2 text-[12px] font-black text-black border-r border-gray-300 uppercase whitespace-nowrap w-[55%]">TOTAL</td>
                            <td className="px-2.5 py-2 text-[15px] font-black text-black text-right font-mono whitespace-nowrap tracking-tight w-[45%] pr-3">{formatCurrency(invoice.grandTotal)}</td>
                          </tr>
                        </tbody>
                      </table>
                    </div>
                  </div>

                  <div className="flex justify-between items-end mt-3">
                    <div className="text-center w-52">
                      <div className="border-t border-black pt-1.5">
                        <p className="text-[11px] font-black uppercase text-gray-900">Customer's Signature</p>
                      </div>
                    </div>
                    <div className="text-center w-72">
                      <p className="text-[12px] font-black uppercase text-gray-900 mb-10">For {companyDetails.name}</p>
                      <div className="border-t border-black pt-1.5">
                        <p className="text-[11px] font-black uppercase text-gray-900">Authorized Signatory</p>
                      </div>
                    </div>
                  </div>

                  <div className="mt-3 text-[10px] text-gray-500 font-bold uppercase border-t border-black pt-1.5 flex justify-between items-center px-1">
                    <span>THIS IS A COMPUTER GENERATED INVOICE FOR SENTHUR CHEMICAL, SUBJECT TO ERODE JURISDICTION</span>
                    {invoice.invoiceTime && (
                      <span className="text-gray-900 font-black ml-2 whitespace-nowrap">BILLING TIME: {formatTime12Hour(invoice.invoiceTime)}</span>
                    )}
                  </div>
                </div>
              ) : (
                <div className="shrink-0 mt-auto text-right text-[11px] font-black uppercase text-gray-700 py-1.5 border-t border-gray-800 bg-gray-50 px-2 flex justify-between items-center w-full">
                  <span className="italic text-gray-500">Continued on Page {pageIdx + 2}...</span>
                  <span>Continued on Next Page →</span>
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}));

export default PrintInvoice;
