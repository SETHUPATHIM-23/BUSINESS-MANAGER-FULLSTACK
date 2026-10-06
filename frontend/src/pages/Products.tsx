import React, { useState, useEffect } from 'react';
import { api, fetchPaginated } from '../utils/api';
import { useAuth } from '../context/AuthContext';
import { toastEvents } from '../utils/toast';
import { 
  Plus, Search, Edit, Trash2, UserX, ChevronLeft, ChevronRight, 
  ArrowUpDown, SlidersHorizontal, AlertCircle, RefreshCw, X, Tag, Calculator,
  Download, Upload, FileText, CheckCircle, FolderPlus, Package
} from 'lucide-react';

interface ProductSummary {
  id: number;
  sku: string;
  name: string;
  hsnCode?: string;
  categoryName?: string;
  unitOfMeasure: string;
  costPrice: number;
  baseSellingPrice: number;
  status: 'ACTIVE' | 'INACTIVE' | 'DISCONTINUED';
}

interface CategoryLookup {
  id: number;
  name: string;
  description?: string;
}

interface TaxRateLookup {
  id: number;
  name: string;
  rate: number;
}

interface ImportReport {
  totalProcessed: number;
  successCount: number;
  failureCount: number;
  errors: { rowNumber: number; sku: String; message: string }[];
}

export const Products: React.FC = () => {
  const { hasPermission } = useAuth();
  const hasWriteAccess = hasPermission('PRODUCT_WRITE');

  // Tab State
  const [activeTab, setActiveTab] = useState<'catalog' | 'categories' | 'import-export'>('catalog');

  // List & Filter States
  const [products, setProducts] = useState<ProductSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  
  // Lookups lists
  const [categories, setCategories] = useState<CategoryLookup[]>([]);
  const [, setTaxRates] = useState<TaxRateLookup[]>([]);
  const [valuation, setValuation] = useState<number | null>(null);

  // Pagination & Sorting States
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [sortField, setSortField] = useState('sku');
  const [sortOrder, setSortOrder] = useState<'asc' | 'desc'>('asc');

  // Category Manager form states
  const [newCatName, setNewCatName] = useState('');
  const [newCatDesc, setNewCatDesc] = useState('');
  const [savingCategory, setSavingCategory] = useState(false);

  // Bulk CSV States
  const [importing, setImporting] = useState(false);
  const [importResult, setImportResult] = useState<ImportReport | null>(null);

  // Modal form states
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [selectedProductId, setSelectedProductId] = useState<number | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [errors, setErrors] = useState<Record<string, string>>({});

  // Form Fields
  const [sku, setSku] = useState('');
  const [name, setName] = useState('');
  const [hsnCode, setHsnCode] = useState('282810');
  const [description, setDescription] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [unitOfMeasure, setUnitOfMeasure] = useState('pcs');
  const [costPrice, setCostPrice] = useState('0.00');
  const [baseSellingPrice, setBaseSellingPrice] = useState('0.00');
  const [, setTaxRateId] = useState('');
  const [reorderLevel, setReorderLevel] = useState('0.00');
  const [reorderQty, setReorderQty] = useState('0.00');
  const [batchTracked, setBatchTracked] = useState(false);
  const [includeInFinancialCalculations, setIncludeInFinancialCalculations] = useState(true);
  const [statusVal, setStatusVal] = useState<'ACTIVE' | 'INACTIVE' | 'DISCONTINUED'>('ACTIVE');

  // Fetch Lookups
  const fetchLookups = async () => {
    try {
      const [catRes, taxRes] = await Promise.all([
        api.get('/api/lookups/categories'),
        api.get('/api/lookups/tax-rates')
      ]);
      setCategories(catRes.data);
      setTaxRates(taxRes.data);
    } catch (err) {
      console.error('Failed to load form lookups', err);
    }
  };

  // Fetch Valuation
  const fetchValuation = async () => {
    try {
      const response = await api.get('/api/products/valuation');
      setValuation(response.data);
    } catch (err) {
      console.error('Failed to load catalog valuation', err);
    }
  };

  // Fetch Products catalog
  const fetchProducts = async () => {
    setLoading(true);
    try {
      const params: any = {
        page,
        size: 10,
        sort: `${sortField},${sortOrder}`
      };
      if (searchQuery.trim()) params.search = searchQuery;
      if (statusFilter) params.status = statusFilter;
      if (categoryFilter) params.categoryId = categoryFilter;

      const data = await fetchPaginated<ProductSummary>('/api/products', params);
      setProducts(data.content);
      setTotalPages(data.totalPages);
      setTotalElements(data.totalElements);
    } catch (err: any) {
      console.error('Failed to fetch products', err);
      toastEvents.error('Failed to load product catalog records.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLookups();
    fetchValuation();
  }, []);

  useEffect(() => {
    if (activeTab === 'catalog') {
      fetchProducts();
    }
  }, [page, sortField, sortOrder, statusFilter, categoryFilter, activeTab]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    fetchProducts();
  };

  const handleSort = (field: string) => {
    if (sortField === field) {
      setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortOrder('asc');
    }
    setPage(0);
  };

  const openCreateModal = () => {
    setModalMode('create');
    setSelectedProductId(null);
    setErrors({});
    setSku('');
    setName('');
    setHsnCode('282810');
    setDescription('');
    setCategoryId(categories[0]?.id.toString() || '');
    setUnitOfMeasure('pcs');
    setCostPrice('0.00');
    setBaseSellingPrice('0.00');
    setReorderLevel('0.00');
    setReorderQty('0.00');
    setBatchTracked(false);
    setIncludeInFinancialCalculations(true);
    setStatusVal('ACTIVE');
    setIsModalOpen(true);
  };

  const openEditModal = async (id: number) => {
    setModalMode('edit');
    setSelectedProductId(id);
    setErrors({});
    setLoading(true);
    try {
      const response = await api.get(`/api/products/${id}`);
      const data = response.data;
      setSku(data.sku);
      setName(data.name);
      setHsnCode(data.hsnCode || '282810');
      setDescription(data.description || '');
      setCategoryId(data.categoryId?.toString() || '');
      setUnitOfMeasure(data.unitOfMeasure);
      setCostPrice(data.costPrice.toString());
      setBaseSellingPrice(data.baseSellingPrice.toString());
      setTaxRateId(data.taxRateId?.toString() || '');
      setReorderLevel(data.reorderLevel.toString());
      setReorderQty(data.reorderQty.toString());
      setBatchTracked(data.batchTracked);
      setIncludeInFinancialCalculations(data.includeInFinancialCalculations);
      setStatusVal(data.status);
      setIsModalOpen(true);
    } catch (err) {
      console.error('Failed to load product details', err);
      toastEvents.error('Failed to retrieve product profile details.');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrors({});

    // Client-side validations
    const clientErrors: Record<string, string> = {};
    if (!name.trim()) clientErrors.name = 'Product name is required';
    if (!sku.trim()) clientErrors.sku = 'SKU code is required';
    if (!unitOfMeasure.trim()) clientErrors.unitOfMeasure = 'Unit of measure is required';
    if (parseFloat(costPrice) < 0) clientErrors.costPrice = 'Cost price must be positive or zero';
    if (parseFloat(baseSellingPrice) < 0) clientErrors.baseSellingPrice = 'Selling price must be positive or zero';
    if (parseFloat(reorderLevel) < 0) clientErrors.reorderLevel = 'Reorder level must be positive or zero';
    if (parseFloat(reorderQty) < 0) clientErrors.reorderQty = 'Reorder quantity must be positive or zero';

    if (Object.keys(clientErrors).length > 0) {
      setErrors(clientErrors);
      toastEvents.error('Validation failed. Please correct form fields.');
      return;
    }

    setSubmitting(true);
    try {
      const payload: any = {
        sku,
        name,
        hsnCode: hsnCode || '282810',
        description,
        categoryId: categoryId ? parseInt(categoryId) : null,
        unitOfMeasure,
        costPrice: parseFloat(costPrice) || 0,
        baseSellingPrice: parseFloat(baseSellingPrice) || 0,
        taxRateId: null,
        reorderLevel: parseFloat(reorderLevel) || 0,
        reorderQty: parseFloat(reorderQty) || 0,
        batchTracked,
        includeInFinancialCalculations,
        status: statusVal
      };

      if (modalMode === 'create') {
        // Optimistic UI update
        const optimisticProduct: ProductSummary = {
          id: Math.random(),
          sku,
          name,
          categoryName: categories.find(c => c.id === parseInt(categoryId))?.name || 'Raw Materials',
          unitOfMeasure,
          costPrice: parseFloat(costPrice),
          baseSellingPrice: parseFloat(baseSellingPrice),
          status: 'ACTIVE'
        };
        setProducts(prev => [optimisticProduct, ...prev]);

        await api.post('/api/products', payload);
        toastEvents.success('Product catalog profile created successfully');
      } else {
        // Optimistic UI update
        setProducts(prev => prev.map(p => p.id === selectedProductId ? {
          ...p,
          sku,
          name,
          categoryName: categories.find(c => c.id === parseInt(categoryId))?.name || p.categoryName,
          unitOfMeasure,
          costPrice: parseFloat(costPrice),
          baseSellingPrice: parseFloat(baseSellingPrice),
          status: statusVal
        } : p));

        await api.put(`/api/products/${selectedProductId}`, payload);
        toastEvents.success('Product catalog profile updated successfully');
      }
      setIsModalOpen(false);
      fetchProducts();
      fetchValuation();
    } catch (err: any) {
      console.error('Save failed', err);
      if (err.response?.status === 400 && err.response?.data?.fieldErrors) {
        const backendErrors: Record<string, string> = {};
        err.response.data.fieldErrors.forEach((fe: any) => {
          backendErrors[fe.field] = fe.message;
        });
        setErrors(backendErrors);
        toastEvents.error('Validation failed. Please correct form fields.');
      } else if (err.response?.data?.message) {
        toastEvents.error(err.response.data.message);
      } else {
        toastEvents.error('An unexpected error occurred.');
      }
      // Revert optimistic update
      fetchProducts();
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeactivate = async (id: number) => {
    if (!window.confirm('Are you sure you want to deactivate this product? It will be marked as inactive.')) return;
    try {
      await api.put(`/api/products/${id}/deactivate`);
      toastEvents.success('Product status deactivated successfully');
      fetchProducts();
    } catch (err: any) {
      console.error('Deactivation failed', err);
      toastEvents.error(err.response?.data?.message || 'Deactivation failed.');
    }
  };

  const handleDelete = async (id: number, sku: string) => {
    if (!window.confirm(`Are you sure you want to delete product SKU: ${sku}?`)) return;
    try {
      await api.delete(`/api/products/${id}`);
      toastEvents.success('Product record deleted successfully');
      fetchProducts();
      fetchValuation();
    } catch (err: any) {
      console.error('Deletion failed', err);
      toastEvents.error(err.response?.data?.message || 'Cannot delete products with transaction history.');
    }
  };

  // Add Category Handler
  const handleAddCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCatName.trim()) return;

    setSavingCategory(true);
    try {
      await api.post('/api/lookups/categories', {
        name: newCatName.trim(),
        description: newCatDesc.trim()
      });
      toastEvents.success('Category created successfully');
      setNewCatName('');
      setNewCatDesc('');
      fetchLookups();
    } catch (err: any) {
      console.error('Failed to create category', err);
      toastEvents.error(err.response?.data?.message || 'Failed to save product category.');
    } finally {
      setSavingCategory(false);
    }
  };

  // Delete Category Handler
  const handleDeleteCategory = async (id: number, name: string) => {
    if (!window.confirm(`Are you sure you want to delete category "${name}"? Any linked products will lose this category reference.`)) return;
    try {
      await api.delete(`/api/lookups/categories/${id}`);
      toastEvents.success('Category deleted successfully');
      fetchLookups();
      fetchProducts();
    } catch (err: any) {
      console.error('Failed to delete category', err);
      toastEvents.error(err.response?.data?.message || 'Cannot delete referenced category.');
    }
  };

  // CSV Export Handler
  const handleExportCSV = async () => {
    try {
      const res = await api.get('/api/products/export', { responseType: 'blob' });
      const url = URL.createObjectURL(new Blob([res.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `product_catalog_${new Date().toISOString().split('T')[0]}.csv`);
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      toastEvents.success('Catalog CSV exported successfully from server');
    } catch (err) {
      console.error('CSV Export failed', err);
      toastEvents.error('CSV Export failed.');
    }
  };

  // CSV Import Handler
  const handleImportCSV = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = async (evt) => {
      const text = evt.target?.result as string;
      if (!text) return;

      const lines = text.split('\n').map(line => line.trim()).filter(line => line.length > 0);
      if (lines.length <= 1) {
        toastEvents.error('CSV file is empty or missing data rows.');
        return;
      }

      // Parse headers
      const headers = lines[0].split(',').map(h => h.replace(/^"|"$/g, '').trim().toLowerCase());
      const dtos: any[] = [];

      for (let i = 1; i < lines.length; i++) {
        // Handle values split with quotes
        const matches = lines[i].match(/(".*?"|[^",\s]+)(?=\s*,|\s*$)/g) || lines[i].split(',');
        const values = matches.map(v => v.replace(/^"|"$/g, '').trim());

        // Map fields
        const skuIdx = headers.indexOf('sku');
        const nameIdx = headers.indexOf('name');
        const catIdx = headers.indexOf('category') !== -1 ? headers.indexOf('category') : headers.indexOf('categoryid');
        const uomIdx = headers.indexOf('unit of measure') !== -1 ? headers.indexOf('unit of measure') : headers.indexOf('unitofmeasure');
        const costIdx = headers.indexOf('cost price') !== -1 ? headers.indexOf('cost price') : headers.indexOf('costprice');
        const sellIdx = headers.indexOf('selling price') !== -1 ? headers.indexOf('selling price') : headers.indexOf('basesellingprice');

        const categoryNameInput = catIdx !== -1 ? values[catIdx] : '';
        const foundCategory = categories.find(c => c.name.toLowerCase() === categoryNameInput.toLowerCase());
        const categoryIdResolved = foundCategory ? foundCategory.id : (parseInt(categoryNameInput) || null);

        dtos.push({
          sku: skuIdx !== -1 ? values[skuIdx] : '',
          name: nameIdx !== -1 ? values[nameIdx] : '',
          categoryId: categoryIdResolved,
          unitOfMeasure: uomIdx !== -1 ? values[uomIdx] : 'pcs',
          costPrice: costIdx !== -1 ? parseFloat(values[costIdx]) || 0 : 0,
          baseSellingPrice: sellIdx !== -1 ? parseFloat(values[sellIdx]) || 0 : 0,
          reorderLevel: 0,
          reorderQty: 0,
          batchTracked: false,
          includeInFinancialCalculations: true
        });
      }

      setImporting(true);
      setImportResult(null);
      try {
        const res = await api.post('/api/products/import', dtos);
        setImportResult(res.data);
        if (res.data.failureCount === 0) {
          toastEvents.success(`Import completed successfully! ${res.data.successCount} products imported.`);
        } else {
          toastEvents.warning(`Import completed with ${res.data.failureCount} row errors.`);
        }
        fetchProducts();
        fetchValuation();
      } catch (err) {
        console.error('Import failed', err);
        toastEvents.error('Failed to import product catalog CSV.');
      } finally {
        setImporting(false);
      }
    };
    reader.readAsText(file);
  };

  return (
    <div className="page-container">
      {/* Header section */}
      <div className="page-header">
        <div className="page-title-group">
          <div className="page-icon">
            <Package size={22} />
          </div>
          <div>
            <h1 className="page-title">Product Catalog</h1>
            <p className="page-description">
              Manage finished merchandise, raw materials parameters, price resolution structures, and reorder margins
            </p>
          </div>
        </div>
        
        <div className="header-actions">
          {valuation !== null && (
            <div className="glass-panel" style={{ padding: '8px 16px', display: 'flex', alignItems: 'center', gap: '0.6rem', border: '1px solid var(--border-color)', height: '40px' }}>
              <Calculator size={16} style={{ color: 'var(--accent-primary)' }} />
              <div>
                <span style={{ fontSize: '0.68rem', color: 'var(--text-muted)', display: 'block', fontWeight: 600, textTransform: 'uppercase' }}>Financial Valuation</span>
                <span style={{ fontSize: '0.9rem', color: '#fff', fontWeight: 700 }}>
                  ${valuation.toLocaleString(undefined, { minimumFractionDigits: 2 })}
                </span>
              </div>
            </div>
          )}

          {hasWriteAccess && activeTab === 'catalog' && (
            <button 
              className="btn btn-primary" 
              onClick={openCreateModal}
            >
              <Plus size={18} />
              <span>Add Product</span>
            </button>
          )}
        </div>
      </div>

      {/* Tabs list navigation */}
      <div style={{ display: 'flex', gap: '0.75rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '2px', marginBottom: '1.5rem', overflowX: 'auto' }}>
        <button 
          onClick={() => setActiveTab('catalog')} 
          className="btn"
          style={{
            padding: '10px 16px',
            fontSize: '0.88rem',
            fontWeight: 600,
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'catalog' ? '2px solid var(--accent-primary)' : '2px solid transparent',
            color: activeTab === 'catalog' ? 'var(--accent-primary)' : 'var(--text-secondary)',
            cursor: 'pointer',
            transition: 'var(--transition-smooth)',
            borderRadius: 0,
            whiteSpace: 'nowrap'
          }}
        >
          Product Catalog
        </button>
        <button 
          onClick={() => setActiveTab('categories')} 
          className="btn"
          style={{
            padding: '10px 16px',
            fontSize: '0.88rem',
            fontWeight: 600,
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'categories' ? '2px solid var(--accent-primary)' : '2px solid transparent',
            color: activeTab === 'categories' ? 'var(--accent-primary)' : 'var(--text-secondary)',
            cursor: 'pointer',
            transition: 'var(--transition-smooth)',
            borderRadius: 0,
            whiteSpace: 'nowrap'
          }}
        >
          Category Manager
        </button>
        <button 
          onClick={() => setActiveTab('import-export')} 
          className="btn"
          style={{
            padding: '10px 16px',
            fontSize: '0.88rem',
            fontWeight: 600,
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'import-export' ? '2px solid var(--accent-primary)' : '2px solid transparent',
            color: activeTab === 'import-export' ? 'var(--accent-primary)' : 'var(--text-secondary)',
            cursor: 'pointer',
            transition: 'var(--transition-smooth)',
            borderRadius: 0,
            whiteSpace: 'nowrap'
          }}
        >
          Bulk CSV Import/Export
        </button>
      </div>

      {/* Tab content 1 - Catalog Directory */}
      {activeTab === 'catalog' && (
        <>
          {/* Filter panel */}
          <div className="filters-panel">
            <form onSubmit={handleSearchSubmit} className="filters-group">
              <div className="search-input-wrapper">
                <Search size={16} className="search-icon" />
                <input
                  type="text"
                  className="search-input"
                  placeholder="Search SKU or product name..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                />
              </div>
              <button type="submit" className="btn btn-secondary">
                Search
              </button>
            </form>

            <div className="filters-group" style={{ flex: 'none' }}>
              {/* Category Filter */}
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <Tag size={16} style={{ color: 'var(--text-muted)' }} />
                <select
                  className="form-select"
                  value={categoryFilter}
                  onChange={(e) => { setCategoryFilter(e.target.value); setPage(0); }}
                  style={{ width: 'auto' }}
                >
                  <option value="">All Categories</option>
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>{c.name}</option>
                  ))}
                </select>
              </div>

              {/* Status Filter */}
              <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                <SlidersHorizontal size={16} style={{ color: 'var(--text-muted)' }} />
                <select
                  className="form-select"
                  value={statusFilter}
                  onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }}
                  style={{ width: 'auto' }}
                >
                  <option value="">All Statuses</option>
                  <option value="ACTIVE">Active</option>
                  <option value="INACTIVE">Inactive</option>
                  <option value="DISCONTINUED">Discontinued</option>
                </select>
              </div>
            </div>
          </div>

          {/* Listing table */}
          <div className="table-wrapper">
            {loading && products.length === 0 ? (
              <div style={{ padding: '4rem', textAlign: 'center' }}>
                <RefreshCw size={24} style={{ animation: 'spin 1s linear infinite', color: 'var(--accent-primary)', marginBottom: '1rem' }} />
                <p style={{ color: 'var(--text-secondary)' }}>Retrieving catalog records...</p>
              </div>
            ) : products.length === 0 ? (
              <div style={{ padding: '4rem', textAlign: 'center', color: 'var(--text-muted)' }}>
                <AlertCircle size={32} style={{ marginBottom: '1rem' }} />
                <p>No products found in the catalog matching the filters.</p>
              </div>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table className="data-table">
                  <thead>
                    <tr className="table-header-row">
                      <th onClick={() => handleSort('sku')}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                          <span>SKU</span>
                          <ArrowUpDown size={12} />
                        </div>
                      </th>
                      <th onClick={() => handleSort('name')}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                          <span>Product Details</span>
                          <ArrowUpDown size={12} />
                        </div>
                      </th>
                      <th>HSN</th>
                      <th>Category</th>
                      <th>UoM</th>
                      <th onClick={() => handleSort('costPrice')} style={{ textAlign: 'right' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', justifyContent: 'flex-end' }}>
                          <span>Cost Price</span>
                          <ArrowUpDown size={12} />
                        </div>
                      </th>
                      <th onClick={() => handleSort('baseSellingPrice')} style={{ textAlign: 'right' }}>
                        <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem', justifyContent: 'flex-end' }}>
                          <span>Selling Price</span>
                          <ArrowUpDown size={12} />
                        </div>
                      </th>
                      <th>Status</th>
                      <th style={{ textAlign: 'right' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {products.map((p) => (
                      <tr key={p.id} className="table-row">
                        <td style={{ fontWeight: 600 }}>{p.sku}</td>
                        <td>
                          <span style={{ fontWeight: 500 }}>{p.name}</span>
                        </td>
                        <td style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', fontFamily: 'monospace' }}>
                          {p.hsnCode || '282810'}
                        </td>
                        <td style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                          {p.categoryName || 'Unassigned'}
                        </td>
                        <td style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                          {p.unitOfMeasure}
                        </td>
                        <td style={{ color: 'var(--text-secondary)', textAlign: 'right' }}>
                          ₹{p.costPrice.toFixed(2)}
                        </td>
                        <td style={{ fontWeight: 600, textAlign: 'right' }}>
                          ₹{p.baseSellingPrice.toFixed(2)}
                        </td>
                        <td>
                          <span className={
                            p.status === 'ACTIVE' ? 'badge badge-success' : 
                            p.status === 'INACTIVE' ? 'badge badge-neutral' : 
                            'badge badge-danger'
                          }>
                            {p.status}
                          </span>
                        </td>
                        <td style={{ textAlign: 'right' }}>
                          <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
                            {hasWriteAccess && (
                              <>
                                <button onClick={() => openEditModal(p.id)} className="btn btn-outline btn-sm" title="Edit Product">
                                  <Edit size={16} />
                                </button>
                                {p.status === 'ACTIVE' && (
                                  <button onClick={() => handleDeactivate(p.id)} className="btn btn-outline btn-sm" title="Deactivate Product">
                                    <UserX size={16} />
                                  </button>
                                )}
                                <button onClick={() => handleDelete(p.id, p.sku)} className="btn btn-danger btn-sm" title="Delete Product">
                                  <Trash2 size={16} />
                                </button>
                              </>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {/* Pagination Panel */}
            {totalPages > 0 && (
              <div className="pagination-panel">
                <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>
                  Showing Page {page + 1} of {totalPages} ({totalElements} total records)
                </span>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <button 
                    onClick={() => setPage(p => Math.max(0, p - 1))} 
                    disabled={page === 0} 
                    className="btn btn-secondary" 
                    style={{ opacity: page === 0 ? 0.4 : 1 }}
                  >
                    <ChevronLeft size={16} />
                  </button>
                  <button 
                    onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} 
                    disabled={page === totalPages - 1} 
                    className="btn btn-secondary" 
                    style={{ opacity: page === totalPages - 1 ? 0.4 : 1 }}
                  >
                    <ChevronRight size={16} />
                  </button>
                </div>
              </div>
            )}
          </div>
        </>
      )}

      {/* Tab content 2 - Category Manager Screen */}
      {activeTab === 'categories' && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 280px), 1fr))', gap: '1.5rem' }}>
          {/* Add Category Form */}
          {hasWriteAccess && (
            <div className="glass-panel" style={{ padding: '2rem', height: 'fit-content' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
                <FolderPlus size={20} style={{ color: 'var(--accent-primary)' }} />
                <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: '#fff' }}>Create Product Category</h3>
              </div>

              <form onSubmit={handleAddCategory}>
                <div className="form-group">
                  <label className="form-label">
                    Category Name <span style={{ color: 'var(--danger)' }}>*</span>
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    required
                    disabled={savingCategory}
                    placeholder="e.g. Electrical Components"
                    value={newCatName}
                    onChange={(e) => setNewCatName(e.target.value)}
                  />
                </div>

                <div className="form-group">
                  <label className="form-label">
                    Description
                  </label>
                  <textarea
                    className="form-control"
                    disabled={savingCategory}
                    placeholder="Brief description of category items..."
                    rows={3}
                    value={newCatDesc}
                    onChange={(e) => setNewCatDesc(e.target.value)}
                    style={{ resize: 'vertical' }}
                  />
                </div>

                <button 
                  type="submit" 
                  className="btn btn-primary" 
                  disabled={savingCategory || !newCatName.trim()}
                  style={{ width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0.5rem' }}
                >
                  {savingCategory ? <RefreshCw size={16} style={{ animation: 'spin 1s linear infinite' }} /> : <Plus size={16} />}
                  <span>Save Category</span>
                </button>
              </form>
            </div>
          )}

          {/* Categories List */}
          <div className="glass-panel" style={{ padding: '2rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
              <Tag size={20} style={{ color: 'var(--accent-primary)' }} />
              <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: '#fff' }}>Categories Registry</h3>
            </div>

            {categories.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', fontSize: '0.88rem' }}>No categories registered.</p>
            ) : (
              <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
                {categories.map((c) => (
                  <div 
                    key={c.id} 
                    style={{ 
                      display: 'flex', 
                      justifyContent: 'space-between', 
                      alignItems: 'center', 
                      padding: '12px 16px', 
                      borderRadius: '6px', 
                      backgroundColor: 'rgba(255,255,255,0.01)', 
                      border: '1px solid var(--border-color)' 
                    }}
                  >
                    <div>
                      <span style={{ display: 'block', fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-primary)' }}>{c.name}</span>
                      {c.description && <span style={{ display: 'block', fontSize: '0.78rem', color: 'var(--text-secondary)', marginTop: '2px' }}>{c.description}</span>}
                    </div>
                    {hasWriteAccess && (
                      <button 
                        onClick={() => handleDeleteCategory(c.id, c.name)}
                        className="btn btn-outline btn-sm"
                        style={{ border: 'none' }}
                        title="Delete Category"
                      >
                        <Trash2 size={16} />
                      </button>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tab content 3 - Bulk CSV Import/Export Screen */}
      {activeTab === 'import-export' && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 260px), 1fr))', gap: '1.5rem' }}>
            {/* Export Panel */}
            <div className="glass-panel" style={{ padding: '2rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
                <Download size={20} style={{ color: 'var(--accent-primary)' }} />
                <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: '#fff' }}>Export Catalog Data</h3>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: '1.35rem', marginBottom: '1.5rem' }}>
                Download your entire product catalog including SKUs, prices, unit of measure, and active statuses as a standard CSV spreadsheet file.
              </p>
              <button 
                onClick={handleExportCSV} 
                className="btn btn-secondary" 
                style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', width: '100%', justifyContent: 'center' }}
              >
                <Download size={16} />
                <span>Export Product Catalog (CSV)</span>
              </button>
            </div>

            {/* Import Panel */}
            {hasWriteAccess ? (
              <div className="glass-panel" style={{ padding: '2rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem' }}>
                  <Upload size={20} style={{ color: 'var(--accent-primary)' }} />
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: '#fff' }}>Upload Bulk Products</h3>
                </div>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: '1.35rem', marginBottom: '1.5rem' }}>
                  Upload a CSV file containing columns for `sku`, `name`, `category`, `unit of measure`, `cost price`, and `selling price` to bulk seed records.
                </p>
                <div style={{ position: 'relative', overflow: 'hidden', display: 'inline-block', width: '100%' }}>
                  <button 
                    className="btn btn-primary" 
                    disabled={importing}
                    style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', width: '100%', justifyContent: 'center', height: '40px' }}
                  >
                    {importing ? <RefreshCw size={16} style={{ animation: 'spin 1s linear infinite' }} /> : <Upload size={16} />}
                    <span>Select CSV Data File</span>
                  </button>
                  <input
                    type="file"
                    accept=".csv"
                    disabled={importing}
                    onChange={handleImportCSV}
                    style={{ position: 'absolute', left: 0, top: 0, opacity: 0, width: '100%', height: '100%', cursor: 'pointer' }}
                  />
                </div>
              </div>
            ) : (
              <div className="glass-panel" style={{ padding: '2rem', display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', textAlign: 'center', border: '1px dashed var(--border-color)' }}>
                <AlertCircle size={32} style={{ color: 'var(--text-muted)', marginBottom: '1rem' }} />
                <h3 style={{ color: '#fff', fontSize: '1rem', fontWeight: 600 }}>Import Restraints</h3>
                <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginTop: '0.25rem' }}>
                  Bulk product uploads require administrator or purchasing staff permissions.
                </p>
              </div>
            )}
          </div>

          {/* Import results report - Row level validations feedback */}
          {importResult && (
            <div className="glass-panel" style={{ padding: '2rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.75rem', marginBottom: '1.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  <FileText size={20} style={{ color: 'var(--accent-primary)' }} />
                  <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: '#fff' }}>CSV Import Report Summary</h3>
                </div>
                <div style={{ display: 'flex', gap: '1rem' }}>
                  <span style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>Processed: <strong>{importResult.totalProcessed}</strong></span>
                  <span style={{ fontSize: '0.85rem', color: 'var(--success)' }}>Success: <strong>{importResult.successCount}</strong></span>
                  <span style={{ fontSize: '0.85rem', color: 'var(--danger)' }}>Failures: <strong>{importResult.failureCount}</strong></span>
                </div>
              </div>

              {importResult.failureCount === 0 ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--success)', fontSize: '0.9rem' }}>
                  <CheckCircle size={18} />
                  <span>All rows passed validations and were imported successfully!</span>
                </div>
              ) : (
                <div>
                  <h4 style={{ color: 'var(--danger)', fontSize: '0.9rem', marginBottom: '0.75rem', fontWeight: 600 }}>Validation Row-Level Errors:</h4>
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem', maxHeight: '220px', overflowY: 'auto', border: '1px solid var(--border-color)', borderRadius: '6px', padding: '1rem', backgroundColor: 'rgba(0,0,0,0.2)' }}>
                    {importResult.errors.map((e, idx) => (
                      <div key={idx} style={{ display: 'flex', gap: '1rem', fontSize: '0.82rem', borderBottom: idx < importResult.errors.length - 1 ? '1px solid rgba(255,255,255,0.03)' : 'none', paddingBottom: '6px', paddingTop: '2px' }}>
                        <span style={{ color: 'var(--text-secondary)', width: '60px', fontWeight: 600 }}>Row {e.rowNumber}:</span>
                        {e.sku && <span style={{ color: 'var(--accent-primary)', width: '120px', fontFamily: 'monospace' }}>[{e.sku}]</span>}
                        <span style={{ color: 'var(--danger)' }}>{e.message}</span>
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

        </div>
      )}

      {/* Modal - Create/Edit Overlay */}
      {isModalOpen && (
        <div className="modal-backdrop">
          <div className="modal-content">
            {/* Modal Header */}
            <div className="modal-header">
              <h2 className="modal-title">
                {modalMode === 'create' ? 'Add New Product' : 'Edit Product Profile'}
              </h2>
              <button onClick={() => setIsModalOpen(false)} className="modal-close-btn">
                <X size={20} />
              </button>
            </div>

            <form onSubmit={handleSave}>
              <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(min(100%, 240px), 1fr))', gap: '1.25rem', marginBottom: '1.25rem' }}>
                
                {/* SKU */}
                <div className="form-group">
                  <label className="form-label">
                    SKU Code <span style={{ color: 'var(--danger)' }}>*</span>
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    required
                    disabled={modalMode === 'edit' || submitting}
                    placeholder="e.g. RAW-STL-001"
                    value={sku}
                    onChange={(e) => setSku(e.target.value)}
                    style={{
                      border: errors.sku ? '1px solid var(--danger)' : undefined,
                      opacity: modalMode === 'edit' ? 0.6 : 1
                    }}
                  />
                  {errors.sku && <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{errors.sku}</span>}
                </div>

                {/* Name */}
                <div className="form-group">
                  <label className="form-label">
                    Product Name <span style={{ color: 'var(--danger)' }}>*</span>
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    required
                    disabled={submitting}
                    placeholder="e.g. Stainless Steel Sheets"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    style={{ border: errors.name ? '1px solid var(--danger)' : undefined }}
                  />
                  {errors.name && <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{errors.name}</span>}
                </div>

                {/* HSN Code */}
                <div className="form-group">
                  <label className="form-label">
                    HSN / SAC Code
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    disabled={submitting}
                    placeholder="e.g. 282810"
                    value={hsnCode}
                    onChange={(e) => setHsnCode(e.target.value)}
                  />
                </div>

                {/* Category */}
                <div className="form-group">
                  <label className="form-label">
                    Product Category
                  </label>
                  <select
                    className="form-select"
                    disabled={submitting}
                    value={categoryId}
                    onChange={(e) => setCategoryId(e.target.value)}
                  >
                    <option value="">-- Choose Category --</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>{c.name}</option>
                    ))}
                  </select>
                </div>

                {/* Unit of Measure */}
                <div className="form-group">
                  <label className="form-label">
                    Unit of Measure <span style={{ color: 'var(--danger)' }}>*</span>
                  </label>
                  <input
                    type="text"
                    className="form-control"
                    required
                    disabled={submitting}
                    placeholder="e.g. pcs, kgs, meters"
                    value={unitOfMeasure}
                    onChange={(e) => setUnitOfMeasure(e.target.value)}
                    style={{ border: errors.unitOfMeasure ? '1px solid var(--danger)' : undefined }}
                  />
                  {errors.unitOfMeasure && <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{errors.unitOfMeasure}</span>}
                </div>

                {/* Cost Price */}
                <div className="form-group">
                  <label className="form-label">
                    Cost Price (₹) <span style={{ color: 'var(--danger)' }}>*</span>
                  </label>
                  <input
                    type="number"
                    className="form-control"
                    step="0.01"
                    required
                    disabled={submitting}
                    value={costPrice}
                    onChange={(e) => setCostPrice(e.target.value)}
                    style={{ border: errors.costPrice ? '1px solid var(--danger)' : undefined }}
                  />
                  {errors.costPrice && <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{errors.costPrice}</span>}
                </div>

                {/* Base Selling Price */}
                <div className="form-group">
                  <label className="form-label">
                    Base Selling Price (₹) <span style={{ color: 'var(--danger)' }}>*</span>
                  </label>
                  <input
                    type="number"
                    className="form-control"
                    step="0.01"
                    required
                    disabled={submitting}
                    value={baseSellingPrice}
                    onChange={(e) => setBaseSellingPrice(e.target.value)}
                    style={{ border: errors.baseSellingPrice ? '1px solid var(--danger)' : undefined }}
                  />
                  {errors.baseSellingPrice && <span style={{ color: 'var(--danger)', fontSize: '0.75rem', marginTop: '4px', display: 'block' }}>{errors.baseSellingPrice}</span>}
                </div>




                {/* Status Selection */}
                {modalMode === 'edit' && (
                  <div className="form-group">
                    <label className="form-label">
                      Catalog Status
                    </label>
                    <select
                      className="form-select"
                      disabled={submitting}
                      value={statusVal}
                      onChange={(e) => setStatusVal(e.target.value as any)}
                    >
                      <option value="ACTIVE">Active</option>
                      <option value="INACTIVE">Inactive</option>
                      <option value="DISCONTINUED">Discontinued</option>
                    </select>
                  </div>
                )}
              </div>

              {/* Description */}
              <div className="form-group" style={{ gridColumn: '1 / -1' }}>
                <label className="form-label">
                  Product Description
                </label>
                <textarea
                  className="form-control"
                  disabled={submitting}
                  placeholder="e.g. Standard grade cold-rolled stainless sheets, 2mm thickness"
                  rows={2}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  style={{ resize: 'vertical' }}
                />
              </div>

              {/* Boolean Flags */}
              <div style={{ display: 'flex', gap: '2rem', marginBottom: '2rem', flexWrap: 'wrap', gridColumn: '1 / -1' }}>
                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.88rem', color: 'var(--text-primary)' }}>
                  <input
                    type="checkbox"
                    disabled={submitting}
                    checked={batchTracked}
                    onChange={(e) => setBatchTracked(e.target.checked)}
                    style={{ cursor: 'pointer', width: '16px', height: '16px' }}
                  />
                  <span>Enable Batch/Lot Tracking (PROD-100)</span>
                </label>

                <label style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', cursor: 'pointer', fontSize: '0.88rem', color: 'var(--text-primary)' }}>
                  <input
                    type="checkbox"
                    disabled={submitting}
                    checked={includeInFinancialCalculations}
                    onChange={(e) => setIncludeInFinancialCalculations(e.target.checked)}
                    style={{ cursor: 'pointer', width: '16px', height: '16px' }}
                  />
                  <span>Include in Financial Valuations (PROD-060)</span>
                </label>
              </div>

              {/* Action Buttons */}
              <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end', borderTop: '1px solid var(--border-color)', paddingTop: '1.25rem', gridColumn: '1 / -1' }}>
                <button type="button" disabled={submitting} onClick={() => setIsModalOpen(false)} className="btn btn-secondary">Cancel</button>
                <button type="submit" className="btn btn-primary" disabled={submitting} style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', minWidth: '100px', justifyContent: 'center' }}>
                  {submitting ? <RefreshCw size={16} style={{ animation: 'spin 1s linear infinite' }} /> : 'Save Product'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
      <style dangerouslySetInnerHTML={{__html: `
        @keyframes spin {
          to { transform: rotate(360deg); }
        }
      `}} />
    </div>
  );
};
export default Products;
