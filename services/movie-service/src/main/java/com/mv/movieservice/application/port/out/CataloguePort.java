package com.mv.movieservice.application.port.out;

import com.mv.movieservice.application.dto.PageResult;
import com.mv.movieservice.domain.model.movie.valueobject.Genre;
import com.mv.movieservice.domain.model.movie.valueobject.Person;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/** Genres and persons (directors / actors) referenced by movies. */
public interface CataloguePort {

    List<Genre> findGenresByIds(Collection<UUID> ids);

    /** All genres, sorted by name. */
    List<Genre> findAllGenres();

    boolean genreNameExists(String name);

    Genre saveGenre(Genre genre);

    List<Person> findPersonsByIds(Collection<UUID> ids);

    /** @param keyword part of the name (case-insensitive), or null for everyone */
    PageResult<Person> searchPersons(String keyword, int page, int size);

    Person savePerson(Person person);
}
