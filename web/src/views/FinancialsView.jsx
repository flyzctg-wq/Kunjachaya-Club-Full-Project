import React, { useState, useEffect, useRef } from 'react';
import Icon from '../components/Icon';
import { httpsCallable } from 'firebase/functions';
import { doc, onSnapshot } from 'firebase/firestore';
import { translations } from '../translations';
import { generatePdfReceipt } from '../pdfGenerator';
import { db, functions } from '../firebase';

export default function FinancialsView({ lang, currentUser, financials }) {
  const t = translations[lang];

  const [selectedRecord, setSelectedRecord] = useState(null);
  const [walletPhone, setWalletPhone] = useState(currentUser?.primaryContact || currentUser?.phone || '');
  const [isCreatingCheckout, setIsCreatingCheckout] = useState(false);
  const [checkoutError, setCheckoutError] = useState('');
  const [activeOrderId, setActiveOrderId] = useState(null);
  const [orderStatus, setOrderStatus] = useState(null);
  const orderUnsubRef = useRef(null);

  const pendingDues = financials.filter(f => f.type === 'Due' && f.status !== 'Completed');
  const outstandingTotal = pendingDues.reduce((sum, f) => sum + (f.amount || 0), 0);
  const nextDue = pendingDues[0];

  const handleOpenModal = (record) => {
    setSelectedRecord(record);
    setWalletPhone(currentUser?.primaryContact || currentUser?.phone || '');
    setCheckoutError('');
    setActiveOrderId(null);
    setOrderStatus(null);
  };

  const handleCloseModal = () => {
    if (orderUnsubRef.current) orderUnsubRef.current();
    setSelectedRecord(null);
    setActiveOrderId(null);
    setOrderStatus(null);
  };

  const handleStartCheckout = async (e) => {
    e.preventDefault();
    if (!selectedRecord || !currentUser) return;
    setIsCreatingCheckout(true);
    setCheckoutError('');
    try {
      const createCheckout = httpsCallable(functions, 'createPipraPayCheckout');
      const result = await createCheckout({
        amount: selectedRecord.amount,
        title: selectedRecord.titleEn,
        userId: currentUser.id,
        recordId: selectedRecord._docId || '',
        customerPhone: walletPhone,
        customerEmail: currentUser.primaryContact || '',
      });

      const { orderId, checkoutUrl } = result.data || {};
      if (!orderId || !checkoutUrl) {
        throw new Error('Payment gateway did not return a valid checkout session.');
      }

      setActiveOrderId(orderId);
      window.open(checkoutUrl, '_blank', 'noopener,noreferrer');

      if (orderUnsubRef.current) orderUnsubRef.current();
      orderUnsubRef.current = onSnapshot(doc(db, 'orders', orderId), (snap) => {
        if (snap.exists()) setOrderStatus(snap.data().status);
      });
    } catch (err) {
      setCheckoutError(err?.message || 'Could not start checkout. Please try again.');
    } finally {
      setIsCreatingCheckout(false);
    }
  };

  useEffect(() => {
    if (orderStatus === 'completed') {
      const timer = setTimeout(() => handleCloseModal(), 1200);
      return () => clearTimeout(timer);
    }
  }, [orderStatus]);

  useEffect(() => () => { if (orderUnsubRef.current) orderUnsubRef.current(); }, []);

  return (
    <>
      {/* Summary Hero */}
      <section className="relative group mb-lg">
        <div className="absolute -inset-1 bg-gradient-to-r from-primary/20 to-tertiary/20 rounded-[32px] blur opacity-25" />
        <div className="relative bg-surface-container-lowest border border-outline-variant/30 rounded-[24px] p-lg shadow-sm overflow-hidden">
          <div className="flex justify-between items-start">
            <div>
              <p className="font-label-lg text-on-surface-variant mb-1">Outstanding Dues</p>
              <h2 className="font-display text-[40px] font-extrabold text-on-surface tracking-tight">
                ৳ {outstandingTotal.toLocaleString()}
              </h2>
              {nextDue && (
                <p className="font-body-md text-error flex items-center gap-1 mt-1">
                  <Icon name="schedule" className="text-sm" />
                  {nextDue.monthYear}
                </p>
              )}
            </div>
            <div className="w-16 h-16 rounded-full bg-secondary-container/50 flex items-center justify-center">
              <Icon name="account_balance_wallet" className="text-on-secondary-container text-3xl" />
            </div>
          </div>
          {nextDue && (
            <div className="mt-8 flex gap-3">
              <button
                onClick={() => handleOpenModal(nextDue)}
                className="flex-1 bg-primary-container text-on-primary-container h-14 rounded-full font-label-lg flex items-center justify-center gap-2 active:scale-95 transition-transform"
              >
                <Icon name="payments" />
                {t.payNow}
              </button>
            </div>
          )}
        </div>
      </section>

      {/* Ledger List */}
      <section className="space-y-4">
        <h3 className="font-title-lg text-title-lg text-on-surface">Billing Records</h3>

        {financials.map((item) => {
          const isPaid = item.status === 'Completed';
          return (
            <div
              key={item._docId || item.id}
              className={`p-md rounded-xl card-shadow border flex items-center justify-between gap-3 ${
                isPaid ? 'bg-surface-container-lowest border-outline-variant/20' : 'bg-white dark:bg-surface-container border-error/30'
              }`}
            >
              <div className="min-w-0">
                <p className="font-label-sm text-on-surface-variant uppercase tracking-wide">{item.monthYear}</p>
                <p className="font-title-lg text-on-surface truncate">{lang === 'bn' ? item.titleBn : item.titleEn}</p>
                <p className="font-body-md text-on-surface-variant mt-1">৳{(item.amount || 0).toLocaleString()} • {item.date}</p>
                {isPaid && item.transactionId && (
                  <p className="text-[11px] text-on-surface-variant/70 font-mono mt-1">Ref: {item.transactionId}</p>
                )}
              </div>

              {isPaid ? (
                <button
                  onClick={() => generatePdfReceipt(item, currentUser, lang)}
                  className="shrink-0 w-11 h-11 rounded-full bg-secondary-container text-on-secondary-container flex items-center justify-center active:scale-90 transition-transform"
                  title={t.downloadReceipt}
                >
                  <Icon name="download" />
                </button>
              ) : (
                <button
                  onClick={() => handleOpenModal(item)}
                  className="shrink-0 px-4 py-2.5 rounded-full bg-error text-on-error font-label-sm active:scale-95 transition-transform"
                >
                  {t.payNow}
                </button>
              )}
            </div>
          );
        })}

        {financials.length === 0 && (
          <div className="text-center py-16 text-on-surface-variant">
            <Icon name="receipt_long" className="text-4xl mb-2 opacity-40" />
            <p>No billing records yet.</p>
          </div>
        )}
      </section>

      {/* PipraPay Checkout Modal */}
      {selectedRecord && (
        <div className="fixed inset-0 z-50 bg-inverse-surface/70 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="bg-surface-container-lowest w-full max-w-md rounded-[24px] p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between pb-3 border-b border-outline-variant/30">
              <div className="flex items-center gap-2 font-title-lg text-title-lg text-on-surface">
                <Icon name="lock" className="text-primary" />
                <span>PipraPay Checkout</span>
              </div>
              <button onClick={handleCloseModal} className="p-1 text-on-surface-variant hover:text-on-surface">
                <Icon name="close" />
              </button>
            </div>

            <div className="p-3 bg-inverse-surface text-inverse-on-surface rounded-xl flex items-center justify-between">
              <div>
                <p className="text-[11px] text-white/60 font-mono">{selectedRecord.monthYear}</p>
                <p className="text-xs font-bold truncate max-w-[200px]">{selectedRecord.titleEn}</p>
              </div>
              <div className="text-right">
                <p className="text-[10px] text-primary-fixed uppercase font-bold">Amount</p>
                <p className="text-lg font-black">BDT {(selectedRecord.amount || 0).toLocaleString()}</p>
              </div>
            </div>

            {!activeOrderId ? (
              <form onSubmit={handleStartCheckout} className="space-y-4">
                <div className="space-y-xs">
                  <label className="font-label-lg text-on-surface-variant px-1">Wallet / Account Mobile Number</label>
                  <input
                    type="text"
                    required
                    value={walletPhone}
                    onChange={(e) => setWalletPhone(e.target.value)}
                    placeholder="e.g. 01XXXXXXXXX"
                    className="w-full px-4 py-3 rounded-xl bg-surface-container border-none focus:ring-2 focus:ring-primary-container text-body-md font-mono"
                  />
                </div>

                {checkoutError && (
                  <div className="p-3 bg-error-container rounded-xl text-xs text-on-error-container flex items-start gap-2">
                    <Icon name="warning" className="text-sm shrink-0 mt-0.5" />
                    <span>{checkoutError}</span>
                  </div>
                )}

                <button
                  type="submit"
                  disabled={isCreatingCheckout}
                  className="w-full py-4 bg-primary text-white font-label-lg rounded-full shadow-lg flex items-center justify-center gap-2 active:scale-95 transition-all disabled:opacity-60"
                >
                  {isCreatingCheckout ? <Icon name="progress_activity" className="animate-spin" /> : <Icon name="open_in_new" />}
                  <span>Open PipraPay Checkout</span>
                </button>
              </form>
            ) : (
              <div className="space-y-3 py-2">
                <div className="p-4 bg-inverse-surface text-inverse-on-surface rounded-xl space-y-2 font-mono text-xs">
                  <p className="text-primary-fixed font-bold flex items-center gap-2">
                    <Icon name="progress_activity" className="animate-spin" />
                    Waiting for PipraPay confirmation...
                  </p>
                  <p className="text-white/60">Order: {activeOrderId}</p>
                  <p className="text-white/60">Status: {orderStatus || 'pending'}</p>
                </div>
                {orderStatus === 'completed' && (
                  <div className="p-3 bg-primary text-white rounded-xl font-bold text-center text-xs flex items-center justify-center gap-2">
                    <Icon name="verified" filled />
                    {t.paymentSuccess}
                  </div>
                )}
                {orderStatus === 'failed' && (
                  <div className="p-3 bg-error text-on-error rounded-xl font-bold text-center text-xs">
                    Payment failed or was cancelled.
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      )}
    </>
  );
}
