package com.mv.movieservice.infrastructure.adapter;

import com.mv.movieservice.application.dto.PageResult;
import com.mv.movieservice.application.port.out.CataloguePort;
import com.mv.movieservice.domain.model.movie.valueobject.Genre;
import com.mv.movieservice.domain.model.movie.valueobject.Person;
import com.mv.movieservice.infrastructure.persistence.entity.GenreEntity;
import com.mv.movieservice.infrastructure.persistence.entity.PersonEntity;
import com.mv.movieservice.infrastructure.persistence.mapper.MoviePersistenceMapper;
import com.mv.movieservice.infrastructure.persistence.repository.GenreJpaRepository;
import com.mv.movieservice.infrastructure.persistence.repository.PersonJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CatalogueRepositoryAdapter implements CataloguePort {

    private final GenreJpaRepository genreJpaRepository;
    private final PersonJpaRepository personJpaRepository;
    private final MoviePersistenceMapper mapper;

    @Override
    public List<Genre> findGenresByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return genreJpaRepository.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Genre> findAllGenres() {
        return genreJpaRepository.findAll(Sort.by("name")).stream().map(mapper::toDomain).toList();
    }

    @Override
    public boolean genreNameExists(String name) {
        return genreJpaRepository.existsByNameIgnoreCase(name);
    }

    @Override
    public Genre saveGenre(Genre genre) {
        GenreEntity saved = genreJpaRepository.save(GenreEntity.builder().id(genre.id()).name(genre.name()).build());
        return mapper.toDomain(saved);
    }

    @Override
    public List<Person> findPersonsByIds(Collection<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return personJpaRepository.findAllById(ids).stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<Person> searchPersons(String keyword, int page, int size) {
        Page<PersonEntity> result = personJpaRepository.search(keyword, PageRequest.of(page, size, Sort.by("name")));
        return new PageResult<>(result.getContent().stream().map(mapper::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    @Override
    public Person savePerson(Person person) {
        return mapper.toDomain(personJpaRepository.save(mapper.toEntity(person)));
    }
}
