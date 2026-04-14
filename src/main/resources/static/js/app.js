/**
 * This is main script file that contains JS code.
 */
(function ($) {
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
    // 1. Tìm khu vực bao quanh nó (để tách biệt giữa Modal và Trang chủ)
    let container = element.closest('.pd-detail'); 
    
    container.querySelectorAll('.color-btn').forEach(btn => btn.classList.remove('active'));
    element.classList.add('active');
    
    // 2. Lưu màu vừa chọn trực tiếp vào khu vực đó
    container.setAttribute('data-selected-color', element.getAttribute('data-color'));
    checkAndApplyVariant(container);
}

function selectSize(element) {
    let container = element.closest('.pd-detail');
    container.querySelectorAll('.size-btn').forEach(btn => btn.classList.remove('active'));
    element.classList.add('active');
    
    container.setAttribute('data-selected-size', element.getAttribute('data-size'));
    checkAndApplyVariant(container);
}

function checkAndApplyVariant(container) {
    // 1. Lấy các phần tử DOM thông qua class
    const btnAdd = container.querySelector('.js-btn-add-cart');
    const stockStatus = container.querySelector('.js-stock-status');
    const inputId = container.querySelector('.js-variant-id');
    const quantityInput = container.querySelector('.js-quantity-input');
    const priceDisplay = container.querySelector('.pd-detail__price');

    const selectedColor = container.getAttribute('data-selected-color');
    const selectedSize = container.getAttribute('data-selected-size');

    // Nếu chưa chọn đủ Màu và Size
    if (!selectedColor || !selectedSize) {
        if(stockStatus) {
            stockStatus.style.display = 'block';
            stockStatus.innerHTML = '<i class="fas fa-info-circle"></i> Vui lòng chọn Màu sắc và Kích thước.';
            stockStatus.className = 'js-stock-status u-s-m-b-15 text-warning fw-bold';
        }
        if(btnAdd) btnAdd.disabled = true;
        return;
    }

    // Tìm biến thể khớp với Màu và Size đã chọn
    const matchedVariant = danhSachBienThe.find(v => v.mauSac === selectedColor && v.trongLuong === selectedSize);

    if (matchedVariant) {
        // --- CẬP NHẬT THÔNG TIN CƠ BẢN ---
        inputId.value = matchedVariant.id;
        btnAdd.disabled = false;
        
        if(stockStatus) {
            stockStatus.style.display = 'block';
            stockStatus.innerHTML = `<i class="fas fa-check-circle"></i> Sản phẩm có sẵn (Còn ${matchedVariant.soLuongTon})`;
            stockStatus.className = 'js-stock-status u-s-m-b-15 text-success fw-bold';
        }

        if (quantityInput) quantityInput.setAttribute('data-max', matchedVariant.soLuongTon);
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
        inputId.value = "";
        btnAdd.disabled = true;
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
                  window.location.href = '/user/dang-nhap';
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
              type: 'GET',
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
                  window.location.href = '/user/dang-nhap';
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