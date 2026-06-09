/**
 * This is main script file that contains JS code.
 */
(function ($) {
    // Global jQuery AJAX CSRF configuration
    $(document).ajaxSend(function(event, xhr, options) {
        const token = $('#csrf-token-holder').data('csrf-token');
        const header = $('#csrf-token-holder').data('csrf-header');
        if (token && header) {
            xhr.setRequestHeader(header, token);
        }
    });

    // Main Object
    var RESHOP = {};

    
    

    

    // Predefined variables
    var
        $filterGridWrapper = $('.filter__grid-wrapper'),
        $collectionOfFilterBtn = $('.filter__btn'),
        $primarySlider = $('#hero-slider'),
        $testimonialSlider = $('#testimonial-slider'),
        $collectionaClickScroll = $('[data-click-scroll]'),
        $collectionProductSlider = $('.product-slider'),
        $collectionTabSlider = $('.tab-slider'),
        $collectionInputCounter = $('.input-counter'),
        $collectionCartModalLink = $('[data-modal="modal"]'),
        $defaultAddressCheckbox = $('#get-address'),
        $collectionFormBill = $('[data-bill]'),
        $productDetailElement = $('#pd-o-initiate'),
        $productDetailElementThumbnail = $('#pd-o-thumbnail'),
        $modalProductDetailElement = $('#js-product-detail-modal'),
        $modalProductDetailElementThumbnail = $('#js-product-detail-modal-thumbnail'),
        $shopCategoryToggleSpan = $('.shop-w__category-list .has-list > .js-shop-category-span'),// Recursive
        $shopGridBtn = $('.js-shop-grid-target'),
        $shopListBtn = $('.js-shop-list-target'),
        $shopPerspectiveRow = $('.shop-p__collection > div'),
        $shopFilterBtn = $('.js-shop-filter-target');



    // Bind Scroll Up to all pages
    RESHOP.initScrollUp = function() {
        $.scrollUp({
            scrollName: 'topScroll',
            scrollText: '<i class="fas fa-long-arrow-alt-up"></i>',
            easingType: 'linear',
            scrollSpeed: 900,
            animation: 'fade',
            zIndex: 100
        });
    };

    RESHOP.initScrollSpy = function() {
        var $bodyScrollSpy = $('#js-scrollspy-trigger');
        if ($bodyScrollSpy.length) {
            $bodyScrollSpy.scrollspy({
                target: '#init-scrollspy'
            });
        }
    };

    RESHOP.onClickScroll = function() {
        $collectionaClickScroll.on('click', function (e) {
            // prevent default behavior means page doesn't move or show up id's on browser status-bar
            e.preventDefault();
            // Get Target
            var target = $(this).data('click-scroll');
            // check if anchor has hash
            if ($(target).length) {
                $('html').animate({
                    // .offset() is jQuery function and it returns jQuery object which
                    // has top, left, bottom property and returns total distance from the html container
                    scrollTop: $(target).offset().top
                }, 1000, function () {
                });
            }
        });
    };

    // Bind Tooltip to all pages
    RESHOP.initTooltip = function() {

        $('[data-tooltip="tooltip"]').tooltip({
            // The default value for trigger is 'hover focus',
            // thus the tooltip stay visible after a button is clicked,
            // until another button is clicked, because the button is focused.
            trigger : 'hover'
        });
    };

    // Bind Modals
    RESHOP.initModal = function() {
        // Check if these anchors are on page
        if ($collectionCartModalLink.length) {
            $collectionCartModalLink.on('click',function () {
                var getElemId = $(this).data('modal-id');
                $(getElemId).modal({
                    backdrop: 'static',
                    keyboard: false,
                    show:true
                });


            });
        }

    };

    // Default Billing Address
    RESHOP.defaultAddressCheckbox = function() {
        if ($defaultAddressCheckbox.length) {
            $defaultAddressCheckbox.change(function () {
                if (this.checked) {
                    $collectionFormBill.prop("disabled", true);
                    $('#make-default-address').prop("checked", false);
                } else {
                    $collectionFormBill.prop("disabled", false);
                }
            });

        }
    };





    RESHOP.reshopNavigation = function() {
        $('#navigation').shopNav();
        $('#navigation1').shopNav();
        $('#navigation2').shopNav();
        $('#navigation3').shopNav();
    };

    RESHOP.onTabActiveRefreshSlider = function() {
        // When showing a new tab, the events fire.
        // Specificity = 2
        $('.tab-list [data-toggle="tab"]').on('shown.bs.tab', function (e) {
            // Get the current click id of tab
            var currentID = $(e.target).attr('href');
            // Trigger refresh `event` to current active `tab`
            $(currentID + '.active').find('.tab-slider').trigger('refresh.owl.carousel');
        });
    };

    // Bind all sliders into the page
    RESHOP.primarySlider = function() {
        if ($primarySlider.length) {
            $primarySlider.owlCarousel({
                items: 1,
                autoplayTimeout: 8000,
                loop: true,
                margin: -1,
                dots: false,
                smartSpeed: 1500,
                rewind: false, // Go backwards when the boundary has reached
                nav: false,
                responsive: {
                    992: {
                        dots: true
                    }
                }
            });
        }
    };

    // Bind all sliders into the page
    RESHOP.productSlider = function() {
        // 0 is falsy value, 1 is truthy
        if ($collectionProductSlider.length) {
            $collectionProductSlider.on('initialize.owl.carousel', function () {
                $(this).closest('.slider-fouc').removeAttr('class');
            }).each(function () {
                var thisInstance = $(this);
                var itemPerLine = thisInstance.data('item');
                thisInstance.owlCarousel({
                    autoplay: false,
                    loop: false,
                    dots: false,
                    rewind: true,
                    smartSpeed: 1500,
                    nav: true,
                    navElement: 'div',
                    navClass: ['p-prev', 'p-next'],
                    navText: ['<i class="fas fa-long-arrow-alt-left"></i>', '<i class="fas fa-long-arrow-alt-right"></i>'],
                    responsive: {
                        0: {
                            items: 1
                        },
                        768: {
                            items: itemPerLine - 2
                        },
                        991: {
                            items: itemPerLine - 1
                        },
                        1200: {
                            items: itemPerLine
                        }
                    }
                });
            });
        }
    };


    // Bind all sliders into the page
    RESHOP.tabSlider = function() {
        if ($collectionTabSlider.length) {
            $collectionTabSlider.on('initialize.owl.carousel', function () {
                $(this).closest('.slider-fouc').removeAttr('class');
            }).each(function () {
                var thisInstance = $(this);
                var itemPerLine = thisInstance.data('item');
                thisInstance.owlCarousel({
                    autoplay: false,
                    loop: false,
                    dots: false,
                    rewind: true,
                    smartSpeed: 1500,
                    nav: true,
                    navElement: 'div',
                    navClass: ['t-prev', 't-next'],
                    navText: ['<i class="fas fa-long-arrow-alt-left"></i>', '<i class="fas fa-long-arrow-alt-right"></i>'],
                    responsive: {
                        0: {
                            items: 1
                        },
                        768: {
                            items: itemPerLine - 2
                        },
                        991: {
                            items: itemPerLine - 1
                        },
                        1200: {
                            items: itemPerLine
                        }
                    }
                });
            });
        }
    };

    // Bind Brand slider
    RESHOP.brandSlider = function() {
        var $brandSlider = $('#brand-slider');
        // Check if brand slider on the page
        if ($brandSlider.length) {
            var itemPerLine = $brandSlider.data('item');
            $brandSlider.on('initialize.owl.carousel', function () {
                $(this).closest('.slider-fouc').removeAttr('class');
            }).owlCarousel({
                autoplay: false,
                loop: false,
                dots: false,
                rewind: true,
                nav: true,
                navElement: 'div',
                navClass: ['b-prev', 'b-next'],
                navText: ['<i class="fas fa-angle-left"></i>', '<i class="fas fa-angle-right"></i>'],
                responsive: {
                    0: {
                        items: 1
                    },
                    768: {
                        items: 3,
                    },
                    991: {
                        items: itemPerLine
                    },
                    1200: {
                        items: itemPerLine
                    }
                }

            });
        }
    };

    // Testimonial Slider
    RESHOP.testimonialSlider = function() {
        // Check if Testimonial-Slider on the page
        if ($testimonialSlider.length) {
            $testimonialSlider.on('initialize.owl.carousel', function () {
                $(this).closest('.slider-fouc').removeAttr('class');
            }).owlCarousel({
                items:1,
                autoplay: false,
                loop: false,
                dots: true,
                rewind: false,
                smartSpeed: 1500,
                nav: false
            });
        }
    };
    // Remove Class from body element
    RESHOP.appConfiguration = function() {
        $('body').removeAttr('class');
        $('.preloader').removeClass('is-active');
    };





	// Input Counter
	RESHOP.initInputCounter = function() {
	    // Quét lại toàn bộ DOM để tìm các thẻ input-counter mới sinh ra
	    var $dynamicInputCounter = $('.input-counter'); 
	    
	    if ($dynamicInputCounter.length) {
	        // Gỡ các sự kiện click cũ trước khi gắn mới để không bị nhân đôi sự kiện
	        $dynamicInputCounter.find('.input-counter__plus, .input-counter__minus').off('click');
	        $dynamicInputCounter.find('input').off('change');

	        // Attach Click event to plus button
	        $dynamicInputCounter.find('.input-counter__plus').on('click',function () {
	            var $input = $(this).parent().find('input');
	            var count = parseInt($input.val()) + 1;
	            $input.val(count).change();
	        });
	        // Attach Click event to minus button
	        $dynamicInputCounter.find('.input-counter__minus').on('click',function () {
	            var $input = $(this).parent().find('input');
	            var count = parseInt($input.val()) - 1;
	            $input.val(count).change();
	        });
	        // Fires when the value of the element is changed
	        $dynamicInputCounter.find('input').change(function () {
	            var $this = $(this);
	            var min = $this.data('min');
	            var max = $this.data('max');
	            var val = parseInt($this.val());
	            if (!val) { val = 1; }
	            val = Math.min(val,max);
	            val = Math.max(val,min);
	            $this.val(val);
	        });
	    }
	};






    // Product Detail Init
    RESHOP.productDetailInit = function() {
      if ($productDetailElement.length && $productDetailElementThumbnail.length) {

          var ELEVATE_ZOOM_OBJ = {
              borderSize: 1,
              autoWidth:true,
              zoomWindowWidth: 540,
              zoomWindowHeight: 540,
              zoomWindowOffetx: 10,
              borderColour: '#e9e9e9',
              cursor: 'pointer'
          };
            // Fires after first initialization
          $productDetailElement.on('init', function () {
              $(this).closest('.slider-fouc').removeClass('slider-fouc');
          });

          $productDetailElement.slick({
              slidesToShow: 1,
              slidesToScroll: 1,
              infinite:false,
              arrows: false,
              dots: false,
              fade: true,
              asNavFor: $productDetailElementThumbnail
          });
          // Init elevate zoom plugin to the first image
          $('#pd-o-initiate .slick-current img').elevateZoom(ELEVATE_ZOOM_OBJ);

          // Fires before slide change
          $productDetailElement.on('beforeChange', function(event, slick, currentSlide, nextSlide){
              // Get the next slide image
              var $img = $(slick.$slides[nextSlide]).find('img');
              // Remove old zoom elements
              $('.zoomWindowContainer,.zoomContainer').remove();
              // Reinit elevate zoom plugin to the next slide image
              $($img).elevateZoom(ELEVATE_ZOOM_OBJ);
          });

          // Init Lightgallery plugin
          $productDetailElement.lightGallery({
              selector: '.pd-o-img-wrap',// lightgallery-core
              download: false,// lightgallery-core
              thumbnail: false,// Thumbnails
              autoplayControls: false,// Autoplay-plugin
              actualSize: false,// Zoom-plugin: Enable actual pixel icon
              hash: false, // Hash-plugin
              share: false// share-plugin
          });
          // Thumbnail images
          // Fires after first initialization
          $productDetailElementThumbnail.on('init', function () {
              $(this).closest('.slider-fouc').removeAttr('class');
          });

          $productDetailElementThumbnail.slick({
              slidesToShow: 4,
              slidesToScroll: 1,
              infinite:false,
              arrows: true,
              dots: false,
              focusOnSelect: true,
              asNavFor: $productDetailElement,
              prevArrow:'<div class="pt-prev"><i class="fas fa-angle-left"></i>',
              nextArrow:'<div class="pt-next"><i class="fas fa-angle-right"></i>',
              responsive: [
                  {
                      breakpoint: 1200,
                      settings: {
                          slidesToShow: 4
                      }
                  },
                  {
                      breakpoint: 992,
                      settings: {
                          slidesToShow: 3
                      }
                  },
                  {
                      breakpoint: 576,
                      settings: {
                          slidesToShow: 2
                      }
                  }
              ]
          });
      }
    };

	// Modal Product Detail Init
	RESHOP.modalProductDetailInit = function() {
	    // Ép quét lại DOM mỗi khi gọi hàm
	    var $dynamicModalDetail = $('#js-product-detail-modal');
	    var $dynamicModalThumbnail = $('#js-product-detail-modal-thumbnail');

	    if ($dynamicModalDetail.length && $dynamicModalThumbnail.length) {
	        // Hủy Slick cũ nếu đã từng khởi tạo trước đó (để không bị lỗi dồn file)
	        if ($dynamicModalDetail.hasClass('slick-initialized')) {
	            $dynamicModalDetail.slick('unslick');
	        }
	        if ($dynamicModalThumbnail.hasClass('slick-initialized')) {
	            $dynamicModalThumbnail.slick('unslick');
	        }

	        $dynamicModalDetail.slick({
	            slidesToShow: 1,
	            slidesToScroll: 1,
	            infinite:false,
	            arrows: false,
	            dots: false,
	            fade: true,
	            asNavFor: $dynamicModalThumbnail
	        });

	        $dynamicModalThumbnail.slick({
	            slidesToShow: 4,
	            slidesToScroll: 1,
	            infinite:false,
	            arrows: true,
	            dots: false,
	            focusOnSelect: true,
	            asNavFor: $dynamicModalDetail,
	            prevArrow:'<div class="pt-prev"><i class="fas fa-angle-left"></i>',
	            nextArrow:'<div class="pt-next"><i class="fas fa-angle-right"></i>',
	            responsive: [
	                { breakpoint: 1200, settings: { slidesToShow: 4 } },
	                { breakpoint: 992, settings: { slidesToShow: 3 } },
	                { breakpoint: 576, settings: { slidesToShow: 2 } }
	            ]
	        });

	        // Cập nhật lại kích thước khi Modal mở ra
	        $('#quick-look').on('shown.bs.modal', function () {
	            $dynamicModalDetail.resize();
	        });
	    }
	};
    // Shop Category Toggle Functionality
    RESHOP.shopCategoryToggle = function() {
        if ($shopCategoryToggleSpan.length) {
            $shopCategoryToggleSpan.on('click', function () {
                $(this).toggleClass('is-expanded');
                $(this).next('ul').stop(true, true).slideToggle();
            });
        }
    };



    // Shop Perspective Change
    RESHOP.shopPerspectiveChange = function() {
          if ($shopGridBtn.length && $shopListBtn.length)   {
              $shopGridBtn.on('click',function () {
                  $(this).addClass('is-active');
                  $shopListBtn.removeClass('is-active');
                  $shopPerspectiveRow.removeClass('is-list-active');
                  $shopPerspectiveRow.addClass('is-grid-active');
              });
              $shopListBtn.on('click',function () {
                  $(this).addClass('is-active');
                  $shopGridBtn.removeClass('is-active');
                  $shopPerspectiveRow.removeClass('is-grid-active');
                  $shopPerspectiveRow.addClass('is-list-active');
              });
          }
    };
    // Shop Side Filter Settings
    RESHOP.shopSideFilter = function() {
        if ($shopFilterBtn.length) {
            $shopFilterBtn.on('click',function () {
                // Add Class Active
                $(this).toggleClass('is-active');
                // Get Value of the attribute data-side
                var target = $(this).attr('data-side');
                // Open Side
                $(target).toggleClass('is-open');
            });
        }
    };



    // Check everything including DOM elements and images loaded
    $(window).on('load',function () {
        if ($primarySlider.length) {
            // Play slider when everything is loaded
            $primarySlider.data('owl.carousel').options.autoplay = true;
            $primarySlider.trigger('refresh.owl.carousel');
        }
    });


        RESHOP.initScrollUp();
        RESHOP.initTooltip();
        RESHOP.initModal();
        RESHOP.defaultAddressCheckbox();
        RESHOP.initScrollSpy();
        RESHOP.onClickScroll();
        RESHOP.reshopNavigation();
        RESHOP.primarySlider();
        RESHOP.productSlider();
        RESHOP.tabSlider();
        RESHOP.onTabActiveRefreshSlider();
        RESHOP.brandSlider();
        RESHOP.testimonialSlider();
        RESHOP.appConfiguration();
        RESHOP.initInputCounter();
        RESHOP.productDetailInit();
        RESHOP.modalProductDetailInit();
        RESHOP.shopCategoryToggle();
        RESHOP.shopPerspectiveChange();
        RESHOP.shopSideFilter();
		window.RESHOP = RESHOP;
})(jQuery);

/*==============================================================
  # CUSTOM JS: Xử lý Logic Chọn Phân Loại (Không dùng ID)
  ==============================================================*/
function selectColor(element) {
    if (!element) return;
    // 1. Tìm khu vực bao quanh nó (để tách biệt giữa Modal và Trang chủ)
    let container = element.closest('.pd-detail'); 
    if (!container) return;
    
    container.querySelectorAll('.color-btn').forEach(btn => btn.classList.remove('active'));
    element.classList.add('active');
    
    // 2. Lưu màu vừa chọn trực tiếp vào khu vực đó
    container.setAttribute('data-selected-color', element.getAttribute('data-color'));
    checkAndApplyVariant(container);
}

function selectSize(element) {
    if (!element) return;
    let container = element.closest('.pd-detail');
    if (!container) return;
    
    container.querySelectorAll('.size-btn').forEach(btn => btn.classList.remove('active'));
    element.classList.add('active');
    
    container.setAttribute('data-selected-size', element.getAttribute('data-size'));
    checkAndApplyVariant(container);
}

function checkAndApplyVariant(container) {
    if (!container) return;
    // 1. Lấy các phần tử DOM thông qua class
    const btnAdd = container.querySelector('.js-btn-add-cart');
    const stockStatus = container.querySelector('.js-stock-status');
    const inputId = container.querySelector('.js-variant-id');
    const quantityInput = container.querySelector('.js-quantity-input');
    // Tìm phần tử hiển thị giá và tồn kho bên trong container trước để tránh trùng lặp giữa trang chi tiết và quick-look modal
    const priceDisplay = container.querySelector('.pd-detail__price') || document.getElementById('js-display-price');
    
    // Panel tồn kho riêng
    const stockInfoPanel = container.querySelector('.js-variant-stock-info') || document.getElementById('js-variant-stock-info');
    const stockCountEl = container.querySelector('.js-variant-stock-count') || document.getElementById('js-variant-stock-count');
    const stockBadgeEl = container.querySelector('.js-variant-stock-badge') || document.getElementById('js-variant-stock-badge');

    const selectedColor = container.getAttribute('data-selected-color');
    const selectedSize = container.getAttribute('data-selected-size');

    // Nếu chưa chọn đủ Màu và Size
    if (!selectedColor || !selectedSize) {
        if(stockStatus) {
            stockStatus.style.display = 'block';
            stockStatus.innerHTML = '<i class="fas fa-info-circle"></i> Vui lòng chọn Màu sắc và Kích thước.';
            stockStatus.className = 'js-stock-status u-s-m-b-15 text-warning fw-bold';
        }
        if (stockInfoPanel) stockInfoPanel.style.display = 'none';
        if(btnAdd) btnAdd.disabled = true;
        return;
    }

    const variants = container.danhSachBienThe;
    if (typeof variants === 'undefined' || !variants) {
        console.error("Lỗi: Không tìm thấy danh sách biến thể trên container!", container);
        if (stockStatus) {
            stockStatus.style.display = 'block';
            stockStatus.innerHTML = '<i class="fas fa-exclamation-triangle"></i> Lỗi hệ thống: Không tải được thông tin phân loại.';
            stockStatus.className = 'js-stock-status u-s-m-b-15 text-danger fw-bold';
        }
        return;
    }

    // Tìm biến thể khớp với Màu và Size đã chọn (Bọc trim() và toLowerCase() phòng trường hợp khoảng trắng thừa trong DB)
    const matchedVariant = variants.find(v => 
        v.mauSac && selectedColor && v.mauSac.trim().toLowerCase() === selectedColor.trim().toLowerCase() &&
        v.trongLuong && selectedSize && v.trongLuong.trim().toLowerCase() === selectedSize.trim().toLowerCase()
    );

    if (matchedVariant) {
        // --- CẬP NHẬT THÔNG TIN CƠ BẢN ---
        if (inputId) inputId.value = matchedVariant.id;
        
        const soLuong = matchedVariant.soLuongTon || 0;
        const conHang = soLuong > 0;
        
        if(btnAdd) btnAdd.disabled = !conHang;
        
        // Ẩn thông báo cảnh báo
        if(stockStatus) {
            stockStatus.style.display = 'none';
        }

        // Hiển thị panel tồn kho riêng
        if (stockInfoPanel) {
            stockInfoPanel.style.display = 'block';
            if (stockCountEl) {
                stockCountEl.innerText = soLuong + ' sản phẩm';
                stockCountEl.style.color = conHang ? '#009444' : '#ff4500';
            }
            if (stockBadgeEl) {
                if (conHang) {
                    stockBadgeEl.innerText = 'Còn hàng';
                    stockBadgeEl.className = 'js-variant-stock-badge pd-detail__stock';
                } else {
                    stockBadgeEl.innerText = 'Hết hàng';
                    stockBadgeEl.className = 'js-variant-stock-badge pd-detail__left';
                }
            }
        }

        if (quantityInput) quantityInput.setAttribute('data-max', soLuong);
        if (priceDisplay) priceDisplay.innerText = new Intl.NumberFormat('vi-VN').format(matchedVariant.giaBan) + " đ";

        // --- CẬP NHẬT HÌNH ẢNH (TÍCH HỢP CẢ QUICK LOOK & DETAIL) ---
        // Tìm ngược lên thẻ bọc ngoài cùng (div class="row") để qua cột trái lấy ảnh
        const mainRow = container.closest('.row');
        
        if (mainRow && matchedVariant.hinhAnhSanPham) {
            const newImgSrc = '/uploads/product/' + matchedVariant.hinhAnhSanPham;

            // Ưu tiên tìm ảnh đang active trong slider Slick (Của Modal hoặc Detail)
            let activeImage = mainRow.querySelector('#js-product-detail-modal .slick-current img') ||
                              mainRow.querySelector('#pd-o-initiate .slick-current img');

            // Fallback: Nếu Slick chưa init xong, lấy ảnh đầu tiên nó thấy
            if (!activeImage) {
                activeImage = mainRow.querySelector('#js-product-detail-modal img') ||
                              mainRow.querySelector('#pd-o-initiate img');
            }

            if (activeImage) {
                // Đổi đường dẫn ảnh hiển thị
                activeImage.src = newImgSrc;

                // Nếu ảnh có plugin Kính lúp (ElevateZoom) ở trang Chi tiết
                if (activeImage.hasAttribute('data-zoom-image')) {
                    activeImage.setAttribute('data-zoom-image', newImgSrc);
                    
                    // Gọi API của plugin ElevateZoom để nó nạp lại ảnh phóng to mới
                    let ez = $(activeImage).data('elevateZoom');
                    if (ez) {
                        ez.swaptheimage(newImgSrc, newImgSrc);
                    }
                }
            }
        }

    } else {
        // Trường hợp hết hàng / Không có biến thể này
        if (inputId) inputId.value = "";
        if (btnAdd) btnAdd.disabled = true;
        if (stockInfoPanel) stockInfoPanel.style.display = 'none';
        if(stockStatus) {
            stockStatus.style.display = 'block';
            stockStatus.innerHTML = '<i class="fas fa-times-circle"></i> Hiện đã hết hàng.';
            stockStatus.className = 'js-stock-status u-s-m-b-15 text-danger fw-bold';
        }
    }
}
/*==============================================================
  # CUSTOM JS: Xử lý Quick Look Modal bằng AJAX
  ==============================================================*/
  function openQuickLookModal(productId) {
      $.ajax({
          url: '/modal/quick-look/' + productId,
          type: 'GET',
          success: function(responseHtml) {
              $('#quick-look-content').html(responseHtml);
              
              // 1. Kích hoạt lại nút cộng trừ số lượng
              if(typeof RESHOP.initInputCounter === 'function') {
                  RESHOP.initInputCounter();
              }

              // 2. Kích hoạt lại Slider ảnh cho đúng form
              if(typeof RESHOP.modalProductDetailInit === 'function') {
                  RESHOP.modalProductDetailInit();
              }
              
              $('#quick-look').modal('show');
          },
          error: function(error) {
              console.log("Lỗi khi tải dữ liệu Quick Look", error);
              alert("Có lỗi xảy ra khi tải dữ liệu sản phẩm!");
          }
      });
  }
  /*==============================================================
    # CUSTOM JS: Load dữ liệu Giỏ hàng thu nhỏ (Mini Cart) bằng AJAX
    ==============================================================*/
  function loadMiniCart() {
      $.ajax({
          url: '/gio-hang/api/mini-cart',
          type: 'GET',
          success: function(response) {
              // 1. Cập nhật số lượng đỏ cho CẢ Header VÀ Floating Cart
              $('#cart-icon-count, #floating-cart-count').text(response.tongSoLuong);

              // 2. Gom Selector: Chọn CẢ 2 khu vực chứa danh sách
              var $cartContainer = $('#mini-cart-items, #floating-mini-cart-items');
              var $cartTotal = $('#mini-cart-total, #floating-mini-cart-total');

              // Xử lý trường hợp trống
              if (response.trangThai === 'chuadangnhap' || response.tongSoLuong === 0) {
                  $cartContainer.html('<div class="text-center u-s-p-y-15">Giỏ hàng của bạn đang trống.</div>');
                  $cartTotal.text('0 đ');
                  return; 
              }

              // 3. Format tiền tệ VNĐ và cập nhật Tổng tiền cho CẢ 2
              var formattedTotal = new Intl.NumberFormat('vi-VN').format(response.tongTien) + ' đ';
              $cartTotal.text(formattedTotal);

              // 4. Lặp qua danh sách sản phẩm và tạo thẻ HTML
              var htmlContent = '';
              response.danhSach.forEach(function(item) {
                  var formattedPrice = new Intl.NumberFormat('vi-VN').format(item.giaBan) + ' đ';
                  var productUrl = '/san-pham/' + item.idSanPham;
                  var imageUrl = '/uploads/product/' + item.hinhAnh;

                  // TẠO HTML CHO TỪNG SẢN PHẨM TRONG GIỎ (Dùng Template Literal)
                  htmlContent += `
                      <div class="card-mini-product">
                          <div class="mini-product">
                              <div class="mini-product__image-wrapper">
                                  <a class="mini-product__link" href="${productUrl}">
                                      <img class="u-img-fluid" src="${imageUrl}" alt="">
                                  </a>
                              </div>
                              <div class="mini-product__info-wrapper">
                                  <span class="mini-product__name">
                                      <a href="${productUrl}">${item.tenSanPham}</a>
                                  </span>
                                  <span class="mini-product__quantity">${item.soLuong} x</span>
                                  <span class="mini-product__price">${formattedPrice}</span>
                              </div>
                          </div>
						  <a class="mini-product__delete-link far fa-trash-alt js-delete-cart-item" 
						     href="javascript:void(0)" 
						     data-id="${item.idChiTiet}">
						  </a>
                      </div>
                  `;
              });

              // 5. Bơm toàn bộ HTML vừa tạo vào CẢ 2 khung
              $cartContainer.html(htmlContent);
          },
          error: function(error) {
              console.log("Lỗi khi tải Mini Cart:", error);
          }
      });
  }

  // Lệnh này ép trình duyệt: "Ngay khi trang web vừa load xong thì chạy hàm loadMiniCart() ngay cho tao!"
  $(document).ready(function() {
      loadMiniCart();
  });
  /*==============================================================
    # CUSTOM JS: Xử lý Thêm vào giỏ hàng bằng AJAX (Chống reload trang)
    ==============================================================*/
  $(document).on('submit', '.pd-detail__form', function(e) {
      e.preventDefault(); // Ngăn chặn hành vi reload trang mặc định của form
      
      var form = $(this);
      var url = form.attr('action');
      var data = form.serialize(); // Lấy tự động idSanPhamChiTiet và soLuong

      // Khóa nút bấm lại để tránh khách click đúp 2 lần
      var submitBtn = form.find('button[type="submit"]');
      var originalBtnText = submitBtn.html();
      submitBtn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i> Đang xử lý...');

      $.ajax({
          url: url,
          type: 'POST',
          data: data,
          success: function(response) {
              // Nhả nút bấm ra
              submitBtn.prop('disabled', false).html(originalBtnText);

              if (response.trangThai === 'chuadangnhap') {
                  window.location.href = '/user/dang-nhap?loi=' + encodeURIComponent('Bạn chưa đăng nhập. Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng!');
                  return;
              }

              if (response.trangThai === 'ok') {
                  // 1. Đổ dữ liệu thật vào Modal
                  $('#js-modal-cart-name').text(response.tenSanPham);
                  $('#js-modal-cart-variant').text('Phân loại: ' + response.phanLoai);
                  $('#js-modal-cart-qty').text('Số lượng thêm: ' + response.soLuongThem);
                  
                  var formattedPrice = new Intl.NumberFormat('vi-VN').format(response.giaBan) + ' đ';
                  $('#js-modal-cart-price').text(formattedPrice);
                  
                  $('#js-modal-cart-img').attr('src', '/uploads/product/' + response.hinhAnh);

                  // 2. Ẩn modal Quick Look nếu khách đang thao tác từ Quick Look
                  $('#quick-look').modal('hide');

                  // 3. Hiển thị modal thông báo thành công
                  $('#add-to-cart').modal('show');

                  // 4. Update tự động Mini Cart trên thanh Header
                  if (typeof loadMiniCart === 'function') {
                      loadMiniCart();
                  }
              } else {
                  alert("Lỗi: " + response.message);
              }
          },
          error: function(error) {
              submitBtn.prop('disabled', false).html(originalBtnText);
              alert("Có lỗi kết nối đến máy chủ. Vui lòng thử lại!");
              console.log(error);
          }
      });
  });
  
  /*==============================================================
    # CUSTOM JS: Xóa sản phẩm bằng AJAX (Hiệu ứng mượt, không nhảy trang)
    ==============================================================*/
  $(document).on('click', '.js-delete-cart-item', function(e) {
      e.preventDefault(); // Khóa cứng hành vi load/nhảy trang mặc định
      
      var btn = $(this);
      var idChiTiet = btn.data('id');

      if (confirm('Bạn có chắc chắn muốn xóa sản phẩm này khỏi giỏ?')) {
          // Làm mờ nút đi một chút để báo hiệu đang chờ Server xử lý
          btn.css('opacity', '0.5');

          $.ajax({
              url: '/gio-hang/api/xoa/' + idChiTiet,
              type: 'POST',
              success: function(response) {
                  if (response.trangThai === 'chuadangnhap') {
                      window.location.href = '/user/dang-nhap';
                      return;
                  }
                  
                  if (response.trangThai === 'ok') {
                      // 1. NẾU ĐANG Ở TRANG CART.HTML: Xóa dòng <tr> với hiệu ứng mờ dần
                      var row = btn.closest('tr');
                      if (row.length) {
                          row.fadeOut(300, function() {
                              $(this).remove(); // Cắt bỏ thẻ <tr> khỏi giao diện
                              
                              // Nếu bảng trống trơn, hiển thị thông báo "Giỏ hàng trống"
                              if ($('table.table-p tbody tr').length === 0) {
                                  $('table.table-p tbody').html('<tr><td colspan="4" class="text-center u-s-p-y-30">Giỏ hàng của bạn đang trống. Hãy thêm sản phẩm vào giỏ nhé!</td></tr>');
                                  $('.f-cart').fadeOut(); // Ẩn luôn khu vực Tạm tính tiền
                              }
                          });
                      }

                      // 2. Tái sử dụng lại API loadMiniCart để nó tự lo việc tính toán TỔNG TIỀN và SỐ LƯỢNG MỚI
                      if (typeof loadMiniCart === 'function') {
                          loadMiniCart();
                      }
                      
                      // 3. Nếu ở trang cart.html, cập nhật luôn số TỔNG CỘNG bự chà bá
                      $.ajax({
                          url: '/gio-hang/api/mini-cart',
                          type: 'GET',
                          success: function(res) {
                              if (res.trangThai === 'ok') {
                                  var formattedTotal = new Intl.NumberFormat('vi-VN').format(res.tongTien) + ' đ';
                                  $('.js-cart-summary-total').text(formattedTotal);
                                  $('.f-cart__table tr:first-child td:last-child').text(formattedTotal); // Cập nhật Tạm tính
                              }
                          }
                      });

                  } else {
                      alert("Có lỗi xảy ra khi xóa!");
                      btn.css('opacity', '1');
                  }
              },
              error: function() {
                  alert("Lỗi kết nối máy chủ!");
                  btn.css('opacity', '1');
              }
          });
      }
  });
  /*==============================================================
    # CUSTOM JS: Thêm nhanh vào giỏ (Dành cho SP có duy nhất 1 biến thể)
    ==============================================================*/
  function quickAddToCart(idSanPhamChiTiet) {
      $.ajax({
          url: '/gio-hang/them',
          type: 'POST',
          data: {
              idSanPhamChiTiet: idSanPhamChiTiet,
              soLuong: 1 // Thêm nhanh mặc định là 1 chiếc
          },
          success: function(response) {
              if (response.trangThai === 'chuadangnhap') {
                  window.location.href = '/user/dang-nhap?loi=' + encodeURIComponent('Bạn chưa đăng nhập. Vui lòng đăng nhập để thêm sản phẩm vào giỏ hàng!');
                  return;
              }

              if (response.trangThai === 'ok') {
                  // Đổ dữ liệu thật vào Modal Thành Công
                  $('#js-modal-cart-name').text(response.tenSanPham);
                  $('#js-modal-cart-variant').text('Phân loại: ' + response.phanLoai);
                  $('#js-modal-cart-qty').text('Số lượng thêm: ' + response.soLuongThem);
                  
                  var formattedPrice = new Intl.NumberFormat('vi-VN').format(response.giaBan) + ' đ';
                  $('#js-modal-cart-price').text(formattedPrice);
                  $('#js-modal-cart-img').attr('src', '/uploads/product/' + response.hinhAnh);

                  // Hiển thị modal thông báo thành công
                  $('#add-to-cart').modal('show');

                  // Load lại Mini Cart trên Header
                  if (typeof loadMiniCart === 'function') {
                      loadMiniCart();
                  }
              } else {
                  alert("Không thể thêm: " + response.message);
              }
          },
          error: function(error) {
              alert("Có lỗi kết nối đến máy chủ. Vui lòng thử lại!");
              console.log(error);
          }
      });
  }
  /*==============================================================
    # CUSTOM JS: Xử lý hiển thị Floating Cart khi cuộn chuột
    ==============================================================*/
  $(window).on('scroll', function() {
      // Nếu cuộn chuột xuống quá 300px (nghĩa là đã vượt qua thanh Header)
      if ($(window).scrollTop() > 300) {
          $('#floating-cart').addClass('is-visible');
      } else {
          // Cuộn lên lại đầu trang thì ẩn đi
          $('#floating-cart').removeClass('is-visible');
      }
  });
  /*==============================================================
    # CUSTOM JS: Thêm vào Wishlist bằng AJAX
  ==============================================================*/
  function addToWishlist(idSanPham) {
      $.ajax({
          url: '/wishlist/them',
          type: 'POST',
          data: { idSanPham: idSanPham },
          success: function(res) {
              if (res === 'chuadangnhap') {
                  window.location.href = '/user/dang-nhap';
              } else if (res === 'datontai') {
                  alert('Sản phẩm này đã có trong danh sách yêu thích của bạn!');
              } else if (res === 'ok') {
                  alert('Đã thêm vào danh sách yêu thích!');
                  // Có thể cập nhật số đếm trên Header ở đây nếu cần
              }
          },
          error: function() {
              alert('Có lỗi xảy ra, vui lòng thử lại!');
          }
      });
  }
  /*==============================================================
    # CUSTOM JS: Xóa sản phẩm Yêu Thích bằng AJAX (Hiệu ứng mượt)
  ==============================================================*/
  $(document).on('click', '.js-delete-wishlist-item', function(e) {
      e.preventDefault();
      var btn = $(this);
      var idSanPham = btn.data('id');

      if (confirm('Bạn có chắc chắn muốn xóa sản phẩm này khỏi danh sách yêu thích?')) {
          btn.css('opacity', '0.5');

          $.ajax({
              url: '/wishlist/api/xoa/' + idSanPham,
              type: 'GET',
              success: function(response) {
                  if (response.trangThai === 'chuadangnhap') {
                      window.location.href = '/user/dang-nhap';
                      return;
                  }
                  
                  if (response.trangThai === 'ok') {
                      // Tìm thẻ bọc ngoài cùng của sản phẩm (class .w-r) và làm mờ nó đi
                      var row = btn.closest('.w-r');
                      if (row.length) {
                          row.fadeOut(300, function() {
                              $(this).remove(); // Xóa hẳn khỏi HTML
                              
                              // Nếu xóa hết sạch sản phẩm rồi thì hiện thông báo "Danh sách trống"
                              if ($('.w-r').length === 0) {
                                  var emptyHtml = '<div class="text-center u-s-p-y-60"><h3 class="u-s-m-b-15">Danh sách yêu thích của bạn đang trống!</h3><a class="btn btn--e-brand-b-2" href="/">QUAY LẠI MUA SẮM</a></div>';
                                  $('.section__content .col-lg-12.col-md-12.col-sm-12').html(emptyHtml);
                                  $('.route-box').hide(); // Ẩn luôn các nút Xóa Tất Cả bên dưới
                              }
                          });
                      }
                  } else {
                      alert("Có lỗi xảy ra khi xóa!");
                      btn.css('opacity', '1');
                  }
              },
              error: function() {
                  alert("Lỗi kết nối máy chủ!");
                  btn.css('opacity', '1');
              }
          });
      }
  });
  /*==============================================================
    # CUSTOM JS: Lấy vị trí hiện tại (Geolocation + OpenStreetMap + Bản đồ tương tác)
  ==============================================================*/

  // Global variables for Map and Marker
  let leafletMap = null;
  let mapMarker = null;

  // Dynamic Toast CSS injection for premium notifications
  (function injectToastStyles() {
      if (document.getElementById('custom-toast-styles')) return;
      const styles = `
          .custom-toast-container {
              position: fixed;
              top: 20px;
              right: 20px;
              z-index: 99999;
              display: flex;
              flex-direction: column;
              gap: 10px;
              max-width: 380px;
              width: calc(100% - 40px);
              pointer-events: none;
          }
          .custom-toast {
              display: flex;
              align-items: flex-start;
              gap: 12px;
              background: rgba(255, 255, 255, 0.95);
              backdrop-filter: blur(10px);
              border-radius: 12px;
              padding: 16px;
              box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
              border-left: 5px solid #ccc;
              transform: translateX(120%);
              transition: transform 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275), opacity 0.4s ease;
              opacity: 0;
              pointer-events: auto;
          }
          .custom-toast.show {
              transform: translateX(0);
              opacity: 1;
          }
          .custom-toast-success {
              border-left-color: #2ecc71;
          }
          .custom-toast-warning {
              border-left-color: #f1c40f;
          }
          .custom-toast-error {
              border-left-color: #e74c3c;
          }
          .custom-toast-info {
              border-left-color: #3498db;
          }
          .custom-toast-icon {
              font-size: 1.25rem;
              flex-shrink: 0;
              margin-top: 2px;
          }
          .custom-toast-success .custom-toast-icon { color: #2ecc71; }
          .custom-toast-warning .custom-toast-icon { color: #f1c40f; }
          .custom-toast-error .custom-toast-icon { color: #e74c3c; }
          .custom-toast-info .custom-toast-icon { color: #3498db; }
          
          .custom-toast-content {
              flex-grow: 1;
              color: #2c3e50;
          }
          .custom-toast-title {
              font-weight: 700;
              font-size: 0.95rem;
              margin-bottom: 4px;
          }
          .custom-toast-message {
              font-size: 0.85rem;
              line-height: 1.4;
              color: #7f8c8d;
          }
          .custom-toast-close {
              background: none;
              border: none;
              color: #bdc3c7;
              cursor: pointer;
              font-size: 1rem;
              padding: 0;
              line-height: 1;
              flex-shrink: 0;
              transition: color 0.2s;
          }
          .custom-toast-close:hover {
              color: #7f8c8d;
          }
          
          @media (max-width: 480px) {
              .custom-toast-container {
                  top: 10px;
                  right: 10px;
                  width: calc(100% - 20px);
              }
          }
      `;
      const styleSheet = document.createElement("style");
      styleSheet.id = 'custom-toast-styles';
      styleSheet.type = "text/css";
      styleSheet.innerText = styles;
      document.head.appendChild(styleSheet);
  })();

  // Toast displaying helper
  function showToast(title, message, type = 'info', duration = 5000) {
      let container = document.querySelector('.custom-toast-container');
      if (!container) {
          container = document.createElement('div');
          container.className = 'custom-toast-container';
          document.body.appendChild(container);
      }
      
      const toast = document.createElement('div');
      toast.className = `custom-toast custom-toast-${type}`;
      
      let iconClass = 'fa-info-circle';
      if (type === 'success') iconClass = 'fa-check-circle';
      else if (type === 'warning') iconClass = 'fa-exclamation-triangle';
      else if (type === 'error') iconClass = 'fa-times-circle';
      
      toast.innerHTML = `
          <span class="custom-toast-icon fas ${iconClass}"></span>
          <div class="custom-toast-content">
              <div class="custom-toast-title">${title}</div>
              <div class="custom-toast-message">${message}</div>
          </div>
          <button class="custom-toast-close fas fa-times"></button>
      `;
      
      container.appendChild(toast);
      
      // Force reflow
      toast.offsetHeight;
      toast.classList.add('show');
      
      const closeToast = () => {
          toast.classList.remove('show');
          toast.addEventListener('transitionend', () => {
              toast.remove();
          });
      };
      
      let timer = setTimeout(closeToast, duration);
      
      toast.querySelector('.custom-toast-close').addEventListener('click', () => {
          clearTimeout(timer);
          closeToast();
      });
  }

  // Debounce helper (Requirement: Debounce reverse geocoding when dragging marker)
  function debounce(func, delay) {
      let timer;
      return function(...args) {
          clearTimeout(timer);
          timer = setTimeout(() => func.apply(this, args), delay);
      };
  }

  // Helper: Fetch with timeout using AbortController (Requirement 4 & 7)
  async function fetchWithTimeout(resource, options = {}) {
      const { timeout = 8000 } = options;
      
      const controller = new AbortController();
      const id = setTimeout(() => controller.abort(), timeout);
      
      try {
          const response = await fetch(resource, {
              ...options,
              signal: controller.signal
          });
          clearTimeout(id);
          return response;
      } catch (error) {
          clearTimeout(id);
          if (error.name === 'AbortError') {
              throw new Error('TIMEOUT_ERROR');
          }
          throw error;
      }
  }

  // Validator: Validate coordinates (Requirement 5)
  function isValidCoordinates(lat, lon) {
      return !isNaN(lat) && !isNaN(lon) && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180;
  }

  // Parser: Smart Vietnam Administrative Mapping (Requirement 3)
  function parseNominatimAddress(data) {
      const address = data.address || {};
      let displayParts = [];
      if (data.display_name) {
          displayParts = data.display_name.split(',').map(p => p.trim());
      }

      // Province mapping
      let province = address.city || address.province || address.state || address.county || "";
      
      // District mapping
      let district = address.city_district || address.county || address.district || "";
      
      // Ward / Commune mapping
      let ward = address.suburb || address.neighbourhood || address.quarter || address.village || "";

      // Road mapping
      let road = address.road || address.street || address.footway || address.path || address.pedestrian || "";

      // Specific house/building
      const buildingCandidates = [
          address.house_number,
          address.building,
          address.office,
          address.amenity,
          address.shop,
          address.historic,
          address.tourism,
          address.leisure
      ];
      let localDetails = [];
      buildingCandidates.forEach(val => {
          if (val && val.trim() !== "" && !localDetails.includes(val.trim())) {
              localDetails.push(val.trim());
          }
      });
      let building = localDetails.join(' ');

      let diaChiCuThe = "";
      let tinhThanh = province.trim();
      let quocGia = address.country || "Việt Nam";

      // Build structured diaChiCuThe
      let specificParts = [];
      if (building) specificParts.push(building);
      if (road) specificParts.push(road);
      if (ward) specificParts.push(ward);
      if (district) specificParts.push(district);

      specificParts = specificParts.map(p => p.trim()).filter(Boolean);
      
      // Deduplicate parts
      let uniqueParts = [];
      specificParts.forEach(p => {
          if (!uniqueParts.includes(p)) {
              uniqueParts.push(p);
          }
      });

      if (uniqueParts.length > 0) {
          diaChiCuThe = uniqueParts.join(', ');
      }

      // Fallback to display_name slice if structured parts are too sparse
      if (!diaChiCuThe || uniqueParts.length < 2) {
          if (displayParts.length >= 2) {
              quocGia = displayParts[displayParts.length - 1];
              let penIdx = displayParts.length - 2;
              let penValue = displayParts[penIdx];
              if (/^\d+$/.test(penValue) && penIdx - 1 >= 0) {
                  if (!tinhThanh) tinhThanh = displayParts[penIdx - 1];
                  diaChiCuThe = displayParts.slice(0, penIdx - 1).join(', ');
              } else {
                  if (!tinhThanh) tinhThanh = penValue;
                  diaChiCuThe = displayParts.slice(0, penIdx).join(', ');
              }
          } else {
              diaChiCuThe = data.display_name || "";
          }
      }

      if (!tinhThanh && displayParts.length >= 2) {
          let penIdx = displayParts.length - 2;
          let penValue = displayParts[penIdx];
          if (/^\d+$/.test(penValue) && penIdx - 1 >= 0) {
              tinhThanh = displayParts[penIdx - 1];
          } else {
              tinhThanh = penValue;
          }
      }

      // Format cleanups
      if (tinhThanh) {
          tinhThanh = tinhThanh.replace(/\d+/g, '').trim();
      }
      if (quocGia) {
          quocGia = quocGia.trim();
      }

      return {
          diaChiCuThe: diaChiCuThe,
          tinhThanh: tinhThanh,
          quocGia: quocGia
      };
  }

  // Provider abstraction with Zoom level fallbacks (Requirement 7 & 8)
  async function reverseGeocode(latitude, longitude) {
      if (!navigator.onLine) {
          throw new Error("OFFLINE_ERROR");
      }

      const zoomLevels = [18, 17, 16];
      let fallbackResult = null;

      for (let zoom of zoomLevels) {
          try {
              const apiUrl = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&lat=${latitude}&lon=${longitude}&zoom=${zoom}&addressdetails=1&accept-language=vi`;
              const response = await fetchWithTimeout(apiUrl, { timeout: 6000 });
              
              if (!response.ok) {
                  if (response.status === 429 || response.status >= 500) {
                      throw new Error("PROVIDER_ERROR");
                  }
                  continue; // Try next zoom level on other errors
              }

              const data = await response.json();
              const parsed = parseNominatimAddress(data);

              // Check if details are sufficient (Requirement 7)
              const partsCount = parsed.diaChiCuThe ? parsed.diaChiCuThe.split(',').length : 0;
              if (partsCount >= 3) {
                  return parsed; // Excellent detail level, return immediately
              }
              
              // Keep as fallback if it contains at least some address structure
              if (!fallbackResult || partsCount > (fallbackResult.diaChiCuThe ? fallbackResult.diaChiCuThe.split(',').length : 0)) {
                  fallbackResult = parsed;
              }
          } catch (err) {
              console.warn(`[Geocoding] Zoom ${zoom} failed:`, err.message);
              if (err.message === "OFFLINE_ERROR" || err.message === "PROVIDER_ERROR") {
                  throw err; // Propagate blocking network or provider-rate-limit errors immediately
              }
          }
      }

      if (fallbackResult && fallbackResult.diaChiCuThe) {
          return fallbackResult;
      }

      throw new Error("GEOCODE_FAILED");
  }

  // Debounced reverse geocoding to update UI and inputs (Requirement: Debounce reverse geocoding when dragging marker)
  const debouncedReverseGeocode = debounce(async function(lat, lon) {
      try {
          const parsedAddress = await reverseGeocode(lat, lon);
          populateAddressFields(parsedAddress);
          showToast('Đã cập nhật', 'Đã tự động cập nhật địa chỉ theo toạ độ bản đồ!', 'success');
      } catch (err) {
          console.error('[Geocoding] Debounced geocoding error:', err);
          handleGeocodingError(err);
      }
  }, 400);

  // Populate UI inputs with geocoding results
  function populateAddressFields(addressObj) {
      if (addressObj.diaChiCuThe) {
          document.getElementById('address-street').value = addressObj.diaChiCuThe;
      }
      if (addressObj.tinhThanh) {
          document.getElementById('address-state').value = addressObj.tinhThanh;
      }
      if (addressObj.quocGia) {
          document.getElementById('address-country').value = addressObj.quocGia;
      }
  }

  // Update map marker and inputs when getting location from GPS/IP button (Requirement 10)
  function updateMapFromLocation(lat, lon) {
      if (leafletMap && mapMarker) {
          leafletMap.flyTo([lat, lon], 16);
          mapMarker.setLatLng([lat, lon]);
          if (!leafletMap.hasLayer(mapMarker)) {
              mapMarker.addTo(leafletMap);
          }
          
          const latInput = document.getElementById('address-lat');
          const lonInput = document.getElementById('address-lon');
          if (latInput) latInput.value = lat.toFixed(6);
          if (lonInput) lonInput.value = lon.toFixed(6);
      }
  }

  // Fallback to IP Geolocation through backend proxy (Requirement 1 & 9)
  async function triggerIpFallback(btn, originalText) {
      if (!navigator.onLine) {
          showToast('Lỗi kết nối', 'Không có kết nối mạng. Vui lòng thử lại.', 'error');
          resetButton(btn, originalText);
          return;
      }

      try {
          // Fetch coordinates from backend IP proxy
          const response = await fetchWithTimeout('/api/location/ip', { timeout: 6000 });
          if (!response.ok) {
              if (response.status === 429) {
                  throw new Error("RATE_LIMITED");
              }
              throw new Error("BACKEND_PROXY_FAILED");
          }

          const data = await response.json();
          const lat = data.latitude;
          const lon = data.longitude;

          if (!isValidCoordinates(lat, lon)) {
              throw new Error("INVALID_COORDINATES");
          }

          console.log(`[Geocoding] IP coords retrieved via backend proxy: lat=${lat}, lon=${lon} (Source: ${data.source})`);

          // Update Leaflet Map state
          updateMapFromLocation(lat, lon);

          // Reverse geocode IP coordinates
          const parsedAddress = await reverseGeocode(lat, lon);

          // Save success result to cache
          try {
              localStorage.setItem('location_geocode_cache', JSON.stringify({
                  latitude: lat,
                  longitude: lon,
                  address: parsedAddress,
                  timestamp: Date.now()
              }));
          } catch (cacheStoreErr) {
              console.warn("[Geocoding] Failed to save to localStorage:", cacheStoreErr);
          }

          populateAddressFields(parsedAddress);
          showToast('Định vị qua IP', 'Đã tự động xác định và điền địa chỉ của bạn thành công qua IP!', 'success');

      } catch (err) {
          console.error('[Geocoding] IP Fallback failed:', err);
          handleGeocodingError(err);
      } finally {
          resetButton(btn, originalText);
      }
  }

  // Centralized geocoding error handler (Requirement 9)
  function handleGeocodingError(error) {
      if (!navigator.onLine || error.message === "OFFLINE_ERROR") {
          showToast('Lỗi kết nối', 'Không có kết nối mạng. Vui lòng thử lại.', 'error');
      } else if (error.message === "RATE_LIMITED") {
          showToast('Giới hạn yêu cầu', 'Bạn đã yêu cầu quá nhanh. Vui lòng đợi một phút trước khi thử lại.', 'warning');
      } else if (error.message === "GEOCODE_FAILED" || error.message === "PROVIDER_ERROR") {
          showToast('Lỗi định vị', 'Không thể xác định địa chỉ từ vị trí hiện tại.', 'error');
      } else {
          showToast('Lỗi vị trí', 'Không thể lấy dữ liệu vị trí vào lúc này.', 'error');
      }
  }

  // Restore button state
  function resetButton(btn, originalText) {
      btn.innerHTML = originalText;
      btn.disabled = false;
  }

  // Geolocation main controller (Requirement 2, 4, 5, 6, 9)
  async function getCurrentLocation() {
      const btn = document.getElementById('btn-get-location');
      if (!btn || btn.disabled) return; // Prevent double trigger

      const originalText = btn.innerHTML;

      // 1. Protect user entered data (Requirement 6)
      const streetInput = document.getElementById('address-street');
      const stateInput = document.getElementById('address-state');
      const hasUserData = (streetInput && streetInput.value.trim() !== '') || 
                          (stateInput && stateInput.value.trim() !== '');

      if (hasUserData) {
          const confirmOverwrite = confirm("Địa chỉ hiện tại sẽ bị thay thế bởi vị trí mới. Bạn có muốn tiếp tục không?");
          if (!confirmOverwrite) {
              return; // Cancel
          }
      }

      // 2. Check localStorage cache (Requirement 4)
      try {
          const cachedData = localStorage.getItem('location_geocode_cache');
          if (cachedData) {
              const cache = JSON.parse(cachedData);
              const now = Date.now();
              // Cache valid for 10 minutes (600,000ms)
              if (now - cache.timestamp < 600000 && cache.address && isValidCoordinates(cache.latitude, cache.longitude)) {
                  console.log("[Geocoding] Using valid cached location coordinates:", cache.latitude, cache.longitude);
                  
                  // Update map marker and inputs
                  updateMapFromLocation(cache.latitude, cache.longitude);
                  populateAddressFields(cache.address);
                  showToast('Bộ nhớ tạm', 'Đã tự động điền vị trí từ bộ nhớ tạm thời của bạn!', 'info');
                  return;
              }
          }
      } catch (cacheErr) {
          console.warn("[Geocoding] Cache lookup error:", cacheErr);
      }

      // 3. Set loading state and request protection (Requirement 5)
      btn.innerHTML = '<i class="fas fa-spinner fa-spin u-s-m-r-8"></i> Đang lấy vị trí hiện tại...';
      btn.disabled = true;

      // 4. Try browser GPS location first
      if (navigator.geolocation) {
          navigator.geolocation.getCurrentPosition(
              async function(position) {
                  try {
                      const lat = position.coords.latitude;
                      const lon = position.coords.longitude;
                      const accuracy = position.coords.accuracy;

                      console.log(`[Geocoding] GPS coords retrieved: lat=${lat}, lon=${lon}, accuracy=${accuracy}m`);

                      // Validate coordinates (Requirement 5)
                      if (!isValidCoordinates(lat, lon)) {
                          throw new Error("INVALID_COORDINATES");
                      }

                      // Accuracy check (Requirement 2)
                      if (accuracy > 500) {
                          showToast('Độ chính xác thấp', 'Vị trí hiện tại có độ chính xác thấp. Kết quả địa chỉ có thể không hoàn toàn chính xác.', 'warning', 7000);
                      }

                      // Update map marker
                      updateMapFromLocation(lat, lon);

                      // Call reverse geocoding abstraction
                      const parsedAddress = await reverseGeocode(lat, lon);

                      // Save success result to cache (Requirement 4)
                      try {
                          localStorage.setItem('location_geocode_cache', JSON.stringify({
                              latitude: lat,
                              longitude: lon,
                              address: parsedAddress,
                              timestamp: Date.now()
                          }));
                      } catch (cacheStoreErr) {
                          console.warn("[Geocoding] Failed to save to localStorage:", cacheStoreErr);
                      }

                      populateAddressFields(parsedAddress);
                      showToast('Thành công', 'Đã tự động định vị và điền vị trí của bạn thành công!', 'success');

                  } catch (err) {
                      console.error('[Geocoding] GPS success branch error:', err);
                      handleGeocodingError(err);
                  } finally {
                      resetButton(btn, originalText);
                  }
              },
              function(error) {
                  console.warn('[Geocoding] GPS lookup failed or denied. Transitioning to IP fallback...');
                  // GPS failure: Trigger fallback automatically (Requirement 9)
                  if (error.code === 1) { // PERMISSION_DENIED
                      showToast('Chuyển hướng IP', 'Không thể truy cập GPS. Hệ thống sẽ thử xác định vị trí qua IP.', 'info', 4000);
                  }
                  triggerIpFallback(btn, originalText);
              },
              {
                  enableHighAccuracy: true,
                  timeout: 8000, // Timeout after 8 seconds (Requirement 9)
                  maximumAge: 0
              }
          );
      } else {
          console.warn('[Geocoding] Browser does not support Geolocation. Transitioning to IP fallback...');
          triggerIpFallback(btn, originalText);
      }
  }

  // Interactive Leaflet Map initialization and synchronization
  async function initLocationMap(mapDiv) {
      const latInput = document.getElementById('address-lat');
      const lonInput = document.getElementById('address-lon');
      const streetInput = document.getElementById('address-street');
      const stateInput = document.getElementById('address-state');

      let initialLat = 21.0285;
      let initialLon = 105.8542;
      let zoomLevel = 13;
      let hasCoordinates = false;

      // 5. Use saved coordinates directly (Requirement 5: Dùng tọa độ đã lưu)
      if (latInput && lonInput && latInput.value && lonInput.value) {
          const lat = parseFloat(latInput.value);
          const lon = parseFloat(lonInput.value);
          if (isValidCoordinates(lat, lon)) {
              initialLat = lat;
              initialLon = lon;
              zoomLevel = 16;
              hasCoordinates = true;
          }
      }

      // Initialize Map
      leafletMap = L.map(mapDiv).setView([initialLat, initialLon], zoomLevel);

      // Tile Layer setup (OpenStreetMap Tiles)
      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          maxZoom: 19,
          attribution: '&copy; <a href="https://openstreetmap.org/copyright">OpenStreetMap</a> contributors'
      }).addTo(leafletMap);

      // Draggable Marker
      mapMarker = L.marker([initialLat, initialLon], {
          draggable: true
      });

      if (hasCoordinates) {
          mapMarker.addTo(leafletMap);
      }

      // Add Custom "Locate Me" Control on the Map (Locate self on the map)
      L.Control.LocateMe = L.Control.extend({
          options: {
              position: 'topleft'
          },
          onAdd: function(map) {
              const container = L.DomUtil.create('div', 'leaflet-bar leaflet-control leaflet-control-custom');
              container.style.backgroundColor = '#fff';
              container.style.width = '34px';
              container.style.height = '34px';
              container.style.lineHeight = '30px';
              container.style.textAlign = 'center';
              container.style.cursor = 'pointer';
              container.style.display = 'flex';
              container.style.alignItems = 'center';
              container.style.justifyContent = 'center';
              container.title = "Định vị vị trí của tôi";

              // Target / Crosshairs icon using FontAwesome
              container.innerHTML = '<a href="#" style="display: flex; align-items: center; justify-content: center; width: 100%; height: 100%; color: #333; font-size: 16px; text-decoration: none;"><i class="fas fa-crosshairs"></i></a>';

              L.DomEvent.disableClickPropagation(container);
              L.DomEvent.on(container, 'click', function(e) {
                  L.DomEvent.preventDefault(e);
                  getCurrentLocation();
              });

              return container;
          }
      });
      leafletMap.addControl(new L.Control.LocateMe());

      // Click event on map to select position
      leafletMap.on('click', function(e) {
          const { lat, lng } = e.latlng;
          if (!isValidCoordinates(lat, lng)) return;

          mapMarker.setLatLng(e.latlng);
          if (!leafletMap.hasLayer(mapMarker)) {
              mapMarker.addTo(leafletMap);
          }

          if (latInput) latInput.value = lat.toFixed(6);
          if (lonInput) lonInput.value = lng.toFixed(6);

          debouncedReverseGeocode(lat, lng);
      });

      // Marker drag event (de-bounced geocode lookup)
      mapMarker.on('dragend', function(e) {
          const latlng = mapMarker.getLatLng();
          const { lat, lng } = latlng;
          if (!isValidCoordinates(lat, lng)) return;

          if (latInput) latInput.value = lat.toFixed(6);
          if (lonInput) lonInput.value = lng.toFixed(6);

          debouncedReverseGeocode(lat, lng);
      });

      // If Edit form and NO coordinates are saved, geocode the text address via backend proxy
      if (!hasCoordinates && streetInput && streetInput.value && stateInput && stateInput.value) {
          const addressQuery = `${streetInput.value}, ${stateInput.value}`;
          console.log("[Map] Edit Mode: Geocoding text address via backend proxy:", addressQuery);
          try {
              const response = await fetchWithTimeout(`/api/location/search?q=${encodeURIComponent(addressQuery)}`, { timeout: 5000 });
              if (response.ok) {
                  const data = await response.json();
                  const lat = data.latitude;
                  const lon = data.longitude;
                  if (isValidCoordinates(lat, lon)) {
                      leafletMap.setView([lat, lon], 16);
                      mapMarker.setLatLng([lat, lon]).addTo(leafletMap);
                      if (latInput) latInput.value = lat.toFixed(6);
                      if (lonInput) lonInput.value = lon.toFixed(6);
                  }
              }
          } catch (err) {
              console.warn("[Map] Silent forward geocoding failed:", err.message);
          }
      }

      // If Add form and NO coordinates are saved, center map using silent IP lookup (no UI alerts)
      if (!hasCoordinates && (!streetInput || !streetInput.value)) {
          console.log("[Map] Add Mode: Attempting silent IP geolocate to center map...");
          try {
              const response = await fetchWithTimeout('/api/location/ip', { timeout: 4000 });
              if (response.ok) {
                  const data = await response.json();
                  const lat = data.latitude;
                  const lon = data.longitude;
                  if (isValidCoordinates(lat, lon)) {
                      leafletMap.setView([lat, lon], 14);
                  }
              }
          } catch (err) {
              console.warn("[Map] Silent IP lookup failed:", err.message);
          }
      }
  }

  // DOM initialization wrapper for Map
  $(document).ready(function() {
      const mapDiv = document.getElementById('map');
      if (mapDiv) {
          initLocationMap(mapDiv);
      }
  });
  /*==============================================================
    # CUSTOM JS: Xóa Sổ Địa Chỉ bằng AJAX
  ==============================================================*/
  $(document).on('click', '.js-delete-address', function(e) {
      e.preventDefault();
      var btn = $(this);
      var idDiaChi = btn.data('id');

      if (confirm('Bạn có chắc chắn muốn xóa địa chỉ này?')) {
          // Làm mờ dòng đó đi để báo hiệu đang chờ máy chủ
          var tr = btn.closest('tr');
          tr.css('opacity', '0.5');

          $.ajax({
              url: '/user/address/api/delete/' + idDiaChi,
              type: 'GET',
              success: function(response) {
                  if (response.trangThai === 'chuadangnhap') {
                      window.location.href = '/user/dang-nhap';
                      return;
                  }
                  
                  if (response.trangThai === 'ok') {
                      // Mờ dần và xóa dòng HTML
                      tr.fadeOut(300, function() {
                          $(this).remove();
                          
                          // Nếu xóa hết sạch, hiển thị câu thông báo trống
                          if ($('table.dash__table-2 tbody tr').length === 0) {
                              var emptyMsg = '<tr><td colspan="6" class="text-center text-muted py-4">Bạn chưa có địa chỉ giao hàng nào. Hãy thêm mới nhé!</td></tr>';
                              $('table.dash__table-2 tbody').html(emptyMsg);
                          }
                      });
                  } else {
                      // Báo lỗi (Ví dụ: Lỗi cố tình xóa địa chỉ mặc định)
                      alert(response.message);
                      tr.css('opacity', '1'); // Khôi phục lại hiển thị
                  }
              },
              error: function() {
                  alert("Lỗi kết nối máy chủ!");
                  tr.css('opacity', '1');
              }
          });
      }
  });