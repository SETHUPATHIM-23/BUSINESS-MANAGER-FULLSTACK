import React, { useState } from 'react';
import { AlertTriangle, RotateCcw } from 'lucide-react';

interface RestoreModalProps {
  isOpen: boolean;
  onClose: () => void;
  onConfirm: () => void;
  backup: any | null;
  restoring: boolean;
}

export const RestoreModal: React.FC<RestoreModalProps> = ({ isOpen, onClose, onConfirm, backup, restoring }) => {
  const [confirmationText, setConfirmationText] = useState('');

  if (!isOpen || !backup) return null;

  const handleConfirm = () => {
    if (confirmationText === 'RESTORE') {
      onConfirm();
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm">
      <div className="glass-panel w-full max-w-md p-6 animate-slide-up border border-red-500/30">
        <div className="flex items-center gap-3 text-red-500 mb-4">
          <AlertTriangle size={28} />
          <h2 className="text-xl font-bold">Restore Database</h2>
        </div>
        
        <div className="bg-red-500/10 border border-red-500/20 p-4 rounded-lg mb-6 text-sm text-red-100">
          <p className="font-semibold mb-2">WARNING: DESTRUCTIVE ACTION</p>
          <p>
            You are about to restore the database from the backup taken on 
            <strong className="text-white ml-1">{new Date(backup.createdAt).toLocaleString()}</strong>.
          </p>
          <p className="mt-2">
            This will completely overwrite the current live database. All data created or modified after this backup timestamp will be permanently lost. This action cannot be undone.
          </p>
        </div>

        <div className="mb-6">
          <label className="block text-sm text-secondary mb-2">
            To proceed, please type the word <strong>RESTORE</strong> below:
          </label>
          <input
            type="text"
            className="form-input w-full uppercase"
            value={confirmationText}
            onChange={(e) => setConfirmationText(e.target.value)}
            placeholder="RESTORE"
            disabled={restoring}
          />
        </div>

        <div className="flex justify-end gap-3">
          <button 
            className="btn-secondary" 
            onClick={() => {
              setConfirmationText('');
              onClose();
            }} 
            disabled={restoring}
          >
            Cancel
          </button>
          <button 
            className={`flex items-center gap-2 px-4 py-2 rounded font-medium transition-colors ${
              confirmationText === 'RESTORE' && !restoring
                ? 'bg-red-600 hover:bg-red-700 text-white' 
                : 'bg-gray-700 text-gray-400 cursor-not-allowed'
            }`}
            onClick={handleConfirm}
            disabled={confirmationText !== 'RESTORE' || restoring}
          >
            {restoring ? (
              <>
                <svg className="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                Restoring...
              </>
            ) : (
              <>
                <RotateCcw size={16} /> Confirm Restore
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  );
};
