# search-service – Tìm kiếm phim bằng Elasticsearch

## Luồng dữ liệu

```
Admin tạo / sửa / đổi trạng thái / xoá phim
        │  POST/PUT/PATCH/DELETE /api/v1/movies
        ▼
movie-service ── cùng 1 transaction ──► MySQL: movies + outbox_events (MOVIE_CREATED ...)
        │
        │  MovieOutboxRelay (mỗi 2s, 1 instance nhờ khoá outbox_relay_lock)
        ▼
Kafka topic "movie-events"  (key = movieId → các sự kiện của 1 phim luôn đúng thứ tự)
        │
        ▼
search-service: MovieEventListener → MovieIndexer
        │   ARCHIVED → xoá document, còn lại → upsert snapshot
        │   version = movie.updatedAt (version_type=external_gte): bản cũ đến muộn bị Elasticsearch bỏ qua
        ▼
Elasticsearch index "movies"  ◄── GET /api/v1/search/movies (qua gateway, không cần đăng nhập)
```

- **MySQL là nguồn dữ liệu gốc**, Elasticsearch chỉ là bản đọc (read model), dựng lại được bất cứ lúc nào.
- **Không mất sự kiện khi Kafka sập**: sự kiện nằm trong `outbox_events` cho tới khi gửi được.
- **Không mất sự kiện khi Elasticsearch sập**: consumer retry 1s, 2s, 4s... (tối đa 5 phút), quá hạn thì đẩy sang `movie-events.DLT`.
- **Lấp đầy index lần đầu**: movie-service gửi lại toàn bộ phim mỗi khi khởi động (`movie.search.sync-on-startup=true`), hoặc admin gọi `POST /api/v1/movies/search-sync`.

## Message trên topic `movie-events`

```json
{
  "eventId": "9b2f8a3e-...", "eventType": "MOVIE_CREATED", "aggregateId": "<movieId>",
  "occurredAt": "2026-10-21T08:00:00Z", "version": 1761033600000,
  "payload": {
    "id": "<movieId>", "title": "Lật Mặt 7: Một Điều Ước", "originalTitle": null, "description": "...",
    "durationMinutes": 138, "releaseDate": "2024-04-26", "ageRating": "K", "language": "Vietnamese",
    "country": "Vietnam", "posterUrl": "...", "status": "NOW_SHOWING",
    "genres": [{"id": "...", "name": "Drama"}],
    "directors": [{"id": "...", "name": "Lý Hải"}],
    "actors": [{"id": "...", "name": "...", "characterName": "..."}],
    "createdAt": "...", "updatedAt": "..."
  }
}
```

`eventType`: `MOVIE_CREATED`, `MOVIE_UPDATED`, `MOVIE_STATUS_CHANGED`, `MOVIE_ARCHIVED`, `MOVIE_SYNCED` (gửi lại khi resync).

## Index `movies` (`src/main/resources/elasticsearch/movies-index.json`)

| Field | Kiểu | Dùng cho |
|---|---|---|
| `title`, `originalTitle` | text (`vi_text`) + `.suggest` (edge n-gram) + `title.keyword` | tìm từ khoá, gợi ý khi gõ, sắp xếp theo tên |
| `description`, `characters` | text (`vi_text`) | tìm từ khoá |
| `directors`, `actors` | text + `.keyword` | tìm theo người |
| `genres`, `status`, `ageRating`, `language`, `country` | keyword | lọc + facet |
| `releaseDate`, `durationMinutes` | date, integer | lọc khoảng, sắp xếp |

Analyzer `vi_text` = `lowercase` + `asciifolding(preserve_original)`: gõ **"lat mat"** hay **"Lật Mặt"** đều ra *Lật Mặt 7*.

## API

### `GET /api/v1/search/movies`

| Tham số | Ví dụ | Ý nghĩa |
|---|---|---|
| `q` | `lat mat`, `dune`, `zendaya` | từ khoá: tên phim, tên gốc, đạo diễn, diễn viên, nhân vật, mô tả. Không dấu, gõ sai 1–2 ký tự vẫn ra |
| `person` | `villeneuve` | chỉ tìm theo đạo diễn / diễn viên |
| `genres` | `Drama` (lặp lại hoặc `Drama,Family`) | thể loại (OR trong cùng tiêu chí) |
| `status` | `NOW_SHOWING`, `COMING_SOON`, `ENDED` | trạng thái |
| `ageRating` | `P`, `K`, `T13`, `T16`, `T18` | độ tuổi |
| `language`, `country` | `Vietnamese`, `Vietnam` | ngôn ngữ, quốc gia |
| `releaseFrom`, `releaseTo` | `2024-01-01` | khoảng ngày phát hành |
| `durationMin`, `durationMax` | `90`, `150` | khoảng thời lượng (phút) |
| `sort` | `relevance` (mặc định khi có `q`), `newest` (mặc định khi không có `q`), `oldest`, `title`, `duration_asc`, `duration_desc` | sắp xếp |
| `page`, `size` | `0`, `12` (tối đa 50) | phân trang |

Các tiêu chí khác nhau kết hợp bằng AND. Ví dụ: `genres=Drama&genres=Family&ageRating=K` = (Drama OR Family) AND K.

Response:

```json
{
  "success": true, "message": "Thành công",
  "data": {
    "items": [{
      "id": "...", "title": "Lật Mặt 7: Một Điều Ước", "originalTitle": null, "description": "...",
      "posterUrl": "...", "genres": ["Drama", "Family"], "directors": ["Lý Hải"], "actors": [],
      "ageRating": "K", "language": "Vietnamese", "country": "Vietnam", "status": "NOW_SHOWING",
      "releaseDate": "2024-04-26", "durationMinutes": 138, "score": 8.2,
      "highlight": { "title": ["<em>Lật</em> <em>Mặt</em> 7: Một Điều Ước"] }
    }],
    "page": 0, "size": 12, "totalElements": 1, "totalPages": 1,
    "facets": {
      "genres": [{"value": "Drama", "count": 3}, {"value": "Family", "count": 2}],
      "status": [{"value": "NOW_SHOWING", "count": 4}],
      "ageRating": [], "language": [], "country": []
    },
    "tookMs": 4
  },
  "timestamp": "..."
}
```

- `highlight`: các từ khớp được bọc trong `<em>...</em>`. FE render bằng HTML và chỉ cho phép thẻ `em`.
- `facets` là facet đa chọn: mỗi facet được lọc bởi các tiêu chí **khác**, không lọc bởi chính nó. Vì vậy sau khi tick "Drama", facet thể loại vẫn hiện "Family (2)" để tick thêm.

### `GET /api/v1/search/movies/suggest?q=lat ma&size=5`

Gợi ý khi đang gõ: `[{ id, title, originalTitle, posterUrl, releaseDate, status }]`.

### Admin

| API | Service | Ý nghĩa |
|---|---|---|
| `POST /api/v1/movies/search-sync` | movie-service | gửi lại toàn bộ phim sang index (trả `{ "queued": n }`) |
| `POST /api/v1/search/admin/movies/recreate-index` | search-service | xoá và tạo lại index rỗng (sau khi sửa mapping), sau đó gọi `search-sync` |

Cả hai cần token có role **ADMIN**.

Lỗi: `400` khi tham số sai (ngày không đúng `yyyy-MM-dd`, `sort` lạ, `size > 50`...), `503` khi Elasticsearch không phản hồi.

## Chạy và demo

```bash
docker compose up -d                          # Redis, Kafka, Kafka UI, Keycloak, Zipkin, Elasticsearch
# chạy eureka, gateway, movie-service, search-service từ IDE (search-service: cổng 8087)
# hoặc toàn bộ: docker compose --profile app up -d --build

curl http://localhost:9200/movies/_count                         # số phim đã index
curl "http://localhost:8080/api/v1/search/movies?q=lat%20mat"    # không dấu
curl "http://localhost:8080/api/v1/search/movies?q=dun&sort=relevance"    # gõ dở
curl "http://localhost:8080/api/v1/search/movies?q=avatr"        # gõ sai
curl "http://localhost:8080/api/v1/search/movies?genres=Drama&ageRating=K&releaseFrom=2024-01-01"
curl "http://localhost:8080/api/v1/search/movies/suggest?q=ma"
```

Demo "tạo phim → Kafka → Elasticsearch":

1. Tạo phim mới bằng `POST /api/v1/movies` (token ADMIN).
2. Mở Kafka UI (http://localhost:8090), vào topic `movie-events` để xem message `MOVIE_CREATED`.
3. Khoảng 2–3 giây sau, phim đã tìm được qua `GET /api/v1/search/movies?q=<tên phim>`.
4. Gọi `DELETE /api/v1/movies/{id}` (archive) thì phim biến mất khỏi kết quả tìm kiếm.

Kibana (tuỳ chọn): `docker compose --profile kibana up -d kibana`, mở http://localhost:5601, vào Dev Tools và chạy `GET movies/_search`.
