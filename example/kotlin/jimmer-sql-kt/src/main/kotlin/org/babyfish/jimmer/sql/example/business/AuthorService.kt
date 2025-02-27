package org.babyfish.jimmer.sql.example.business

import org.babyfish.jimmer.client.FetchBy
import org.babyfish.jimmer.client.ThrowsAll
import org.babyfish.jimmer.sql.example.repository.AuthorRepository
import org.babyfish.jimmer.sql.example.model.Author
import org.babyfish.jimmer.sql.example.model.Gender
import org.babyfish.jimmer.sql.example.model.by
import org.babyfish.jimmer.spring.model.SortUtils
import org.babyfish.jimmer.sql.example.model.input.AuthorInput
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher
import org.babyfish.jimmer.sql.runtime.SaveErrorCode
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*

/**
 * A real project should be a three-tier architecture consisting
 * of repository, service, and controller.
 *
 * This demo has no business logic, its purpose is only to tell users
 * how to use jimmer with the <b>least</b> code. Therefore, this demo
 * does not follow this convention, and let services be directly
 * decorated by `@RestController`, not `@Service`.
 */
@RestController
@RequestMapping("/author")
@Transactional
class AuthorService(
    private val authorRepository: AuthorRepository
) {

    @GetMapping("/simpleList")
    fun findSimpleAuthors(
    ): List<@FetchBy("SIMPLE_FETCHER") Author> =
        authorRepository.findAll(SIMPLE_FETCHER) {
            asc(Author::firstName)
            asc(Author::lastName)
        }

    @GetMapping("/list")
    fun findAuthors(
        @RequestParam(defaultValue = "firstName asc, lastName asc") sortCode: String,
        @RequestParam firstName: String?,
        @RequestParam lastName: String?,
        @RequestParam gender: Gender?
    ): List<@FetchBy("DEFAULT_FETCHER") Author> =
        authorRepository.findByFirstNameAndLastNameAndGender(
            SortUtils.toSort(sortCode),
            firstName,
            lastName,
            gender,
            DEFAULT_FETCHER
        )

    @GetMapping("/{id}")
    fun findComplexAuthor(
        @PathVariable id: Long
    ): @FetchBy("COMPLEX_FETCHER") Author? =
        authorRepository.findNullable(id, COMPLEX_FETCHER)

    @PutMapping
    @ThrowsAll(SaveErrorCode::class)
    fun saveAuthor(@RequestBody input: AuthorInput): Author =
        authorRepository.save(input)

    @DeleteMapping("/{id}")
    fun deleteAuthor(@PathVariable id: Long) {
        authorRepository.deleteById(id)
    }

    companion object {

        @JvmStatic
        private val SIMPLE_FETCHER = newFetcher(Author::class).by {
            firstName()
            lastName()
        }

        @JvmStatic
        private val DEFAULT_FETCHER = newFetcher(Author::class).by {
            allScalarFields()
        }

        @JvmStatic
        private val COMPLEX_FETCHER = newFetcher(Author::class).by {
            allScalarFields()
            books {
                allScalarFields()
                tenant(false)
                store {
                    allScalarFields()
                    avgPrice()
                }
            }
        }
    }
}