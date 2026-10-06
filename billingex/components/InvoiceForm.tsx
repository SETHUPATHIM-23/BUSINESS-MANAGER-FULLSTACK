
import React, { useState, useEffect } from 'react';
import { db, Product } from '../db';
import { Customer, Invoice, InvoiceItem, COMPANY_DETAILS, BillType } from '../types';
import { numberToWords, formatCurrency } from '../utils';

interface Props {
  customers: Customer[];
  products: Product[];
  companyDetails: typeof COMPANY_DETAILS;
  onInvoiceCreated: (invoice: Invoice) => void;
}

const DEFAULT_DECLARATION = "WE DECLARE THAT THIS INVOICE SHOWS THE ACTUAL PRICE OF THE GOODS DESCRIBED AND THAT PARTICULARS ARE TRUE AND CORRECT";
const DEFAULT_VEHICLE = "";

const roundTo2 = (num: number) => Math.round((num + Number.EPSILON) * 100) / 100;

const InvoiceForm: React.FC<Props> = ({ customers, products, companyDetails, onInvoiceCreated }) => {
  const [selectedCustomerId, setSelectedCustomerId] = useState<number | string>('');
  
  // Product item inputs
  const [selectedProductId, setSelectedProductId] = useState<number | string>('');
  const [quantityStr, setQuantityStr] = useState<string>('');
  const [rateStr, setRateStr] = useState<string>('');
  
  // Multi-item list for the invoice draft
  const [addedItems, setAddedItems] = useState<InvoiceItem[]>([]);

  const [vehicleNo, setVehicleNo] = useState('');
  const [notes, setNotes] = useState(DEFAULT_DECLARATION);
  const getCurrentTime = () => {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    return `${hours}:${minutes}`;
  };

  const [invoiceNo, setInvoiceNo] = useState('');
  const [invoiceDate, setInvoiceDate] = useState(new Date().toISOString().split('T')[0]);
  const [invoiceTime, setInvoiceTime] = useState(getCurrentTime());
  const [billType, setBillType] = useState<BillType>('tax_exclusive');

  useEffect(() => {
    loadInvoiceNo();
  }, []);

  useEffect(() => {
    if (companyDetails.vehicles && companyDetails.vehicles.length > 0) {
      if (!vehicleNo) {
        setVehicleNo(companyDetails.vehicles[0].vehicleNo);
      }
    }
  }, [companyDetails]);

  useEffect(() => {
    // Default rate selection when product changes
    if (selectedProductId) {
      const prod = products.find(p => p.id === Number(selectedProductId));
      if (prod && prod.rates.length > 0) {
        setRateStr(prod.rates[0].toFixed(2));
      }
    }
  }, [selectedProductId, products]);

  const loadInvoiceNo = async () => {
    const nextNo = await db.getNextInvoiceNumber();
    setInvoiceNo(nextNo);
  };

  // Helper to construct effective items array including currently typed item if not added yet
  const getEffectiveItems = (): InvoiceItem[] => {
    const items = [...addedItems];
    const qty = parseFloat(quantityStr) || 0;
    const rate = parseFloat(rateStr) || 0;
    const prod = products.find(p => p.id === Number(selectedProductId));

    if (prod && qty > 0) {
      const totalGstRate = companyDetails.gstPercentage !== undefined ? companyDetails.gstPercentage : 18;
      const inclusiveDivisor = 1 + (totalGstRate / 100);
      let amt = roundTo2(qty * rate);
      if (billType === 'tax_inclusive') {
        amt = roundTo2((qty * rate) / inclusiveDivisor);
      }
      items.push({
        id: Date.now().toString(),
        productName: prod.name,
        hsnCode: prod.hsn,
        quantity: qty,
        rate: rate,
        amount: amt
      });
    }
    return items;
  };

  const handleAddItem = () => {
    const qty = parseFloat(quantityStr) || 0;
    const rate = parseFloat(rateStr) || 0;
    const prod = products.find(p => p.id === Number(selectedProductId));

    if (!prod) {
      alert('Please select a product first.');
      return;
    }
    if (qty <= 0) {
      alert('Please enter a valid quantity (Liters).');
      return;
    }

    const totalGstRate = companyDetails.gstPercentage !== undefined ? companyDetails.gstPercentage : 18;
    const inclusiveDivisor = 1 + (totalGstRate / 100);
    let amt = roundTo2(qty * rate);
    if (billType === 'tax_inclusive') {
      amt = roundTo2((qty * rate) / inclusiveDivisor);
    }

    const newItem: InvoiceItem = {
      id: Date.now().toString() + Math.random().toString().slice(2, 5),
      productName: prod.name,
      hsnCode: prod.hsn,
      quantity: qty,
      rate: rate,
      amount: amt
    };

    setAddedItems(prev => [...prev, newItem]);
    setSelectedProductId('');
    setQuantityStr('');
    setRateStr('');
  };

  const handleRemoveItem = (index: number) => {
    setAddedItems(prev => prev.filter((_, i) => i !== index));
  };

  const getCalculations = () => {
    const items = getEffectiveItems();
    const totalGstRate = companyDetails.gstPercentage !== undefined ? companyDetails.gstPercentage : 18;
    const halfGstRate = totalGstRate / 2;
    const halfGstMultiplier = halfGstRate / 100;
    const inclusiveDivisor = 1 + (totalGstRate / 100);

    let subtotal = 0;
    let cgst = 0;
    let sgst = 0;
    let grandTotal = 0;

    if (billType === 'tax_exclusive') {
      subtotal = roundTo2(items.reduce((sum, item) => sum + item.amount, 0));
      cgst = roundTo2(subtotal * halfGstMultiplier);
      sgst = roundTo2(subtotal * halfGstMultiplier);
      const totalBeforeRounding = roundTo2(subtotal + cgst + sgst);
      grandTotal = Math.round(totalBeforeRounding);
    } else if (billType === 'tax_inclusive') {
      subtotal = roundTo2(items.reduce((sum, item) => sum + item.amount, 0));
      cgst = roundTo2(subtotal * halfGstMultiplier);
      sgst = roundTo2(subtotal * halfGstMultiplier);
      const totalAmt = roundTo2(items.reduce((sum, item) => sum + (item.quantity * item.rate), 0));
      grandTotal = Math.round(totalAmt);
    } else { // delivery_challan
      subtotal = roundTo2(items.reduce((sum, item) => sum + item.amount, 0));
      cgst = 0;
      sgst = 0;
      grandTotal = Math.round(subtotal);
    }

    const roundOff = roundTo2(grandTotal - (subtotal + cgst + sgst));

    return { items, subtotal, cgst, sgst, grandTotal, roundOff, totalGstRate, halfGstRate };
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const customer = customers.find(c => c.id === Number(selectedCustomerId));
    if (!customer) return alert('Please select a customer.');

    const calcs = getCalculations();
    if (calcs.items.length === 0) {
      return alert('Please add at least one product with quantity to the invoice.');
    }

    const invoice: Invoice = {
      invoiceNo,
      invoiceDate: new Date(invoiceDate).toISOString(),
      invoiceTime,
      customerId: customer.id!,
      customerName: customer.name,
      customerAddress: customer.address,
      customerGSTIN: customer.gstin,
      customerState: customer.state,
      customerStateCode: customer.stateCode || '33',
      vehicleNo,
      billType,
      items: calcs.items,
      subtotal: calcs.subtotal,
      cgst: calcs.cgst,
      sgst: calcs.sgst,
      roundOff: calcs.roundOff,
      grandTotal: calcs.grandTotal,
      totalInWords: numberToWords(calcs.grandTotal),
      notes,
      gstPercentage: calcs.totalGstRate
    };

    try {
      await db.addInvoice(invoice);
      setAddedItems([]);
      setQuantityStr('');
      setSelectedCustomerId('');
      setSelectedProductId('');
      setRateStr('');
      setVehicleNo(companyDetails.vehicles?.[0]?.vehicleNo || '');
      onInvoiceCreated(invoice);
      await loadInvoiceNo();
    } catch (error) {
      console.error(error);
      alert('Failed to save the invoice.');
    }
  };

  const handleNumericWheel = (e: React.WheelEvent<HTMLInputElement>) => {
    (e.target as HTMLInputElement).blur();
  };

  const calcs = getCalculations();
  const inputClasses = "mt-1 block w-full rounded-md border-gray-300 bg-white text-gray-900 shadow-sm focus:border-green-500 focus:ring-green-500 border p-2";
  const selectedProduct = products.find(p => p.id === Number(selectedProductId));

  return (
    <div className="bg-white p-6 rounded-lg shadow-sm border mb-6 no-print">
      <div className="flex flex-col md:flex-row justify-between items-center mb-6 gap-4 border-b pb-4">
        <h2 className="text-xl font-bold flex items-center gap-2 text-gray-800">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6 text-green-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 7h6m0 10v-3m-3 3h.01M9 17h.01M9 14h.01M12 14h.01M15 11h.01M12 11h.01M9 11h.01M7 21h10a2 2 0 002-2V5a2 2 0 00-2-2H7a2 2 0 00-2 2v14a2 2 0 002 2z" />
          </svg>
          New Billing Entry
        </h2>

        <div className="flex bg-gray-100 p-1 rounded-lg border border-gray-200 shadow-inner">
          {[
            { id: 'tax_exclusive', label: 'Tax Bill' },
            { id: 'tax_inclusive', label: 'With Tax Bill' },
            { id: 'delivery_challan', label: 'Delivery Challan' }
          ].map((type) => (
            <button
              key={type.id}
              type="button"
              onClick={() => setBillType(type.id as BillType)}
              className={`px-4 py-1.5 rounded-md text-xs font-black transition-all uppercase tracking-tight ${billType === type.id ? 'bg-white text-blue-700 shadow-md' : 'text-gray-500 hover:text-gray-800'}`}
            >
              {type.label}
            </button>
          ))}
        </div>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-5 gap-4">
          <div className="md:col-span-1">
            <label className="block text-sm font-medium text-gray-700">Select Customer</label>
            <select value={selectedCustomerId} onChange={(e) => setSelectedCustomerId(e.target.value)} required className={inputClasses}>
              <option value="">-- Choose Customer --</option>
              {customers.map(c => (
                <option key={c.id} value={c.id}>{c.name}</option>
              ))}
            </select>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">Invoice Number</label>
            <input type="text" value={invoiceNo} onChange={(e) => setInvoiceNo(e.target.value)} required className="mt-1 block w-full rounded-md border-gray-300 bg-yellow-50 font-bold text-gray-900 shadow-sm border p-2" />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">Billing Date</label>
            <input type="date" value={invoiceDate} onChange={(e) => setInvoiceDate(e.target.value)} required className={inputClasses} />
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">Billing Time</label>
            <input type="time" value={invoiceTime} onChange={(e) => setInvoiceTime(e.target.value)} required className={inputClasses} />
          </div>
          <div>
            <div className="flex justify-between items-center">
              <label className="block text-sm font-medium text-gray-700">Vehicle No</label>
              {companyDetails.vehicles && companyDetails.vehicles.length > 0 && (
                <span className="text-[10px] text-indigo-600 font-bold uppercase">Stored Vehicles</span>
              )}
            </div>
            {companyDetails.vehicles && companyDetails.vehicles.length > 0 && (
              <select
                value={companyDetails.vehicles.some(v => v.vehicleNo.toUpperCase() === vehicleNo.toUpperCase()) ? vehicleNo.toUpperCase() : ''}
                onChange={(e) => {
                  if (e.target.value) setVehicleNo(e.target.value);
                }}
                className="mt-1 block w-full rounded-md border-indigo-200 bg-indigo-50/70 font-bold text-indigo-950 border p-1.5 text-xs mb-1"
              >
                <option value="">-- Choose Vehicle --</option>
                {companyDetails.vehicles.map((v, idx) => (
                  <option key={idx} value={v.vehicleNo.toUpperCase()}>
                    {v.name && v.name !== v.vehicleNo ? `${v.name} (${v.vehicleNo})` : v.vehicleNo}
                  </option>
                ))}
              </select>
            )}
            <input
              type="text"
              value={vehicleNo}
              onChange={(e) => setVehicleNo(e.target.value)}
              placeholder="e.g. TN 36 AB 1234"
              className="mt-1 block w-full rounded-md border-gray-300 bg-white font-bold text-gray-900 border p-2 uppercase"
            />
          </div>
        </div>

        {/* Product selection and Add Item row */}
        <div className="p-4 border border-blue-100 rounded-lg bg-blue-50/40">
          <label className="block text-xs font-bold uppercase tracking-wider text-blue-900 mb-2">Add Product Item to Invoice</label>
          <div className="grid grid-cols-1 md:grid-cols-12 gap-3 items-end">
            <div className="md:col-span-5">
              <label className="block text-xs font-semibold text-gray-700">Select Product</label>
              <select value={selectedProductId} onChange={(e) => setSelectedProductId(e.target.value)} className={inputClasses}>
                <option value="">-- Choose Product --</option>
                {products.map(p => (
                  <option key={p.id} value={p.id}>{p.name}</option>
                ))}
              </select>
            </div>
            <div className="md:col-span-2">
              <label className="block text-xs font-semibold text-gray-700">Rate (₹)</label>
              <select value={rateStr} onChange={(e) => setRateStr(e.target.value)} className={inputClasses} disabled={!selectedProduct}>
                {selectedProduct ? selectedProduct.rates.map(r => (
                  <option key={r} value={r.toFixed(2)}>₹ {r.toFixed(2)}</option>
                )) : <option value="">---</option>}
              </select>
            </div>
            <div className="md:col-span-2">
              <label className="block text-xs font-bold text-gray-700 uppercase">Liters</label>
              <input 
                type="number" 
                step="any" 
                value={quantityStr} 
                onChange={(e) => setQuantityStr(e.target.value)} 
                onWheel={handleNumericWheel}
                className={inputClasses} 
                placeholder="0.00" 
              />
            </div>
            <div className="md:col-span-3">
              <button
                type="button"
                onClick={handleAddItem}
                className="w-full bg-blue-600 text-white font-bold py-2 px-3 rounded-md hover:bg-blue-700 transition text-sm flex items-center justify-center gap-1 shadow-sm uppercase"
              >
                + Add Product
              </button>
            </div>
          </div>
        </div>

        {/* Table of added products */}
        {calcs.items.length > 0 && (
          <div className="border border-gray-200 rounded-lg overflow-hidden bg-white shadow-xs">
            <div className="bg-gray-100 px-4 py-2 border-b flex justify-between items-center">
              <span className="text-xs font-bold text-gray-700 uppercase tracking-wider">
                Invoice Products ({calcs.items.length})
              </span>
              <span className="text-xs text-gray-500 italic">
                All selected products will be included in the bill
              </span>
            </div>
            <table className="w-full text-sm text-left">
              <thead className="bg-gray-50 text-gray-600 text-xs uppercase font-bold border-b">
                <tr>
                  <th className="py-2.5 px-3">#</th>
                  <th className="py-2.5 px-3">Product Name</th>
                  <th className="py-2.5 px-3">HSN</th>
                  <th className="py-2.5 px-3 text-right">Liters</th>
                  <th className="py-2.5 px-3 text-right">Rate (₹)</th>
                  <th className="py-2.5 px-3 text-right">Amount (₹)</th>
                  <th className="py-2.5 px-3 text-center">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100">
                {calcs.items.map((item, idx) => (
                  <tr key={item.id || idx} className="hover:bg-gray-50">
                    <td className="py-2 px-3 font-bold text-gray-500">{idx + 1}</td>
                    <td className="py-2 px-3 font-bold text-gray-900 uppercase">{item.productName}</td>
                    <td className="py-2 px-3 text-gray-600 font-mono text-xs">{item.hsnCode}</td>
                    <td className="py-2 px-3 text-right font-bold">{item.quantity}</td>
                    <td className="py-2 px-3 text-right font-mono">₹ {item.rate.toFixed(2)}</td>
                    <td className="py-2 px-3 text-right font-black text-gray-900">{formatCurrency(item.amount)}</td>
                    <td className="py-2 px-3 text-center">
                      {idx < addedItems.length ? (
                        <button
                          type="button"
                          onClick={() => handleRemoveItem(idx)}
                          className="text-red-500 hover:text-red-700 font-bold p-1 rounded hover:bg-red-50 transition"
                          title="Remove item"
                        >
                          <svg xmlns="http://www.w3.org/2000/svg" className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                          </svg>
                        </button>
                      ) : (
                        <span className="text-[10px] bg-blue-100 text-blue-800 px-1.5 py-0.5 rounded font-bold uppercase">Active Input</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <div className="flex flex-col md:flex-row justify-between items-start gap-6 pt-2">
          <div className="w-full md:w-3/5">
            <label className="block text-sm font-medium text-gray-700">Invoice Notes / Declaration</label>
            <textarea value={notes} onChange={(e) => setNotes(e.target.value)} rows={2} className={inputClasses}></textarea>
          </div>
          <div className="bg-gray-800 p-5 rounded-lg w-full md:w-2/5 text-right border-4 border-gray-900 shadow-xl">
            <div className="space-y-1">
              <div className="flex justify-between text-xs text-gray-400">
                <span>Taxable Value:</span>
                <span className="text-white font-mono">{formatCurrency(calcs.subtotal)}</span>
              </div>
              {billType !== 'delivery_challan' && (
                <>
                  <div className="flex justify-between text-xs text-gray-400">
                    <span>CGST ({calcs.halfGstRate}%):</span>
                    <span className="text-white font-mono">{formatCurrency(calcs.cgst)}</span>
                  </div>
                  <div className="flex justify-between text-xs text-gray-400">
                    <span>SGST ({calcs.halfGstRate}%):</span>
                    <span className="text-white font-mono">{formatCurrency(calcs.sgst)}</span>
                  </div>
                </>
              )}
              <div className="flex justify-between text-xs text-gray-400 border-t border-gray-700 pt-1 mt-1 italic">
                <span>Round Off:</span>
                <span className="text-white font-mono">{formatCurrency(calcs.roundOff)}</span>
              </div>
            </div>
            <div className="mt-3 pt-3 border-t-2 border-gray-700">
              <p className="text-3xl font-black text-green-400 font-mono tracking-tighter">{formatCurrency(calcs.grandTotal)}</p>
              <p className="text-[10px] text-gray-500 uppercase font-bold mt-1">Net Payable Amount</p>
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-4">
          <button type="submit" className="bg-green-600 text-white px-12 py-4 rounded-lg hover:bg-green-700 transition font-black shadow-lg uppercase tracking-wider text-lg">
            Save & Preview {billType === 'delivery_challan' ? 'Challan' : 'Bill'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default InvoiceForm;

