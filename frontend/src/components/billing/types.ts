export interface Customer {
  id?: number;
  name: string;
  address: string;
  state: string;
  stateCode: string;
  gstin?: string;
}

export interface InvoiceItem {
  id?: string | number;
  productName: string;
  dcNo?: string;
  hsnCode: string;
  quantity: number;
  rate: number;
  amount: number;
  sNo?: number;
}

export type BillType = 'tax_exclusive' | 'tax_inclusive' | 'delivery_challan' | string;

export interface TaxComponent {
  name: string;
  rate: number;
}

export interface TaxLine {
  name: string;
  rate: number;
  amount: number;
}

export interface Invoice {
  id?: number;
  invoiceNo: string;
  invoiceDate: string;
  invoiceTime?: string;
  customerId?: number;
  customerName: string;
  customerAddress?: string;
  customerGSTIN?: string;
  customerState?: string;
  customerStateCode?: string;
  vehicleNo?: string;
  billType: BillType;
  items: InvoiceItem[];
  subtotal: number;
  taxLines?: TaxLine[];
  cgst: number;
  sgst: number;
  igst: number;
  roundOff: number;
  grandTotal: number;
  totalInWords: string;
  notes: string;
  taxLabel?: string;
  gstPercentage?: number;
  cgstRate?: number;
  sgstRate?: number;
  igstRate?: number;
  date?: string;
  time?: string;
  taxCategory?: string;
  paymentMethod?: string;
  subTotal?: number;
  cgstAmount?: number;
  sgstAmount?: number;
  igstAmount?: number;
  totalAmount?: number;
}

export interface CompanyVehicle {
  id?: string;
  name: string;
  vehicleNo: string;
}

export interface TaxRate {
  id: string;
  label: string;
  rate: number;
  cgstRate?: number;
  sgstRate?: number;
  igstRate?: number;
  components?: TaxComponent[];
}

export const COMPANY_DETAILS = {
  name: "SENTHUR CHEMICAL",
  legalName: "",
  address: "3/53 Cinema Kodaikar Street,\nKuruppanaicken Palayam – 638301,\nBhavani TK, Erode DT, Tamil Nadu",
  city: "Bhavani",
  state: "Tamil Nadu",
  stateCode: "33",
  pincode: "638301",
  country: "India",
  phone: "",
  mobile: "9842737137, 6381664652",
  email: "",
  website: "",
  gstin: "33GUZPS0025L1Z0",
  pan: "",
  bankName: "INDIAN BANK",
  branch: "BHAVANI",
  accountNo: "8098727705",
  ifscCode: "IDIB000B078",
  taxRates: [
    { id: 'gst18', label: 'GST 18% (CGST 9% + SGST 9%)', rate: 18, cgstRate: 9, sgstRate: 9, igstRate: 0 },
    { id: 'gst12', label: 'GST 12% (CGST 6% + SGST 6%)', rate: 12, cgstRate: 6, sgstRate: 6, igstRate: 0 },
    { id: 'gst5', label: 'GST 5% (CGST 2.5% + SGST 2.5%)', rate: 5, cgstRate: 2.5, sgstRate: 2.5, igstRate: 0 },
    { id: 'gst0', label: 'GST 0%', rate: 0, cgstRate: 0, sgstRate: 0, igstRate: 0 },
    { id: 'igst18', label: 'IGST 18% (Inter-state 18%)', rate: 18, cgstRate: 0, sgstRate: 0, igstRate: 18 },
    { id: 'igst12', label: 'IGST 12% (Inter-state 12%)', rate: 12, cgstRate: 0, sgstRate: 0, igstRate: 12 },
    { id: 'igst5', label: 'IGST 5% (Inter-state 5%)', rate: 5, cgstRate: 0, sgstRate: 0, igstRate: 5 }
  ] as TaxRate[],
  vehicles: [] as CompanyVehicle[]
};

export const PRODUCTS: any[] = [];
