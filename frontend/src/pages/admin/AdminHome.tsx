import { Link } from 'react-router-dom';

const ITEMS = [
  { to: '/admin/movies', title: 'Phim', text: 'Tạo, sửa, đổi trạng thái, lưu trữ phim' },
  { to: '/admin/catalog', title: 'Thể loại · Đoàn phim · Chỉ mục tìm kiếm', text: 'Thêm thể loại, người; đồng bộ Elasticsearch' },
  { to: '/admin/cinemas', title: 'Rạp & phòng chiếu', text: 'Rạp, phòng, sơ đồ ghế, ghế hỏng' },
  { to: '/admin/showtimes', title: 'Suất chiếu', text: 'Xếp lịch, sửa giờ / giá, huỷ suất' },
  { to: '/admin/users', title: 'Người dùng', text: 'Tìm kiếm, khoá / mở khoá tài khoản' },
  { to: '/ops', title: 'Vận hành', text: 'Outbox, DLT payment events' },
];

export default function AdminHome() {
  return (
    <>
      <h1>Quản trị</h1>
      <div className="grid-3">
        {ITEMS.map((i) => (
          <Link key={i.to} to={i.to} className="card link-card">
            <h3>{i.title}</h3>
            <p className="muted small">{i.text}</p>
          </Link>
        ))}
      </div>
    </>
  );
}
