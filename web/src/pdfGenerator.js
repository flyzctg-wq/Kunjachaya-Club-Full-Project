import { jsPDF } from 'jspdf';

export function generatePdfReceipt(record, user, lang = 'en') {
  const doc = new jsPDF({
    orientation: 'portrait',
    unit: 'mm',
    format: 'a4'
  });

  // Header Banner
  doc.setFillColor(26, 54, 93); // #1a365d Primary Navy
  doc.rect(0, 0, 210, 42, 'F');

  // Title
  doc.setTextColor(255, 255, 255);
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(22);
  doc.text('KUNJACHAYA CLUB', 105, 18, { align: 'center' });
  
  doc.setFont('helvetica', 'normal');
  doc.setFontSize(11);
  doc.text('Official Digital Dues & Maintenance Money Receipt', 105, 26, { align: 'center' });
  doc.setFontSize(9);
  doc.text('Kunjachaya Club', 105, 33, { align: 'center' });

  // Receipt Details Box
  doc.setFillColor(248, 250, 252); // #f8fafc
  doc.setDrawColor(226, 232, 240); // #e2e8f0
  doc.roundedRect(15, 50, 180, 110, 3, 3, 'FD');

  doc.setTextColor(30, 41, 59); // Slate-800
  doc.setFont('helvetica', 'bold');
  doc.setFontSize(14);
  doc.text('RECEIPT ACKNOWLEDGEMENT', 20, 62);

  doc.setLineWidth(0.5);
  doc.setDrawColor(212, 175, 55); // Gold
  doc.line(20, 65, 190, 65);

  doc.setFont('helvetica', 'normal');
  doc.setFontSize(10);
  
  let y = 75;
  const addRow = (label, value) => {
    doc.setFont('helvetica', 'bold');
    doc.setTextColor(100, 116, 139);
    doc.text(label, 20, y);
    doc.setFont('helvetica', 'normal');
    doc.setTextColor(15, 23, 42);
    doc.text(String(value), 80, y);
    y += 10;
  };

  addRow('Receipt Voucher No:', record.transactionId || record._docId || record.id);
  addRow('Resident Name:', (lang === 'bn' ? user?.nameBn : user?.nameEn) || user?.phone || 'N/A');
  addRow('Holding / Address:', [user?.holding, user?.road, user?.block].filter(Boolean).join(', ') || 'N/A');
  addRow('Billing Period / Item:', (record.monthYear || '') + ' (' + record.titleEn + ')');
  addRow('Amount Received:', `BDT ${(record.amount || 0).toLocaleString()} Taka Only`);
  addRow('Payment Status:', record.status);
  addRow('Payment Date:', record.date || 'N/A');
  addRow('Payment Gateway:', record.paymentGateway || 'N/A');
  addRow('Transaction Reference:', record.transactionId || 'N/A');

  // Footer / Seal Area
  doc.setFillColor(241, 245, 249);
  doc.rect(15, 170, 180, 25, 'F');
  doc.setFontSize(9);
  doc.setTextColor(71, 85, 105);
  doc.text('This is an electronically generated receipt verified by Kunjachaya Club Executive System.', 105, 180, { align: 'center' });
  doc.text('No physical signature required. Thank you for your timely contribution.', 105, 186, { align: 'center' });

  // Save PDF
  doc.save(`Kunjachaya_Receipt_${record.transactionId || record._docId || record.id}.pdf`);
}
