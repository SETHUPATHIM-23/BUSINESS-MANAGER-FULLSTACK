import React, { useState } from 'react';
import { db } from './db';
import type { Product } from './db';

interface Props {
  products: Product[];
  onProductsChanged: () => void;
}

const ProductManager: React.FC<Props> = ({ products, onProductsChanged }) => {
  const [name, setName] = useState('');
  const [sku, setSku] = useState('');
  const [hsn, setHsn] = useState('282810');
  const [price, setPrice] = useState<number | ''>('');
  const [isExpanded, setIsExpanded] = useState(false);

  const handleAdd = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name) return;
    try {
      await db.addProduct({ name, sku, hsnCode: hsn, baseSellingPrice: price || undefined });
      setName(''); setSku(''); setHsn('282810'); setPrice('');
      onProductsChanged();
      alert('Product saved');
    } catch (err) {
      console.error(err);
      alert('Could not save product');
    }
  };

  const handleDelete = async (e: React.MouseEvent, id?: number, name?: string) => {
    e.stopPropagation();
    if (!id) return;
    if (window.confirm(`Delete ${name || 'product'}?`)) {
      try {
        await db.deleteProduct(id);
        onProductsChanged();
        alert('Product removed');
      } catch (err) {
        console.error(err);
        alert('Failed to remove product');
      }
    }
  };

  const inputClasses = "mt-1 block w-full rounded-md border-gray-300 bg-white text-gray-900 shadow-sm focus:border-blue-500 focus:ring-blue-500 border p-2";

  return (
    <div className="bg-white rounded-lg shadow-sm border mb-6 no-print overflow-hidden">
      <div 
        className="p-6 cursor-pointer flex justify-between items-center bg-emerald-50 hover:bg-emerald-100 transition"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        <h2 className="text-xl font-bold flex items-center gap-2 text-emerald-900">
          <svg xmlns="http://www.w3.org/2000/svg" className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4" />
          </svg>
          Manage Product Catalog ({products.length})
        </h2>
        <svg xmlns="http://www.w3.org/2000/svg" className={`h-6 w-6 transform transition-transform ${isExpanded ? 'rotate-180' : ''}`} fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </div>

      {isExpanded && (
        <div className="p-6 border-t">
          <form onSubmit={handleAdd} className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-8 bg-emerald-50/20 p-4 rounded-lg border border-emerald-100">
            <h3 className="md:col-span-3 text-xs font-bold uppercase text-emerald-800 mb-2">Register New Product</h3>
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase">Product Name</label>
              <input type="text" value={name} onChange={e => setName(e.target.value)} required className={inputClasses} placeholder="E.g. BLEACHING LIQUID" />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase">HSN Code</label>
              <input type="text" value={hsn} onChange={e => setHsn(e.target.value)} required className={inputClasses} placeholder="282810" />
            </div>
            <div>
              <label className="block text-xs font-bold text-gray-700 uppercase">Base Selling Rate (₹)</label>
              <input type="number" step="any" value={price as any} onChange={e => setPrice(e.target.value === '' ? '' : Number(e.target.value))} required className={inputClasses} placeholder="1.80" />
            </div>
            <div className="md:col-span-3 flex justify-end">
              <button type="submit" className="bg-emerald-600 text-white px-8 py-2 rounded hover:bg-emerald-700 transition font-bold uppercase text-xs tracking-widest shadow-md">Add Product</button>
            </div>
          </form>

          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead>
                <tr className="text-xs font-bold uppercase text-gray-500 border-b">
                  <th className="p-2">Name</th>
                  <th className="p-2">HSN</th>
                  <th className="p-2">Selling Rate</th>
                  <th className="p-2 text-right">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y text-sm">
                {products.map(p => {
                  const effectiveHsn = p.hsnCode || p.hsn || 'N/A';
                  const effectiveRates = p.rates || (p.baseSellingPrice ? [p.baseSellingPrice] : [0]);
                  return (
                    <tr key={p.id} className="hover:bg-gray-50">
                      <td className="p-2 font-bold text-gray-900 uppercase">{p.name}</td>
                      <td className="p-2 text-gray-600 font-mono text-xs">{effectiveHsn}</td>
                      <td className="p-2 flex gap-1 flex-wrap">
                        {effectiveRates.map((r, i) => (
                          <span key={i} className="bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded-full text-[10px] font-bold">₹{r.toFixed(2)}</span>
                        ))}
                      </td>
                      <td className="p-2 text-right">
                        <button onClick={(e) => handleDelete(e, p.id, p.name)} className="text-red-500 hover:text-red-700 font-bold uppercase text-[10px]">Remove</button>
                      </td>
                    </tr>
                  );
                })}
                {products.length === 0 && (
                  <tr>
                    <td colSpan={4} className="p-8 text-center text-gray-400 italic">No products saved yet.</td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProductManager;
