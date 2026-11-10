import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  FileText,
  CreditCard,
  Lock,
  ShieldCheck,
  HelpCircle,
  ChevronRight,
  ArrowRight,
  Ticket,
  Film
} from 'lucide-react';
import { isAuthenticated } from '../api';

export default function TermsPage({ initialTab = 'general' }) {
  const [activeTab, setActiveTab] = useState(initialTab);
  const navigate = useNavigate();

  const tabs = [
    { key: 'general', label: 'Điều Khoản Chung', icon: FileText },
    { key: 'transaction', label: 'Điều Khoản Giao Dịch', icon: Ticket },
    { key: 'payment', label: 'Chính Sách Thanh Toán', icon: CreditCard },
    { key: 'privacy', label: 'Chính Sách Bảo Mật', icon: Lock },
    { key: 'faq', label: 'Câu Hỏi Thường Gặp (FAQ)', icon: HelpCircle },
  ];

  const handleBookNow = () => {
    if (!isAuthenticated()) {
      navigate('/login');
    } else {
      navigate('/movies');
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 pb-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 pt-6 space-y-8">
        {/* Page Header */}
        <div className="bg-gradient-to-r from-red-600 via-rose-600 to-red-700 rounded-3xl p-8 sm:p-12 text-white shadow-xl relative overflow-hidden">
          <div className="relative z-10 max-w-2xl">
            <span className="px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-white/20 inline-block mb-3 backdrop-blur-xs">
              MTK LEGAL & POLICIES
            </span>
            <h1 className="text-3xl sm:text-5xl font-black uppercase tracking-tight">
              CHÍNH SÁCH & ĐIỀU KHOẢN
            </h1>
            <p className="text-red-100 text-sm sm:text-base mt-3 leading-relaxed">
              Quy định quyền lợi, nghĩa vụ và hướng dẫn giao dịch an toàn tại toàn bộ hệ thống rạp MTK Cinemas.
            </p>
          </div>
          <div className="absolute right-0 bottom-0 opacity-10 pointer-events-none transform translate-x-12 translate-y-12">
            <Film className="w-80 h-80" />
          </div>
        </div>

        {/* Main Grid: Sidebar & Content */}
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-8">
          {/* Left Sticky Sidebar */}
          <div className="lg:col-span-1">
            <div className="sticky top-24 bg-white border border-slate-200 rounded-3xl p-4 shadow-sm space-y-1.5">
              <span className="text-[11px] font-black uppercase tracking-widest text-slate-400 px-3 py-2 block">
                MỤC LỤC CHÍNH SÁCH
              </span>
              {tabs.map((t) => {
                const IconComp = t.icon;
                const isActive = activeTab === t.key;
                return (
                  <button
                    key={t.key}
                    onClick={() => setActiveTab(t.key)}
                    className={`w-full flex items-center justify-between px-3.5 py-3 rounded-2xl text-xs font-bold transition-all text-left ${
                      isActive
                        ? 'bg-red-600 text-white shadow-sm shadow-red-200'
                        : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                    }`}
                  >
                    <div className="flex items-center gap-2.5">
                      <IconComp className="w-4 h-4 shrink-0" />
                      <span>{t.label}</span>
                    </div>
                    {isActive && <ChevronRight className="w-4 h-4" />}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Right Content Panel */}
          <div className="lg:col-span-3 bg-white border border-slate-200 rounded-3xl p-6 sm:p-10 shadow-sm text-sm text-slate-700 leading-relaxed space-y-6">
            {/* 1. ĐIỀU KHOẢN CHUNG */}
            {activeTab === 'general' && (
              <div className="space-y-6 animate-in fade-in duration-200">
                <div className="border-b border-slate-100 pb-4">
                  <span className="text-xs font-bold text-red-600 uppercase">Phần 1</span>
                  <h2 className="text-2xl font-black text-slate-900 uppercase tracking-tight mt-1">
                    ĐIỀU KHOẢN CHUNG TẠI RẠP CHIẾU PHIM MTK
                  </h2>
                </div>

                <div className="space-y-4">
                  <h3 className="font-bold text-slate-900 text-base">1. Quy định độ tuổi khán giả (Age Rating)</h3>
                  <p>
                    MTK Cinemas tuân thủ nghiêm ngặt theo phân loại độ tuổi của Cục Điện Ảnh Việt Nam:
                  </p>
                  <ul className="list-disc pl-5 space-y-1.5 text-xs text-slate-600">
                    <li><strong className="text-slate-900">P (Mọi lứa tuổi):</strong> Phim phổ biến cho tất cả mọi người.</li>
                    <li><strong className="text-slate-900">K (Khuyến nghị):</strong> Khán giả dưới 13 tuổi nên có phụ huynh đi cùng.</li>
                    <li><strong className="text-slate-900">T13 (Từ đủ 13 tuổi):</strong> Khán giả từ 13 tuổi trở lên.</li>
                    <li><strong className="text-slate-900">T16 (Từ đủ 16 tuổi):</strong> Khán giả từ 16 tuổi trở lên.</li>
                    <li><strong className="text-slate-900">T18 / C18 (Từ đủ 18 tuổi):</strong> Khán giả từ 18 tuổi trở lên (Vui lòng mang theo CCCD/VNeID).</li>
                  </ul>

                  <h3 className="font-bold text-slate-900 text-base pt-2">2. Quy định thức ăn & đồ uống</h3>
                  <p>
                    Để bảo đảm an toàn vệ sinh và tránh gây ảnh hưởng mùi đến các khán giả khác, quý khách vui lòng chỉ sử dụng bắp nước và các món ăn nhẹ được phân phối trực tiếp tại quầy Concession của MTK Cinemas.
                  </p>

                  <h3 className="font-bold text-slate-900 text-base pt-2">3. Bản quyền & Văn hóa rạp chiếu</h3>
                  <p>
                    Nghiêm cấm tuyệt đối mọi hành vi quay phim, chụp ảnh, livestream hoặc ghi âm nội dung phim dưới bất kỳ hình thức nào. Mọi hành vi vi phạm sẽ được xử lý nghiêm theo Luật Sở Hữu Trí Tuệ.
                  </p>
                </div>
              </div>
            )}

            {/* 2. ĐIỀU KHOẢN GIAO DỊCH */}
            {activeTab === 'transaction' && (
              <div className="space-y-6 animate-in fade-in duration-200">
                <div className="border-b border-slate-100 pb-4">
                  <span className="text-xs font-bold text-red-600 uppercase">Phần 2</span>
                  <h2 className="text-2xl font-black text-slate-900 uppercase tracking-tight mt-1">
                    ĐIỀU KHOẢN GIAO DỊCH VÉ TRỰC TUYẾN
                  </h2>
                </div>

                <div className="space-y-4">
                  <h3 className="font-bold text-slate-900 text-base">1. Quy trình đặt vé</h3>
                  <p>
                    Khi chọn xong vị trí ghế, hệ thống sẽ tự động khóa giữ ghế (Seat Hold) cho quý khách trong thời gian <strong className="text-red-600 font-bold">10 phút</strong>. Quý khách vui lòng hoàn tất thanh toán trước khi đồng hồ đếm ngược kết thúc để tránh việc ghế bị giải phóng cho người khác.
                  </p>

                  <h3 className="font-bold text-slate-900 text-base pt-2">2. Mã vé điện tử (E-Ticket)</h3>
                  <p>
                    Sau khi thanh toán thành công qua PayOS VietQR, quý khách sẽ nhận được mã vé (Booking Code) và mã QR Code tại màn hình xác nhận và trong mục <strong className="text-slate-900">"Vé Của Tôi"</strong>. Bạn chỉ cần xuất trình mã này trực tiếp tại cửa rạp để nhân viên quét mã vào phòng chiếu.
                  </p>

                  <h3 className="font-bold text-slate-900 text-base pt-2">3. Chính sách đổi / hủy vé</h3>
                  <p>
                    Khách hàng có thể thực hiện hủy vé tự động trên website trước giờ chiếu ít nhất <strong className="text-slate-900">60 phút</strong>. Hệ thống sẽ hoàn tiền tự động theo quy định giao dịch trực tuyến.
                  </p>
                </div>
              </div>
            )}

            {/* 3. CHÍNH SÁCH THANH TOÁN */}
            {activeTab === 'payment' && (
              <div className="space-y-6 animate-in fade-in duration-200">
                <div className="border-b border-slate-100 pb-4">
                  <span className="text-xs font-bold text-red-600 uppercase">Phần 3</span>
                  <h2 className="text-2xl font-black text-slate-900 uppercase tracking-tight mt-1">
                    CHÍNH SÁCH THANH TOÁN PAYOS & BẢO MẬT GIAO DỊCH
                  </h2>
                </div>

                <div className="space-y-4">
                  <h3 className="font-bold text-slate-900 text-base">1. Cổng thanh toán quốc gia PayOS VietQR</h3>
                  <p>
                    MTK Cinemas tích hợp trực tiếp cổng thanh toán PayOS chuẩn NAPAS 24/7, hỗ trợ quét mã QR bằng tất cả các ứng dụng ngân hàng tại Việt Nam (Vietcombank, MB Bank, Techcombank, BIDV, ACB, VPBank...) và các ví điện tử liên kết.
                  </p>

                  <h3 className="font-bold text-slate-900 text-base pt-2">2. Bảo vệ Idempotency & Giao dịch an toàn</h3>
                  <p>
                    Mọi yêu cầu thanh toán và giữ chỗ đều được bảo vệ bởi khóa Idempotency Key độc nhất và cơ chế Outbox Pattern, bảo đảm không bao giờ xảy ra lỗi trừ tiền trùng lặp cho khách hàng.
                  </p>
                </div>
              </div>
            )}

            {/* 4. CHÍNH SÁCH BẢO MẬT */}
            {activeTab === 'privacy' && (
              <div className="space-y-6 animate-in fade-in duration-200">
                <div className="border-b border-slate-100 pb-4">
                  <span className="text-xs font-bold text-red-600 uppercase">Phần 4</span>
                  <h2 className="text-2xl font-black text-slate-900 uppercase tracking-tight mt-1">
                    CHÍNH SÁCH BẢO MẬT THÔNG TIN KHÁCH HÀNG
                  </h2>
                </div>

                <div className="space-y-4">
                  <p>
                    MTK Cinemas cam kết bảo mật tuyệt đối dữ liệu thông tin cá nhân của quý khách theo các tiêu chuẩn bảo mật điện toán đám mây cao nhất:
                  </p>
                  <ul className="list-disc pl-5 space-y-2 text-xs text-slate-600">
                    <li>Dữ liệu đăng nhập và mật khẩu được mã hóa một chiều qua hệ thống định danh OpenID Connect Keycloak.</li>
                    <li>Không chia sẻ hoặc bán dữ liệu người dùng cho bất kỳ bên thứ ba nào vì mục đích thương mại.</li>
                    <li>Mọi giao dịch ngân hàng đều được định tuyến trực tiếp qua cổng thanh toán được Ngân hàng Nhà nước cấp phép.</li>
                  </ul>
                </div>
              </div>
            )}

            {/* 5. FAQ */}
            {activeTab === 'faq' && (
              <div className="space-y-6 animate-in fade-in duration-200">
                <div className="border-b border-slate-100 pb-4">
                  <span className="text-xs font-bold text-red-600 uppercase">Phần 5</span>
                  <h2 className="text-2xl font-black text-slate-900 uppercase tracking-tight mt-1">
                    CÂU HỎI THƯỜNG GẶP (FAQ)
                  </h2>
                </div>

                <div className="space-y-4">
                  <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200">
                    <h4 className="font-bold text-slate-900 text-sm mb-1">
                      Tôi có cần in vé giấy ra không khi đã mua vé online?
                    </h4>
                    <p className="text-xs text-slate-600 leading-relaxed">
                      Không cần. Bạn chỉ cần mở điện thoại vào mục "Vé Của Tôi" và đưa mã QR Code cho nhân viên soát vé quét trực tiếp tại cửa rạp là có thể vào xem phim ngay.
                    </p>
                  </div>

                  <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200">
                    <h4 className="font-bold text-slate-900 text-sm mb-1">
                      Tôi có thể chọn ghế VIP hoặc ghế đôi Sweetbox như thế nào?
                    </h4>
                    <p className="text-xs text-slate-600 leading-relaxed">
                      Trên sơ đồ ghế ở trang Đặt vé, ghế VIP được tô màu vàng cam nhạt ở hàng trung tâm, còn ghế đôi Sweetbox được ký hiệu bằng biểu tượng ghế rộng ở hàng cuối cùng.
                    </p>
                  </div>

                  <div className="p-4 rounded-2xl bg-slate-50 border border-slate-200">
                    <h4 className="font-bold text-slate-900 text-sm mb-1">
                      Nếu thanh toán bị trừ tiền mà vé chưa cập nhật thì phải làm sao?
                    </h4>
                    <p className="text-xs text-slate-600 leading-relaxed">
                      Hệ thống tự động đồng bộ qua Kafka Outbox. Trong trường hợp hiếm hoi có độ trễ ngân hàng, quý khách hãy liên hệ Hotline 1900 6017 hoặc đưa mã giao dịch cho nhân viên tại quầy vé để được kích hoạt vé tức thì.
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Bottom Actions */}
            <div className="pt-6 border-t border-slate-100 flex flex-col sm:flex-row items-center justify-between gap-4">
              <span className="text-xs text-slate-500">
                Cần thêm sự hỗ trợ? Liên hệ hotline <strong className="text-red-600">1900 6017</strong>
              </span>
              <button
                onClick={handleBookNow}
                className="px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs uppercase tracking-wider flex items-center gap-2 shadow-sm shadow-red-200 transition-all hover:scale-105"
              >
                <Ticket className="w-4 h-4" />
                <span>Đặt Vé Xem Phim</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
