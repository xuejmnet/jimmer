package org.babyfish.jimmer.sql.mutation;

import org.babyfish.jimmer.sql.ast.tuple.Tuple2;
import org.babyfish.jimmer.sql.common.AbstractMutationTest;
import static org.babyfish.jimmer.sql.common.Constants.*;

import org.babyfish.jimmer.sql.model.AuthorTableEx;
import org.babyfish.jimmer.sql.model.BookTableEx;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class AssociationMutationTest extends AbstractMutationTest {

    @Test
    public void testInsertIgnore() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(
                        BookTableEx.class, BookTableEx::authors
                ).saveCommand(
                       learningGraphQLId1, alexId
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select BOOK_ID, AUTHOR_ID " +
                                        "from BOOK_AUTHOR_MAPPING " +
                                        "where (BOOK_ID, AUTHOR_ID) in((?, ?))"
                        );
                        it.variables(learningGraphQLId1, alexId);
                    });
                    ctx.rowCount(0);
                }
        );
    }

    @Test
    public void testInsert() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(
                        BookTableEx.class, BookTableEx::authors
                ).saveCommand(
                        Arrays.asList(
                            new Tuple2<>(learningGraphQLId1, alexId),
                            new Tuple2<>(learningGraphQLId2, borisId),
                            new Tuple2<>(learningGraphQLId3, borisId)
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select BOOK_ID, AUTHOR_ID " +
                                        "from BOOK_AUTHOR_MAPPING " +
                                        "where (BOOK_ID, AUTHOR_ID) in((?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                learningGraphQLId1, alexId,
                                learningGraphQLId2, borisId,
                                learningGraphQLId3, borisId
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_AUTHOR_MAPPING(BOOK_ID, AUTHOR_ID) " +
                                        "values (?, ?), (?, ?)"
                        );
                        it.variables(learningGraphQLId2, borisId, learningGraphQLId3, borisId);
                    });
                    ctx.rowCount(2);
                }
        );
    }

    @Test
    public void testDelete() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(
                        BookTableEx.class, BookTableEx::authors
                ).deleteCommand(
                        Arrays.asList(learningGraphQLId1, learningGraphQLId2, learningGraphQLId3),
                        Arrays.asList(alexId, alexId, borisId)
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where (BOOK_ID, AUTHOR_ID) in ((?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                learningGraphQLId1, alexId,
                                learningGraphQLId2, alexId,
                                learningGraphQLId3, borisId
                        );
                    });
                    ctx.rowCount(2);
                }
        );
    }

    @Test
    public void testInverseInsertIgnore() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(
                        AuthorTableEx.class, AuthorTableEx::books
                ).saveCommand(
                        alexId, learningGraphQLId1
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select AUTHOR_ID, BOOK_ID " +
                                        "from BOOK_AUTHOR_MAPPING " +
                                        "where (AUTHOR_ID, BOOK_ID) in((?, ?))"
                        );
                        it.variables(alexId, learningGraphQLId1);
                    });
                    ctx.rowCount(0);
                }
        );
    }

    @Test
    public void testInverseInsert() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(
                        AuthorTableEx.class, AuthorTableEx::books
                ).saveCommand(
                        Arrays.asList(
                                new Tuple2<>(alexId, learningGraphQLId1),
                                new Tuple2<>(borisId, learningGraphQLId2),
                                new Tuple2<>(borisId, learningGraphQLId3)
                        )
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "select AUTHOR_ID, BOOK_ID " +
                                        "from BOOK_AUTHOR_MAPPING " +
                                        "where (AUTHOR_ID, BOOK_ID) in((?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                alexId, learningGraphQLId1,
                                borisId, learningGraphQLId2,
                                borisId, learningGraphQLId3
                        );
                    });
                    ctx.statement(it -> {
                        it.sql(
                                "insert into BOOK_AUTHOR_MAPPING(AUTHOR_ID, BOOK_ID) " +
                                        "values (?, ?), (?, ?)"
                        );
                        it.variables(borisId, learningGraphQLId2, borisId, learningGraphQLId3);
                    });
                    ctx.rowCount(2);
                }
        );
    }

    @Test
    public void testInverseDelete() {
        executeAndExpectRowCount(
                getSqlClient().getAssociations(
                        AuthorTableEx.class, AuthorTableEx::books
                ).deleteCommand(
                        Arrays.asList(alexId, alexId, borisId),
                        Arrays.asList(learningGraphQLId1, learningGraphQLId2, learningGraphQLId3)
                ),
                ctx -> {
                    ctx.statement(it -> {
                        it.sql(
                                "delete from BOOK_AUTHOR_MAPPING " +
                                        "where (AUTHOR_ID, BOOK_ID) in ((?, ?), (?, ?), (?, ?))"
                        );
                        it.variables(
                                alexId, learningGraphQLId1,
                                alexId, learningGraphQLId2,
                                borisId, learningGraphQLId3
                        );
                    });
                    ctx.rowCount(2);
                }
        );
    }
}
