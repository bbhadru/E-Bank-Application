// Main Application JavaScript
document.addEventListener('DOMContentLoaded', function () {
    // Auto-dismiss alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert:not(.alert-permanent)');
    alerts.forEach(function (alert) {
        setTimeout(function () {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });

    // Populate transaction modal account ID dynamically
    const txnModal = document.getElementById('transactionModal');
    if (txnModal) {
        txnModal.addEventListener('show.bs.modal', function (event) {
            const button = event.relatedTarget;
            const accountId = button.getAttribute('data-account-id');
            const accountNo = button.getAttribute('data-account-number');
            const txnType = button.getAttribute('data-txn-type');

            const modalAccountId = txnModal.querySelector('#modalAccountId');
            const modalTitle = txnModal.querySelector('#modalTitle');
            const modalForm = txnModal.querySelector('#modalForm');

            if (modalAccountId) modalAccountId.value = accountId;
            if (modalTitle) modalTitle.textContent = txnType + ' - ' + accountNo;
            if (modalForm) {
                modalForm.action = txnType === 'Deposit' ? '/accounts/savings/deposit' : '/accounts/savings/withdraw';
            }
        });
    }
});

function formatCurrency(amount) {
    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR'
    }).format(amount);
}
