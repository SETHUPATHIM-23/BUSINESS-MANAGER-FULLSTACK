import React, { useState, useEffect } from 'react';
import { X, Upload, ShieldCheck, ArrowRight, AlertCircle, RefreshCw } from 'lucide-react';
import { api, fetchPaginated } from '../../utils/api';
import { toastEvents } from '../../utils/toast';

interface BankStatementLine {
  id: string; // generated uuid for internal react tracking
  date: string;
  amount: number;
  reference: string;
}

interface FundTransactionDto {
  id: number;
  fundAccountName: string;
  type: string;
  amount: number;
  transactionDate: string;
  referenceDocumentType: string | null;
  referenceDocumentId: number | null;
  description: string | null;
}

interface BankReconciliationScreenProps {
  onClose: () => void;
}

export const BankReconciliationScreen: React.FC<BankReconciliationScreenProps> = ({ onClose }) => {
  const [csvData, setCsvData] = useState('');
  const [statementLines, setStatementLines] = useState<BankStatementLine[]>([]);
  const [transactions, setTransactions] = useState<FundTransactionDto[]>([]);
  const [loadingTx, setLoadingTx] = useState(false);
  
  const [selectedBankLineId, setSelectedBankLineId] = useState<string | null>(null);
  const [selectedTxId, setSelectedTxId] = useState<number | null>(null);
  const [matching, setMatching] = useState(false);

  useEffect(() => {
    fetchUnreconciledTransactions();
  }, []);

  const fetchUnreconciledTransactions = async () => {
    setLoadingTx(true);
    try {
      // Fetch recent transactions (using size 100 for this workspace view)
      const data = await fetchPaginated<FundTransactionDto>('/api/funds/transactions', { page: 0, size: 100, sort: 'transactionDate,desc' });
      // In a real production scenario with specific backend support, we'd filter by 'unreconciled'
      setTransactions(data.content);
    } catch (err) {
      toastEvents.error('Failed to load system transactions');
    } finally {
      setLoadingTx(false);
    }
  };

  const parseCsv = () => {
    if (!csvData.trim()) return;
    
    try {
      const lines = csvData.trim().split('\n');
      const parsed: BankStatementLine[] = lines.map((line, idx) => {
        // Assume format: Date, Amount, Reference
        const parts = line.split(',');
        if (parts.length < 3) {
          throw new Error(`Invalid format on line ${idx + 1}`);
        }
        return {
          id: `bank-line-${Date.now()}-${idx}`,
          date: parts[0].trim(),
          amount: parseFloat(parts[1].trim()),
          reference: parts[2].trim()
        };
      });
      
      setStatementLines(prev => [...prev, ...parsed]);
      setCsvData('');
      toastEvents.success(`Imported ${parsed.length} statement lines`);
    } catch (err: any) {
      toastEvents.error('CSV Parsing Error: ' + err.message);
    }
  };

  const handleMatch = async () => {
    if (!selectedBankLineId || !selectedTxId) return;
    
    const bankLine = statementLines.find(l => l.id === selectedBankLineId);
    if (!bankLine) return;

    setMatching(true);
    try {
      // 1. Create the reconciliation record linking the transaction and bank ref
      const recRes = await api.post('/api/funds/reconciliations', {
        fundTransactionId: selectedTxId,
        bankStatementLineRef: bankLine.reference
      });
      
      // 2. Mark it as matched
      const recId = recRes.data.id;
      await api.post(`/api/funds/reconciliations/${recId}/match`);

      toastEvents.success('Successfully matched and reconciled!');
      
      // Remove from client queues
      setStatementLines(prev => prev.filter(l => l.id !== selectedBankLineId));
      setTransactions(prev => prev.filter(tx => tx.id !== selectedTxId));
      
      setSelectedBankLineId(null);
      setSelectedTxId(null);
    } catch (err: any) {
      toastEvents.error(err.response?.data?.message || 'Failed to match record');
    } finally {
      setMatching(false);
    }
  };

  return (
    <div style={{ position: 'fixed', inset: 0, zIndex: 50, backgroundColor: 'var(--bg-primary)', display: 'flex', flexDirection: 'column', height: '100vh' }}>
      <div className="glass-panel" style={{ borderRadius: 0, borderBottom: '1px solid var(--border-color)', padding: '1rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{ padding: '0.5rem', backgroundColor: 'var(--info-bg)', color: 'var(--info)', borderRadius: '8px' }}>
            <ShieldCheck size={24} />
          </div>
          <div>
            <h1 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0, color: 'var(--text-primary)' }}>Bank Reconciliation Workspace</h1>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', margin: 0 }}>Match bank statement lines against system transactions</p>
          </div>
        </div>
        <button onClick={onClose} className="close-button">
          <X size={20} />
        </button>
      </div>

      <div style={{ flex: 1, overflow: 'hidden', padding: '1.5rem' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 200px 1fr', gap: '1.5rem', height: '100%' }}>
          
          {/* Left Pane: Bank Statement Lines */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', height: '100%' }}>
            <div className="glass-panel" style={{ padding: '1rem', display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
              <h2 style={{ fontSize: '1.1rem', fontWeight: 600, marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <Upload size={18} /> Bank Statement Lines
              </h2>
              
              <div style={{ marginBottom: '1rem' }}>
                <label className="form-label">
                  Paste CSV Data (Date, Amount, Reference)
                </label>
                <div style={{ display: 'flex', gap: '0.5rem' }}>
                  <textarea 
                    className="form-control"
                    style={{ flex: 1, height: '5rem', fontFamily: 'monospace' }}
                    placeholder="2023-10-01, -500.00, TXN-12345&#10;2023-10-02, 1200.00, DEP-98765"
                    value={csvData}
                    onChange={(e) => setCsvData(e.target.value)}
                  />
                  <button className="btn btn-primary" style={{ whiteSpace: 'nowrap', height: '5rem' }} onClick={parseCsv}>
                    Import
                  </button>
                </div>
              </div>

              <div style={{ flex: 1, overflowY: 'auto', paddingRight: '0.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {statementLines.length === 0 ? (
                  <div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '2.5rem' }}>
                    <AlertCircle size={32} style={{ margin: '0 auto 0.5rem auto', opacity: 0.5 }} />
                    <p>No bank statement lines imported.</p>
                  </div>
                ) : (
                  statementLines.map(line => (
                    <div 
                      key={line.id} 
                      onClick={() => setSelectedBankLineId(line.id)}
                      style={{ 
                        padding: '0.75rem', 
                        border: selectedBankLineId === line.id ? '2px solid var(--accent-primary)' : '1px solid var(--border-color)', 
                        borderRadius: '8px', 
                        cursor: 'pointer', 
                        backgroundColor: selectedBankLineId === line.id ? 'var(--info-bg)' : 'transparent',
                        transition: 'var(--transition-smooth)'
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.25rem' }}>
                        <span style={{ fontSize: '0.875rem', fontWeight: 500, color: 'var(--text-secondary)' }}>{line.date}</span>
                        <span style={{ fontWeight: 700, color: line.amount < 0 ? 'var(--danger)' : 'var(--success)' }}>
                          ₹{Math.abs(line.amount).toFixed(2)}
                        </span>
                      </div>
                      <div style={{ fontSize: '0.875rem', color: 'var(--text-primary)', fontFamily: 'monospace' }}>
                        {line.reference}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>

          {/* Center Pane: Match Action */}
          <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', padding: '1rem 0' }}>
            <div style={{ textAlign: 'center', marginBottom: '1rem' }}>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginBottom: '0.5rem' }}>Select one item from each side</p>
              <ArrowRight size={32} style={{ margin: '0 auto', color: 'var(--border-color)' }} />
            </div>
            <button 
              className="btn btn-primary"
              style={{ 
                padding: '0.75rem 1.5rem', 
                display: 'flex', 
                flexDirection: 'column', 
                alignItems: 'center', 
                gap: '0.25rem', 
                width: '100%', 
                maxWidth: '200px', 
                opacity: (!selectedBankLineId || !selectedTxId || matching) ? 0.5 : 1, 
                cursor: (!selectedBankLineId || !selectedTxId || matching) ? 'not-allowed' : 'pointer' 
              }}
              disabled={!selectedBankLineId || !selectedTxId || matching}
              onClick={handleMatch}
            >
              <ShieldCheck size={24} />
              <span style={{ fontWeight: 700 }}>{matching ? 'Matching...' : 'Match & Reconcile'}</span>
            </button>
          </div>

          {/* Right Pane: System Transactions */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', height: '100%' }}>
            <div className="glass-panel" style={{ padding: '1rem', display: 'flex', flexDirection: 'column', height: '100%', overflow: 'hidden' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
                <h2 style={{ fontSize: '1.1rem', fontWeight: 600, display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                  System Transactions
                </h2>
                <button className="close-button" style={{ color: 'var(--text-secondary)' }} onClick={fetchUnreconciledTransactions} title="Refresh">
                  <RefreshCw size={16} className={loadingTx ? 'spin' : ''} />
                </button>
              </div>

              <div style={{ flex: 1, overflowY: 'auto', paddingRight: '0.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                {loadingTx ? (
                  <div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '2.5rem' }}>Loading...</div>
                ) : transactions.length === 0 ? (
                  <div style={{ textAlign: 'center', color: 'var(--text-muted)', marginTop: '2.5rem' }}>No system transactions found.</div>
                ) : (
                  transactions.map(tx => (
                    <div 
                      key={tx.id} 
                      onClick={() => setSelectedTxId(tx.id)}
                      style={{ 
                        padding: '0.75rem', 
                        border: selectedTxId === tx.id ? '2px solid var(--accent-primary)' : '1px solid var(--border-color)', 
                        borderRadius: '8px', 
                        cursor: 'pointer', 
                        backgroundColor: selectedTxId === tx.id ? 'var(--info-bg)' : 'transparent',
                        transition: 'var(--transition-smooth)'
                      }}
                    >
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: '0.25rem' }}>
                        <div style={{ display: 'flex', flexDirection: 'column' }}>
                          <span style={{ fontSize: '0.875rem', fontWeight: 500, color: 'var(--text-secondary)' }}>{tx.transactionDate}</span>
                          <span style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', marginTop: '0.125rem' }}>{tx.fundAccountName}</span>
                        </div>
                        <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column' }}>
                          <span style={{ fontWeight: 700, color: 'var(--text-primary)' }}>
                            ₹{tx.amount.toFixed(2)}
                          </span>
                          <span style={{ fontSize: '0.625rem', marginTop: '0.25rem', padding: '0.125rem 0.375rem', borderRadius: '4px', textTransform: 'uppercase', fontWeight: 700, alignSelf: 'flex-end', backgroundColor: tx.type === 'RECEIPT' ? 'var(--success-bg)' : tx.type === 'PAYMENT' ? 'var(--danger-bg)' : 'var(--info-bg)', color: tx.type === 'RECEIPT' ? 'var(--success-text)' : tx.type === 'PAYMENT' ? 'var(--danger-text)' : 'var(--info-text)' }}>
                            {tx.type}
                          </span>
                        </div>
                      </div>
                      <div style={{ fontSize: '0.875rem', color: 'var(--text-primary)', marginTop: '0.5rem', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
                        {tx.description || `${tx.referenceDocumentType || 'Manual Entry'} ${tx.referenceDocumentId ? '#' + tx.referenceDocumentId : ''}`}
                      </div>
                    </div>
                  ))
                )}
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
};
