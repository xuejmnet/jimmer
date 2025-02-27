package org.babyfish.jimmer.sql.ast.table.spi;

import org.babyfish.jimmer.Input;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ImmutableProps;
import org.babyfish.jimmer.sql.JoinType;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.NumericExpression;
import org.babyfish.jimmer.sql.ast.Predicate;
import org.babyfish.jimmer.sql.ast.Selection;
import org.babyfish.jimmer.sql.ast.impl.ExampleImpl;
import org.babyfish.jimmer.sql.ast.impl.PropExpressionImpl;
import org.babyfish.jimmer.sql.ast.impl.table.*;
import org.babyfish.jimmer.sql.ast.query.Example;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.ast.table.TableEx;
import org.babyfish.jimmer.sql.ast.table.WeakJoin;
import org.babyfish.jimmer.sql.fetcher.Fetcher;

import java.util.Objects;
import java.util.function.Function;

public abstract class AbstractTypedTable<E> implements TableProxy<E> {

    private final ImmutableType immutableType;

    protected final TableImplementor<E> raw;

    private final DelayedOperation<E> delayedOperation;

    private final String joinDisabledReason;

    private final Object identifier;

    protected AbstractTypedTable(ImmutableType type) {
        this.immutableType = type;
        this.raw = null;
        this.delayedOperation = null;
        this.joinDisabledReason = null;
        this.identifier = new Object();
    }

    protected AbstractTypedTable(Class<E> entityType) {
        this.immutableType = ImmutableType.get(entityType);
        this.raw = null;
        this.delayedOperation = null;
        this.joinDisabledReason = null;
        this.identifier = new Object();
    }

    protected AbstractTypedTable(Class<E> entityType, DelayedOperation<E> delayedOperation) {
        this.immutableType = ImmutableType.get(entityType);
        this.raw = null;
        this.delayedOperation = delayedOperation;
        this.joinDisabledReason = null;
        this.identifier = new Object();
    }

    protected AbstractTypedTable(TableImplementor<E> raw) {
        this.immutableType = raw.getImmutableType();
        this.raw = raw;
        this.joinDisabledReason = null;
        this.delayedOperation = null;
        this.identifier = new Object();
    }

    protected AbstractTypedTable(AbstractTypedTable<E> base, String joinDisabledReason) {
        this.immutableType = base.immutableType;
        this.raw = base.raw;
        this.delayedOperation = base.delayedOperation;
        this.joinDisabledReason = joinDisabledReason != null ? joinDisabledReason : base.joinDisabledReason;
        this.identifier = base.identifier;
    }

    @Override
    public ImmutableType getImmutableType() {
        return immutableType;
    }

    @Override
    public Predicate eq(Table<E> other) {
        if (raw != null) {
            return raw.eq(other);
        }
        if (other.getImmutableType() != immutableType) {
            throw new IllegalArgumentException("Cannot compare tables of different types");
        }
        String idPropName = immutableType.getIdProp().getName();
        return this.<Expression<Object>>get(idPropName).eq(other.get(idPropName));
    }

    @Override
    public Predicate eq(Example<E> example) {
        return ((ExampleImpl<E>) example).toPredicate(this);
    }

    @Override
    public Predicate eq(E example) {
        return eq(Example.of(example));
    }

    @Override
    public Predicate eq(Input<E> input) {
        return eq(Example.of(input));
    }

    @Override
    public Predicate isNull() {
        if (raw != null) {
            return raw.isNull();
        }
        String idPropName = immutableType.getIdProp().getName();
        return this.get(idPropName).isNull();
    }

    @Override
    public Predicate isNotNull() {
        if (raw != null) {
            return raw.isNotNull();
        }
        String idPropName = immutableType.getIdProp().getName();
        return this.get(idPropName).isNotNull();
    }

    @Override
    public NumericExpression<Long> count() {
        if (raw != null) {
            return raw.count();
        }
        String idPropName = immutableType.getIdProp().getName();
        return this.get(idPropName).count();
    }

    @Override
    public NumericExpression<Long> count(boolean distinct) {
        if (raw != null) {
            return raw.count();
        }
        String idPropName = immutableType.getIdProp().getName();
        return this.get(idPropName).count();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <XE extends Expression<?>> XE get(String prop) {
        if (raw != null) {
            return raw.get(prop);
        }
        ImmutableProp immutableProp = immutableType.getProp(prop);
        ImmutableProp idViewBaseProp = immutableProp.getIdViewBaseProp();
        if (idViewBaseProp != null && idViewBaseProp.isReference(TargetLevel.ENTITY)) {
            return join(idViewBaseProp.getName(), idViewBaseProp.isNullable() ? JoinType.LEFT : JoinType.INNER)
                    .get(idViewBaseProp.getTargetType().getIdProp().getName());
        }
        return (XE)PropExpressionImpl.of(this, immutableProp);
    }

    @Override
    public <XT extends Table<?>> XT join(String prop) {
        if (raw != null) {
            __beforeJoin();
            return raw.join(prop);
        }
        return TableProxies.fluent(
                new DelayJoin<>(
                        this,
                        immutableType.getProp(prop),
                        JoinType.INNER,
                        null
                )
        );
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType) {
        if (raw != null) {
            __beforeJoin();
            return raw.join(prop, joinType);
        }
        return TableProxies.fluent(
                new DelayJoin<>(
                        this,
                        immutableType.getProp(prop),
                        joinType,
                        null
                )
        );
    }

    @Override
    public <XT extends Table<?>> XT join(String prop, JoinType joinType, ImmutableType treatedAs) {
        if (raw != null) {
            __beforeJoin();
            return raw.join(prop, joinType, treatedAs);
        }
        return TableProxies.fluent(
                new DelayJoin<>(
                        this,
                        immutableType.getProp(prop),
                        joinType,
                        treatedAs
                )
        );
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop) {
        if (raw != null) {
            __beforeJoin();
            return raw.inverseJoin(prop);
        }
        return TableProxies.fluent(
                new DelayInverseJoin<>(
                        this,
                        prop,
                        JoinType.INNER
                )
        );
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(ImmutableProp prop, JoinType joinType) {
        if (raw != null) {
            __beforeJoin();
            return raw.inverseJoin(prop, joinType);
        }
        return TableProxies.fluent(new DelayInverseJoin<>(this, prop, joinType));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop) {
        if (raw != null) {
            __beforeJoin();
            return raw.inverseJoin(prop);
        }
        return TableProxies.fluent(new DelayInverseJoin<>(this, prop.unwrap(), JoinType.INNER));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(TypedProp.Association<?, ?> prop, JoinType joinType) {
        if (raw != null) {
            __beforeJoin();
            return raw.inverseJoin(prop, joinType);
        }
        return TableProxies.fluent(new DelayInverseJoin<>(this, prop.unwrap(), joinType));
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<?>> backPropBlock
    ) {
        if (raw != null) {
            __beforeJoin();
            return raw.inverseJoin(targetTableType, backPropBlock);
        }
        return TableProxies.fluent(
                new DelayInverseJoin<>(
                        this,
                        ImmutableProps.join(targetTableType, backPropBlock),
                        JoinType.INNER
                )
        );
    }

    @Override
    public <XT extends Table<?>> XT inverseJoin(
            Class<XT> targetTableType,
            Function<XT, ? extends Table<?>> backPropBlock,
            JoinType joinType
    ) {
        if (raw != null) {
            __beforeJoin();
            return raw.inverseJoin(targetTableType, backPropBlock, joinType);
        }
        return TableProxies.fluent(
                new DelayInverseJoin<>(
                        this,
                        ImmutableProps.join(targetTableType, backPropBlock),
                        joinType
                )
        );
    }

    @Override
    public Selection<E> fetch(Fetcher<E> fetcher) {
        if (fetcher == null) {
            return this;
        }
        if (raw != null) {
            return raw.fetch(fetcher);
        }
        return new FetcherSelectionImpl<E>(this, fetcher);
    }

    @Override
    public Table<?> __parent() {
        if (raw != null) {
            return raw.getParent();
        }
        if (delayedOperation != null) {
            return delayedOperation.parent();
        }
        return null;
    }

    @Override
    public ImmutableProp __prop() {
        if (raw != null) {
            return raw.getJoinProp();
        }
        if (delayedOperation != null) {
            return delayedOperation.prop();
        }
        return null;
    }

    @Override
    public boolean __isInverse() {
        return delayedOperation instanceof DelayInverseJoin<?>;
    }

    @Override
    public TableImplementor<E> __unwrap() {
        return raw;
    }

    @Override
    public TableImplementor<E> __resolve(RootTableResolver resolver) {
        if (raw != null) {
            return raw;
        }
        if (delayedOperation != null) {
            return delayedOperation.resolve(resolver);
        }
        if (resolver == null) {
            throw new IllegalArgumentException("resolver cannot be null when the table proxy is not wrapper");
        }
        return resolver.resolveRootTable(this);
    }

    protected void __beforeJoin() {
        if (joinDisabledReason != null) {
            throw new IllegalStateException("Table join is disabled because " + joinDisabledReason);
        }
    }

    @Override
    public String toString() {
        if (raw != null) {
            return raw.toString();
        }
        if (delayedOperation != null) {
            return delayedOperation.toString();
        }
        return immutableType.toString();
    }

    protected <X> DelayedOperation<X> joinOperation(String prop) {
        return new DelayJoin<>(this, immutableType.getProp(prop), JoinType.INNER, null);
    }

    protected <X> DelayedOperation<X> joinOperation(String prop, JoinType joinType) {
        return new DelayJoin<>(this, immutableType.getProp(prop), joinType, null);
    }

    protected <X> DelayedOperation<X> joinOperation(String prop, JoinType joinType, ImmutableType treatedAs) {
        return new DelayJoin<>(this, immutableType.getProp(prop), joinType, treatedAs);
    }

    protected <X> DelayedOperation<X> joinOperation(Class<? extends WeakJoin<?, ?>> weakJoinType, JoinType joinType) {
        return new DelayJoin<>(this, weakJoinType, joinType);
    }

    public static boolean __refEquals(Table<?> a, Table<?> b) {
        if (a == b) {
            return true;
        }
        if (a instanceof AbstractTypedTable<?> && b instanceof AbstractTypedTable<?>) {
            return ((AbstractTypedTable<?>)a).identifier == ((AbstractTypedTable<?>)b).identifier;
        }
        return false;
    }

    public interface DelayedOperation<E> {

        Table<?> parent();

        ImmutableProp prop();

        WeakJoinHandle weakJoinHandle();

        ImmutableType targetType();

        TableImplementor<E> resolve(RootTableResolver ctx);
    }

    private static class DelayJoin<E> implements DelayedOperation<E> {

        private final AbstractTypedTable<?> parent;
        private final ImmutableProp prop;
        private final WeakJoinHandle weakJoinHandle;
        private final JoinType joinType;
        private final ImmutableType treatedAs;

        DelayJoin(
                AbstractTypedTable<?> parent,
                ImmutableProp prop,
                JoinType joinType,
                ImmutableType treatedAs
        ) {
            this.parent = parent;
            this.joinType = joinType;
            this.treatedAs = treatedAs;
            this.prop = prop;
            this.weakJoinHandle = null;
        }

        private DelayJoin(
                AbstractTypedTable<?> parent,
                Class<? extends WeakJoin<?, ?>> weakJoinType,
                JoinType joinType
        ) {
            this.parent = parent;
            this.joinType = joinType;
            this.treatedAs = null;
            this.prop = null;
            this.weakJoinHandle = WeakJoinHandle.of(weakJoinType);
        }

        @Override
        public Table<?> parent() {
            return parent;
        }

        @Override
        public ImmutableProp prop() {
            return prop;
        }

        @Override
        public WeakJoinHandle weakJoinHandle() {
            return weakJoinHandle;
        }

        @Override
        public ImmutableType targetType() {
            if (treatedAs != null) {
                return treatedAs;
            }
            if (prop != null) {
                return prop.getTargetType();
            }
            return weakJoinHandle.getTargetType();
        }

        @Override
        public TableImplementor<E> resolve(RootTableResolver ctx) {
            if (prop != null) {
                return parent.__resolve(ctx).joinImplementor(prop.getName(), joinType, treatedAs);
            }
            return parent.__resolve(ctx).weakJoinImplementor(weakJoinHandle, joinType);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DelayJoin<?> delayJoin = (DelayJoin<?>) o;
            return parent.equals(delayJoin.parent) && prop.equals(delayJoin.prop) && Objects.equals(weakJoinHandle, delayJoin.weakJoinHandle) && joinType == delayJoin.joinType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(parent, prop, weakJoinHandle, joinType);
        }

        @Override
        public String toString() {
            return prop.toString();
        }
    }

    private static class DelayInverseJoin<E> implements DelayedOperation<E> {

        private final AbstractTypedTable<?> parent;

        private final ImmutableProp prop;

        private final JoinType joinType;

        private DelayInverseJoin(AbstractTypedTable<?> parent, ImmutableProp prop, JoinType joinType) {
            this.parent = parent;
            this.prop = prop;
            this.joinType = joinType;
        }

        @Override
        public Table<?> parent() {
            return parent;
        }

        @Override
        public ImmutableProp prop() {
            return prop;
        }

        @Override
        public WeakJoinHandle weakJoinHandle() {
            return null;
        }

        @Override
        public ImmutableType targetType() {
            return prop.getDeclaringType();
        }

        @Override
        public TableImplementor<E> resolve(RootTableResolver ctx) {
            return parent.__resolve(ctx).inverseJoinImplementor(prop, joinType);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DelayInverseJoin<?> that = (DelayInverseJoin<?>) o;
            return parent.equals(that.parent) && prop.equals(that.prop) && joinType == that.joinType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(parent, prop, joinType);
        }

        @Override
        public String toString() {
            ImmutableProp opposite = prop.getOpposite();
            if (opposite != null) {
                return opposite.toString();
            }
            return parent + "[← " + prop + ']';
        }
    }
}
