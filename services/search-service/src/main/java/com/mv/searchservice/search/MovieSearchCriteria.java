package com.mv.searchservice.search;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Filters of GET /api/v1/search/movies. Lists are OR inside one criterion and AND across criteria
 * (genres=Action&genres=Drama&ageRating=T13 = (Action OR Drama) AND T13).
 *
 * @param keyword      free text: title, original title, directors, actors, characters, description
 * @param person       director or actor name only
 * @param genres       genre names
 * @param statuses     NOW_SHOWING / COMING_SOON / ENDED
 * @param ageRatings   P, K, T13, T16, T18
 * @param languages    exact language values
 * @param countries    exact country values
 * @param releaseFrom  release date >= (inclusive)
 * @param releaseTo    release date <= (inclusive)
 * @param durationMin  minutes >=
 * @param durationMax  minutes <=
 * @param sort         relevance (default) | newest | oldest | title | duration_asc | duration_desc
 */
public record MovieSearchCriteria(
        String keyword,
        String person,
        List<String> genres,
        List<String> statuses,
        List<String> ageRatings,
        List<String> languages,
        List<String> countries,
        LocalDate releaseFrom,
        LocalDate releaseTo,
        Integer durationMin,
        Integer durationMax,
        Sort sort,
        int page,
        int size
) {

    public static final int MAX_SIZE = 50;

    public enum Sort {
        RELEVANCE, NEWEST, OLDEST, TITLE, DURATION_ASC, DURATION_DESC;

        public static Sort parse(String value) {
            if (value == null || value.isBlank()) {
                return RELEVANCE;
            }
            try {
                return valueOf(value.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("sort phải là relevance, newest, oldest, title, duration_asc hoặc duration_desc");
            }
        }
    }

    public MovieSearchCriteria {
        keyword = blankToNull(keyword);
        person = blankToNull(person);
        genres = clean(genres, false);
        statuses = clean(statuses, true);
        ageRatings = clean(ageRatings, true);
        languages = clean(languages, false);
        countries = clean(countries, false);
        sort = sort == null ? Sort.RELEVANCE : sort;
        if (page < 0) {
            throw new IllegalArgumentException("page phải >= 0");
        }
        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException("size phải từ 1 đến " + MAX_SIZE);
        }
        if (releaseFrom != null && releaseTo != null && releaseFrom.isAfter(releaseTo)) {
            throw new IllegalArgumentException("releaseFrom phải trước hoặc bằng releaseTo");
        }
        if (durationMin != null && durationMax != null && durationMin > durationMax) {
            throw new IllegalArgumentException("durationMin phải nhỏ hơn hoặc bằng durationMax");
        }
        if (page * size + size > 10_000) {
            throw new IllegalArgumentException("Chỉ xem được 10.000 kết quả đầu tiên, hãy thu hẹp bộ lọc");
        }
    }

    /** Relevance without a keyword makes no sense: fall back to newest first. */
    public Sort effectiveSort() {
        return sort == Sort.RELEVANCE && keyword == null && person == null ? Sort.NEWEST : sort;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** Trims, drops blanks / duplicates, accepts comma-separated values (genres=Action,Drama). */
    private static List<String> clean(List<String> values, boolean upperCase) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .flatMap(v -> java.util.Arrays.stream(v.split(",")))
                .map(String::trim)
                .filter(v -> !v.isEmpty())
                .map(v -> upperCase ? v.toUpperCase(Locale.ROOT) : v)
                .distinct()
                .toList();
    }
}
