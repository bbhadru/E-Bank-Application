// Chart.js Dashboard Visualizations
document.addEventListener('DOMContentLoaded', function () {
    const depositTrendCanvas = document.getElementById('depositTrendChart');
    const loanRecoveryCanvas = document.getElementById('loanRecoveryChart');

    if (depositTrendCanvas && loanRecoveryCanvas) {
        fetch('/api/dashboard/chart-data')
            .then(response => response.json())
            .then(data => {
                // Deposit Trend Chart
                new Chart(depositTrendCanvas.getContext('2d'), {
                    type: 'line',
                    data: {
                        labels: data.labels,
                        datasets: [{
                            label: 'Deposits (INR)',
                            data: data.deposits,
                            borderColor: '#2563eb',
                            backgroundColor: 'rgba(37, 99, 235, 0.1)',
                            fill: true,
                            tension: 0.3,
                            borderWidth: 2,
                            pointBackgroundColor: '#2563eb'
                        }]
                    },
                    options: {
                        responsive: true,
                        plugins: {
                            legend: { display: false }
                        },
                        scales: {
                            y: {
                                beginAtZero: true,
                                grid: { color: '#f1f5f9' }
                            },
                            x: {
                                grid: { display: false }
                            }
                        }
                    }
                });

                // Loan Recovery Chart
                new Chart(loanRecoveryCanvas.getContext('2d'), {
                    type: 'bar',
                    data: {
                        labels: data.labels,
                        datasets: [{
                            label: 'Loan Recovery (INR)',
                            data: data.loanRecovery,
                            backgroundColor: '#10b981',
                            borderRadius: 6
                        }]
                    },
                    options: {
                        responsive: true,
                        plugins: {
                            legend: { display: false }
                        },
                        scales: {
                            y: {
                                beginAtZero: true,
                                grid: { color: '#f1f5f9' }
                            },
                            x: {
                                grid: { display: false }
                            }
                        }
                    }
                });
            })
            .catch(error => console.error('Error fetching dashboard chart data:', error));
    }
});
