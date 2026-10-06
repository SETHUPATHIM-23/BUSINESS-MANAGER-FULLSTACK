
import React, { useRef, useState, useImperativeHandle, forwardRef, memo } from 'react';
import { Invoice, COMPANY_DETAILS } from '../types';
import { formatCurrency, formatTime12Hour, exportInvoicesToCSV, getStructuredFolder, saveFileToFolder, getCSVContent } from '../utils';
import { jsPDF } from 'jspdf';
import html2canvas from 'html2canvas';

interface Props {
  invoice: Invoice;
  storageHandle?: any;
  companyDetails: typeof COMPANY_DETAILS;
}

export interface PrintInvoiceHandle {
  printToSystem: () => Promise<void>;
}

const PrintInvoice = memo(forwardRef<PrintInvoiceHandle, Props>(({ invoice, storageHandle, companyDetails }, ref) => {
  const invoiceRef = useRef<HTMLDivElement>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const [saveStatus, setSaveStatus] = useState<string>('');

  const generateDualPagePDF = async (autoPrint: boolean = false): Promise<{ blob: Blob; url: string }> => {
    if (!invoiceRef.current) throw new Error("Ref not found");
    const element = invoiceRef.current;
    
    const scaleFactor = 2.5; 

    const canvas = await html2canvas(element, {
      scale: scaleFactor, 
      useCORS: true,
      logging: false,
      backgroundColor: '#ffffff',
      width: 793.7, 
      height: 1122.5,
      onclone: (clonedDoc) => {
        const labelEl = clonedDoc.querySelector('#copy-label-slot');
        if (labelEl) (labelEl as HTMLElement).style.display = 'none';
        const container = clonedDoc.querySelector('.invoice-container') as HTMLElement;
        if (container) {
          container.style.boxShadow = 'none';
          container.style.margin = '0';
        }
      }
    });

    const imgData = canvas.toDataURL('image/jpeg', 0.95);
    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4',
      compress: true
    });

    if (autoPrint) {
      pdf.autoPrint();
    }

    pdf.addImage(imgData, 'JPEG', 0, 0, 210, 297, undefined, 'FAST');
    pdf.setFontSize(8);
    pdf.setFont('helvetica', 'bold');
    pdf.text('(ORIGINAL FOR RECIPIENT)', 200, 8, { align: 'right' });

    pdf.addPage();
    pdf.addImage(imgData, 'JPEG', 0, 0, 210, 297, undefined, 'FAST');
    pdf.text('(DUPLICATE FOR TRANSPORTER)', 200, 8, { align: 'right' });

    const blob = pdf.output('blob');
    const url = URL.createObjectURL(blob);
    return { blob, url };
  };

  const handlePrintDirect = async () => {
    if (isProcessing) return;
    setIsProcessing(true);
    try {
      const { url } = await generateDualPagePDF(true);
      
      const iframe = document.createElement('iframe');
      // We keep the iframe in the DOM indefinitely to prevent the OS print dialog from closing.
      // Using opacity instead of visibility/display hidden for maximum browser compatibility.
      iframe.style.position = 'fixed';
      iframe.style.right = '0';
      iframe.style.bottom = '0';
      iframe.style.width = '1px';
      iframe.style.height = '1px';
      iframe.style.border = 'none';
      iframe.style.opacity = '0.01';
      iframe.style.pointerEvents = 'none';
      iframe.src = url;
      
      document.body.appendChild(iframe);
      
      iframe.onload = () => {
        // Sufficient delay for the browser's PDF engine to spool the document.
        setTimeout(() => {
          if (iframe.contentWindow) {
            iframe.contentWindow.focus();
            try {
              iframe.contentWindow.print();
            } catch (e) {
              const win = window.open(url, '_blank');
              if (win) win.print();
            }
          }
          // Note: We deliberately DO NOT remove the iframe or revoke the URL here.
          // This keeps the print process stable until the user closes the dialog or the app.
        }, 1200);
      };
    } catch (err) {
      console.error(err);
      alert("Failed to prepare print. Please try again.");
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

  useImperativeHandle(ref, () => ({
    printToSystem: handlePrintDirect
  }));

  const isChallan = invoice.billType === 'delivery_challan';
  const displayGstPct = invoice.gstPercentage !== undefined ? invoice.gstPercentage : (companyDetails?.gstPercentage !== undefined ? companyDetails.gstPercentage : 18);
  const cgstPct = displayGstPct / 2;
  const sgstPct = displayGstPct / 2;

  return (
    <div className="flex flex-col items-center w-full">
      <div className="flex flex-col items-center gap-2 mb-6 no-print">
        <div className="flex gap-4">
          <button 
            onClick={handleDownloadAndSave} 
            disabled={isProcessing} 
            className={`${isProcessing ? 'bg-gray-400 cursor-not-allowed' : 'bg-indigo-700 hover:bg-indigo-800 shadow-lg active:scale-95'} text-white px-8 py-3 rounded-lg transition-all font-bold flex items-center gap-2 uppercase text-xs tracking-widest`}
          >
            {isProcessing ? 'Processing...' : 'Download & Auto-Save'}
          </button>
          <button 
            onClick={() => exportInvoicesToCSV([invoice])} 
            className="bg-emerald-600 hover:bg-emerald-700 text-white px-8 py-3 rounded-lg transition-all font-bold flex items-center gap-2 shadow-lg active:scale-95 uppercase text-xs tracking-widest"
          >
            Export CSV
          </button>
        </div>
        {saveStatus && <div className="text-xs font-black text-indigo-600 uppercase tracking-widest animate-bounce">{saveStatus}</div>}
      </div>

      <div 
        ref={invoiceRef} 
        className="invoice-container flex flex-col p-[8mm] relative shadow-none" 
        style={{ boxSizing: 'border-box', backgroundColor: 'white', fontFamily: 'Arial, sans-serif', width: '210mm', height: '297mm', overflow: 'hidden' }}
      >
        <div id="copy-label-slot" className="absolute top-[4mm] right-[8mm] text-[11px] font-bold text-gray-800 uppercase" style={{ display: 'none' }}></div>

        <div className="border border-black h-full p-6 flex flex-col relative" style={{ boxSizing: 'border-box' }}>
          <div className="text-center mb-2 shrink-0">
            <h1 className="text-2xl font-bold uppercase inline-block px-4 py-1">
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
              <div className="border border-gray-800 w-full max-w-[220px] overflow-hidden rounded-sm">
                {[
                  ['Invoice No', invoice.invoiceNo],
                  ['Date', new Date(invoice.invoiceDate).toLocaleDateString('en-IN')],
                  ['State', 'Tamil Nadu'],
                  ['State Code', '33'],
                  ['Vehicle No', invoice.vehicleNo || 'N/A']
                ].map(([label, value]) => (
                  <div key={label} className="flex border-b border-gray-800 last:border-0">
                    <div className="w-[95px] border-r border-gray-800 p-1.5 text-[11px] font-bold uppercase flex items-center bg-gray-50 shrink-0">{label}</div>
                    <div className="flex-1 p-1.5 text-[13px] font-black text-gray-900 flex items-center uppercase overflow-hidden truncate">{value}</div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          <div className="mb-2 flex gap-2 shrink-0">
            {[ { label: 'RECEIVER (BILL TO):', content: invoice }, { label: 'CONSIGNEE (SHIP TO):', content: invoice } ].map((box, i) => (
              <div key={i} className="w-1/2 p-2 border border-gray-400 rounded-sm min-h-[110px] flex flex-col">
                <h3 className="font-bold border-b border-gray-300 mb-1 pb-0.5 text-[11px] uppercase text-gray-600">{box.label}</h3>
                <p className="font-black text-base mb-1 uppercase text-gray-900 leading-snug break-words">{box.content.customerName}</p>
                <p className="text-[12px] whitespace-pre-line leading-tight mb-1 font-bold text-gray-900 uppercase flex-grow">{box.content.customerAddress}</p>
                <div className="text-[12px] font-bold uppercase text-gray-900 mt-auto pt-1 border-t border-gray-100">
                  <p>GSTIN: <span className="font-black">{box.content.customerGSTIN || 'N/A'}</span></p>
                </div>
              </div>
            ))}
          </div>

          <div className="border-2 border-gray-800 overflow-hidden mb-2 flex-grow bg-white flex flex-col">
            <table className="w-full border-collapse text-sm table-fixed flex-grow h-full">
              <thead className="bg-white">
                <tr className="border-b-[4px] border-double border-gray-800 h-10">
                  <th style={{ width: '6%' }} className="border-r border-gray-800 p-1 text-center font-black uppercase text-[11px]">S.No</th>
                  <th style={{ width: '46%' }} className="border-r border-gray-800 p-1 text-center font-black uppercase text-[11px]">Description of Goods</th>
                  <th style={{ width: '12%' }} className="border-r border-gray-800 p-1 text-center font-black uppercase text-[11px]">HSN</th>
                  <th style={{ width: '12%' }} className="border-r border-gray-800 p-1 text-center font-black uppercase text-[11px]">Qty (Ltrs)</th>
                  <th style={{ width: '12%' }} className="border-r border-gray-800 p-1 text-center font-black uppercase text-[11px]">Rate</th>
                  <th style={{ width: '12%' }} className="p-1 text-center font-black uppercase text-[11px]">Amount</th>
                </tr>
              </thead>
              <tbody className="h-full">
                {invoice.items.map((item, idx) => (
                  <tr key={idx} className="border-b border-gray-300">
                    <td className="border-r border-gray-800 py-2 px-1 text-center font-black text-[13px]">{idx + 1}</td>
                    <td className="border-r border-gray-800 py-2 px-2 text-left font-black text-[12px] uppercase whitespace-normal break-words leading-snug">
                      {item.productName}
                    </td>
                    <td className="border-r border-gray-800 py-2 px-1 text-center font-black text-[12px]">{item.hsnCode}</td>
                    <td className="border-r border-gray-800 py-2 px-1 text-center font-black text-[13px]">
                      {Number.isInteger(item.quantity) ? item.quantity : item.quantity.toFixed(2)}
                    </td>
                    <td className="border-r border-gray-800 py-2 px-1 text-center font-black text-[12px]">{item.rate.toFixed(2)}</td>
                    <td className="py-2 px-1 text-right pr-2 font-black text-[13px] whitespace-nowrap">{formatCurrency(item.amount)}</td>
                  </tr>
                ))}
                <tr className="h-full">
                  <td className="border-r border-gray-800"></td>
                  <td className="border-r border-gray-800"></td>
                  <td className="border-r border-gray-800"></td>
                  <td className="border-r border-gray-800"></td>
                  <td className="border-r border-gray-800"></td>
                  <td></td>
                </tr>
              </tbody>
            </table>
          </div>

          <div className="shrink-0 mt-auto">
            <div className="flex border-2 border-gray-800 rounded-sm overflow-hidden mb-2">
              <div className="w-[55%] flex flex-col border-r-2 border-gray-800">
                <div className="p-2.5 border-b-2 border-gray-800 bg-white">
                  <p className="text-[11px] font-bold uppercase mb-0.5 text-gray-600">Total Amount in Words:</p>
                  <p className="text-[13px] font-black leading-tight uppercase tracking-wide text-gray-900">{invoice.totalInWords}</p>
                </div>
                <div className="flex flex-grow bg-white min-h-[100px]">
                  <div className="w-3/5 p-2.5 border-r-2 border-gray-800 flex flex-col">
                    <h4 className="text-[11px] font-black uppercase text-black mb-1.5 border-b border-gray-300 pb-1">Bank Details</h4>
                    <table className="w-full border-collapse border border-gray-800 text-[11px] table-fixed">
                        <tbody>
                            <tr className="border-b border-gray-200">
                                <td className="w-24 p-1 bg-gray-50 border-r border-gray-200 font-bold uppercase">Bank Name</td>
                                <td className="p-1 uppercase font-black truncate">{companyDetails.bankName}</td>
                            </tr>
                            <tr className="border-b border-gray-200">
                                <td className="p-1 bg-gray-50 border-r border-gray-200 font-bold uppercase">Acc No</td>
                                <td className="p-1 uppercase font-black truncate">{companyDetails.accountNo}</td>
                            </tr>
                            <tr className="border-b border-gray-200">
                                <td className="p-1 bg-gray-50 border-r border-gray-200 font-bold uppercase">IFSC Code</td>
                                <td className="p-1 uppercase font-black truncate">{companyDetails.ifscCode}</td>
                            </tr>
                            <tr>
                                <td className="p-1 bg-gray-50 border-r border-gray-200 font-bold uppercase">Branch</td>
                                <td className="p-1 uppercase font-black truncate">{companyDetails.branch}</td>
                            </tr>
                        </tbody>
                    </table>
                  </div>
                  <div className="w-2/5 p-2.5 flex flex-col justify-between">
                    <div>
                      <p className="text-[10px] font-bold text-gray-600 uppercase mb-1">Declaration:</p>
                      <p className="text-[11px] leading-tight font-bold italic text-gray-800 uppercase">{invoice.notes}</p>
                    </div>
                  </div>
                </div>
              </div>
              <div className="w-[45%] bg-white flex flex-col justify-between">
                <table className="w-full h-full border-collapse">
                  <tbody className="text-[12px] font-bold">
                    <tr className="border-b border-gray-300">
                      <td className="px-2.5 py-1.5 border-r border-gray-300 uppercase whitespace-nowrap text-[12px]">Subtotal</td>
                      <td className="px-2.5 py-1.5 text-right font-black whitespace-nowrap text-[14px]">{formatCurrency(invoice.subtotal)}</td>
                    </tr>
                    {!isChallan && (
                      <>
                        <tr className="border-b border-gray-300">
                          <td className="px-2.5 py-1.5 border-r border-gray-300 uppercase whitespace-nowrap text-[12px]">CGST ({cgstPct}%)</td>
                          <td className="px-2.5 py-1.5 text-right font-black whitespace-nowrap text-[14px]">{formatCurrency(invoice.cgst)}</td>
                        </tr>
                        <tr className="border-b border-gray-300">
                          <td className="px-2.5 py-1.5 border-r border-gray-300 uppercase whitespace-nowrap text-[12px]">SGST ({sgstPct}%)</td>
                          <td className="px-2.5 py-1.5 text-right font-black whitespace-nowrap text-[14px]">{formatCurrency(invoice.sgst)}</td>
                        </tr>
                      </>
                    )}
                    <tr className="border-b border-gray-300">
                      <td className="px-2.5 py-1.5 border-r border-gray-300 uppercase whitespace-nowrap text-[12px]">Round Off</td>
                      <td className="px-2.5 py-1.5 text-right font-black whitespace-nowrap text-[14px]">{formatCurrency(invoice.roundOff)}</td>
                    </tr>
                    <tr className="bg-gray-50">
                      <td className="px-2.5 py-2 text-[13px] font-black text-black border-r border-gray-300 uppercase whitespace-nowrap">TOTAL</td>
                      <td className="px-2.5 py-2 text-[16px] font-black text-black text-right whitespace-nowrap tracking-tight">{formatCurrency(invoice.grandTotal)}</td>
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
        </div>
      </div>
    </div>
  );
}));

export default PrintInvoice;
