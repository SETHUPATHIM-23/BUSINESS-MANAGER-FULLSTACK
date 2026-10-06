import React, { useState, useEffect } from 'react';
import { api, fetchPaginated } from '../utils/api';
import { toastEvents } from '../utils/toast';
import { useAuth } from '../context/AuthContext';
import { 
  Plus, Search, Edit, Trash2, Eye, ChevronLeft, ChevronRight, 
  ArrowUpDown, AlertCircle, RefreshCw, Package, Printer
} from 'lucide-react';
import { PurchaseForm } from '../components/purchasing/PurchaseForm';

interface PurchaseOrderSummary {
  id: number;
  poNumber: string;
  supplierId: number;
  supplierName: string;
  orderDate: string;
  status: string;
  totalAmount: number;
}

interface FilterState {
  search: string;
  status: string;
}

export const Purchases: React.FC = () => {
  const { hasPermission } = useAuth();
  const hasWriteAccess = hasPermission('PURCHASE_WRITE');
  
  const [purchaseOrders, setPurchaseOrders] = useState<PurchaseOrderSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [filters, setFilters] = useState<FilterState>({ search: '', status: '' });
  
  const [page, setPage] = useState(0);
  const [pageSize] = useState(10);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  
  const [sortBy, setSortBy] = useState('orderDate');
  const [sortDir, setSortDir] = useState<'asc' | 'desc'>('desc');

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [selectedPoId, setSelectedPoId] = useState<number | null>(null);

  const fetchPOs = async () => {
    try {
      setLoading(true);
      const params: Record<string, any> = {
        page,
        size: pageSize,
        sortBy,
        sortDir
      };
      
      if (filters.search) params.search = filters.search;
      if (filters.status) params.status = filters.status;
      
      const response = await fetchPaginated<PurchaseOrderSummary>('/api/purchases', params);
      setPurchaseOrders(response.content);
      setTotalPages(response.totalPages);
      setTotalElements(response.totalElements);
    } catch (err) {
      toastEvents.error('Failed to load purchase orders');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPOs();
  }, [page, pageSize, sortBy, sortDir, filters]);

  const handleSort = (column: string) => {
    if (sortBy === column) {
      setSortDir(sortDir === 'asc' ? 'desc' : 'asc');
    } else {
      setSortBy(column);
      setSortDir('asc');
    }
    setPage(0);
  };

  const handleDelete = async (id: number, poNumber: string, status: string) => {
    const isReceived = status === 'RECEIVED' || status === 'PARTIALLY_RECEIVED';
    if (isReceived) {
      toastEvents.error('Cannot delete a purchase order that has recorded goods receipts. Please cancel it first via status change.');
      return;
    }

    const isDraft = status === 'DRAFT';
    const action = isDraft ? 'permanently delete DRAFT' : 'cancel and remove';
    if (!window.confirm(`Are you sure you want to ${action} purchase order ${poNumber}?`)) return;
    try {
      if (!isDraft) {
        // Cancel non-DRAFT POs via the cancel endpoint
        await api.post(`/api/purchases/${id}/cancel`);
      }
      await api.delete(`/api/purchases/${id}`);
      toastEvents.success('Purchase order deleted successfully');
      fetchPOs();
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Delete failed. Please try again.');
    }
  };



  const handlePrint = () => {
    window.print();
  };

  const openCreateModal = () => {
    setSelectedPoId(null);
    setIsModalOpen(true);
  };

  const openEditModal = (id: number) => {
    setSelectedPoId(id);
    setIsModalOpen(true);
  };



  return (
    <div className="page-container">
      <div className="page-header">
        <div className="page-title-group">
          <div className="page-icon">
            <Package size={22} />
          </div>
          <div>
            <h1 className="page-title">Purchase Orders</h1>
            <p className="page-description">Manage procurement and supplier invoices</p>
          </div>
        </div>
        
        <div className="header-actions">
          {hasWriteAccess && (
            <button onClick={openCreateModal} className="btn btn-primary">
              <Plus size={18} />
              <span>New Purchase Order</span>
            </button>
          )}
        </div>
      </div>

      <div className="filters-panel">
        <div className="filters-group">
          <div className="search-input-wrapper">
            <Search size={16} className="search-icon" />
            <input 
              type="text" 
              className="search-input"
              placeholder="Search PO number or supplier name..." 
              value={filters.search}
              onChange={(e) => setFilters(prev => ({ ...prev, search: e.target.value }))}
            />
          </div>
        </div>
      </div>

      <div className="table-wrapper">
        <div style={{ overflowX: 'auto' }}>
          <table className="data-table">
            <thead>
              <tr className="table-header-row">
                {['PO Number', 'Supplier', 'Order Date', 'Total', 'Actions'].map((header) => {
                  const isSortable = ['PO Number', 'Supplier', 'Order Date', 'Total'].includes(header);
                  const columnKey = header === 'PO Number' ? 'poNumber' : header === 'Order Date' ? 'orderDate' : header.toLowerCase();
                  
                  return (
                    <th key={header} style={header === 'Total' || header === 'Actions' ? { textAlign: 'right' } : {}}>
                      {isSortable ? (
                        <div 
                          style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', userSelect: 'none', justifyContent: header === 'Total' ? 'flex-end' : 'flex-start' }}
                          onClick={() => handleSort(columnKey)}
                        >
                          {header}
                          <ArrowUpDown size={14} style={{ opacity: sortBy === columnKey ? 1 : 0.3 }} />
                        </div>
                      ) : header}
                    </th>
                  );
                })}
              </tr>
            </thead>
            <tbody>
              {loading && purchaseOrders.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ padding: '3rem', textAlign: 'center' }}>
                    <RefreshCw size={24} className="spin" style={{ color: 'var(--accent-primary)', margin: '0 auto', opacity: 0.5 }} />
                  </td>
                </tr>
              ) : purchaseOrders.length === 0 ? (
                <tr>
                  <td colSpan={5} style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '1rem' }}>
                      <AlertCircle size={32} style={{ opacity: 0.3 }} />
                      <p>No purchase orders found.</p>
                    </div>
                  </td>
                </tr>
              ) : (
                purchaseOrders.map((po) => {
                  return (
                    <tr key={po.id} className="table-row">
                      <td style={{ fontWeight: 600 }}>{po.poNumber}</td>
                      <td>{po.supplierName}</td>
                      <td style={{ color: 'var(--text-secondary)' }}>{po.orderDate}</td>
                      <td style={{ fontWeight: 600, textAlign: 'right' }}>₹{po.totalAmount?.toFixed(2) || '0.00'}</td>
                      <td style={{ textAlign: 'right' }}>
                        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                          <button className="btn btn-outline btn-sm" style={{ border: 'none' }} title="View Details">
                            <Eye size={16} />
                          </button>
                          
                          <button onClick={() => handlePrint()} className="btn btn-outline btn-sm" style={{ border: 'none' }} title="Print">
                            <Printer size={16} />
                          </button>
                          
                          {hasWriteAccess && (
                            <button onClick={() => openEditModal(po.id)} className="btn btn-outline btn-sm" style={{ border: 'none' }} title="Edit Order">
                              <Edit size={16} />
                            </button>
                          )}
                          
                          {hasWriteAccess && (
                            <button
                              onClick={() => handleDelete(po.id, po.poNumber, po.status)}
                              disabled={po.status === 'RECEIVED' || po.status === 'PARTIALLY_RECEIVED'}
                              title={
                                po.status === 'RECEIVED' || po.status === 'PARTIALLY_RECEIVED'
                                  ? 'Cannot delete — goods have been received'
                                  : po.status === 'DRAFT'
                                    ? 'Delete DRAFT Purchase Order'
                                    : 'Cancel & Remove Purchase Order'
                              }
                              className={`btn btn-danger btn-sm ${
                                po.status === 'RECEIVED' || po.status === 'PARTIALLY_RECEIVED'
                                  ? 'opacity-30 cursor-not-allowed'
                                  : ''
                              }`}
                              style={{ border: 'none' }}
                            >
                              <Trash2 size={16} />
                            </button>
                          )}
                          
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
        
        {totalElements > 0 && (
          <div className="pagination-panel">
            <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
              Showing {page * pageSize + 1} to {Math.min((page + 1) * pageSize, totalElements)} of {totalElements} entries
            </span>
            <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
              <button 
                onClick={() => setPage(p => Math.max(0, p - 1))}
                disabled={page === 0}
                className="btn btn-secondary"
                style={{ opacity: page === 0 ? 0.4 : 1, cursor: page === 0 ? 'not-allowed' : 'pointer' }}
              >
                <ChevronLeft size={16} />
              </button>
              <button 
                onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
                disabled={page >= totalPages - 1}
                className="btn btn-secondary"
                style={{ opacity: page >= totalPages - 1 ? 0.4 : 1, cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer' }}
              >
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>

      {isModalOpen && (
        <div className="modal-backdrop">
          <div className="modal-content" style={{ width: '900px', maxWidth: '95vw', padding: 0 }}>
            <PurchaseForm 
              poId={selectedPoId} 
              onClose={() => setIsModalOpen(false)} 
              onSave={() => { setIsModalOpen(false); fetchPOs(); }} 
            />
          </div>
        </div>
      )}
    </div>
  );
};
