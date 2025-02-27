package org.babyfish.jimmer.sql.ast.impl;

import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.PropExpression;
import org.babyfish.jimmer.sql.ast.table.spi.PropExpressionImplementor;
import org.babyfish.jimmer.sql.runtime.ExecutionException;
import org.babyfish.jimmer.sql.runtime.ScalarProvider;
import org.babyfish.jimmer.sql.runtime.SqlBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

class InCollectionPredicate extends AbstractPredicate {

    private Expression<?> expression;

    private Collection<?> values;

    private Collection<?> convertedValues;

    private boolean negative;

    public InCollectionPredicate(
            Expression<?> expression,
            Collection<?> values,
            boolean negative
    ) {
        this.expression = expression;
        this.values = values;
        this.negative = negative;
    }

    @Override
    public void accept(@NotNull AstVisitor visitor) {
        ((Ast) expression).accept(visitor);
    }

    @Override
    public void renderTo(@NotNull SqlBuilder builder) {
        if (values.isEmpty()) {
            builder.sql(negative ? "1 = 1" : "1 = 0");
        } else {
            renderChild((Ast) expression, builder);
            builder.sql(negative ? " not in " : " in ").enter(SqlBuilder.ScopeType.LIST);
            Collection<?> convertedValues = this.convertedValues;
            if (convertedValues == null) {
                convertedValues = Literals.convert(values, expression, builder.getAstContext().getSqlClient());
                this.convertedValues = convertedValues;
            }
            for (Object value : convertedValues) {
                builder.separator().variable(value);
            }
            builder.leave();
        }
    }

    @Override
    public int precedence() {
        return 0;
    }

    @Override
    public Predicate not() {
        return new InCollectionPredicate(expression, values, !negative);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InCollectionPredicate)) return false;
        InCollectionPredicate that = (InCollectionPredicate) o;
        return negative == that.negative && expression.equals(that.expression) && values.equals(that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expression, values, negative);
    }
}
