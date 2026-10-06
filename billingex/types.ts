export interface Customer {
  id?: number;
  name: string;
  address: string;
  state: string;
  stateCode: string;
  gstin?: string;
}

export interface InvoiceItem {
  id?: string;
  productName: string;
  hsnCode: string;
  quantity: number;
  rate: number;
  amount: number;
}

export type BillType = 'tax_exclusive' | 'tax_inclusive' | 'delivery_challan';

export interface Invoice {
  id?: number;
  invoiceNo: string;
  invoiceDate: string;
  invoiceTime?: string;
  customerId: number;
  customerName: string;
  customerAddress: string;
  customerGSTIN?: string;
  customerState: string;
  customerStateCode: string;
  vehicleNo?: string;
  billType: BillType;
  items: InvoiceItem[];
  subtotal: number;
  cgst: number;
  sgst: number;
  roundOff: number;
  grandTotal: number;
  totalInWords: string;
  notes: string;
  gstPercentage?: number;
}

export interface CompanyVehicle {
  id?: string;
  name: string;
  vehicleNo: string;
}

export const COMPANY_DETAILS = {
  name: "SENTHUR CHEMICAL",
  address: "3/53 Cinema Kodaikar Street,\nKuruppanaicken Palayam – 638301,\nBhavani TK, Erode DT, Tamil Nadu",
  gstin: "33GUZPS0025L1Z0",
  mobile: "9842737137, 6381664652",
  bankName: "INDIAN BANK",
  branch: "BHAVANI",
  accountNo: "8098727705",
  ifscCode: "IDIB000B078",
  gstPercentage: 18,
  vehicles: [] as CompanyVehicle[]
};

export const PRODUCTS: any[] = [];