const fs = require('fs');
let code = fs.readFileSync('e:\\\\ENPT\\\\BusinessManagerEnterprise\\\\frontend\\\\src\\\\pages\\\\Trucks.tsx', 'utf8');

// 1. Replace imports
const importTarget = `import {
  Truck as TruckIcon, Plus, Search, Edit, Trash2, ChevronLeft, ChevronRight,
  SlidersHorizontal, AlertCircle, RefreshCw, X, Eye, Wrench, DollarSign,
  CheckCircle, Clock, Calendar, User, Package, List, AlertTriangle
} from 'lucide-react';

import { TruckFormModal, TruckFormData } from '../components/trucks/TruckFormModal';`;

const importReplacement = `import {
  Truck as TruckIcon, Plus, Search, Edit, Trash2, ChevronLeft, ChevronRight,
  SlidersHorizontal, AlertCircle, RefreshCw, X, Eye, Wrench, DollarSign,
  CheckCircle, Clock, Calendar, User, Package, List, AlertTriangle, Grid
} from 'lucide-react';

import { TruckFormModal, TruckFormData } from '../components/trucks/TruckFormModal';
import { DeliveryKanbanBoard } from '../components/trucks/DeliveryKanbanBoard';
import { DeliveryAssignmentModal } from '../components/trucks/DeliveryAssignmentModal';
import { MaintenanceLogModal } from '../components/trucks/MaintenanceLogModal';`;

code = code.replace(importTarget, importReplacement);

// 2. Replace Kanban Board
const kanbanStartStr = '          {/* KANBAN BOARD VIEW */}';
const kanbanEndStr = '          )}';

const kanbanStart = code.indexOf(kanbanStartStr);
// The first ')}' after the kanban board start is the end of the ternary expression for viewMode
const kanbanEnd = code.indexOf(kanbanEndStr, kanbanStart) + kanbanEndStr.length;

if (kanbanStart !== -1 && kanbanEnd !== -1) {
    const beforeKanban = code.substring(0, kanbanStart);
    const afterKanban = code.substring(kanbanEnd);

    const kanbanReplacement = `          {/* KANBAN BOARD VIEW */}
          <DeliveryKanbanBoard
            assignments={assignments}
            loading={assignLoading}
            viewMode={assignViewMode}
            hasWriteAccess={hasWriteAccess}
            onUpdateStatus={handleUpdateAssignStatus}
            onDelete={handleDeleteAssignment}
          />`;

    code = beforeKanban + kanbanReplacement + afterKanban;
} else {
    console.error('Could not find kanban board section');
    process.exit(1);
}

// 3. Replace Modals
const modalTarget = `      {/* ── MODAL: POST VEHICLE EXPENSE TO GL ─────────────────────────────── */}`;
const modalReplacement = `      {/* ── MODAL: DELIVERY ASSIGNMENT ───────────────────────────────────── */}
      <DeliveryAssignmentModal
        isOpen={isAssignModalOpen}
        trucks={trucks}
        invoices={allInvoices}
        isSubmitting={submittingAssign}
        formAssignTruckId={formAssignTruckId}
        setFormAssignTruckId={setFormAssignTruckId}
        formAssignInvoiceId={formAssignInvoiceId}
        setFormAssignInvoiceId={setFormAssignInvoiceId}
        formAssignStatus={formAssignStatus}
        setFormAssignStatus={setFormAssignStatus}
        onClose={() => setIsAssignModalOpen(false)}
        onSubmit={handleSaveAssignment}
      />

      {/* ── MODAL: MAINTENANCE LOG ───────────────────────────────────────── */}
      <MaintenanceLogModal
        isOpen={isLogModalOpen}
        trucks={trucks}
        isSubmitting={submittingLog}
        formLogTruckId={formLogTruckId}
        setFormLogTruckId={setFormLogTruckId}
        formLogDate={formLogDate}
        setFormLogDate={setFormLogDate}
        formLogType={formLogType}
        setFormLogType={setFormLogType}
        formLogCost={formLogCost}
        setFormLogCost={setFormLogCost}
        formLogOdometer={formLogOdometer}
        setFormLogOdometer={setFormLogOdometer}
        onClose={() => setIsLogModalOpen(false)}
        onSubmit={handleSaveLog}
      />

      {/* ── MODAL: POST VEHICLE EXPENSE TO GL ─────────────────────────────── */}`;

code = code.replace(modalTarget, modalReplacement);

fs.writeFileSync('e:\\\\ENPT\\\\BusinessManagerEnterprise\\\\frontend\\\\src\\\\pages\\\\Trucks.tsx', code);
console.log('Success');
