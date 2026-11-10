import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Tv, Volume2, Armchair, Sparkles, CheckCircle2, Ticket, ArrowRight, Film } from 'lucide-react';
import { isAuthenticated } from '../api';

export default function FeaturesPage() {
  const navigate = useNavigate();

  const handleBookNow = () => {
    if (!isAuthenticated()) {
      navigate('/login');
    } else {
      navigate('/movies');
    }
  };

  const features = [
    {
      id: 'imax',
      title: 'MÀN HÌNH IMAX LASER CỰC ĐẠI',
      subtitle: 'Trải Nghiệm Điện Ảnh Đỉnh Cao Chuẩn Quốc Tế',
      badge: 'CÔNG NGHỆ FLAGSHIP',
      icon: Tv,
      image: 'https://images.unsplash.com/photo-1517604931442-7e0c8ed2963c?w=1000&auto=format&fit=crop&q=80',
      description:
        'Hệ thống phòng chiếu IMAX Laser của MTK Cinemas sở hữu màn hình cong tràn viền cao tương đương tòa nhà 5 tầng với diện tích vượt trội trên 300m². Sử dụng bộ đôi máy chiếu 4K Laser độc quyền, mang lại độ sắc nét tuyệt đối, dải màu rộng gấp đôi và độ tương phản sâu thẳm.',
      highlights: [
        'Màn hình cong cực đại tràn viền góc nhìn trọn vẹn',
        'Máy chiếu Laser kép thế hệ mới siêu sáng',
        'Âm thanh điều chỉnh laser độc quyền IMAX',
        'Trải nghiệm đắm chìm tối đa trong từng khung hình',
      ],
    },
    {
      id: 'dolby-atmos',
      title: 'ÂM THANH VÒM DOLBY ATMOS 360°',
      subtitle: 'Âm Thanh Đa Chiều Đánh Thức Mọi Giác Quan',
      badge: 'CHUẨN ÂM THANH MỸ',
      icon: Volume2,
      image: 'https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=1000&auto=format&fit=crop&q=80',
      description:
        'Dolby Atmos giải phóng âm thanh khỏi các kênh truyền thống để di chuyển tự do xung quanh và phía trên đầu người xem trong không gian 3 chiều. Từ tiếng lá rơi nhẹ nhàng cho đến tiếng gầm rú của động cơ phản lực, bạn sẽ cảm giác như mọi sự kiện đang diễn ra ngay cạnh mình.',
      highlights: [
        'Hơn 64 loa độc lập phân bố cả trên trần và xung quanh',
        'Phân tách đối tượng âm thanh Object-Based Audio tiên tiến',
        'Tái tạo tiếng bass trầm sâu rung chuyển phòng chiếu',
        'Khử nhiễu âm quang học bằng vách tiêu âm cao cấp',
      ],
    },
    {
      id: 'sweetbox',
      title: 'GHẾ ĐÔI SWEETBOX RIÊNG TƯ',
      subtitle: 'Không Gian Lãng Mạn Hoàn Hảo Dành Cho Cặp Đôi',
      badge: 'ĐỘC QUYỀN CẶP ĐÔI',
      icon: Armchair,
      image: 'https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=1000&auto=format&fit=crop&q=80',
      description:
        'Được bố trí ở hàng ghế cuối cùng với tầm nhìn bao quát toàn bộ màn chiếu, ghế đôi SweetBox mang đến sự riêng tư tuyệt đối cho các cặp đôi nhờ vách ngăn cao cách điệu. Nệm ghế bọc da nhung êm ái, diện tích rộng rãi cùng khay để bắp nước đôi tiện lợi.',
      highlights: [
        'Vách ngăn riêng tư cao cách âm nhẹ nhàng',
        'Chất liệu da nhung cao cấp siêu êm ái',
        'Tầm nhìn trọn vẹn từ vị trí trung tâm hàng cuối',
        'Khay để bắp nước và đồ uống dung tích lớn',
      ],
    },
    {
      id: 'screenx',
      title: 'CÔNG NGHỆ SCREENX 270 ĐỘ',
      subtitle: 'Góc Nhìn Siêu Rộng Mở Ra 2 Bên Tường Phòng Chiếu',
      badge: 'ĐỘT PHÁ CÔNG NGHỆ',
      icon: Sparkles,
      image: 'https://images.unsplash.com/photo-1489599849927-2ee91cede3ba?w=1000&auto=format&fit=crop&q=80',
      description:
        'ScreenX là công nghệ chiếu phim đa diện đầu tiên trên thế giới, mở rộng hình ảnh từ màn hình chính sang cả hai bên tường của rạp chiếu. Khán giả không chỉ nhìn về phía trước mà còn được bao bọc bởi góc nhìn 270 độ, đưa trải nghiệm xem phim vượt ra khỏi giới hạn của màn ảnh truyền thống.',
      highlights: [
        'Góc nhìn mở rộng 270 độ toàn cảnh',
        'Hệ thống máy chiếu phụ đồng bộ hai bên vách tường',
        'Gia tăng cảm giác nghẹt thở trong các pha rượt đuổi',
        'Thiết kế phòng chiếu tối ưu góc nhìn từ mọi vị trí',
      ],
    },
  ];

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 pb-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 pt-6 space-y-10">
        {/* Header Banner */}
        <div className="bg-gradient-to-r from-red-600 via-rose-600 to-red-700 rounded-3xl p-8 sm:p-12 text-white shadow-xl relative overflow-hidden">
          <div className="relative z-10 max-w-2xl">
            <span className="px-3 py-1 rounded-full text-xs font-black uppercase tracking-wider bg-white/20 inline-block mb-3 backdrop-blur-xs">
              MTK SIGNATURE EXPERIENCES
            </span>
            <h1 className="text-3xl sm:text-5xl font-black uppercase tracking-tight">
              CÔNG NGHỆ & ĐẶC TRƯNG MTK
            </h1>
            <p className="text-red-100 text-sm sm:text-base mt-3 leading-relaxed">
              Khám phá hệ sinh thái phòng chiếu tiêu chuẩn quốc tế và những công nghệ nghe nhìn độc quyền định hình thương hiệu MTK Cinemas.
            </p>
          </div>
          <div className="absolute right-0 bottom-0 opacity-10 pointer-events-none transform translate-x-12 translate-y-12">
            <Film className="w-80 h-80" />
          </div>
        </div>

        {/* Feature Sections */}
        <div className="space-y-8">
          {features.map((feat, idx) => {
            const IconComp = feat.icon;
            const isEven = idx % 2 === 1;

            return (
              <div
                key={feat.id}
                className={`grid grid-cols-1 lg:grid-cols-2 gap-8 items-center bg-white border border-slate-200 rounded-3xl p-6 sm:p-10 shadow-sm ${
                  isEven ? 'lg:flex-row-reverse' : ''
                }`}
              >
                <div className={`space-y-4 ${isEven ? 'lg:order-2' : ''}`}>
                  <div className="flex items-center gap-2">
                    <div className="w-10 h-10 rounded-xl bg-red-50 border border-red-100 flex items-center justify-center text-red-600 shadow-xs">
                      <IconComp className="w-5 h-5" />
                    </div>
                    <span className="px-2.5 py-1 rounded-full text-[10px] font-black uppercase tracking-wider bg-amber-50 text-amber-700 border border-amber-200">
                      {feat.badge}
                    </span>
                  </div>

                  <h2 className="text-2xl sm:text-3xl font-black text-slate-900 uppercase tracking-tight">
                    {feat.title}
                  </h2>
                  <p className="text-xs font-bold text-red-600 uppercase tracking-wider">
                    {feat.subtitle}
                  </p>
                  <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
                    {feat.description}
                  </p>

                  {/* Highlights */}
                  <div className="pt-2 grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
                    {feat.highlights.map((h, i) => (
                      <div key={i} className="flex items-start gap-2 text-slate-700 font-medium">
                        <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                        <span>{h}</span>
                      </div>
                    ))}
                  </div>

                  <div className="pt-4">
                    <button
                      onClick={handleBookNow}
                      className="px-5 py-2.5 rounded-xl bg-red-600 hover:bg-red-700 text-white font-bold text-xs uppercase tracking-wider flex items-center gap-2 shadow-sm shadow-red-200 transition-all hover:scale-105 active:scale-95"
                    >
                      <Ticket className="w-4 h-4" />
                      <span>Trải Nghiệm Suất Chiếu Này</span>
                    </button>
                  </div>
                </div>

                <div className={`relative rounded-3xl overflow-hidden border border-slate-200 shadow-md aspect-[16/10] ${isEven ? 'lg:order-1' : ''}`}>
                  <img
                    src={feat.image}
                    alt={feat.title}
                    className="w-full h-full object-cover hover:scale-105 transition-transform duration-700"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-slate-950/40 via-transparent to-transparent" />
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
