package com.mv.movieservice.application.service;

import com.mv.movieservice.application.dto.PageResult;
import com.mv.movieservice.application.port.in.ManageCatalogueUseCase;
import com.mv.movieservice.application.port.out.CataloguePort;
import com.mv.movieservice.domain.model.movie.exception.InvalidMovieException;
import com.mv.movieservice.domain.model.movie.valueobject.Genre;
import com.mv.movieservice.domain.model.movie.valueobject.Person;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ManageCatalogueService implements ManageCatalogueUseCase {

    private final CataloguePort cataloguePort;

    @Override
    @Transactional(readOnly = true)
    public List<Genre> listGenres() {
        return cataloguePort.findAllGenres();
    }

    @Override
    public Genre createGenre(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidMovieException("Tên thể loại không được để trống");
        }
        if (trimmed.length() > 100) {
            throw new InvalidMovieException("Tên thể loại tối đa 100 ký tự");
        }
        if (cataloguePort.genreNameExists(trimmed)) {
            throw new InvalidMovieException("Thể loại '" + trimmed + "' đã tồn tại");
        }
        return cataloguePort.saveGenre(new Genre(UUID.randomUUID(), trimmed));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Person> searchPersons(String keyword, int page, int size) {
        String q = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return cataloguePort.searchPersons(q, Math.max(0, page), Math.min(100, Math.max(1, size)));
    }

    @Override
    public Person createPerson(String name, String avatarUrl, String biography) {
        return cataloguePort.savePerson(new Person(UUID.randomUUID(), name, avatarUrl, biography));
    }
}
