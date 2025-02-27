package org.babyfish.jimmer.sql.example.model.input;

import lombok.Data;
import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.sql.example.model.Book;
import org.jetbrains.annotations.Nullable;
import org.mapstruct.*;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BookInput implements Input<Book> {

    private static final Converter CONVERTER = Mappers.getMapper(Converter.class);

    @Nullable
    private Long id;

    private String name;

    private int edition;

    private BigDecimal price;

    private Long storeId;

    private List<Long> authorIds;

    @Override
    public Book toEntity() {
        return CONVERTER.toBook(this);
    }

    @Mapper
    interface Converter {

        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
        Book toBook(BookInput input);
    }

    /*
     * If `Book` does not support `storeId` and `authorIds` which
     * are decorated by `@IdView`, the mapper should look like this
     */
//    @Mapper
//    interface Converter {
//
//        @Mapping(target = "store", source = "storeId")
//        @Mapping(target = "authors", source = "authorIds")
//        @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE)
//        Book toBook(BookInput input);
//
//        @BeanMapping(ignoreByDefault = true)
//        @Mapping(target = "id", source = ".")
//        BookStore toBookStore(Long id);
//
//        @BeanMapping(ignoreByDefault = true)
//        @Mapping(target = "id", source = ".")
//        Author toAuthor(Long id);
//    }
}
