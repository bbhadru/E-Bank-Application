// Client-side Validation Script
document.addEventListener('DOMContentLoaded', function () {
    const forms = document.querySelectorAll('.needs-validation');

    Array.prototype.slice.call(forms).forEach(function (form) {
        form.addEventListener('submit', function (event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        }, false);
    });

    // Mobile Number Validator (10-15 digits)
    const mobileInputs = document.querySelectorAll('input[type="tel"], input[name="mobile"]');
    mobileInputs.forEach(function (input) {
        input.addEventListener('input', function () {
            this.value = this.value.replace(/[^0-9]/g, '');
        });
    });

    // Currency/Amount Validator (Positive numbers only)
    const amountInputs = document.querySelectorAll('input[name="amount"], input[name="principal"], input[name="monthlyAmount"]');
    amountInputs.forEach(function (input) {
        input.addEventListener('input', function () {
            if (parseFloat(this.value) < 0) {
                this.value = '0';
            }
        });
    });
});
