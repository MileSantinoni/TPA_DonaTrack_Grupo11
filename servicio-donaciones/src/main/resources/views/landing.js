function showRegistrationPopup() {
    const popup = document.getElementById('registrationPopup');
    popup.style.display = 'flex';
    document.body.style.overflow = 'hidden'; // Prevent background scrolling
}

function hideRegistrationPopup() {
    const popup = document.getElementById('registrationPopup');
    popup.style.display = 'none';
    document.body.style.overflow = ''; // Restore scrolling
}

function switchTab(tabName) {
    // Hide all sections
    document.getElementById('humanaSection').style.display = 'none';
    document.getElementById('juridicaSection').style.display = 'none';
    
    // Remove active class from all tabs
    const tabs = document.querySelectorAll('.popup-tab');
    tabs.forEach(tab => tab.classList.remove('active'));
    
    // Show selected section and activate tab
    if (tabName === 'humana') {
        document.getElementById('humanaSection').style.display = 'block';
        document.querySelector('.popup-tab[onclick="switchTab(\'humana\')"]').classList.add('active');
    } else {
        document.getElementById('juridicaSection').style.display = 'block';
        document.querySelector('.popup-tab[onclick="switchTab(\'juridica\')"]').classList.add('active');
    }
}

function submitRegistration(tipo) {
    const form = document.getElementById('registrationForm');
    const formData = new FormData(form);
    
    // Add tipo parameter
    formData.set('tipo', tipo);
    
    // Determine endpoint based on tab
    const endpoint = tipo === 'humana' ? '/registro/donante' : '/registro/donante/juridica';
    
    fetch(endpoint, {
        method: 'POST',
        body: formData
    })
    .then(response => response.json())
    .then(data => {
        if (data.success) {
            showMessage('success', data.message || 'Registro exitoso');
            setTimeout(() => {
                hideRegistrationPopup();
                form.reset();
                // Reset tabs to persona humana
                switchTab('humana');
            }, 1500);
        } else {
            showMessage('error', data.message || 'Error en el registro');
        }
    })
    .catch(error => {
        showMessage('error', 'Error de conexión con el servidor');
        console.error('Error:', error);
    });
}

function showMessage(type, message) {
    // Remove any existing message
    const existing = document.querySelector('.popup-message');
    if (existing) {
        existing.remove();
    }
    
    const msgDiv = document.createElement('div');
    msgDiv.className = `popup-message ${type}`;
    msgDiv.textContent = message;
    
    // Style the message
    msgDiv.style.position = 'fixed';
    msgDiv.style.top = '20px';
    msgDiv.style.left = '50%';
    msgDiv.style.transform = 'translateX(-50%)';
    msgDiv.style.padding = '12px 24px';
    msgDiv.style.background = type === 'success' ? 'var(--surface-white)' : 'rgba(255, 95, 87, 0.95)';
    msgDiv.style.color = type === 'success' ? 'var(--brand-marron)' : 'white';
    msgDiv.style.borderRadius = '8px';
    msgDiv.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.15)';
    msgDiv.style.zIndex = '1001';
    msgDiv.style.fontSize = '14px';
    
    document.body.appendChild(msgDiv);
    
    // Auto remove after 3 seconds
    setTimeout(() => {
        msgDiv.style.transition = 'opacity 0.3s';
        msgDiv.style.opacity = '0';
        setTimeout(() => msgDiv.remove(), 300);
    }, 3000);
}