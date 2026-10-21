package com.mv.movieservice.application.port.in;

import com.mv.movieservice.application.dto.PageResult;
import com.mv.movieservice.domain.model.movie.valueobject.Genre;
import com.mv.movieservice.domain.model.movie.valueobject.Person;

import java.util.List;

/** Genres and persons that movies can reference. */
public interface ManageCatalogueUseCase {

    List<Genre> listGenres();

    Genre createGenre(String name);

    PageResult<Person> searchPersons(String keyword, int page, int size);

    Person createPerson(String name, String avatarUrl, String biography);
}
