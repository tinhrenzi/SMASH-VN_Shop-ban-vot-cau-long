/**
 * SMASH-VN Admin Layout Interaction & Helper Engine
 */

document.addEventListener('DOMContentLoaded', function () {
    // 1. Sidebar Mobile Toggle & Backdrop handling
    const sidebar = document.getElementById('adminSidebar');
    const toggleBtn = document.getElementById('adminSidebarToggle');
    const closeBtn = document.getElementById('adminSidebarClose');
    const backdrop = document.getElementById('adminSidebarBackdrop');

    function openSidebar() {
        if (sidebar) sidebar.classList.add('show');
        if (backdrop) backdrop.classList.add('show');
        document.body.style.overflow = 'hidden';
    }

    function closeSidebar() {
        if (sidebar) sidebar.classList.remove('show');
        if (backdrop) backdrop.classList.remove('show');
        document.body.style.overflow = '';
    }

    if (toggleBtn) {
        toggleBtn.addEventListener('click', function (e) {
            e.preventDefault();
            if (sidebar && sidebar.classList.contains('show')) {
                closeSidebar();
            } else {
                openSidebar();
            }
        });
    }

    if (closeBtn) closeBtn.addEventListener('click', closeSidebar);
    if (backdrop) backdrop.addEventListener('click', closeSidebar);

    // Auto-close mobile sidebar when clicking navigation links
    const navLinks = document.querySelectorAll('.admin-sidebar .admin-nav-link:not([data-bs-toggle])');
    navLinks.forEach(function (link) {
        link.addEventListener('click', function () {
            if (window.innerWidth < 992) {
                closeSidebar();
            }
        });
    });

    // 2. Submenu Collapse Handler for "Bình luận & đánh giá"
    const submenuToggles = document.querySelectorAll('.admin-nav-link[data-submenu-target]');
    submenuToggles.forEach(function (toggle) {
        toggle.addEventListener('click', function (e) {
            e.preventDefault();
            const targetId = this.getAttribute('data-submenu-target');
            const targetSubmenu = document.getElementById(targetId);
            const isExpanded = this.getAttribute('aria-expanded') === 'true';

            if (targetSubmenu) {
                if (isExpanded) {
                    targetSubmenu.classList.remove('show');
                    this.setAttribute('aria-expanded', 'false');
                } else {
                    targetSubmenu.classList.add('show');
                    this.setAttribute('aria-expanded', 'true');
                }
            }
        });
    });

    // Auto-expand submenu if active item inside
    const activeSubmenuLink = document.querySelector('.admin-nav-submenu .admin-nav-link.active');
    if (activeSubmenuLink) {
        const parentSubmenu = activeSubmenuLink.closest('.admin-nav-submenu');
        if (parentSubmenu) {
            parentSubmenu.classList.add('show');
            const toggleBtn = document.querySelector(`.admin-nav-link[data-submenu-target="${parentSubmenu.id}"]`);
            if (toggleBtn) {
                toggleBtn.setAttribute('aria-expanded', 'true');
            }
        }
    }

    // 3. Global Search Engine Handler
    const globalSearch = document.getElementById("adminGlobalSearch");
    if (globalSearch) {
        const urlParams = new URLSearchParams(window.location.search);
        const urlQuery = urlParams.get('q') || urlParams.get('productSearch');
        if (urlQuery) {
            globalSearch.value = urlQuery;
        }

        globalSearch.addEventListener("keypress", function (e) {
            if (e.key === "Enter") {
                executeSearch();
            }
        });

        const searchBtn = document.querySelector(".admin-search-bar .search-btn");
        if (searchBtn) {
            searchBtn.addEventListener("click", executeSearch);
        }
    }

    // 4. Scroll Top Button Handler
    const scrollTopBtn = document.getElementById("topScroll");
    if (scrollTopBtn) {
        window.addEventListener("scroll", function () {
            if (window.scrollY > 300) {
                scrollTopBtn.classList.add("show");
            } else {
                scrollTopBtn.classList.remove("show");
            }
        });

        scrollTopBtn.addEventListener("click", function () {
            window.scrollTo({ top: 0, behavior: "smooth" });
        });
    }

    // 5. Relocate Footer helper if placeholder exists
    const footerPlaceholder = document.querySelector(".admin-footer-placeholder");
    const mainContent = document.querySelector(".admin-content");
    if (footerPlaceholder && mainContent) {
        mainContent.appendChild(footerPlaceholder);
        footerPlaceholder.style.display = "block";
    }
});

// Global Search Execution Function
window.executeSearch = function () {
    const globalSearch = document.getElementById("adminGlobalSearch");
    if (!globalSearch) return;
    const query = globalSearch.value.trim().toLowerCase();

    const localSearch = document.getElementById("productSearchInput");
    if (localSearch) {
        localSearch.value = query;
        localSearch.dispatchEvent(new Event('input', { bubbles: true }));
        return;
    }

    const posSearch = document.getElementById("posSearchInput") || document.querySelector("input[placeholder*='Tìm kiếm sản phẩm']");
    if (posSearch) {
        posSearch.value = query;
        posSearch.dispatchEvent(new Event('input', { bubbles: true }));
        return;
    }

    window.location.href = "/admin/san-pham?q=" + encodeURIComponent(query);
};

// Global Toast Notification Helper
window.showToast = function (message, type) {
    type = type || 'info';
    let container = document.getElementById('custom-toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'custom-toast-container';
        document.body.appendChild(container);
    }

    let iconClass = 'fa-check-circle';
    if (type === 'error' || type === 'danger') {
        iconClass = 'fa-times-circle';
        type = 'error';
    } else if (type === 'warning') {
        iconClass = 'fa-exclamation-triangle';
    } else if (type === 'info') {
        iconClass = 'fa-info-circle';
    }

    const toast = document.createElement('div');
    toast.className = 'custom-toast toast-' + type;
    toast.innerHTML = `
        <div class="custom-toast-content">
            <i class="fas ${iconClass}"></i>
            <span class="custom-toast__message">${message}</span>
        </div>
        <button class="toast-close" type="button"><i class="fas fa-times"></i></button>
    `;

    container.appendChild(toast);
    toast.offsetHeight;
    toast.classList.add('show');

    const dismissTimer = setTimeout(function () {
        dismissToast(toast);
    }, 4000);

    const closeBtn = toast.querySelector('.toast-close');
    if (closeBtn) {
        closeBtn.addEventListener('click', function () {
            clearTimeout(dismissTimer);
            dismissToast(toast);
        });
    }
};

function dismissToast(toast) {
    toast.classList.remove('show');
    setTimeout(function () {
        if (toast.parentNode) {
            toast.parentNode.removeChild(toast);
        }
    }, 400);
}

window.alert = function (message) {
    window.showToast(message, 'warning');
};

// Global SweetAlert2 Form Confirmation Helper
window.confirmSubmitForm = function (formElement, message) {
    if (formElement.dataset.confirmed === "true") {
        return true;
    }
    const msgLower = (message || '').toLowerCase();
    const isDelete = msgLower.includes('xóa') || msgLower.includes('khóa') || msgLower.includes('tạm dừng') || msgLower.includes('ngừng') || msgLower.includes('ẩn');

    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: isDelete ? 'Xác nhận thực hiện' : 'Xác nhận cập nhật',
            text: message || 'Bạn có chắc chắn muốn thực hiện thao tác này?',
            icon: isDelete ? 'warning' : 'question',
            showCancelButton: true,
            confirmButtonColor: isDelete ? '#dc2626' : '#f4511e',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Xác nhận',
            cancelButtonText: 'Hủy bỏ',
            reverseButtons: true
        }).then((result) => {
            if (result.isConfirmed) {
                formElement.dataset.confirmed = "true";
                const submitBtn = formElement.querySelector("button[type='submit']");
                if (submitBtn) {
                    submitBtn.disabled = true;
                    submitBtn.innerHTML = `<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span> Đang xử lý...`;
                }
                formElement.submit();
            }
        });
        return false;
    } else {
        return confirm(message || 'Bạn có chắc chắn muốn thực hiện thao tác này?');
    }
};

// Global SweetAlert2 Action Link Confirmation Helper
window.confirmActionLink = function (event, linkElement, message) {
    if (event) event.preventDefault();
    const href = linkElement.getAttribute('href');
    const msgLower = (message || '').toLowerCase();
    const isDelete = msgLower.includes('xóa') || msgLower.includes('khóa') || msgLower.includes('tạm dừng') || msgLower.includes('ngừng') || msgLower.includes('ẩn');

    if (typeof Swal !== 'undefined') {
        Swal.fire({
            title: 'Xác nhận thực hiện',
            text: message || 'Bạn có chắc chắn muốn thực hiện thao tác này?',
            icon: isDelete ? 'warning' : 'question',
            showCancelButton: true,
            confirmButtonColor: isDelete ? '#dc2626' : '#f4511e',
            cancelButtonColor: '#6c757d',
            confirmButtonText: 'Xác nhận',
            cancelButtonText: 'Hủy bỏ',
            reverseButtons: true
        }).then((result) => {
            if (result.isConfirmed) {
                window.location.href = href;
            }
        });
        return false;
    } else {
        if (confirm(message || 'Bạn có chắc chắn muốn thực hiện thao tác này?')) {
            window.location.href = href;
        }
        return false;
    }
};
