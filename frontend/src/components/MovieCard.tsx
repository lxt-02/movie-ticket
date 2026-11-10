import { Link } from 'react-router-dom';
import { StatusBadge } from './ui';

interface Props {
  id: string;
  title: string;
  posterUrl?: string | null;
  status: string;
  meta?: string;
  highlightHtml?: string;
}

export function MovieCard({ id, title, posterUrl, status, meta, highlightHtml }: Props) {
  return (
    <Link to={`/movies/${id}`} className="movie-card">
      <div className="poster">{posterUrl ? <img src={posterUrl} alt={title} loading="lazy" /> : <span>🎞</span>}</div>
      <div className="movie-card-body">
        <div className="movie-title">{title}</div>
        <StatusBadge value={status} />
        {meta && <div className="muted small">{meta}</div>}
        {/* search highlight contains <em> tags from Elasticsearch */}
        {highlightHtml && <div className="small highlight" dangerouslySetInnerHTML={{ __html: highlightHtml }} />}
      </div>
    </Link>
  );
}
