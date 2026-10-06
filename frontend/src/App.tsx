import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { AppShell } from './components/layout/AppShell';
import { AuthProvider } from './context/AuthContext';
import { ProtectedRoute } from './components/security/ProtectedRoute';
import {
  Dashboard,
  Customers,
  Suppliers,
  Products,
  Billing,
  Sales,
  Purchases,
  Funds,
  Employees,
  Reports,
  Backups,
  Admin,
  CustomerStatement,
  SupplierStatement,
  Login,
  FirstRunSetup
} from './pages';
import { ToastContainer } from './components/common/ToastContainer';

import { ThemeProvider } from './context/ThemeContext';

function App() {
  return (
    <ThemeProvider>
      <AuthProvider>
        <BrowserRouter>
          <ToastContainer />
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/setup" element={<FirstRunSetup />} />
          
          
          <Route path="/" element={<ProtectedRoute><AppShell /></ProtectedRoute>}>
            <Route index element={<Dashboard />} />
            
            <Route path="customers" element={
              <ProtectedRoute requiredPermission="CUSTOMER_READ"><Customers /></ProtectedRoute>
            } />
            <Route path="customers/statement" element={
              <ProtectedRoute requiredPermission="CUSTOMER_READ"><CustomerStatement /></ProtectedRoute>
            } />
            <Route path="customers/:customerId/statement" element={
              <ProtectedRoute requiredPermission="CUSTOMER_READ"><CustomerStatement /></ProtectedRoute>
            } />
            <Route path="suppliers" element={
              <ProtectedRoute requiredPermission="SUPPLIER_READ"><Suppliers /></ProtectedRoute>
            } />
            <Route path="suppliers/statement" element={
              <ProtectedRoute requiredPermission="SUPPLIER_READ"><SupplierStatement /></ProtectedRoute>
            } />
            <Route path="suppliers/:supplierId/statement" element={
              <ProtectedRoute requiredPermission="SUPPLIER_READ"><SupplierStatement /></ProtectedRoute>
            } />
            <Route path="products" element={
              <ProtectedRoute requiredPermission="PRODUCT_READ"><Products /></ProtectedRoute>
            } />
            <Route path="billing" element={
              <ProtectedRoute requiredPermission="BILLING_READ"><Billing /></ProtectedRoute>
            } />
            <Route path="sales" element={
              <ProtectedRoute requiredPermission="BILLING_READ"><Sales /></ProtectedRoute>
            } />
            <Route path="purchases" element={
              <ProtectedRoute requiredPermission="PURCHASE_READ"><Purchases /></ProtectedRoute>
            } />
            <Route path="funds" element={
              <ProtectedRoute requiredPermission="FUND_READ"><Funds /></ProtectedRoute>
            } />
            <Route path="employees" element={
              <ProtectedRoute requiredPermission="EMPLOYEE_READ"><Employees /></ProtectedRoute>
            } />
            <Route path="reports" element={
              <ProtectedRoute requiredPermission="SYSTEM_READ"><Reports /></ProtectedRoute>
            } />

            <Route path="backups" element={
              <ProtectedRoute requiredPermission="SYSTEM_READ"><Backups /></ProtectedRoute>
            } />
            <Route path="admin" element={
              <ProtectedRoute requiredPermission="SYSTEM_WRITE"><Admin /></ProtectedRoute>
            } />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  </ThemeProvider>
  );
}

export default App;
