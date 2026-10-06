import React, { useState, useEffect } from 'react';

interface PrinterModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (data: any) => Promise<void>;
  initialData?: any;
}

const DOCUMENT_TYPES = [
  'INVOICE',
  'RECEIPT',
  'PURCHASE_ORDER',
  'REPORT',
  'BARCODE_LABEL',
  'PAYSLIP'
];

export const PrinterModal: React.FC<PrinterModalProps> = ({
  isOpen,
  onClose,
  onSave,
  initialData
}) => {
  const [name, setName] = useState('');
  const [cupsIdentifier, setCupsIdentifier] = useState('');
  const [location, setLocation] = useState('');
  const [isActive, setIsActive] = useState(true);
  const [supportedTypes, setSupportedTypes] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (initialData) {
      setName(initialData.name || '');
      setCupsIdentifier(initialData.cupsIdentifier || '');
      setLocation(initialData.location || '');
      setIsActive(initialData.isActive !== false);
      setSupportedTypes(initialData.supportedDocumentTypes || []);
    } else {
      setName('');
      setCupsIdentifier('');
      setLocation('');
      setIsActive(true);
      setSupportedTypes([]);
    }
  }, [initialData, isOpen]);

  if (!isOpen) return null;

  const toggleDocType = (type: string) => {
    if (supportedTypes.includes(type)) {
      setSupportedTypes(supportedTypes.filter(t => t !== type));
    } else {
      setSupportedTypes([...supportedTypes, type]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      await onSave({
        name,
        cupsIdentifier,
        location,
        isActive,
        supportedDocumentTypes: supportedTypes
      });
      onClose();
    } catch (error) {
      console.error(error);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-lg">
        <div className="px-6 py-4 border-b border-gray-200">
          <h2 className="text-xl font-semibold text-gray-800">
            {initialData ? 'Edit Printer' : 'Register Printer'}
          </h2>
        </div>
        
        <form onSubmit={handleSubmit} className="p-6">
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Printer Name
              </label>
              <input
                type="text"
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500"
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Front Desk Receipt Printer"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Printer Address / Queue Identifier
              </label>
              <input
                type="text"
                required
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500"
                value={cupsIdentifier}
                onChange={(e) => setCupsIdentifier(e.target.value)}
                placeholder="e.g. 192.168.1.100 or EPSON_TM_T88V"
              />
              <p className="text-xs text-gray-500 mt-1">The exact network IP address or printer queue identifier.</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1">
                Location (Optional)
              </label>
              <input
                type="text"
                className="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:ring-blue-500 focus:border-blue-500"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="e.g. Warehouse 1"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Supported Document Types
              </label>
              <div className="grid grid-cols-2 gap-2">
                {DOCUMENT_TYPES.map(type => (
                  <label key={type} className="flex items-center space-x-2">
                    <input
                      type="checkbox"
                      checked={supportedTypes.includes(type)}
                      onChange={() => toggleDocType(type)}
                      className="rounded text-blue-600 focus:ring-blue-500 h-4 w-4"
                    />
                    <span className="text-sm text-gray-700">{type}</span>
                  </label>
                ))}
              </div>
            </div>

            <div className="flex items-center space-x-2 pt-2">
              <input
                type="checkbox"
                id="isActive"
                checked={isActive}
                onChange={(e) => setIsActive(e.target.checked)}
                className="rounded text-blue-600 focus:ring-blue-500 h-4 w-4"
              />
              <label htmlFor="isActive" className="text-sm font-medium text-gray-700">
                Printer is Active
              </label>
            </div>
          </div>

          <div className="mt-6 flex justify-end space-x-3">
            <button
              type="button"
              onClick={onClose}
              disabled={isSubmitting}
              className="px-4 py-2 border border-gray-300 rounded-md text-gray-700 hover:bg-gray-50 focus:outline-none"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || supportedTypes.length === 0}
              className="px-4 py-2 bg-blue-600 text-white rounded-md hover:bg-blue-700 focus:outline-none disabled:bg-blue-300"
            >
              {isSubmitting ? 'Saving...' : 'Save Printer'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
